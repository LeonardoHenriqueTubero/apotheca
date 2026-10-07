package br.dev.leonardo.apotheca.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.dev.leonardo.apotheca.TestcontainersConfiguration;
import br.dev.leonardo.apotheca.entity.MemberRole;
import br.dev.leonardo.apotheca.repository.HouseholdMemberRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class HouseholdControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private HouseholdMemberRepository memberRepository;

	@Test
	void requiresAToken() throws Exception {
		mockMvc.perform(post("/api/households").contentType(MediaType.APPLICATION_JSON).content(body("Casa")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createsAHouseholdWithTheCreatorAsOwner() throws Exception {
		long id = createHousehold(maria(), "  Casa da Maria  ");

		mockMvc.perform(get("/api/households/" + id).with(maria()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("Casa da Maria")))
				.andExpect(jsonPath("$.role", is("OWNER")));

		assertThat(memberRepository.findAll())
				.singleElement()
				.satisfies(member -> {
					assertThat(member.getHousehold().getId()).isEqualTo(id);
					assertThat(member.getUser().getFirebaseUid()).isEqualTo("maria-uid");
					assertThat(member.getRole()).isEqualTo(MemberRole.OWNER);
				});
	}

	@Test
	void answersCreatedWithTheNewHouseholdLocation() throws Exception {
		mockMvc.perform(post("/api/households").with(maria())
				.contentType(MediaType.APPLICATION_JSON).content(body("Casa")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern("/api/households/\\d+")));
	}

	@Test
	void rejectsABlankNameWithAProblemDetail() throws Exception {
		mockMvc.perform(post("/api/households").with(maria())
				.contentType(MediaType.APPLICATION_JSON).content(body("   ")))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status", is(400)))
				.andExpect(jsonPath("$.errors.name").exists());
	}

	@Test
	void rejectsANameLongerThan100Characters() throws Exception {
		mockMvc.perform(post("/api/households").with(maria())
				.contentType(MediaType.APPLICATION_JSON).content(body("a".repeat(101))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists());
	}

	@Test
	void listsOnlyTheHouseholdsTheUserBelongsTo() throws Exception {
		createHousehold(maria(), "Casa da Maria");
		createHousehold(maria(), "Casa da praia");
		createHousehold(joao(), "Casa do João");

		mockMvc.perform(get("/api/households").with(maria()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].name", is("Casa da Maria")))
				.andExpect(jsonPath("$[1].name", is("Casa da praia")));
	}

	@Test
	void listsNothingForANewUser() throws Exception {
		mockMvc.perform(get("/api/households").with(joao()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void hidesAHouseholdFromNonMembersAsNotFound() throws Exception {
		long id = createHousehold(maria(), "Casa da Maria");

		mockMvc.perform(get("/api/households/" + id).with(joao()))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status", is(404)))
				.andExpect(jsonPath("$.detail", is("Household " + id + " not found")));
	}

	@Test
	void answersNotFoundForAnUnknownHousehold() throws Exception {
		mockMvc.perform(get("/api/households/999999").with(maria()))
				.andExpect(status().isNotFound());
	}

	private long createHousehold(RequestPostProcessor user, String name) throws Exception {
		String response = mockMvc.perform(post("/api/households").with(user)
				.contentType(MediaType.APPLICATION_JSON).content(body(name)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(response, "$.id")).longValue();
	}

	private static String body(String name) {
		return "{\"name\": \"" + name + "\"}";
	}

	private static RequestPostProcessor maria() {
		return jwt().jwt(token -> token
				.subject("maria-uid")
				.claim("email", "maria@example.com")
				.claim("name", "Maria"));
	}

	private static RequestPostProcessor joao() {
		return jwt().jwt(token -> token
				.subject("joao-uid")
				.claim("email", "joao@example.com")
				.claim("name", "João"));
	}

}
