package br.dev.leonardo.apotheca.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.dev.leonardo.apotheca.entity.Household;

public interface HouseholdRepository extends JpaRepository<Household, Long> {
}
