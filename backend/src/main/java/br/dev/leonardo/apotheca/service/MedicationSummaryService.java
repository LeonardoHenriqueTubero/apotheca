package br.dev.leonardo.apotheca.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.dev.leonardo.apotheca.entity.Batch;
import br.dev.leonardo.apotheca.entity.Medication;
import br.dev.leonardo.apotheca.entity.User;
import br.dev.leonardo.apotheca.repository.BatchRepository;

@Service
public class MedicationSummaryService {

	private final MedicationService medicationService;
	private final BatchRepository batchRepository;
	private final ExpiryService expiryService;
	private final LowStockService lowStockService;

	public MedicationSummaryService(MedicationService medicationService, BatchRepository batchRepository,
			ExpiryService expiryService, LowStockService lowStockService) {
		this.medicationService = medicationService;
		this.batchRepository = batchRepository;
		this.expiryService = expiryService;
		this.lowStockService = lowStockService;
	}

	@Transactional(readOnly = true)
	public List<MedicationSummary> list(Long householdId, User user) {
		List<Medication> medications = medicationService.list(householdId, user);
		Map<Long, List<Batch>> batchesByMedication = batchRepository.findByMedicationHouseholdId(householdId)
				.stream()
				.collect(Collectors.groupingBy(batch -> batch.getMedication().getId()));

		return medications.stream()
				.map(medication -> summarize(medication, batchesByMedication.getOrDefault(medication.getId(), List.of())))
				.toList();
	}

	private MedicationSummary summarize(Medication medication, List<Batch> batches) {
		List<BatchStatus> statuses = batches.stream().map(expiryService::status).toList();
		List<Batch> active = batches.stream().filter(this::isActive).toList();

		BigDecimal availableQuantity = active.stream()
				.map(Batch::getCurrentQuantity)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		LocalDate nextExpiry = active.stream()
				.map(expiryService::effectiveExpiry)
				.min(Comparator.naturalOrder())
				.orElse(null);

		return new MedicationSummary(medication, availableQuantity, nextExpiry, worst(statuses),
				lowStockService.isLowStock(medication, batches));
	}

	private boolean isActive(Batch batch) {
		BatchStatus status = expiryService.status(batch);
		return status != BatchStatus.EMPTY && status != BatchStatus.EXPIRED;
	}

	private static BatchStatus worst(List<BatchStatus> statuses) {
		for (BatchStatus status : List.of(BatchStatus.EXPIRED, BatchStatus.EXPIRING_SOON, BatchStatus.OK)) {
			if (statuses.contains(status)) {
				return status;
			}
		}
		return BatchStatus.EMPTY;
	}

}
