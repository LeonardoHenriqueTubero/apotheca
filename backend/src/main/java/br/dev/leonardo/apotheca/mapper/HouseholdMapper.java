package br.dev.leonardo.apotheca.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import br.dev.leonardo.apotheca.dto.HouseholdResponse;
import br.dev.leonardo.apotheca.entity.HouseholdMember;

@Mapper(componentModel = "spring")
public interface HouseholdMapper {

	@Mapping(target = "id", source = "household.id")
	@Mapping(target = "name", source = "household.name")
	HouseholdResponse toResponse(HouseholdMember membership);

}
