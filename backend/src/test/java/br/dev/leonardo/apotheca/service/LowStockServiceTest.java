package br.dev.leonardo.apotheca.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.dev.leonardo.apotheca.entity.Batch;
import br.dev.leonardo.apotheca.entity.Medication;

class LowStockServiceTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 3);
	private static final LocalDate VALID = TODAY.plusYears(1);
	private static final LocalDate EXPIRED = TODAY.minusDays(1);

	private final LowStockService service = new LowStockService(new ExpiryService(
			Clock.fixed(Instant.parse("2026-10-03T15:00:00Z"), ZoneId.of("America/Sao_Paulo"))));

	@Test
	void medicationWithoutMinimumIsNeverLow() {
		Medication medication = medication(null);

		assertThat(service.isLowStock(medication, List.of())).isFalse();
	}

	@Test
	void isLowWhenActiveQuantityIsBelowMinimum() {
		Medication medication = medication("10");

		assertThat(service.isLowStock(medication, List.of(
				batch(medication, "4", VALID),
				batch(medication, "5", VALID)))).isTrue();
	}

	@Test
	void isNotLowWhenActiveQuantityEqualsMinimum() {
		Medication medication = medication("10");

		assertThat(service.isLowStock(medication, List.of(
				batch(medication, "4", VALID),
				batch(medication, "6", VALID)))).isFalse();
	}

	@Test
	void expiredBatchesDoNotCountAsStock() {
		Medication medication = medication("10");

		assertThat(service.isLowStock(medication, List.of(
				batch(medication, "2", VALID),
				batch(medication, "30", EXPIRED)))).isTrue();
	}

	@Test
	void isLowWhenThereAreNoBatches() {
		assertThat(service.isLowStock(medication("1"), List.of())).isTrue();
	}

	@Test
	void sumsDecimalQuantities() {
		Medication medication = medication("5");

		assertThat(service.isLowStock(medication, List.of(
				batch(medication, "2.5", VALID),
				batch(medication, "2.50", VALID),
				batch(medication, "0", VALID)))).isFalse();
	}

	private static Medication medication(String minimumQuantity) {
		Medication medication = new Medication();
		medication.setMinimumQuantity(minimumQuantity == null ? null : new BigDecimal(minimumQuantity));
		return medication;
	}

	private static Batch batch(Medication medication, String quantity, LocalDate expirationDate) {
		Batch batch = new Batch();
		batch.setMedication(medication);
		batch.setCurrentQuantity(new BigDecimal(quantity));
		batch.setExpirationDate(expirationDate);
		return batch;
	}

}
