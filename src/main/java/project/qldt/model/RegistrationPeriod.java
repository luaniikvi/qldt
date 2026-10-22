package project.qldt.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RegistrationPeriod {

	private Long id;
	private String name;
	private PeriodType type;
	private LocalDateTime teacherRegisterStart;
	private LocalDateTime teacherRegisterEnd;
	private LocalDateTime studentRegisterStart;
	private LocalDateTime studentRegisterEnd;
	private LocalDate reviewerScoreDeadline;
	private LocalDate councilReportDate;

	public RegistrationPeriod() {
	}

	public RegistrationPeriod(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public PeriodType getType() {
		return type;
	}

	public void setType(PeriodType type) {
		this.type = type;
	}

	public LocalDateTime getTeacherRegisterStart() {
		return teacherRegisterStart;
	}

	public void setTeacherRegisterStart(LocalDateTime teacherRegisterStart) {
		this.teacherRegisterStart = teacherRegisterStart;
	}

	public LocalDateTime getTeacherRegisterEnd() {
		return teacherRegisterEnd;
	}

	public void setTeacherRegisterEnd(LocalDateTime teacherRegisterEnd) {
		this.teacherRegisterEnd = teacherRegisterEnd;
	}

	public LocalDateTime getStudentRegisterStart() {
		return studentRegisterStart;
	}

	public void setStudentRegisterStart(LocalDateTime studentRegisterStart) {
		this.studentRegisterStart = studentRegisterStart;
	}

	public LocalDateTime getStudentRegisterEnd() {
		return studentRegisterEnd;
	}

	public void setStudentRegisterEnd(LocalDateTime studentRegisterEnd) {
		this.studentRegisterEnd = studentRegisterEnd;
	}

	public LocalDate getReviewerScoreDeadline() {
		return reviewerScoreDeadline;
	}

	public void setReviewerScoreDeadline(LocalDate reviewerScoreDeadline) {
		this.reviewerScoreDeadline = reviewerScoreDeadline;
	}

	public LocalDate getCouncilReportDate() {
		return councilReportDate;
	}

	public void setCouncilReportDate(LocalDate councilReportDate) {
		this.councilReportDate = councilReportDate;
	}
}
