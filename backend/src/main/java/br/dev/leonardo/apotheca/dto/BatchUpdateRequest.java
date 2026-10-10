package br.dev.leonardo.apotheca.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record BatchUpdateRequest(
		@NotNull Long storageLocationId,
		@NotNull LocalDate expirationDate,
		LocalDate openedAt) {
}
