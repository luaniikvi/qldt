package project.qldt.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.exception.InvalidNotificationException;
import project.qldt.model.Account;
import project.qldt.model.Council;
import project.qldt.model.CouncilMember;
import project.qldt.model.NotificationType;
import project.qldt.model.RegistrationPeriod;
import project.qldt.model.Student;
import project.qldt.model.Team;

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
		for (Team team : teamService.findAll()) {
			if (!team.getPeriod().getId().equals(periodId) || team.getTopic() == null) {
				continue;
			}
			if (reportService.findByTeam(team) != null) {
				continue;
			}
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
		for (Council council : councilService.findAll()) {
			if (!council.getTopic().getPeriod().getId().equals(periodId)) {
				continue;
			}
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