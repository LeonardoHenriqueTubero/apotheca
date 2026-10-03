package br.dev.leonardo.apotheca.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "medications")
@Getter
@Setter
@NoArgsConstructor
public class Medication {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Household household;

	private String name;

	private String activeIngredient;

	private String strength;

	@Enumerated(EnumType.STRING)
	private MedicationForm form;

	@Enumerated(EnumType.STRING)
	private MedicationUnit unit;

	private Integer shelfLifeAfterOpeningDays;

	@Column(precision = 10, scale = 2)
	private BigDecimal minimumQuantity;

	@CreationTimestamp
	private Instant createdAt;

}
