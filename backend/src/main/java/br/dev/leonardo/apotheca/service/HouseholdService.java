package br.dev.leonardo.apotheca.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.dev.leonardo.apotheca.entity.Household;
import br.dev.leonardo.apotheca.entity.HouseholdMember;
import br.dev.leonardo.apotheca.entity.MemberRole;
import br.dev.leonardo.apotheca.entity.User;
import br.dev.leonardo.apotheca.exception.NotFoundException;
import br.dev.leonardo.apotheca.repository.HouseholdMemberRepository;
import br.dev.leonardo.apotheca.repository.HouseholdRepository;

@Service
public class HouseholdService {

	private final HouseholdRepository householdRepository;
	private final HouseholdMemberRepository memberRepository;

	public HouseholdService(HouseholdRepository householdRepository, HouseholdMemberRepository memberRepository) {
		this.householdRepository = householdRepository;
		this.memberRepository = memberRepository;
	}

	/** Creates a household; its creator becomes the owner. */
	@Transactional
	public HouseholdMember create(User creator, String name) {
		Household household = new Household();
		household.setName(name.strip());
		householdRepository.save(household);

		HouseholdMember owner = new HouseholdMember();
		owner.setHousehold(household);
		owner.setUser(creator);
		owner.setRole(MemberRole.OWNER);
		return memberRepository.save(owner);
	}

	@Transactional(readOnly = true)
	public List<HouseholdMember> listFor(User user) {
		return memberRepository.findByUserIdOrderByIdAsc(user.getId());
	}

	/**
	 * The access check every household-scoped request goes through.
	 * Throws {@link NotFoundException} when the household does not exist <em>or</em> the user is not a member.
	 */
	@Transactional(readOnly = true)
	public HouseholdMember requireMembership(Long householdId, User user) {
		return memberRepository.findByHouseholdIdAndUserId(householdId, user.getId())
				.orElseThrow(() -> new NotFoundException("Household " + householdId + " not found"));
	}

}
