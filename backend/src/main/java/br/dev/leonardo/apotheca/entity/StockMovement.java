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
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
public class StockMovement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Batch batch;

	@Enumerated(EnumType.STRING)
	private StockMovementType type;

	@Column(precision = 10, scale = 2)
	private BigDecimal quantityChange;

	@CreationTimestamp
	private Instant occurredAt;

	public StockMovement(Batch batch, StockMovementType type, BigDecimal quantityChange) {
		this.batch = batch;
		this.type = type;
		this.quantityChange = quantityChange;
	}

}
