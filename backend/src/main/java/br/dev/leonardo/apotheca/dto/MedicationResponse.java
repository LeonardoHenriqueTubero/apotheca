package br.dev.leonardo.apotheca.dto;

import java.math.BigDecimal;

import br.dev.leonardo.apotheca.entity.MedicationForm;
import br.dev.leonardo.apotheca.entity.MedicationUnit;

public record MedicationResponse(
		Long id,
		String name,
		String activeIngredient,
		String strength,
		MedicationForm form,
		MedicationUnit unit,
		Integer shelfLifeAfterOpeningDays,
		BigDecimal minimumQuantity) {
}
