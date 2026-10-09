package br.dev.leonardo.apotheca.service;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.dev.leonardo.apotheca.entity.HouseholdMember;
import br.dev.leonardo.apotheca.entity.StorageLocation;
import br.dev.leonardo.apotheca.entity.User;
import br.dev.leonardo.apotheca.exception.ConflictException;
import br.dev.leonardo.apotheca.exception.NotFoundException;
import br.dev.leonardo.apotheca.repository.BatchRepository;
import br.dev.leonardo.apotheca.repository.StorageLocationRepository;

@Service
public class StorageLocationService {

	private final StorageLocationRepository locationRepository;
	private final BatchRepository batchRepository;
	private final HouseholdService householdService;

	public StorageLocationService(StorageLocationRepository locationRepository, BatchRepository batchRepository,
			HouseholdService householdService) {
		this.locationRepository = locationRepository;
		this.batchRepository = batchRepository;
		this.householdService = householdService;
	}

	@Transactional(readOnly = true)
	public List<StorageLocation> list(Long householdId, User user) {
		householdService.requireMembership(householdId, user);
		// Sorted here, not in SQL: database collations differ (the Alpine image sorts "Z" before "a").
		Collator portuguese = Collator.getInstance(Locale.of("pt", "BR"));
		return locationRepository.findByHouseholdId(householdId).stream()
				.sorted(Comparator.comparing(StorageLocation::getName, portuguese))
				.toList();
	}

	@Transactional
	public StorageLocation create(Long householdId, User user, String name) {
		HouseholdMember membership = householdService.requireMembership(householdId, user);
		String cleanName = name.strip();
		if (locationRepository.existsByHouseholdIdAndNameIgnoreCase(householdId, cleanName)) {
			throw duplicateName(cleanName);
		}

		StorageLocation location = new StorageLocation();
		location.setHousehold(membership.getHousehold());
		location.setName(cleanName);
		return locationRepository.save(location);
	}

	@Transactional
	public StorageLocation rename(Long householdId, Long locationId, User user, String name) {
		StorageLocation location = requireLocation(householdId, locationId, user);
		String cleanName = name.strip();
		if (locationRepository.existsByHouseholdIdAndNameIgnoreCaseAndIdNot(householdId, cleanName, locationId)) {
			throw duplicateName(cleanName);
		}

		location.setName(cleanName);
		return location;
	}

	@Transactional
	public void delete(Long householdId, Long locationId, User user) {
		StorageLocation location = requireLocation(householdId, locationId, user);
		if (batchRepository.existsByStorageLocationId(locationId)) {
			throw new ConflictException("Storage location " + locationId + " still holds batches");
		}
		locationRepository.delete(location);
	}

	/** The location must belong to the household in the URL, or it answers 404 like any other household's data. */
	private StorageLocation requireLocation(Long householdId, Long locationId, User user) {
		householdService.requireMembership(householdId, user);
		return locationRepository.findByIdAndHouseholdId(locationId, householdId)
				.orElseThrow(() -> new NotFoundException("Storage location " + locationId + " not found"));
	}

	private static ConflictException duplicateName(String name) {
		return new ConflictException("A storage location named '" + name + "' already exists in this household");
	}

}
