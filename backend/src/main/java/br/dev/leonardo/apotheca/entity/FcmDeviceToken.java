package br.dev.leonardo.apotheca.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "fcm_device_tokens")
@Getter
@Setter
@NoArgsConstructor
public class FcmDeviceToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private User user;

	private String token;

	@Enumerated(EnumType.STRING)
	private DevicePlatform platform;

	@CreationTimestamp
	private Instant createdAt;

	private Instant lastUsedAt;

}
