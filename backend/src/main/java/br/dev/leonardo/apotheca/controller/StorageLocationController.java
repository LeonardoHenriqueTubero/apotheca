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

import br.dev.leonardo.apotheca.dto.StorageLocationRequest;
import br.dev.leonardo.apotheca.dto.StorageLocationResponse;
import br.dev.leonardo.apotheca.entity.User;
import br.dev.leonardo.apotheca.mapper.StorageLocationMapper;
import br.dev.leonardo.apotheca.service.StorageLocationService;
import br.dev.leonardo.apotheca.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/households/{householdId}/storage-locations")
public class StorageLocationController {

	private final StorageLocationService locationService;
	private final UserService userService;
	private final StorageLocationMapper locationMapper;

	public StorageLocationController(StorageLocationService locationService, UserService userService,
			StorageLocationMapper locationMapper) {
		this.locationService = locationService;
		this.userService = userService;
		this.locationMapper = locationMapper;
	}

	@GetMapping
	public List<StorageLocationResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable Long householdId) {
		return locationService.list(householdId, userService.getOrCreate(jwt)).stream()
				.map(locationMapper::toResponse)
				.toList();
	}

	@PostMapping
	public ResponseEntity<StorageLocationResponse> create(@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long householdId, @Valid @RequestBody StorageLocationRequest request) {
		User user = userService.getOrCreate(jwt);
		StorageLocationResponse location = locationMapper.toResponse(
				locationService.create(householdId, user, request.name()));
		URI uri = URI.create("/api/households/" + householdId + "/storage-locations/" + location.id());
		return ResponseEntity.created(uri).body(location);
	}

	@PutMapping("/{locationId}")
	public StorageLocationResponse rename(@AuthenticationPrincipal Jwt jwt, @PathVariable Long householdId,
			@PathVariable Long locationId, @Valid @RequestBody StorageLocationRequest request) {
		User user = userService.getOrCreate(jwt);
		return locationMapper.toResponse(locationService.rename(householdId, locationId, user, request.name()));
	}

	@DeleteMapping("/{locationId}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long householdId,
			@PathVariable Long locationId) {
		locationService.delete(householdId, locationId, userService.getOrCreate(jwt));
		return ResponseEntity.noContent().build();
	}

}
