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

import br.dev.leonardo.apotheca.dto.AdjustRequest;
import br.dev.leonardo.apotheca.dto.BatchRequest;
import br.dev.leonardo.apotheca.dto.BatchResponse;
import br.dev.leonardo.apotheca.dto.BatchUpdateRequest;
import br.dev.leonardo.apotheca.dto.StockMovementResponse;
import br.dev.leonardo.apotheca.dto.UseRequest;
import br.dev.leonardo.apotheca.entity.Batch;
import br.dev.leonardo.apotheca.entity.User;
import br.dev.leonardo.apotheca.mapper.BatchMapper;
import br.dev.leonardo.apotheca.mapper.StockMovementMapper;
import br.dev.leonardo.apotheca.service.BatchService;
import br.dev.leonardo.apotheca.service.ExpiryService;
import br.dev.leonardo.apotheca.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/households/{householdId}/medications/{medicationId}/batches")
public class BatchController {

	private final BatchService batchService;
	private final ExpiryService expiryService;
	private final UserService userService;
	private final BatchMapper batchMapper;
	private final StockMovementMapper movementMapper;

	public BatchController(BatchService batchService, ExpiryService expiryService, UserService userService,
			BatchMapper batchMapper, StockMovementMapper movementMapper) {
		this.batchService = batchService;
		this.expiryService = expiryService;
		this.userService = userService;
		this.batchMapper = batchMapper;
		this.movementMapper = movementMapper;
	}

	@GetMapping
	public List<BatchResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable Long householdId,
			@PathVariable Long medicationId) {
		return batchService.list(householdId, medicationId, userService.getOrCreate(jwt)).stream()
				.map(this::toResponse)
				.toList();
	}

	@GetMapping("/{batchId}")
	public BatchResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long householdId,
			@PathVariable Long medicationId, @PathVariable Long batchId) {
		User user = userService.getOrCreate(jwt);
		return toResponse(batchService.get(householdId, medicationId, batchId, user));
	}

	@PostMapping
	public ResponseEntity<BatchResponse> create(@AuthenticationPrincipal Jwt jwt, @PathVariable Long householdId,
			@PathVariable Long medicationId, @Valid @RequestBody BatchRequest request) {
		User user = userService.getOrCreate(jwt);
		BatchResponse batch = toResponse(batchService.create(householdId, medicationId, user, request));
		URI uri = URI.create("/api/households/" + householdId + "/medications/" + medicationId
				+ "/batches/" + batch.id());
		return ResponseEntity.created(uri).body(batch);
	}

	@PutMapping("/{batchId}")
	public BatchResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long householdId,
			@PathVariable Long medicationId, @PathVariable Long batchId,
			@Valid @RequestBody BatchUpdateRequest request) {
		User user = userService.getOrCreate(jwt);
		return toResponse(batchService.update(householdId, medicationId, batchId, user, request));
	}

	@DeleteMapping("/{batchId}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long householdId,
			@PathVariable Long medicationId, @PathVariable Long batchId) {
		batchService.delete(householdId, medicationId, batchId, userService.getOrCreate(jwt));
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{batchId}/use")
	public BatchResponse use(@AuthenticationPrincipal Jwt jwt, @PathVariable Long householdId,
			@PathVariable Long medicationId, @PathVariable Long batchId, @Valid @RequestBody UseRequest request) {
		User user = userService.getOrCreate(jwt);
		return toResponse(batchService.use(householdId, medicationId, batchId, user, request.quantity()));
	}

	@PostMapping("/{batchId}/discard")
	public BatchResponse discard(@AuthenticationPrincipal Jwt jwt, @PathVariable Long householdId,
			@PathVariable Long medicationId, @PathVariable Long batchId) {
		User user = userService.getOrCreate(jwt);
		return toResponse(batchService.discard(householdId, medicationId, batchId, user));
	}

	@PostMapping("/{batchId}/adjust")
	public BatchResponse adjust(@AuthenticationPrincipal Jwt jwt, @PathVariable Long householdId,
			@PathVariable Long medicationId, @PathVariable Long batchId, @Valid @RequestBody AdjustRequest request) {
		User user = userService.getOrCreate(jwt);
		return toResponse(batchService.adjust(householdId, medicationId, batchId, user, request.quantity()));
	}

	@GetMapping("/{batchId}/movements")
	public List<StockMovementResponse> movements(@AuthenticationPrincipal Jwt jwt, @PathVariable Long householdId,
			@PathVariable Long medicationId, @PathVariable Long batchId) {
		User user = userService.getOrCreate(jwt);
		return batchService.movements(householdId, medicationId, batchId, user).stream()
				.map(movementMapper::toResponse)
				.toList();
	}

	private BatchResponse toResponse(Batch batch) {
		return batchMapper.toResponse(batch, expiryService.effectiveExpiry(batch), expiryService.status(batch));
	}

}
