package br.dev.leonardo.apotheca.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.dev.leonardo.apotheca.entity.HouseholdMember;

public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, Long> {

	/** Loads the household in the same query, so callers can read its name without lazy loading. */
	@EntityGraph(attributePaths = "household")
	List<HouseholdMember> findByUserIdOrderByIdAsc(Long userId);

	@EntityGraph(attributePaths = "household")
	Optional<HouseholdMember> findByHouseholdIdAndUserId(Long householdId, Long userId);

}
