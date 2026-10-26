package project.qldt.model.topic;

import project.qldt.model.account.Lecturer;
import project.qldt.model.department.Department;
import project.qldt.model.period.RegistrationPeriod;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "topics")
public class Topic {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 250)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "period_id", nullable = false)
	private RegistrationPeriod period;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "primary_advisor_id", nullable = false)
	private Lecturer primaryAdvisor;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "department_id")
	private Department department;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "topic_advisors", joinColumns = @JoinColumn(name = "topic_id"), inverseJoinColumns = @JoinColumn(name = "lecturer_id"))
	private List<Lecturer> advisors = new ArrayList<>();

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "reviewer_id")
	private Lecturer reviewer;

	@Column(name = "reviewer_score")
	private Double reviewerScore;

	@Column(name = "reviewer_comment", length = 1000)
	private String reviewerComment;

	@Column(name = "reviewer_scored_at")
	private LocalDateTime reviewerScoredAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "approval_status", nullable = false, length = 20)
	private TopicStatus status = TopicStatus.PENDING;

	@Enumerated(EnumType.STRING)
	@Column(name = "registration_status", nullable = false, length = 20)
	private TopicAvailability availability = TopicAvailability.AVAILABLE;

	@Column(name = "rejection_reason", length = 500)
	private String rejectionReason;

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "co_advisor_invitation_id")
	private AdvisorInvitation invitation;
}