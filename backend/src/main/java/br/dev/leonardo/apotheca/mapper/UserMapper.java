package br.dev.leonardo.apotheca.mapper;

import org.mapstruct.Mapper;

import br.dev.leonardo.apotheca.dto.UserResponse;
import br.dev.leonardo.apotheca.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

	UserResponse toResponse(User user);

}
