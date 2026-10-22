package project.qldt.model;

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
@Table(name = "topic")
public class Topic {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(length = 1000)
	private String description;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "period_id", nullable = false)
	private RegistrationPeriod period;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "primary_advisor_id", nullable = false)
	private Lecturer primaryAdvisor;

	@Column(length = 120)
	private String department;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "topic_advisor", joinColumns = @JoinColumn(name = "topic_id"), inverseJoinColumns = @JoinColumn(name = "lecturer_id"))
	private List<Lecturer> advisors = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TopicStatus status = TopicStatus.PENDING;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TopicAvailability availability = TopicAvailability.AVAILABLE;

	@Column(name = "rejection_reason", length = 500)
	private String rejectionReason;

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "invitation_id")
	private AdvisorInvitation invitation;
}