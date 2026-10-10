package br.dev.leonardo.apotheca.mapper;

import java.time.LocalDate;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import br.dev.leonardo.apotheca.dto.BatchResponse;
import br.dev.leonardo.apotheca.entity.Batch;
import br.dev.leonardo.apotheca.service.BatchStatus;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BatchMapper {

	@Mapping(target = "id", source = "batch.id")
	@Mapping(target = "storageLocationId", source = "batch.storageLocation.id")
	@Mapping(target = "storageLocationName", source = "batch.storageLocation.name")
	@Mapping(target = "expirationDate", source = "batch.expirationDate")
	@Mapping(target = "openedAt", source = "batch.openedAt")
	@Mapping(target = "currentQuantity", source = "batch.currentQuantity")
	BatchResponse toResponse(Batch batch, LocalDate effectiveExpiry, BatchStatus status);

}
