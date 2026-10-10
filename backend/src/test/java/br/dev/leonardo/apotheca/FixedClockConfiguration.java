package br.dev.leonardo.apotheca;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class FixedClockConfiguration {

	public static final LocalDate TODAY = LocalDate.of(2026, 10, 3);

	@Bean
	@Primary
	Clock fixedClock() {
		return Clock.fixed(Instant.parse("2026-10-03T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
	}

}
