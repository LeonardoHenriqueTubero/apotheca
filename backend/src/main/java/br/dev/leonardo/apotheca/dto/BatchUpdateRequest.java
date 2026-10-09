package br.dev.leonardo.apotheca.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

public record BatchUpdateRequest(
		@NotNull Long storageLocationId,
		@NotNull LocalDate expirationDate,
		@PastOrPresent LocalDate openedAt) {
}
