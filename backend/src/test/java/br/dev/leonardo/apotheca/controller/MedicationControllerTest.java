package br.dev.leonardo.apotheca.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import br.dev.leonardo.apotheca.entity.StockMovement;
import br.dev.leonardo.apotheca.entity.StockMovementType;
import br.dev.leonardo.apotheca.entity.StorageLocation;
import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class MedicationControllerTest {

	private static final String SYRUP = """
			{"name": "  Amoxicilina  ", "activeIngredient": "Amoxicilina tri-hidratada",
			 "strength": "250 mg/5 ml", "form": "SUSPENSION", "unit": "ML",
			 "shelfLifeAfterOpeningDays": 14, "minimumQuantity": 50}""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EntityManager entityManager;

	@Test
	void requiresAToken() throws Exception {
		mockMvc.perform(get("/api/households/1/medications"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createsAMedicationWithAllFields() throws Exception {
		long household = createHousehold(maria());

		mockMvc.perform(post(medications(household)).with(maria())
				.contentType(MediaType.APPLICATION_JSON).content(SYRUP))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location",
						matchesPattern("/api/households/" + household + "/medications/\\d+")))
				.andExpect(jsonPath("$.name", is("Amoxicilina")))
				.andExpect(jsonPath("$.activeIngredient", is("Amoxicilina tri-hidratada")))
				.andExpect(jsonPath("$.strength", is("250 mg/5 ml")))
				.andExpect(jsonPath("$.form", is("SUSPENSION")))
				.andExpect(jsonPath("$.unit", is("ML")))
				.andExpect(jsonPath("$.shelfLifeAfterOpeningDays", is(14)))
				.andExpect(jsonPath("$.minimumQuantity", is(50)));
	}

	@Test
	void createsAMedicationWithOnlyTheRequiredFieldsAndStoresBlankTextAsNull() throws Exception {
		long household = createHousehold(maria());

		mockMvc.perform(post(medications(household)).with(maria())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "Dipirona", "activeIngredient": "   ", "strength": "",
						 "form": "TABLET", "unit": "UNIT"}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.activeIngredient", nullValue()))
				.andExpect(jsonPath("$.strength", nullValue()))
				.andExpect(jsonPath("$.shelfLifeAfterOpeningDays", nullValue()))
				.andExpect(jsonPath("$.minimumQuantity", nullValue()));
	}

	@Test
	void rejectsInvalidFieldsNamingEachOne() throws Exception {
		long household = createHousehold(maria());

		mockMvc.perform(post(medications(household)).with(maria())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": " ", "strength": "%s",
						 "shelfLifeAfterOpeningDays": 0, "minimumQuantity": 1.555}""".formatted("x".repeat(51))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists())
				.andExpect(jsonPath("$.errors.strength").exists())
				.andExpect(jsonPath("$.errors.form").exists())
				.andExpect(jsonPath("$.errors.unit").exists())
				.andExpect(jsonPath("$.errors.shelfLifeAfterOpeningDays").exists())
				.andExpect(jsonPath("$.errors.minimumQuantity").exists());
	}

	@Test
	void rejectsAnUnknownForm() throws Exception {
		long household = createHousehold(maria());

		mockMvc.perform(post(medications(household)).with(maria())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "Dipirona", "form": "PILULA", "unit": "UNIT"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", is(400)));
	}

	@Test
	void listsMedicationsInPortugueseOrderThenByStrength() throws Exception {
		long household = createHousehold(maria());
		createMedication(household, "Paracetamol", "750 mg");
		createMedication(household, "dipirona", "500 mg");
		createMedication(household, "Ácido acetilsalicílico", null);
		createMedication(household, "dipirona", "1 g");

		mockMvc.perform(get(medications(household)).with(maria()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name",
						contains("Ácido acetilsalicílico", "dipirona", "dipirona", "Paracetamol")))
				.andExpect(jsonPath("$[1].strength", is("1 g")))
				.andExpect(jsonPath("$[2].strength", is("500 mg")));
	}

	@Test
	void getsOneMedication() throws Exception {
		long household = createHousehold(maria());
		long medication = createMedication(household, "Dipirona", "500 mg");

		mockMvc.perform(get(medications(household) + "/" + medication).with(maria()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("Dipirona")));
	}

	@Test
	void updatesEveryField() throws Exception {
		long household = createHousehold(maria());
		long medication = createMedication(household, "Amoxicilina", null);

		mockMvc.perform(put(medications(household) + "/" + medication).with(maria())
				.contentType(MediaType.APPLICATION_JSON).content(SYRUP))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.strength", is("250 mg/5 ml")))
				.andExpect(jsonPath("$.form", is("SUSPENSION")));
		reloadFromTheDatabase();

		mockMvc.perform(get(medications(household) + "/" + medication).with(maria()))
				.andExpect(jsonPath("$.shelfLifeAfterOpeningDays", is(14)))
				.andExpect(jsonPath("$.minimumQuantity", is(50.0)));
	}

	@Test
	void hidesAHouseholdsMedicationsFromNonMembers() throws Exception {
		long household = createHousehold(maria());

		mockMvc.perform(get(medications(household)).with(joao()))
				.andExpect(status().isNotFound());
	}

	@Test
	void doesNotLetAMemberReachAnotherHouseholdsMedicationThroughTheUrl() throws Exception {
		long mariasHousehold = createHousehold(maria());
		long joaosHousehold = createHousehold(joao());
		long joaosMedication = idOf(mockMvc.perform(post(medications(joaosHousehold)).with(joao())
				.contentType(MediaType.APPLICATION_JSON).content(body("Remédio do João", null)))
				.andReturn().getResponse().getContentAsString());
		String url = medications(mariasHousehold) + "/" + joaosMedication;

		mockMvc.perform(get(url).with(maria())).andExpect(status().isNotFound());
		mockMvc.perform(put(url).with(maria()).contentType(MediaType.APPLICATION_JSON).content(SYRUP))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete(url).with(maria())).andExpect(status().isNotFound());

		mockMvc.perform(get(medications(joaosHousehold)).with(joao()))
				.andExpect(jsonPath("$[*].name", contains("Remédio do João")));
	}

	@Test
	void deletesAMedicationTogetherWithItsBatchesAndHistory() throws Exception {
		long household = createHousehold(maria());
		long medication = createMedication(household, "Dipirona", "500 mg");
		storeABatchWithHistory(household, medication);

		mockMvc.perform(delete(medications(household) + "/" + medication).with(maria()))
				.andExpect(status().isNoContent());
		reloadFromTheDatabase();

		assertThat(countRows("batches")).isZero();
		assertThat(countRows("stock_movements")).isZero();
		mockMvc.perform(get(medications(household)).with(maria()))
				.andExpect(jsonPath("$", hasSize(0)));
	}

	private void storeABatchWithHistory(long householdId, long medicationId) {
		StorageLocation location = new StorageLocation();
		location.setHousehold(entityManager.getReference(Household.class, householdId));
		location.setName("Armário do banheiro");
		entityManager.persist(location);

		Batch batch = new Batch();
		batch.setMedication(entityManager.getReference(Medication.class, medicationId));
		batch.setStorageLocation(location);
		batch.setExpirationDate(LocalDate.of(2027, 10, 31));
		batch.setCurrentQuantity(new BigDecimal("10"));
		entityManager.persist(batch);

		StockMovement movement = new StockMovement();
		movement.setBatch(batch);
		movement.setType(StockMovementType.INITIAL);
		movement.setQuantityChange(new BigDecimal("10"));
		entityManager.persist(movement);
		reloadFromTheDatabase();
	}

	/**
	 * The whole test is one transaction, so changes may still wait in Hibernate's memory: flush sends
	 * them to PostgreSQL, and clear makes the next read come from the database, not from the cache.
	 */
	private void reloadFromTheDatabase() {
		entityManager.flush();
		entityManager.clear();
	}

	private long countRows(String table) {
		return ((Number) entityManager.createNativeQuery("SELECT count(*) FROM " + table).getSingleResult())
				.longValue();
	}

	private long createHousehold(RequestPostProcessor user) throws Exception {
		return idOf(mockMvc.perform(post("/api/households").with(user)
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Casa\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString());
	}

	private long createMedication(long householdId, String name, String strength) throws Exception {
		return idOf(mockMvc.perform(post(medications(householdId)).with(maria())
				.contentType(MediaType.APPLICATION_JSON).content(body(name, strength)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString());
	}

	private static String body(String name, String strength) {
		String strengthJson = strength == null ? "null" : "\"" + strength + "\"";
		return """
				{"name": "%s", "strength": %s, "form": "TABLET", "unit": "UNIT"}""".formatted(name, strengthJson);
	}

	private static long idOf(String json) {
		return ((Number) JsonPath.read(json, "$.id")).longValue();
	}

	private static String medications(long householdId) {
		return "/api/households/" + householdId + "/medications";
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
