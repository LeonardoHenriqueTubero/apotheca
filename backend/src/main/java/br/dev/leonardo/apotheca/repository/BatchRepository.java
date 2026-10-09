package br.dev.leonardo.apotheca.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.dev.leonardo.apotheca.entity.Batch;

public interface BatchRepository extends JpaRepository<Batch, Long> {

	boolean existsByStorageLocationId(Long storageLocationId);

	@EntityGraph(attributePaths = { "medication", "storageLocation" })
	List<Batch> findByMedicationId(Long medicationId);

	@EntityGraph(attributePaths = { "medication", "storageLocation" })
	Optional<Batch> findByIdAndMedicationId(Long id, Long medicationId);

}
