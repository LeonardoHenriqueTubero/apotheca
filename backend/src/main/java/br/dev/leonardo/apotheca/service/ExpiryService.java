package br.dev.leonardo.apotheca.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import br.dev.leonardo.apotheca.entity.Batch;

@Service
public class ExpiryService {

	static final int EXPIRING_SOON_DAYS = 30;

	private final Clock clock;

	public ExpiryService(Clock clock) {
		this.clock = clock;
	}

	public LocalDate effectiveExpiry(Batch batch) {
		LocalDate printedExpiry = batch.getExpirationDate();
		LocalDate openedAt = batch.getOpenedAt();
		Integer shelfLifeDays = batch.getMedication().getShelfLifeAfterOpeningDays();

		if (openedAt == null || shelfLifeDays == null) {
			return printedExpiry;
		}

		LocalDate afterOpening = openedAt.plusDays(shelfLifeDays);
		return afterOpening.isBefore(printedExpiry) ? afterOpening : printedExpiry;
	}

	public BatchStatus status(Batch batch) {
		LocalDate today = LocalDate.now(clock);
		LocalDate effectiveExpiry = effectiveExpiry(batch);

		if (today.isAfter(effectiveExpiry)) {
			return BatchStatus.EXPIRED;
		}
		if (!effectiveExpiry.isAfter(today.plusDays(EXPIRING_SOON_DAYS))) {
			return BatchStatus.EXPIRING_SOON;
		}
		return BatchStatus.OK;
	}

}
