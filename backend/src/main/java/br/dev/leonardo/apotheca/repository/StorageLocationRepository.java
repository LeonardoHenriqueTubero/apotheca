package br.dev.leonardo.apotheca.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.dev.leonardo.apotheca.entity.StorageLocation;

public interface StorageLocationRepository extends JpaRepository<StorageLocation, Long> {

	List<StorageLocation> findByHouseholdId(Long householdId);

	Optional<StorageLocation> findByIdAndHouseholdId(Long id, Long householdId);

	boolean existsByHouseholdIdAndNameIgnoreCase(Long householdId, String name);

	boolean existsByHouseholdIdAndNameIgnoreCaseAndIdNot(Long householdId, String name, Long id);

}
