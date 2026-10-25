package project.qldt.dto.period;

import java.time.LocalDate;
import java.time.LocalDateTime;

import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;

public record RegistrationPeriodResponse(Long id, String name, PeriodType type, LocalDateTime lecturerRegisterStart,
		LocalDateTime lecturerRegisterEnd, LocalDateTime studentRegisterStart, LocalDateTime studentRegisterEnd,
		LocalDate reportDeadline, LocalDate reviewerScoreDeadline, LocalDate councilReportDate,
		boolean registrationLocked) {

	public static RegistrationPeriodResponse of(RegistrationPeriod period) {
		return new RegistrationPeriodResponse(period.getId(), period.getName(), period.getType(),
				period.getTeacherRegisterStart(), period.getTeacherRegisterEnd(), period.getStudentRegisterStart(),
				period.getStudentRegisterEnd(), period.getReportDeadline(), period.getReviewerScoreDeadline(),
				period.getCouncilReportDate(), period.isRegistrationLocked());
	}
}
