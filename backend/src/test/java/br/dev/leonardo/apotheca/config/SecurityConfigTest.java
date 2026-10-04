package br.dev.leonardo.apotheca.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import br.dev.leonardo.apotheca.controller.HealthController;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void otherEndpointsRequireAToken() throws Exception {
		mockMvc.perform(get("/api/anything"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void allowsCorsFromTheFrontend() throws Exception {
		mockMvc.perform(options("/api/me")
						.header("Origin", "http://localhost:4200")
						.header("Access-Control-Request-Method", "GET"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
	}

	@Test
	void rejectsCorsFromOtherOrigins() throws Exception {
		mockMvc.perform(options("/api/me")
						.header("Origin", "https://evil.example.com")
						.header("Access-Control-Request-Method", "GET"))
				.andExpect(status().isForbidden());
	}

}
