package br.dev.leonardo.apotheca.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.dev.leonardo.apotheca.entity.MedicationForm;
import br.dev.leonardo.apotheca.entity.MedicationUnit;
import br.dev.leonardo.apotheca.service.BatchStatus;

public record MedicationSummaryResponse(
		Long id,
		String name,
		String strength,
		MedicationForm form,
		MedicationUnit unit,
		BigDecimal availableQuantity,
		LocalDate nextExpiry,
		BatchStatus status,
		boolean lowStock) {
}
