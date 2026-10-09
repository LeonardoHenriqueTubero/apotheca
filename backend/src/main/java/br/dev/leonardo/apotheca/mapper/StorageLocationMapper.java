package br.dev.leonardo.apotheca.mapper;

import org.mapstruct.Mapper;

import br.dev.leonardo.apotheca.dto.StorageLocationResponse;
import br.dev.leonardo.apotheca.entity.StorageLocation;

@Mapper(componentModel = "spring")
public interface StorageLocationMapper {

	StorageLocationResponse toResponse(StorageLocation location);

}
