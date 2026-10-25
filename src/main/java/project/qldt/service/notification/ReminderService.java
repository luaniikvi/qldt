package project.qldt.service.notification;

import project.qldt.service.council.CouncilService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.report.ReportService;
import project.qldt.service.team.TeamService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.exception.InvalidNotificationException;
import project.qldt.model.account.Account;
import project.qldt.model.council.Council;
import project.qldt.model.council.CouncilMember;
import project.qldt.model.notification.NotificationType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.report.Report;
import project.qldt.model.account.Student;
import project.qldt.model.team.Team;

@Service
@Transactional
public class ReminderService {

	private final RegistrationPeriodService periodService;
	private final TeamService teamService;
	private final ReportService reportService;
	private final CouncilService councilService;
	private final NotificationService notificationService;

	public ReminderService(RegistrationPeriodService periodService, TeamService teamService,
			ReportService reportService, CouncilService councilService, NotificationService notificationService) {
		this.periodService = periodService;
		this.teamService = teamService;
		this.reportService = reportService;
		this.councilService = councilService;
		this.notificationService = notificationService;
	}

	@Transactional(readOnly = true)
	public List<RegistrationPeriod> periods() {
		return periodService.findAll();
	}

	public int remindReportDeadline(Long periodId) {
		RegistrationPeriod period = periodService.findById(periodId);
		int sent = 0;
		for (Team team : teamService.findUnreportedByPeriod(periodId)) {
			for (Student member : team.getMembers()) {
				sent += sendOnce(member, "Nhắc hạn nộp báo cáo đợt " + period.getName(),
						"Nhóm của bạn chưa nộp báo cáo. Hạn nộp: " + period.getReportDeadline(), "/teams");
			}
		}
		return sent;
	}

	public int remindCouncilDate(Long periodId) {
		RegistrationPeriod period = periodService.findById(periodId);
		if (period.getCouncilReportDate() == null) {
			throw new InvalidNotificationException("Đợt này không có ngày báo cáo hội đồng");
		}
		int sent = 0;
		for (Council council : councilService.findByPeriod(periodId)) {
			for (CouncilMember member : council.getMembers()) {
				sent += sendOnce(member.getLecturer(), "Nhắc lịch báo cáo hội đồng đợt " + period.getName(),
						"Hội đồng \"" + council.getName() + "\" báo cáo ngày " + period.getCouncilReportDate(),
						"/councils");
			}
		}
		return sent;
	}

	private int sendOnce(Account recipient, String title, String content, String link) {
		if (notificationService.exists(recipient, title)) {
			return 0;
		}
		notificationService.send(recipient, NotificationType.REMINDER, title, content, link);
		return 1;
	}
}