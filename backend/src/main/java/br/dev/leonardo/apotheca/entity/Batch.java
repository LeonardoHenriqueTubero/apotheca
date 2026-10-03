package br.dev.leonardo.apotheca.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "batches")
@Getter
@Setter
@NoArgsConstructor
public class Batch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Medication medication;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private StorageLocation storageLocation;

	private LocalDate expirationDate;

	@Column(precision = 10, scale = 2)
	private BigDecimal currentQuantity;

	private LocalDate openedAt;

	@CreationTimestamp
	private Instant createdAt;

}
