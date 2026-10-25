package project.qldt.model.report;

import project.qldt.model.team.Team;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "reports")
public class Report {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "team_id", nullable = false, unique = true)
	private Team team;

	@Column(name = "original_file_name", nullable = false, length = 255)
	private String originalFileName;

	@Column(name = "stored_file_name", nullable = false, length = 255)
	private String storedFileName;

	@Column(name = "file_size", nullable = false)
	private long size;

	@Column(nullable = false)
	private int version;

	@Column(name = "submitted_at", nullable = false)
	private LocalDateTime submittedAt;
}