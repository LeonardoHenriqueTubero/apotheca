package br.dev.leonardo.apotheca.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.dev.leonardo.apotheca.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByFirebaseUid(String firebaseUid);

}
