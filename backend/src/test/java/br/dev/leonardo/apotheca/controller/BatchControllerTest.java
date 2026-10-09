package br.dev.leonardo.apotheca.controller;

import static br.dev.leonardo.apotheca.FixedClockConfiguration.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import br.dev.leonardo.apotheca.FixedClockConfiguration;
import br.dev.leonardo.apotheca.TestcontainersConfiguration;
import br.dev.leonardo.apotheca.entity.StockMovementType;
import br.dev.leonardo.apotheca.repository.StockMovementRepository;
import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, FixedClockConfiguration.class })
@Transactional
class BatchControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StockMovementRepository movementRepository;

	@Autowired
	private EntityManager entityManager;

	private long household;
	private long syrup;
	private long cabinet;
	private long bag;

	@BeforeEach
	void createHouseholdWithAMedicationAndTwoLocations() throws Exception {
		household = idOf(postJson("/api/households", maria(), "{\"name\": \"Casa\"}"));
		syrup = idOf(postJson(medications(household), maria(), """
				{"name": "Amoxicilina", "form": "SUSPENSION", "unit": "ML", "shelfLifeAfterOpeningDays": 14}"""));
		cabinet = idOf(postJson(locations(household), maria(), "{\"name\": \"Armário\"}"));
		bag = idOf(postJson(locations(household), maria(), "{\"name\": \"Bolsa\"}"));
	}

	@Test
	void requiresAToken() throws Exception {
		mockMvc.perform(get(batches(household, syrup)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createsABatchAndRecordsTheInitialMovement() throws Exception {
		createBatch(cabinet, "2027-10-31", "60", null)
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern(batches(household, syrup) + "/\\d+")))
				.andExpect(jsonPath("$.storageLocationId").value(cabinet))
				.andExpect(jsonPath("$.storageLocationName", is("Armário")))
				.andExpect(jsonPath("$.expirationDate", is("2027-10-31")))
				.andExpect(jsonPath("$.openedAt").doesNotExist())
				.andExpect(jsonPath("$.effectiveExpiry", is("2027-10-31")))
				.andExpect(jsonPath("$.currentQuantity").value(60))
				.andExpect(jsonPath("$.status", is("OK")));

		assertThat(movementRepository.findAll())
				.singleElement()
				.satisfies(movement -> {
					assertThat(movement.getType()).isEqualTo(StockMovementType.INITIAL);
					assertThat(movement.getQuantityChange()).isEqualByComparingTo("60");
				});
	}

	@Test
	void usesTheShelfLifeAfterOpeningForTheEffectiveExpiry() throws Exception {
		createBatch(cabinet, "2027-10-31", "60", TODAY.minusDays(10).toString())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.effectiveExpiry", is(TODAY.plusDays(4).toString())))
				.andExpect(jsonPath("$.status", is("EXPIRING_SOON")));
	}

	@Test
	void marksABatchPastItsExpiryAsExpired() throws Exception {
		createBatch(cabinet, TODAY.minusDays(1).toString(), "60", null)
				.andExpect(jsonPath("$.status", is("EXPIRED")));
	}

	@Test
	void listsBatchesByEffectiveExpirySoonestFirst() throws Exception {
		long later = idOf(createBatch(cabinet, "2028-01-31", "60", null));
		long opened = idOf(createBatch(bag, "2028-06-30", "60", TODAY.minusDays(1).toString()));
		long sooner = idOf(createBatch(bag, "2027-03-31", "60", null));
		reloadFromTheDatabase();

		mockMvc.perform(get(batches(household, syrup)).with(maria()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[*].id", contains((int) opened, (int) sooner, (int) later)))
				.andExpect(jsonPath("$[1].storageLocationName", is("Bolsa")))
				.andExpect(jsonPath("$[1].currentQuantity").value(60));
	}

	@Test
	void rejectsInvalidFieldsNamingEachOne() throws Exception {
		postJson(batches(household, syrup), maria(), """
				{"quantity": 0, "openedAt": "2999-01-01"}""", status().isBadRequest())
				.andExpect(jsonPath("$.errors.storageLocationId").exists())
				.andExpect(jsonPath("$.errors.expirationDate").exists())
				.andExpect(jsonPath("$.errors.quantity").exists())
				.andExpect(jsonPath("$.errors.openedAt").exists());
	}

	@Test
	void rejectsALocationFromAnotherHousehold() throws Exception {
		long otherHousehold = idOf(postJson("/api/households", joao(), "{\"name\": \"Casa do João\"}"));
		long joaosLocation = idOf(postJson(locations(otherHousehold), joao(), "{\"name\": \"Gaveta\"}"));

		createBatch(joaosLocation, "2027-10-31", "60", null)
				.andExpect(status().isNotFound());
	}

	@Test
	void hidesBatchesFromNonMembers() throws Exception {
		long batch = idOf(createBatch(cabinet, "2027-10-31", "60", null));

		mockMvc.perform(get(batches(household, syrup)).with(joao()))
				.andExpect(status().isNotFound());
		mockMvc.perform(get(batches(household, syrup) + "/" + batch).with(joao()))
				.andExpect(status().isNotFound());
	}

	@Test
	void doesNotReachABatchThroughAnotherMedicationsUrl() throws Exception {
		long batch = idOf(createBatch(cabinet, "2027-10-31", "60", null));
		long tablets = idOf(postJson(medications(household), maria(), """
				{"name": "Dipirona", "form": "TABLET", "unit": "UNIT"}"""));

		mockMvc.perform(get(batches(household, tablets) + "/" + batch).with(maria()))
				.andExpect(status().isNotFound());
	}

	@Test
	void updatesLocationAndDatesButNotTheQuantity() throws Exception {
		long batch = idOf(createBatch(cabinet, "2027-10-31", "60", null));

		mockMvc.perform(put(batches(household, syrup) + "/" + batch).with(maria())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"storageLocationId": %d, "expirationDate": "2027-12-31", "openedAt": "%s",
						 "quantity": 999}""".formatted(bag, TODAY)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.storageLocationName", is("Bolsa")))
				.andExpect(jsonPath("$.expirationDate", is("2027-12-31")))
				.andExpect(jsonPath("$.openedAt", is(TODAY.toString())))
				.andExpect(jsonPath("$.effectiveExpiry", is(TODAY.plusDays(14).toString())))
				.andExpect(jsonPath("$.currentQuantity").value(60));
	}

	@Test
	void deletesABatchTogetherWithItsHistory() throws Exception {
		long batch = idOf(createBatch(cabinet, "2027-10-31", "60", null));
		reloadFromTheDatabase();

		mockMvc.perform(delete(batches(household, syrup) + "/" + batch).with(maria()))
				.andExpect(status().isNoContent());
		reloadFromTheDatabase();

		mockMvc.perform(get(batches(household, syrup)).with(maria()))
				.andExpect(jsonPath("$", hasSize(0)));
		assertThat(movementRepository.count()).isZero();
	}

	@Test
	void keepsAStorageLocationThatHoldsABatch() throws Exception {
		createBatch(cabinet, "2027-10-31", "60", null);

		mockMvc.perform(delete(locations(household) + "/" + cabinet).with(maria()))
				.andExpect(status().isConflict());
	}

	private ResultActions createBatch(long locationId, String expirationDate, String quantity, String openedAt)
			throws Exception {
		String opened = openedAt == null ? "null" : "\"" + openedAt + "\"";
		return mockMvc.perform(post(batches(household, syrup)).with(maria())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"storageLocationId": %d, "expirationDate": "%s", "quantity": %s, "openedAt": %s}"""
						.formatted(locationId, expirationDate, quantity, opened)));
	}

	private ResultActions postJson(String url, RequestPostProcessor user, String json) throws Exception {
		return postJson(url, user, json, status().isCreated());
	}

	private ResultActions postJson(String url, RequestPostProcessor user, String json,
			ResultMatcher expected) throws Exception {
		return mockMvc.perform(post(url).with(user).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(expected);
	}

	private void reloadFromTheDatabase() {
		entityManager.flush();
		entityManager.clear();
	}

	private static long idOf(ResultActions result) throws Exception {
		String json = result.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(json, "$.id")).longValue();
	}

	private static String medications(long householdId) {
		return "/api/households/" + householdId + "/medications";
	}

	private static String locations(long householdId) {
		return "/api/households/" + householdId + "/storage-locations";
	}

	private static String batches(long householdId, long medicationId) {
		return medications(householdId) + "/" + medicationId + "/batches";
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
