package br.dev.leonardo.apotheca.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import br.dev.leonardo.apotheca.TestcontainersConfiguration;
import br.dev.leonardo.apotheca.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class MeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Test
	void requiresAToken() throws Exception {
		mockMvc.perform(get("/api/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createsTheUserOnFirstAccess() throws Exception {
		mockMvc.perform(get("/api/me").with(maria()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email", is("maria@example.com")))
				.andExpect(jsonPath("$.displayName", is("Maria")))
				.andExpect(jsonPath("$.firebaseUid").doesNotExist());

		assertThat(userRepository.findByFirebaseUid("maria-uid")).isPresent();
	}

	@Test
	void returnsTheSameUserOnLaterAccesses() throws Exception {
		mockMvc.perform(get("/api/me").with(maria())).andExpect(status().isOk());
		mockMvc.perform(get("/api/me").with(maria())).andExpect(status().isOk());

		assertThat(userRepository.count()).isEqualTo(1);
	}

	private static RequestPostProcessor maria() {
		return jwt().jwt(token -> token
				.subject("maria-uid")
				.claim("email", "maria@example.com")
				.claim("name", "Maria"));
	}

}
