package br.dev.leonardo.apotheca.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import br.dev.leonardo.apotheca.entity.Batch;
import br.dev.leonardo.apotheca.entity.Medication;

class ExpiryServiceTest {

	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
	private static final LocalDate TODAY = LocalDate.of(2026, 10, 3);

	private final ExpiryService service = new ExpiryService(
			Clock.fixed(Instant.parse("2026-10-03T15:00:00Z"), SAO_PAULO));

	@Test
	void closedBatchUsesPrintedExpiry() {
		Batch batch = batch(LocalDate.of(2027, 10, 31), null, 14);

		assertThat(service.effectiveExpiry(batch)).isEqualTo(LocalDate.of(2027, 10, 31));
	}

	@Test
	void openedBatchWithoutShelfLifeUsesPrintedExpiry() {
		Batch batch = batch(LocalDate.of(2027, 10, 31), LocalDate.of(2026, 10, 1), null);

		assertThat(service.effectiveExpiry(batch)).isEqualTo(LocalDate.of(2027, 10, 31));
	}

	@Test
	void openedBatchUsesShelfLifeWhenItEndsFirst() {
		Batch batch = batch(LocalDate.of(2027, 10, 31), LocalDate.of(2026, 10, 1), 14);

		assertThat(service.effectiveExpiry(batch)).isEqualTo(LocalDate.of(2026, 10, 15));
	}

	@Test
	void openedBatchUsesPrintedExpiryWhenItEndsFirst() {
		Batch batch = batch(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1), 14);

		assertThat(service.effectiveExpiry(batch)).isEqualTo(LocalDate.of(2026, 10, 10));
	}

	@Test
	void batchThatExpiredYesterdayIsExpired() {
		assertThat(service.status(batch(TODAY.minusDays(1), null, null))).isEqualTo(BatchStatus.EXPIRED);
	}

	@Test
	void batchThatExpiresTodayIsStillUsable() {
		assertThat(service.status(batch(TODAY, null, null))).isEqualTo(BatchStatus.EXPIRING_SOON);
	}

	@Test
	void batchThatExpiresInThirtyDaysIsExpiringSoon() {
		assertThat(service.status(batch(TODAY.plusDays(30), null, null))).isEqualTo(BatchStatus.EXPIRING_SOON);
	}

	@Test
	void batchThatExpiresInThirtyOneDaysIsOk() {
		assertThat(service.status(batch(TODAY.plusDays(31), null, null))).isEqualTo(BatchStatus.OK);
	}

	@Test
	void openedSyrupPastItsShelfLifeIsExpiredBeforeThePrintedDate() {
		Batch syrup = batch(LocalDate.of(2027, 10, 31), TODAY.minusDays(20), 14);

		assertThat(service.status(syrup)).isEqualTo(BatchStatus.EXPIRED);
	}

	@Test
	void emptyBatchIsEmptyEvenWhenItsDateHasPassed() {
		Batch usedUp = batch(TODAY.minusDays(100), null, null);
		usedUp.setCurrentQuantity(new BigDecimal("0.00"));

		assertThat(service.status(usedUp)).isEqualTo(BatchStatus.EMPTY);
	}

	@Test
	void usesSaoPauloDateWhenUtcIsAlreadyTheNextDay() {
		ExpiryService lateEvening = new ExpiryService(
				Clock.fixed(Instant.parse("2026-10-03T02:00:00Z"), SAO_PAULO));

		Batch batch = batch(LocalDate.of(2026, 10, 2), null, null);

		assertThat(lateEvening.status(batch)).isEqualTo(BatchStatus.EXPIRING_SOON);
	}

	private static Batch batch(LocalDate expirationDate, LocalDate openedAt, Integer shelfLifeDays) {
		Medication medication = new Medication();
		medication.setShelfLifeAfterOpeningDays(shelfLifeDays);

		Batch batch = new Batch();
		batch.setMedication(medication);
		batch.setExpirationDate(expirationDate);
		batch.setOpenedAt(openedAt);
		batch.setCurrentQuantity(BigDecimal.TEN);
		return batch;
	}

}
