package br.dev.leonardo.apotheca.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import br.dev.leonardo.apotheca.entity.Batch;
import br.dev.leonardo.apotheca.entity.Medication;

@Service
public class LowStockService {

	private final ExpiryService expiryService;

	public LowStockService(ExpiryService expiryService) {
		this.expiryService = expiryService;
	}

	public boolean isLowStock(Medication medication, List<Batch> batches) {
		BigDecimal minimumQuantity = medication.getMinimumQuantity();
		if (minimumQuantity == null) {
			return false;
		}

		BigDecimal activeQuantity = batches.stream()
				.filter(this::isActive)
				.map(Batch::getCurrentQuantity)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		return activeQuantity.compareTo(minimumQuantity) < 0;
	}

	private boolean isActive(Batch batch) {
		return batch.getCurrentQuantity().signum() > 0
				&& expiryService.status(batch) != BatchStatus.EXPIRED;
	}

}
