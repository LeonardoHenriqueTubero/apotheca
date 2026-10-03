package br.dev.leonardo.apotheca.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

	@Bean
	public Clock clock(@Value("${apotheca.time-zone}") String timeZone) {
		return Clock.system(ZoneId.of(timeZone));
	}

}
