package br.dev.leonardo.apotheca.dto;

import br.dev.leonardo.apotheca.entity.MemberRole;

/** A household as seen by the current user, including the user's role in it. */
public record HouseholdResponse(Long id, String name, MemberRole role) {
}
