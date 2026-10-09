package br.dev.leonardo.apotheca.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import br.dev.leonardo.apotheca.dto.StockMovementResponse;
import br.dev.leonardo.apotheca.entity.StockMovement;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StockMovementMapper {

	StockMovementResponse toResponse(StockMovement movement);

}
