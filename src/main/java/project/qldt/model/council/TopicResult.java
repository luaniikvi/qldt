package project.qldt.model.council;

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
@Table(name = "topic_results")
public class TopicResult {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "council_id", nullable = false, unique = true)
	private Council council;

	@Column(name = "average_council_score", nullable = false)
	private double averageScore;

	@Column(name = "final_score")
	private Double finalScore;

	@Column(name = "is_published", nullable = false)
	private boolean published;

	@Column(name = "published_at")
	private LocalDateTime publishedAt;
}