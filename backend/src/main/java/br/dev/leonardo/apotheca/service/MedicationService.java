package br.dev.leonardo.apotheca.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.dev.leonardo.apotheca.dto.MedicationRequest;
import br.dev.leonardo.apotheca.entity.HouseholdMember;
import br.dev.leonardo.apotheca.entity.Medication;
import br.dev.leonardo.apotheca.entity.User;
import br.dev.leonardo.apotheca.exception.NotFoundException;
import br.dev.leonardo.apotheca.mapper.MedicationMapper;
import br.dev.leonardo.apotheca.repository.MedicationRepository;

@Service
public class MedicationService {

	private static final Comparator<Medication> ORDER = NameOrder.<Medication>by(Medication::getName)
			.thenComparing(Medication::getStrength, Comparator.nullsFirst(Comparator.naturalOrder()));

	private final MedicationRepository medicationRepository;
	private final HouseholdService householdService;
	private final MedicationMapper medicationMapper;

	public MedicationService(MedicationRepository medicationRepository, HouseholdService householdService,
			MedicationMapper medicationMapper) {
		this.medicationRepository = medicationRepository;
		this.householdService = householdService;
		this.medicationMapper = medicationMapper;
	}

	@Transactional(readOnly = true)
	public List<Medication> list(Long householdId, User user) {
		householdService.requireMembership(householdId, user);
		return medicationRepository.findByHouseholdId(householdId).stream().sorted(ORDER).toList();
	}

	@Transactional(readOnly = true)
	public Medication get(Long householdId, Long medicationId, User user) {
		return requireMedication(householdId, medicationId, user);
	}

	@Transactional
	public Medication create(Long householdId, User user, MedicationRequest request) {
		HouseholdMember membership = householdService.requireMembership(householdId, user);
		Medication medication = new Medication();
		medication.setHousehold(membership.getHousehold());
		medicationMapper.updateEntity(request, medication);
		return medicationRepository.save(medication);
	}

	@Transactional
	public Medication update(Long householdId, Long medicationId, User user, MedicationRequest request) {
		Medication medication = requireMedication(householdId, medicationId, user);
		medicationMapper.updateEntity(request, medication);
		return medication;
	}

	/** Its batches and stock movements go with it ({@code ON DELETE CASCADE}, ADR 0011). */
	@Transactional
	public void delete(Long householdId, Long medicationId, User user) {
		medicationRepository.delete(requireMedication(householdId, medicationId, user));
	}

	private Medication requireMedication(Long householdId, Long medicationId, User user) {
		householdService.requireMembership(householdId, user);
		return medicationRepository.findByIdAndHouseholdId(medicationId, householdId)
				.orElseThrow(() -> new NotFoundException("Medication " + medicationId + " not found"));
	}

}
