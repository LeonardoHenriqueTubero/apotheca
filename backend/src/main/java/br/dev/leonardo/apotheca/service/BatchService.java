package br.dev.leonardo.apotheca.service;

import java.math.BigDecimal;
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

	public BatchService(BatchRepository batchRepository, StockMovementRepository movementRepository,
			MedicationService medicationService, StorageLocationService locationService,
			ExpiryService expiryService) {
		this.batchRepository = batchRepository;
		this.movementRepository = movementRepository;
		this.medicationService = medicationService;
		this.locationService = locationService;
		this.expiryService = expiryService;
	}

	@Transactional(readOnly = true)
	public List<Batch> list(Long householdId, Long medicationId, User user) {
		medicationService.get(householdId, medicationId, user);
		return batchRepository.findByMedicationId(medicationId).stream()
				.sorted(Comparator.comparing(expiryService::effectiveExpiry).thenComparing(Batch::getId))
				.toList();
	}

	@Transactional(readOnly = true)
	public Batch get(Long householdId, Long medicationId, Long batchId, User user) {
		return requireBatch(householdId, medicationId, batchId, user);
	}

	@Transactional
	public Batch create(Long householdId, Long medicationId, User user, BatchRequest request) {
		Medication medication = medicationService.get(householdId, medicationId, user);
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
		batch.setStorageLocation(locationService.requireLocation(householdId, request.storageLocationId(), user));
		batch.setExpirationDate(request.expirationDate());
		batch.setOpenedAt(request.openedAt());
		return batch;
	}

	@Transactional
	public void delete(Long householdId, Long medicationId, Long batchId, User user) {
		batchRepository.delete(requireBatch(householdId, medicationId, batchId, user));
	}

	private void record(Batch batch, StockMovementType type, BigDecimal quantityChange) {
		movementRepository.save(new StockMovement(batch, type, quantityChange));
	}

	private Batch requireBatch(Long householdId, Long medicationId, Long batchId, User user) {
		medicationService.get(householdId, medicationId, user);
		return batchRepository.findByIdAndMedicationId(batchId, medicationId)
				.orElseThrow(() -> new NotFoundException("Batch " + batchId + " not found"));
	}

}
