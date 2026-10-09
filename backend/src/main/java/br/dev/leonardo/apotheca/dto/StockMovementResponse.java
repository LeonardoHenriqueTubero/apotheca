package br.dev.leonardo.apotheca.dto;

import java.math.BigDecimal;
import java.time.Instant;

import br.dev.leonardo.apotheca.entity.StockMovementType;

public record StockMovementResponse(Long id, StockMovementType type, BigDecimal quantityChange, Instant occurredAt) {
}
