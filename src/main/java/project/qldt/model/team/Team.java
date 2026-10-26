package project.qldt.model.team;

import project.qldt.model.account.Student;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.topic.Topic;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "teams", uniqueConstraints = @UniqueConstraint(name = "uk_teams_period_leader", columnNames = {
		"period_id", "leader_id"}))
public class Team {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "period_id", nullable = false)
	private RegistrationPeriod period;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "leader_id", nullable = false)
	private Student leader;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "team_members", joinColumns = @JoinColumn(name = "team_id"), inverseJoinColumns = @JoinColumn(name = "student_id"))
	private List<Student> members = new ArrayList<>();

	@OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	private List<TeamInvitation> invitations = new ArrayList<>();

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "topic_id", unique = true)
	private Topic topic;

	@Column(name = "registration_rejection_reason", length = 500)
	private String registrationRejectionReason;
}