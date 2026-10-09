package br.dev.leonardo.apotheca.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import br.dev.leonardo.apotheca.entity.Batch;
import jakarta.persistence.LockModeType;

public interface BatchRepository extends JpaRepository<Batch, Long> {

	boolean existsByStorageLocationId(Long storageLocationId);

	@EntityGraph(attributePaths = { "medication", "storageLocation" })
	List<Batch> findByMedicationId(Long medicationId);

	@EntityGraph(attributePaths = { "medication", "storageLocation" })
	Optional<Batch> findByIdAndMedicationId(Long id, Long medicationId);

	@EntityGraph(attributePaths = "medication")
	List<Batch> findByMedicationHouseholdId(Long householdId);

	/** Locks the row until the transaction ends, so two phones using the same box cannot both read 10. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = { "medication", "storageLocation" })
	Optional<Batch> findForUpdateByIdAndMedicationId(Long id, Long medicationId);

}
