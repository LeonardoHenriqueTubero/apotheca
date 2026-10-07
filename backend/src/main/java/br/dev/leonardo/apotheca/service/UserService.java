package br.dev.leonardo.apotheca.service;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.dev.leonardo.apotheca.entity.User;
import br.dev.leonardo.apotheca.repository.UserRepository;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/** The user behind a validated Firebase ID token, created on first sight. */
	@Transactional
	public User getOrCreate(Jwt jwt) {
		return getOrCreate(jwt.getSubject(), jwt.getClaimAsString("email"), jwt.getClaimAsString("name"));
	}

	@Transactional
	public User getOrCreate(String firebaseUid, String email, String displayName) {
		return userRepository.findByFirebaseUid(firebaseUid)
				.orElseGet(() -> {
					User user = new User();
					user.setFirebaseUid(firebaseUid);
					user.setEmail(email);
					user.setDisplayName(displayName);
					return userRepository.save(user);
				});
	}

}
