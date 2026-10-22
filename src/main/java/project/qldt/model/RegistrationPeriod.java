package project.qldt.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "registration_period")
public class RegistrationPeriod {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PeriodType type;

	@Column(name = "teacher_register_start", nullable = false)
	private LocalDateTime teacherRegisterStart;

	@Column(name = "teacher_register_end", nullable = false)
	private LocalDateTime teacherRegisterEnd;

	@Column(name = "student_register_start", nullable = false)
	private LocalDateTime studentRegisterStart;

	@Column(name = "student_register_end", nullable = false)
	private LocalDateTime studentRegisterEnd;

	@Column(name = "reviewer_score_deadline")
	private LocalDate reviewerScoreDeadline;

	@Column(name = "council_report_date")
	private LocalDate councilReportDate;

	@Column(name = "report_deadline")
	private LocalDate reportDeadline;

	@Column(name = "registration_locked", nullable = false)
	private boolean registrationLocked;

	public RegistrationPeriod() {
	}

	public RegistrationPeriod(Long id) {
		this.id = id;
	}
}