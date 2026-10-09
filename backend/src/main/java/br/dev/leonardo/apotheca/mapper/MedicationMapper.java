package br.dev.leonardo.apotheca.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import br.dev.leonardo.apotheca.dto.MedicationRequest;
import br.dev.leonardo.apotheca.dto.MedicationResponse;
import br.dev.leonardo.apotheca.entity.Medication;

/** {@code unmappedTargetPolicy = ERROR}: a new entity field that nobody maps breaks the build. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MedicationMapper {

	MedicationResponse toResponse(Medication medication);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "household", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "name", qualifiedByName = "strip")
	@Mapping(target = "activeIngredient", qualifiedByName = "blankToNull")
	@Mapping(target = "strength", qualifiedByName = "blankToNull")
	void updateEntity(MedicationRequest request, @MappingTarget Medication medication);

	@Named("strip")
	default String strip(String value) {
		return value.strip();
	}

	@Named("blankToNull")
	default String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

}
