package br.dev.leonardo.apotheca.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.dev.leonardo.apotheca.service.BatchStatus;

public record BatchResponse(
		Long id,
		Long storageLocationId,
		String storageLocationName,
		LocalDate expirationDate,
		LocalDate openedAt,
		LocalDate effectiveExpiry,
		BigDecimal currentQuantity,
		BatchStatus status) {
}
