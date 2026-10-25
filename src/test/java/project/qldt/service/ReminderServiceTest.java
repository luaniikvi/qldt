package project.qldt.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.dto.topic.TopicRequest;
import project.qldt.exception.InvalidNotificationException;
import project.qldt.model.account.Lecturer;
import project.qldt.model.account.Student;
import project.qldt.model.council.Council;
import project.qldt.model.council.CouncilRole;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;
import project.qldt.service.account.AccountService;
import project.qldt.service.council.CouncilService;
import project.qldt.service.notification.NotificationService;
import project.qldt.service.notification.ReminderService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.report.ReportService;
import project.qldt.service.team.TeamService;
import project.qldt.service.topic.TopicRegistrationService;
import project.qldt.service.topic.TopicService;
import project.qldt.support.Beans;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReminderServiceTest {

	private ReminderService reminderService;
	private RegistrationPeriodService periodService;
	private AccountService accountService;
	private TopicService topicService;
	private TeamService teamService;
	private TopicRegistrationService registrationService;
	private ReportService reportService;
	private CouncilService councilService;
	private NotificationService notificationService;

	private RegistrationPeriod period;
	private Lecturer gv1;
	private Lecturer gv2;
	private Lecturer gv3;
	private Lecturer gv4;
	private Student sv1;
	private Student sv2;
	private Student sv3;
	private Student sv4;

	@BeforeEach
	void setUp() {
		reminderService = Beans.get(ReminderService.class);
		periodService = Beans.get(RegistrationPeriodService.class);
		accountService = Beans.get(AccountService.class);
		topicService = Beans.get(TopicService.class);
		teamService = Beans.get(TeamService.class);
		registrationService = Beans.get(TopicRegistrationService.class);
		reportService = Beans.get(ReportService.class);
		councilService = Beans.get(CouncilService.class);
		notificationService = Beans.get(NotificationService.class);

		String stamp = String.valueOf(System.nanoTime());
		gv1 = accountService.createLecturer("123456", "GV 1 " + stamp, "gv1" + stamp + "@test.local", "GVREM1" + stamp,
				"CNPM");
		gv2 = accountService.createLecturer("123456", "GV 2 " + stamp, "gv2" + stamp + "@test.local", "GVREM2" + stamp,
				"CNPM");
		gv3 = accountService.createLecturer("123456", "GV 3 " + stamp, "gv3" + stamp + "@test.local", "GVREM3" + stamp,
				"CNPM");
		gv4 = accountService.createLecturer("123456", "GV 4 " + stamp, "gv4" + stamp + "@test.local", "GVREM4" + stamp,
				"CNPM");
		sv1 = accountService.createStudent("123456", "SV 1 " + stamp, "sv1" + stamp + "@test.local", "SVREM1" + stamp);
		sv2 = accountService.createStudent("123456", "SV 2 " + stamp, "sv2" + stamp + "@test.local", "SVREM2" + stamp);
		sv3 = accountService.createStudent("123456", "SV 3 " + stamp, "sv3" + stamp + "@test.local", "SVREM3" + stamp);
		sv4 = accountService.createStudent("123456", "SV 4 " + stamp, "sv4" + stamp + "@test.local", "SVREM4" + stamp);

		LocalDateTime now = LocalDateTime.now();
		RegistrationPeriodRequest request = new RegistrationPeriodRequest();
		request.setName("Đợt test reminder " + stamp);
		request.setType(PeriodType.GRADUATION_THESIS);
		request.setTeacherRegisterStart(now.minusDays(2));
		request.setTeacherRegisterEnd(now.plusDays(2));
		request.setStudentRegisterStart(now.plusDays(3));
		request.setStudentRegisterEnd(now.plusDays(10));
		request.setReportDeadline(LocalDate.now().plusDays(15));
		request.setReviewerScoreDeadline(LocalDate.now().plusDays(20));
		request.setCouncilReportDate(LocalDate.now().plusDays(25));
		period = periodService.create(request);
	}

	private void moveToStudentPhase(RegistrationPeriod period) {
		LocalDateTime now = LocalDateTime.now();
		period.setTeacherRegisterStart(now.minusDays(10));
		period.setTeacherRegisterEnd(now.minusDays(9));
		period.setStudentRegisterStart(now.minusDays(1));
		period.setStudentRegisterEnd(now.plusDays(1));
	}

	private Team createTeamWithThreeMembers(RegistrationPeriod period, Student leader, Student m1, Student m2) {
		Team team = teamService.create(period.getId(), leader);
		teamService.invite(team.getId(), m1.getStudentCode(), leader);
		teamService.respondInvitation(team.getId(), m1, true, null);
		teamService.invite(team.getId(), m2.getStudentCode(), leader);
		teamService.respondInvitation(team.getId(), m2, true, null);
		return team;
	}

	@Test
	@DisplayName("Lấy danh sách các đợt đăng ký trong ReminderService")
	void periods_ReturnsList() {
		List<RegistrationPeriod> periods = reminderService.periods();
		assertNotNull(periods);
		assertFalse(periods.isEmpty());
	}

	@Test
	@DisplayName("Nhắc hạn nộp báo cáo chỉ gửi cho nhóm có đề tài và chưa nộp báo cáo")
	void remindReportDeadline_SendsOnlyToPendingTeams() {
		// Tạo đề tài
		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài reminder");
		tr.setDescription("Mô tả");
		tr.setPeriodId(period.getId());
		Topic topic = topicService.create(tr, gv1);
		topicService.approve(topic.getId());
		topicService.publish(period.getId());

		moveToStudentPhase(period);

		Team team1 = createTeamWithThreeMembers(period, sv1, sv2, sv3);
		registrationService.register(team1.getId(), topic.getId(), sv1);
		registrationService.approve(topic.getId(), gv1);

		int sent1 = reminderService.remindReportDeadline(period.getId());
		assertTrue(sent1 >= 1);
		assertEquals(4, notificationService.countUnread(sv1));

		int sent2 = reminderService.remindReportDeadline(period.getId());
		assertEquals(0, sent2);
	}

	@Test
	@DisplayName("Nhắc hạn nộp báo cáo không gửi cho nhóm đã nộp báo cáo")
	void remindReportDeadline_SkipsReportedTeams() {
		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài reminder 2");
		tr.setDescription("Mô tả");
		tr.setPeriodId(period.getId());
		Topic topic = topicService.create(tr, gv1);
		topicService.approve(topic.getId());
		topicService.publish(period.getId());

		moveToStudentPhase(period);

		Team team = createTeamWithThreeMembers(period, sv1, sv2, sv3);
		registrationService.register(team.getId(), topic.getId(), sv1);
		registrationService.approve(topic.getId(), gv1);

		// Nộp báo cáo
		MockMultipartFile file = new MockMultipartFile("file", "baocao.pdf", "application/pdf", "demo".getBytes());
		reportService.submit(team.getId(), sv1, file);

		// Nhắc hạn nộp báo cáo
		int sent = reminderService.remindReportDeadline(period.getId());
		assertEquals(0, sent);
	}

	@Test
	@DisplayName("Nhắc lịch báo cáo hội đồng gửi thông báo cho các giảng viên trong hội đồng")
	void remindCouncilDate_SendsToMembers() {
		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài reminder 3");
		tr.setDescription("Mô tả");
		tr.setPeriodId(period.getId());
		Topic topic = topicService.create(tr, gv1);
		topicService.approve(topic.getId());
		topicService.publish(period.getId());

		moveToStudentPhase(period);

		Team team = createTeamWithThreeMembers(period, sv1, sv2, sv3);
		registrationService.register(team.getId(), topic.getId(), sv1);
		registrationService.approve(topic.getId(), gv1);

		// Lập hội đồng 3 giảng viên
		councilService.create(topic.getId(), "Hội đồng 1",
				List.of(gv2.getLecturerCode(), gv3.getLecturerCode(), gv4.getLecturerCode()),
				List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER));

		int sent1 = reminderService.remindCouncilDate(period.getId());
		assertTrue(sent1 >= 3);

		// Lần 2: Không gửi trùng lặp
		int sent2 = reminderService.remindCouncilDate(period.getId());
		assertEquals(0, sent2);
	}

	@Test
	@DisplayName("Nhắc lịch báo cáo hội đồng cho đợt không có ngày báo cáo ném ngoại lệ")
	void remindCouncilDate_NoDate_ThrowsException() {
		LocalDateTime now = LocalDateTime.now();
		RegistrationPeriodRequest request = new RegistrationPeriodRequest();
		request.setName("Đợt không có ngày báo cáo");
		request.setType(PeriodType.SPECIALIZED_PROJECT);
		request.setTeacherRegisterStart(now.minusDays(5));
		request.setTeacherRegisterEnd(now.minusDays(3));
		request.setStudentRegisterStart(now.minusDays(2));
		request.setStudentRegisterEnd(now.plusDays(5));
		request.setReportDeadline(LocalDate.now().plusDays(10));
		request.setReviewerScoreDeadline(LocalDate.now().plusDays(15));
		request.setCouncilReportDate(null);
		RegistrationPeriod noDatePeriod = periodService.create(request);

		assertThrows(InvalidNotificationException.class,
				() -> reminderService.remindCouncilDate(noDatePeriod.getId()));
	}
}
