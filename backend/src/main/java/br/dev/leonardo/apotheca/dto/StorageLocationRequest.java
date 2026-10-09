package br.dev.leonardo.apotheca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StorageLocationRequest(@NotBlank @Size(max = 100) String name) {
}
