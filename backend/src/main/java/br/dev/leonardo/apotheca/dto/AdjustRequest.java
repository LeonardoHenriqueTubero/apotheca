package br.dev.leonardo.apotheca.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** {@code quantity} is what is left in the box after counting, not the difference. */
public record AdjustRequest(@NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal quantity) {
}
