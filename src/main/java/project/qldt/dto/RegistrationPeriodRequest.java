package project.qldt.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import project.qldt.model.PeriodType;
import project.qldt.model.RegistrationPeriod;

public class RegistrationPeriodRequest {

	@NotBlank(message = "Tên đợt đăng ký không được để trống")
	@Size(max = 150, message = "Tên đợt đăng ký tối đa 150 ký tự")
	private String name;

	@NotNull(message = "Loại đợt đăng ký không được để trống")
	private PeriodType type;

	@NotNull(message = "Thời điểm bắt đầu cho GV đăng ký không được để trống")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private LocalDateTime teacherRegisterStart;

	@NotNull(message = "Thời điểm kết thúc cho GV đăng ký không được để trống")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private LocalDateTime teacherRegisterEnd;

	@NotNull(message = "Thời điểm bắt đầu cho SV đăng ký không được để trống")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private LocalDateTime studentRegisterStart;

	@NotNull(message = "Thời điểm kết thúc cho SV đăng ký không được để trống")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private LocalDateTime studentRegisterEnd;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate reviewerScoreDeadline;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate councilReportDate;

	public static RegistrationPeriodRequest from(RegistrationPeriod period) {
		RegistrationPeriodRequest request = new RegistrationPeriodRequest();
		request.setName(period.getName());
		request.setType(period.getType());
		request.setTeacherRegisterStart(period.getTeacherRegisterStart());
		request.setTeacherRegisterEnd(period.getTeacherRegisterEnd());
		request.setStudentRegisterStart(period.getStudentRegisterStart());
		request.setStudentRegisterEnd(period.getStudentRegisterEnd());
		request.setReviewerScoreDeadline(period.getReviewerScoreDeadline());
		request.setCouncilReportDate(period.getCouncilReportDate());
		return request;
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
