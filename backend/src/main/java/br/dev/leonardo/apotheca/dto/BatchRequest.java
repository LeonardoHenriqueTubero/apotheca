package br.dev.leonardo.apotheca.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

public record BatchRequest(
		@NotNull Long storageLocationId,
		@NotNull LocalDate expirationDate,
		@NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal quantity,
		@PastOrPresent LocalDate openedAt) {
}
