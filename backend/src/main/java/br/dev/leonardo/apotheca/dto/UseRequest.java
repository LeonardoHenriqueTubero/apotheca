package br.dev.leonardo.apotheca.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UseRequest(@NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal quantity) {
}
