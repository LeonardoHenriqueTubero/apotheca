package br.dev.leonardo.apotheca.controller;

import static br.dev.leonardo.apotheca.FixedClockConfiguration.TODAY;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.dev.leonardo.apotheca.FixedClockConfiguration;
import br.dev.leonardo.apotheca.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, FixedClockConfiguration.class })
@Transactional
class MedicationSummaryTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EntityManager entityManager;

	private long household;
	private long cabinet;

	@BeforeEach
	void createHouseholdAndLocation() throws Exception {
		household = idOf(postJson("/api/households", maria(), "{\"name\": \"Casa\"}"));
		cabinet = idOf(postJson(base() + "/storage-locations", maria(), "{\"name\": \"Armário\"}"));
	}

	@Test
	void requiresMembership() throws Exception {
		mockMvc.perform(get(summary()).with(joao()))
				.andExpect(status().isNotFound());
	}

	@Test
	void showsAMedicationWithoutBatchesAsEmpty() throws Exception {
		medication("Dipirona", null);

		mockMvc.perform(get(summary()).with(maria()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name", is("Dipirona")))
				.andExpect(jsonPath("$[0].form", is("TABLET")))
				.andExpect(jsonPath("$[0].availableQuantity").value(0))
				.andExpect(jsonPath("$[0].nextExpiry", nullValue()))
				.andExpect(jsonPath("$[0].status", is("EMPTY")))
				.andExpect(jsonPath("$[0].lowStock", is(false)));
	}

	@Test
	void countsOnlyActiveBatchesAndReportsTheWorstStatus() throws Exception {
		long dipirona = medication("Dipirona", null);
		batch(dipirona, TODAY.minusDays(1).toString(), "8");
		batch(dipirona, "2027-10-31", "20");
		batch(dipirona, TODAY.plusDays(40).toString(), "10");
		reloadFromTheDatabase();

		mockMvc.perform(get(summary()).with(maria()))
				.andExpect(jsonPath("$[0].availableQuantity").value(30))
				.andExpect(jsonPath("$[0].nextExpiry", is(TODAY.plusDays(40).toString())))
				.andExpect(jsonPath("$[0].status", is("EXPIRED")));
	}

	@Test
	void reportsExpiringSoonBeforeOk() throws Exception {
		long dipirona = medication("Dipirona", null);
		batch(dipirona, "2027-10-31", "20");
		batch(dipirona, TODAY.plusDays(30).toString(), "10");
		reloadFromTheDatabase();

		mockMvc.perform(get(summary()).with(maria()))
				.andExpect(jsonPath("$[0].status", is("EXPIRING_SOON")))
				.andExpect(jsonPath("$[0].nextExpiry", is(TODAY.plusDays(30).toString())));
	}

	@Test
	void ignoresAUsedUpBoxEvenIfItsDateHasPassed() throws Exception {
		long dipirona = medication("Dipirona", null);
		long usedUp = batch(dipirona, TODAY.minusDays(1).toString(), "8");
		mockMvc.perform(post(batches(dipirona) + "/" + usedUp + "/discard").with(maria()));
		batch(dipirona, "2027-10-31", "20");
		reloadFromTheDatabase();

		mockMvc.perform(get(summary()).with(maria()))
				.andExpect(jsonPath("$[0].status", is("OK")))
				.andExpect(jsonPath("$[0].availableQuantity").value(20));
	}

	@Test
	void flagsLowStockFromTheMinimumQuantity() throws Exception {
		long low = medication("Amoxicilina", "50");
		batch(low, "2027-10-31", "30");
		batch(low, TODAY.minusDays(1).toString(), "100");
		long enough = medication("Dipirona", "10");
		batch(enough, "2027-10-31", "10");
		reloadFromTheDatabase();

		mockMvc.perform(get(summary()).with(maria()))
				.andExpect(jsonPath("$[*].name", contains("Amoxicilina", "Dipirona")))
				.andExpect(jsonPath("$[0].lowStock", is(true)))
				.andExpect(jsonPath("$[1].lowStock", is(false)));
	}

	@Test
	void flagsAMedicationWithAMinimumAndNoBatchesAsLowStock() throws Exception {
		medication("Insulina", "1");

		mockMvc.perform(get(summary()).with(maria()))
				.andExpect(jsonPath("$[0].status", is("EMPTY")))
				.andExpect(jsonPath("$[0].lowStock", is(true)));
	}

	@Test
	void doesNotMixBatchesOfAnotherHousehold() throws Exception {
		long dipirona = medication("Dipirona", null);
		batch(dipirona, TODAY.minusDays(1).toString(), "8");

		long joaosHousehold = idOf(postJson("/api/households", joao(), "{\"name\": \"Casa do João\"}"));
		postJson("/api/households/" + joaosHousehold + "/medications", joao(), """
				{"name": "Dipirona", "form": "TABLET", "unit": "UNIT"}""");
		reloadFromTheDatabase();

		mockMvc.perform(get("/api/households/" + joaosHousehold + "/medications/summary").with(joao()))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].status", is("EMPTY")));
	}

	private long medication(String name, String minimumQuantity) throws Exception {
		return idOf(postJson(base() + "/medications", maria(), """
				{"name": "%s", "form": "TABLET", "unit": "UNIT", "minimumQuantity": %s}"""
				.formatted(name, minimumQuantity)));
	}

	private long batch(long medication, String expirationDate, String quantity) throws Exception {
		return idOf(postJson(batches(medication), maria(), """
				{"storageLocationId": %d, "expirationDate": "%s", "quantity": %s}"""
				.formatted(cabinet, expirationDate, quantity)));
	}

	private ResultActions postJson(String url, RequestPostProcessor user, String json) throws Exception {
		return mockMvc.perform(post(url).with(user).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated());
	}

	private void reloadFromTheDatabase() {
		entityManager.flush();
		entityManager.clear();
	}

	private String base() {
		return "/api/households/" + household;
	}

	private String summary() {
		return base() + "/medications/summary";
	}

	private String batches(long medication) {
		return base() + "/medications/" + medication + "/batches";
	}

	private static long idOf(ResultActions result) throws Exception {
		String json = result.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(json, "$.id")).longValue();
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
