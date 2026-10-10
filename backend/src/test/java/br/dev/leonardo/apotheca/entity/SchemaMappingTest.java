package br.dev.leonardo.apotheca.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import br.dev.leonardo.apotheca.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class SchemaMappingTest {

	@Autowired
	private Flyway flyway;

	@Autowired
	private EntityManager entityManager;

	@Test
	void appliesMigrationsUpToV1() {
		assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
	}

	@Test
	void persistsAndReloadsABatchWithItsMovement() {
		Household household = persistHousehold();
		StorageLocation location = persistLocation(household, "Bathroom cabinet");
		Medication medication = persistMedication(household, MedicationForm.SUSPENSION, MedicationUnit.ML);
		Batch batch = persistBatch(medication, location, new BigDecimal("60.00"));
		persistMovement(batch, StockMovementType.INITIAL, new BigDecimal("60.00"));
		entityManager.flush();
		entityManager.clear();

		Batch reloaded = entityManager.find(Batch.class, batch.getId());

		assertThat(reloaded.getExpirationDate()).isEqualTo(LocalDate.of(2027, 10, 31));
		assertThat(reloaded.getCurrentQuantity()).isEqualByComparingTo("60");
		assertThat(reloaded.getCreatedAt()).isNotNull();
		assertThat(reloaded.getMedication().getForm()).isEqualTo(MedicationForm.SUSPENSION);
		assertThat(reloaded.getStorageLocation().getName()).isEqualTo("Bathroom cabinet");
	}

	@Test
	void acceptsEveryMedicationFormAndUnit() {
		Household household = persistHousehold();
		for (MedicationForm form : MedicationForm.values()) {
			for (MedicationUnit unit : MedicationUnit.values()) {
				persistMedication(household, form, unit);
			}
		}

		entityManager.flush();
	}

	@Test
	void savesHouseholdMembershipAndInvite() {
		Household household = persistHousehold();
		User owner = persistUser("owner-uid");

		HouseholdMember member = new HouseholdMember();
		member.setHousehold(household);
		member.setUser(owner);
		member.setRole(MemberRole.OWNER);
		entityManager.persist(member);

		HouseholdInvite invite = new HouseholdInvite();
		invite.setHousehold(household);
		invite.setToken("random-token");
		invite.setCreatedBy(owner);
		invite.setExpiresAt(Instant.now().plusSeconds(24 * 60 * 60));
		entityManager.persist(invite);

		NotificationPreference preference = new NotificationPreference();
		preference.setUser(owner);
		preference.setHousehold(household);
		entityManager.persist(preference);

		entityManager.flush();

		assertThat(member.getJoinedAt()).isNotNull();
		assertThat(preference.getFrequency()).isEqualTo(AlertFrequency.DAILY);
	}

	@Test
	void rejectsNegativeQuantity() {
		Household household = persistHousehold();
		StorageLocation location = persistLocation(household, "Bag");
		Medication medication = persistMedication(household, MedicationForm.TABLET, MedicationUnit.UNIT);

		assertThatThrownBy(() -> persistBatch(medication, location, new BigDecimal("-1")))
				.isInstanceOf(PersistenceException.class);
	}

	@Test
	void doesNotDeleteALocationThatHoldsBatches() {
		Household household = persistHousehold();
		StorageLocation location = persistLocation(household, "Kitchen drawer");
		Medication medication = persistMedication(household, MedicationForm.TABLET, MedicationUnit.UNIT);
		persistBatch(medication, location, new BigDecimal("20"));
		entityManager.flush();

		assertThatThrownBy(() -> entityManager
				.createNativeQuery("DELETE FROM storage_locations WHERE id = :id")
				.setParameter("id", location.getId())
				.executeUpdate())
				.isInstanceOf(PersistenceException.class);
	}

	@Test
	void deletingAMedicationDeletesItsBatchesAndMovements() {
		Household household = persistHousehold();
		StorageLocation location = persistLocation(household, "Bedroom");
		Medication medication = persistMedication(household, MedicationForm.DROPS, MedicationUnit.ML);
		Batch batch = persistBatch(medication, location, new BigDecimal("20"));
		persistMovement(batch, StockMovementType.INITIAL, new BigDecimal("20"));
		entityManager.flush();

		entityManager.createNativeQuery("DELETE FROM medications WHERE id = :id")
				.setParameter("id", medication.getId())
				.executeUpdate();
		entityManager.clear();

		assertThat(entityManager.find(Batch.class, batch.getId())).isNull();
		assertThat(countRows("stock_movements")).isZero();
	}

	private Household persistHousehold() {
		Household household = new Household();
		household.setName("Family");
		entityManager.persist(household);
		return household;
	}

	private User persistUser(String firebaseUid) {
		User user = new User();
		user.setFirebaseUid(firebaseUid);
		user.setEmail(firebaseUid + "@example.com");
		entityManager.persist(user);
		return user;
	}

	private StorageLocation persistLocation(Household household, String name) {
		StorageLocation location = new StorageLocation();
		location.setHousehold(household);
		location.setName(name);
		entityManager.persist(location);
		return location;
	}

	private Medication persistMedication(Household household, MedicationForm form, MedicationUnit unit) {
		Medication medication = new Medication();
		medication.setHousehold(household);
		medication.setName("Amoxicillin");
		medication.setForm(form);
		medication.setUnit(unit);
		medication.setShelfLifeAfterOpeningDays(14);
		entityManager.persist(medication);
		return medication;
	}

	private Batch persistBatch(Medication medication, StorageLocation location, BigDecimal quantity) {
		Batch batch = new Batch();
		batch.setMedication(medication);
		batch.setStorageLocation(location);
		batch.setExpirationDate(LocalDate.of(2027, 10, 31));
		batch.setCurrentQuantity(quantity);
		entityManager.persist(batch);
		return batch;
	}

	private void persistMovement(Batch batch, StockMovementType type, BigDecimal quantityChange) {
		entityManager.persist(new StockMovement(batch, type, quantityChange));
	}

	private long countRows(String table) {
		return ((Number) entityManager.createNativeQuery("SELECT count(*) FROM " + table).getSingleResult())
				.longValue();
	}

}
