package br.dev.leonardo.apotheca.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.dev.leonardo.apotheca.dto.MedicationRequest;
import br.dev.leonardo.apotheca.dto.MedicationResponse;
import br.dev.leonardo.apotheca.entity.User;
import br.dev.leonardo.apotheca.mapper.MedicationMapper;
import br.dev.leonardo.apotheca.service.MedicationService;
import br.dev.leonardo.apotheca.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/households/{householdId}/medications")
public class MedicationController {

	private final MedicationService medicationService;
	private final UserService userService;
	private final MedicationMapper medicationMapper;

	public MedicationController(MedicationService medicationService, UserService userService,
			MedicationMapper medicationMapper) {
		this.medicationService = medicationService;
		this.userService = userService;
		this.medicationMapper = medicationMapper;
	}

	@GetMapping
	public List<MedicationResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable Long householdId) {
		return medicationService.list(householdId, userService.getOrCreate(jwt)).stream()
				.map(medicationMapper::toResponse)
				.toList();
	}

	@GetMapping("/{medicationId}")
	public MedicationResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long householdId,
			@PathVariable Long medicationId) {
		User user = userService.getOrCreate(jwt);
		return medicationMapper.toResponse(medicationService.get(householdId, medicationId, user));
	}

	@PostMapping
	public ResponseEntity<MedicationResponse> create(@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long householdId, @Valid @RequestBody MedicationRequest request) {
		User user = userService.getOrCreate(jwt);
		MedicationResponse medication = medicationMapper.toResponse(
				medicationService.create(householdId, user, request));
		URI uri = URI.create("/api/households/" + householdId + "/medications/" + medication.id());
		return ResponseEntity.created(uri).body(medication);
	}

	@PutMapping("/{medicationId}")
	public MedicationResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long householdId,
			@PathVariable Long medicationId, @Valid @RequestBody MedicationRequest request) {
		User user = userService.getOrCreate(jwt);
		return medicationMapper.toResponse(medicationService.update(householdId, medicationId, user, request));
	}

	@DeleteMapping("/{medicationId}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long householdId,
			@PathVariable Long medicationId) {
		medicationService.delete(householdId, medicationId, userService.getOrCreate(jwt));
		return ResponseEntity.noContent().build();
	}

}
