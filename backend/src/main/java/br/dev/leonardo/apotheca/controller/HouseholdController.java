package br.dev.leonardo.apotheca.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.dev.leonardo.apotheca.dto.CreateHouseholdRequest;
import br.dev.leonardo.apotheca.dto.HouseholdResponse;
import br.dev.leonardo.apotheca.entity.User;
import br.dev.leonardo.apotheca.mapper.HouseholdMapper;
import br.dev.leonardo.apotheca.service.HouseholdService;
import br.dev.leonardo.apotheca.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/households")
public class HouseholdController {

	private final HouseholdService householdService;
	private final UserService userService;
	private final HouseholdMapper householdMapper;

	public HouseholdController(HouseholdService householdService, UserService userService,
			HouseholdMapper householdMapper) {
		this.householdService = householdService;
		this.userService = userService;
		this.householdMapper = householdMapper;
	}

	@PostMapping
	public ResponseEntity<HouseholdResponse> create(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateHouseholdRequest request) {
		User user = userService.getOrCreate(jwt);
		HouseholdResponse household = householdMapper.toResponse(householdService.create(user, request.name()));
		return ResponseEntity.created(URI.create("/api/households/" + household.id())).body(household);
	}

	@GetMapping
	public List<HouseholdResponse> list(@AuthenticationPrincipal Jwt jwt) {
		return householdService.listFor(userService.getOrCreate(jwt)).stream()
				.map(householdMapper::toResponse)
				.toList();
	}

	@GetMapping("/{householdId}")
	public HouseholdResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long householdId) {
		User user = userService.getOrCreate(jwt);
		return householdMapper.toResponse(householdService.requireMembership(householdId, user));
	}

}
