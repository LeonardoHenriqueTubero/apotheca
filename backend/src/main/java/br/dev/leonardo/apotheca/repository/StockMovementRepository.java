package br.dev.leonardo.apotheca.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.dev.leonardo.apotheca.entity.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
}
