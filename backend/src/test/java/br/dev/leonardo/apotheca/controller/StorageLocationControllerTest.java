package br.dev.leonardo.apotheca.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

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
import br.dev.leonardo.apotheca.entity.Batch;
import br.dev.leonardo.apotheca.entity.Household;
import br.dev.leonardo.apotheca.entity.Medication;
import br.dev.leonardo.apotheca.entity.MedicationForm;
import br.dev.leonardo.apotheca.entity.MedicationUnit;
import br.dev.leonardo.apotheca.entity.StorageLocation;
import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class StorageLocationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EntityManager entityManager;

	@Test
	void requiresAToken() throws Exception {
		mockMvc.perform(get("/api/households/1/storage-locations"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void listsNothingForANewHousehold() throws Exception {
		long household = createHousehold(maria());

		mockMvc.perform(get(locations(household)).with(maria()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void createsALocationAndAnswersWithItsAddress() throws Exception {
		long household = createHousehold(maria());

		mockMvc.perform(post(locations(household)).with(maria())
				.contentType(MediaType.APPLICATION_JSON).content(body("  Armário do banheiro  ")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location",
						matchesPattern("/api/households/" + household + "/storage-locations/\\d+")))
				.andExpect(jsonPath("$.name", is("Armário do banheiro")));
	}

	@Test
	void listsLocationsInPortugueseAlphabeticalOrder() throws Exception {
		long household = createHousehold(maria());
		createLocation(maria(), household, "Gaveta da cozinha");
		createLocation(maria(), household, "bolsa");
		createLocation(maria(), household, "Água");

		mockMvc.perform(get(locations(household)).with(maria()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", contains("Água", "bolsa", "Gaveta da cozinha")));
	}

	@Test
	void rejectsABlankName() throws Exception {
		long household = createHousehold(maria());

		mockMvc.perform(post(locations(household)).with(maria())
				.contentType(MediaType.APPLICATION_JSON).content(body("   ")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists());
	}

	@Test
	void rejectsANameLongerThan100Characters() throws Exception {
		long household = createHousehold(maria());

		mockMvc.perform(post(locations(household)).with(maria())
				.contentType(MediaType.APPLICATION_JSON).content(body("a".repeat(101))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists());
	}

	@Test
	void rejectsADuplicateNameIgnoringCase() throws Exception {
		long household = createHousehold(maria());
		createLocation(maria(), household, "Bolsa");

		mockMvc.perform(post(locations(household)).with(maria())
				.contentType(MediaType.APPLICATION_JSON).content(body("bolsa")))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status", is(409)));
	}

	@Test
	void hidesTheLocationsOfAHouseholdFromNonMembers() throws Exception {
		long household = createHousehold(maria());

		mockMvc.perform(get(locations(household)).with(joao()))
				.andExpect(status().isNotFound());
	}

	@Test
	void doesNotLetAMemberChangeAnotherHouseholdsLocationThroughTheUrl() throws Exception {
		long mariasHousehold = createHousehold(maria());
		long joaosHousehold = createHousehold(joao());
		long joaosLocation = createLocation(joao(), joaosHousehold, "Bolsa do João");

		mockMvc.perform(put(locations(mariasHousehold) + "/" + joaosLocation).with(maria())
				.contentType(MediaType.APPLICATION_JSON).content(body("Minha bolsa")))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete(locations(mariasHousehold) + "/" + joaosLocation).with(maria()))
				.andExpect(status().isNotFound());

		mockMvc.perform(get(locations(joaosHousehold)).with(joao()))
				.andExpect(jsonPath("$[*].name", contains("Bolsa do João")));
	}

	@Test
	void renamesALocation() throws Exception {
		long household = createHousehold(maria());
		long location = createLocation(maria(), household, "Banheiro");

		mockMvc.perform(put(locations(household) + "/" + location).with(maria())
				.contentType(MediaType.APPLICATION_JSON).content(body("Armário do banheiro")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("Armário do banheiro")));
	}

	@Test
	void letsALocationChangeOnlyTheCaseOfItsOwnName() throws Exception {
		long household = createHousehold(maria());
		long location = createLocation(maria(), household, "banheiro");

		mockMvc.perform(put(locations(household) + "/" + location).with(maria())
				.contentType(MediaType.APPLICATION_JSON).content(body("Banheiro")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("Banheiro")));
	}

	@Test
	void rejectsRenamingToAnotherLocationsName() throws Exception {
		long household = createHousehold(maria());
		createLocation(maria(), household, "Cozinha");
		long location = createLocation(maria(), household, "Banheiro");

		mockMvc.perform(put(locations(household) + "/" + location).with(maria())
				.contentType(MediaType.APPLICATION_JSON).content(body("cozinha")))
				.andExpect(status().isConflict());
	}

	@Test
	void deletesAnEmptyLocation() throws Exception {
		long household = createHousehold(maria());
		long location = createLocation(maria(), household, "Bolsa");

		mockMvc.perform(delete(locations(household) + "/" + location).with(maria()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get(locations(household)).with(maria()))
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void refusesToDeleteALocationThatStillHoldsBatches() throws Exception {
		long household = createHousehold(maria());
		long location = createLocation(maria(), household, "Armário do banheiro");
		storeABatchIn(household, location);

		mockMvc.perform(delete(locations(household) + "/" + location).with(maria()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", is("Storage location " + location + " still holds batches")));
	}

	private void storeABatchIn(long householdId, long locationId) {
		Medication medication = new Medication();
		medication.setHousehold(entityManager.getReference(Household.class, householdId));
		medication.setName("Dipirona");
		medication.setForm(MedicationForm.TABLET);
		medication.setUnit(MedicationUnit.UNIT);
		entityManager.persist(medication);

		Batch batch = new Batch();
		batch.setMedication(medication);
		batch.setStorageLocation(entityManager.getReference(StorageLocation.class, locationId));
		batch.setExpirationDate(LocalDate.of(2027, 10, 31));
		batch.setCurrentQuantity(new BigDecimal("10"));
		entityManager.persist(batch);
		entityManager.flush();
	}

	private long createHousehold(RequestPostProcessor user) throws Exception {
		return idOf(mockMvc.perform(post("/api/households").with(user)
				.contentType(MediaType.APPLICATION_JSON).content(body("Casa")))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString());
	}

	private long createLocation(RequestPostProcessor user, long householdId, String name) throws Exception {
		return idOf(mockMvc.perform(post(locations(householdId)).with(user)
				.contentType(MediaType.APPLICATION_JSON).content(body(name)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString());
	}

	private static long idOf(String json) {
		return ((Number) JsonPath.read(json, "$.id")).longValue();
	}

	private static String locations(long householdId) {
		return "/api/households/" + householdId + "/storage-locations";
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
