package project.qldt.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "council_member")
public class CouncilMember {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "council_id", nullable = false)
	private Council council;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "lecturer_id", nullable = false)
	private Lecturer lecturer;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CouncilRole role;

	private Double score;

	@Column(length = 500)
	private String comment;

	@Column(name = "scored_at")
	private LocalDateTime scoredAt;
}