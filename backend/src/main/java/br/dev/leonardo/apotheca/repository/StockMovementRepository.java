package br.dev.leonardo.apotheca.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.dev.leonardo.apotheca.entity.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

	List<StockMovement> findByBatchIdOrderByOccurredAtDescIdDesc(Long batchId);

}
