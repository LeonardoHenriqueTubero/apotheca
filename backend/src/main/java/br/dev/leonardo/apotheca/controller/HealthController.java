package br.dev.leonardo.apotheca.controller;

import br.dev.leonardo.apotheca.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

	@GetMapping("/health")
	public HealthResponse health() {
		return new HealthResponse("ok");
	}

}
