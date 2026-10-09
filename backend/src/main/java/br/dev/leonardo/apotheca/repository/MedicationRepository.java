package br.dev.leonardo.apotheca.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.dev.leonardo.apotheca.entity.Medication;

public interface MedicationRepository extends JpaRepository<Medication, Long> {

	List<Medication> findByHouseholdId(Long householdId);

	Optional<Medication> findByIdAndHouseholdId(Long id, Long householdId);

}
