package br.dev.leonardo.apotheca.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.dev.leonardo.apotheca.entity.Batch;

public interface BatchRepository extends JpaRepository<Batch, Long> {

	boolean existsByStorageLocationId(Long storageLocationId);

}
