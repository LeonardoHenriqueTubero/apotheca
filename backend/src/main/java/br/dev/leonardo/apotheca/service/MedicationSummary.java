package br.dev.leonardo.apotheca.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.dev.leonardo.apotheca.entity.Medication;

public record MedicationSummary(
		Medication medication,
		BigDecimal availableQuantity,
		LocalDate nextExpiry,
		BatchStatus status,
		boolean lowStock) {
}
