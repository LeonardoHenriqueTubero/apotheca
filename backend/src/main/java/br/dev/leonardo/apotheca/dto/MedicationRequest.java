package br.dev.leonardo.apotheca.dto;

import java.math.BigDecimal;

import br.dev.leonardo.apotheca.entity.MedicationForm;
import br.dev.leonardo.apotheca.entity.MedicationUnit;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MedicationRequest(
		@NotBlank @Size(max = 150) String name,
		@Size(max = 150) String activeIngredient,
		@Size(max = 50) String strength,
		@NotNull MedicationForm form,
		@NotNull MedicationUnit unit,
		@Positive Integer shelfLifeAfterOpeningDays,
		@Positive @Digits(integer = 8, fraction = 2) BigDecimal minimumQuantity) {
}
