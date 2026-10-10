package br.dev.leonardo.apotheca.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.dev.leonardo.apotheca.dto.BatchRequest;
import br.dev.leonardo.apotheca.dto.BatchUpdateRequest;
import br.dev.leonardo.apotheca.entity.Batch;
import br.dev.leonardo.apotheca.entity.Medication;
import br.dev.leonardo.apotheca.entity.StockMovement;
import br.dev.leonardo.apotheca.entity.StockMovementType;
import br.dev.leonardo.apotheca.entity.User;
import br.dev.leonardo.apotheca.exception.ConflictException;
import br.dev.leonardo.apotheca.exception.InvalidFieldException;
import br.dev.leonardo.apotheca.exception.NotFoundException;
import br.dev.leonardo.apotheca.repository.BatchRepository;
import br.dev.leonardo.apotheca.repository.StockMovementRepository;

@Service
public class BatchService {

	private final BatchRepository batchRepository;
	private final StockMovementRepository movementRepository;
	private final MedicationService medicationService;
	private final StorageLocationService locationService;
	private final ExpiryService expiryService;
	private final Clock clock;

	public BatchService(BatchRepository batchRepository, StockMovementRepository movementRepository,
			MedicationService medicationService, StorageLocationService locationService,
			ExpiryService expiryService, Clock clock) {
		this.batchRepository = batchRepository;
		this.movementRepository = movementRepository;
		this.medicationService = medicationService;
		this.locationService = locationService;
		this.expiryService = expiryService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<Batch> list(Long householdId, Long medicationId, User user) {
		medicationService.get(householdId, medicationId, user);
		return batchRepository.findByMedicationId(medicationId).stream()
				.sorted(Comparator.comparing((Batch batch) -> batch.getCurrentQuantity().signum() == 0)
						.thenComparing(expiryService::effectiveExpiry)
						.thenComparing(Batch::getId))
				.toList();
	}

	@Transactional(readOnly = true)
	public Batch get(Long householdId, Long medicationId, Long batchId, User user) {
		return requireBatch(householdId, medicationId, batchId, user);
	}

	@Transactional
	public Batch create(Long householdId, Long medicationId, User user, BatchRequest request) {
		Medication medication = medicationService.get(householdId, medicationId, user);
		requireNotInTheFuture(request.openedAt());
		Batch batch = new Batch();
		batch.setMedication(medication);
		batch.setStorageLocation(locationService.requireLocation(householdId, request.storageLocationId(), user));
		batch.setExpirationDate(request.expirationDate());
		batch.setOpenedAt(request.openedAt());
		batch.setCurrentQuantity(request.quantity());
		batchRepository.save(batch);
		record(batch, StockMovementType.INITIAL, request.quantity());
		return batch;
	}

	@Transactional
	public Batch update(Long householdId, Long medicationId, Long batchId, User user, BatchUpdateRequest request) {
		Batch batch = requireBatch(householdId, medicationId, batchId, user);
		requireNotInTheFuture(request.openedAt());
		batch.setStorageLocation(locationService.requireLocation(householdId, request.storageLocationId(), user));
		batch.setExpirationDate(request.expirationDate());
		batch.setOpenedAt(request.openedAt());
		return batch;
	}

	@Transactional
	public void delete(Long householdId, Long medicationId, Long batchId, User user) {
		batchRepository.delete(requireBatch(householdId, medicationId, batchId, user));
	}

	/** The first use of a closed box opens it today, which starts its shelf life after opening. */
	@Transactional
	public Batch use(Long householdId, Long medicationId, Long batchId, User user, BigDecimal quantity) {
		Batch batch = lockBatch(householdId, medicationId, batchId, user);
		if (quantity.compareTo(batch.getCurrentQuantity()) > 0) {
			throw new ConflictException("Cannot use " + quantity.toPlainString() + " from batch " + batchId
					+ ", it has " + batch.getCurrentQuantity().toPlainString());
		}
		if (batch.getOpenedAt() == null) {
			batch.setOpenedAt(today());
		}
		change(batch, StockMovementType.USE, quantity.negate());
		return batch;
	}

	@Transactional
	public Batch discard(Long householdId, Long medicationId, Long batchId, User user) {
		Batch batch = lockBatch(householdId, medicationId, batchId, user);
		if (batch.getCurrentQuantity().signum() == 0) {
			throw new ConflictException("Batch " + batchId + " is already empty");
		}
		change(batch, StockMovementType.DISCARD, batch.getCurrentQuantity().negate());
		return batch;
	}

	@Transactional
	public Batch adjust(Long householdId, Long medicationId, Long batchId, User user, BigDecimal countedQuantity) {
		Batch batch = lockBatch(householdId, medicationId, batchId, user);
		BigDecimal difference = countedQuantity.subtract(batch.getCurrentQuantity());
		if (difference.signum() != 0) {
			change(batch, StockMovementType.ADJUSTMENT, difference);
		}
		return batch;
	}

	@Transactional(readOnly = true)
	public List<StockMovement> movements(Long householdId, Long medicationId, Long batchId, User user) {
		requireBatch(householdId, medicationId, batchId, user);
		return movementRepository.findByBatchIdOrderByOccurredAtDescIdDesc(batchId);
	}

	private void change(Batch batch, StockMovementType type, BigDecimal quantityChange) {
		batch.setCurrentQuantity(batch.getCurrentQuantity().add(quantityChange));
		record(batch, type, quantityChange);
	}

	private void record(Batch batch, StockMovementType type, BigDecimal quantityChange) {
		movementRepository.save(new StockMovement(batch, type, quantityChange));
	}

	private void requireNotInTheFuture(LocalDate openedAt) {
		if (openedAt != null && openedAt.isAfter(today())) {
			throw new InvalidFieldException("openedAt", "must not be in the future");
		}
	}

	private LocalDate today() {
		return LocalDate.now(clock);
	}

	private Batch requireBatch(Long householdId, Long medicationId, Long batchId, User user) {
		medicationService.get(householdId, medicationId, user);
		return batchRepository.findByIdAndMedicationId(batchId, medicationId)
				.orElseThrow(() -> notFound(batchId));
	}

	private Batch lockBatch(Long householdId, Long medicationId, Long batchId, User user) {
		medicationService.get(householdId, medicationId, user);
		return batchRepository.findForUpdateByIdAndMedicationId(batchId, medicationId)
				.orElseThrow(() -> notFound(batchId));
	}

	private static NotFoundException notFound(Long batchId) {
		return new NotFoundException("Batch " + batchId + " not found");
	}

}
