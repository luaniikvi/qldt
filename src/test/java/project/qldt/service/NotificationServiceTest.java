package project.qldt.service;

import project.qldt.service.account.AccountService;
import project.qldt.service.council.CouncilService;
import project.qldt.service.notification.NotificationService;
import project.qldt.service.notification.ReminderService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.report.ReportService;
import project.qldt.service.team.TeamService;
import project.qldt.service.topic.TopicRegistrationService;
import project.qldt.service.topic.TopicService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.dto.topic.TopicRequest;
import project.qldt.exception.InvalidNotificationException;
import project.qldt.model.council.Council;
import project.qldt.model.council.CouncilRole;
import project.qldt.model.account.Lecturer;
import project.qldt.model.notification.Notification;
import project.qldt.model.notification.NotificationType;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.account.Student;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceTest {

	@Autowired
	private AccountService accountService;

	@Autowired
	private RegistrationPeriodService periodService;

	@Autowired
	private TopicService topicService;

	@Autowired
	private TeamService teamService;

	@Autowired
	private TopicRegistrationService registrationService;

	@Autowired
	private ReportService reportService;

	@Autowired
	private CouncilService councilService;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private ReminderService reminderService;

	private Lecturer advisor;
	private Lecturer chair;
	private Student sv801;
	private Student sv802;
	private RegistrationPeriod period;
	private Topic topicOne;
	private Topic topicTwo;
	private Topic topicThree;
	private Team teamOne;
	private Council council;

	@BeforeEach
	void setUp() {
		advisor = accountService.createLecturer("123456", "GV 801", "gv801@qldt.local", "GV801", "CNPM");
		chair = accountService.createLecturer("123456", "GV 802", "gv802@qldt.local", "GV802", "CNPM");
		Lecturer secretary = accountService.createLecturer("123456", "GV 803", "gv803@qldt.local", "GV803", "CNPM");
		Lecturer member = accountService.createLecturer("123456", "GV 804", "gv804@qldt.local", "GV804", "CNPM");
		sv801 = accountService.createStudent("123456", "SV 801", "sv801@qldt.local", "SV801");
		sv802 = accountService.createStudent("123456", "SV 802", "sv802@qldt.local", "SV802");
		Student sv803 = accountService.createStudent("123456", "SV 803", "sv803@qldt.local", "SV803");
		Student sv804 = accountService.createStudent("123456", "SV 804", "sv804@qldt.local", "SV804");
		accountService.createStudent("123456", "SV 805", "sv805@qldt.local", "SV805");
		accountService.createStudent("123456", "SV 806", "sv806@qldt.local", "SV806");

		period = periodService.create(periodRequest());
		topicOne = topicService.create(topicRequest(period.getId(), "De tai thong bao"), advisor);
		topicService.approve(topicOne.getId());
		topicTwo = topicService.create(topicRequest(period.getId(), "De tai nhac han"), advisor);
		topicService.approve(topicTwo.getId());
		topicThree = topicService.create(topicRequest(period.getId(), "De tai moi GVHD"), chair);
		topicService.publish(period.getId());
		moveToStudentPhase(period);

		teamOne = teamService.create(period.getId(), sv801);
		teamService.invite(teamOne.getId(), "SV803", sv801);
		teamService.respondInvitation(teamOne.getId(), sv803, true, null);
		teamService.invite(teamOne.getId(), "SV804", sv801);
		teamService.respondInvitation(teamOne.getId(), sv804, true, null);
		registrationService.register(teamOne.getId(), topicOne.getId(), sv801);
		registrationService.approve(topicOne.getId(), advisor);

		council = councilService.create(topicOne.getId(), "Hoi dong thong bao", List.of("GV802", "GV803", "GV804"),
				List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER));
		councilService.score(council.getId(), chair, 8.0, "Tot");
		councilService.score(council.getId(), accountService.findLecturerByCode("GV803"), 8.0, "Tot");
		councilService.score(council.getId(), member, 8.0, "Tot");
	}

	@Test
	void moiVaoNhomTaoThongBaoChoSinhVien() {
		teamService.invite(teamOne.getId(), "SV802", sv801);

		assertTrue(hasNotification(sv802, NotificationType.INVITATION, "Lời mời vào nhóm"));
		assertEquals(1, notificationService.countUnread(sv802));
	}

	@Test
	void moiGiangVienHuongDanTaoThongBao() {
		topicService.inviteAdvisor(topicThree.getId(), "GV801", chair);

		assertTrue(hasNotification(advisor, NotificationType.INVITATION, "Lời mời làm giảng viên hướng dẫn"));
	}

	@Test
	void duyetDangKyGuiThongBaoChoTruongNhom() {
		assertTrue(hasNotification(sv801, NotificationType.APPROVAL, "Đăng ký đề tài được duyệt"));
	}

	@Test
	void nopBaoCaoGuiThongBaoChoTruongNhomVaLecturer() {
		reportService.submit(teamOne.getId(), sv801,
				new MockMultipartFile("file", "bao-cao.pdf", "application/pdf", "noi dung".getBytes()));

		assertTrue(hasNotification(advisor, NotificationType.APPROVAL, "Báo cáo mới được nộp"));
	}

	@Test
	void congBoDiemGuiThongBaoChoSinhVienVaGiangVien() {
		var result = councilService.confirm(council.getId(), chair);
		councilService.publish(result.getId());

		assertTrue(hasNotification(sv801, NotificationType.RESULT, "Đã công bố điểm bảo vệ"));
		assertTrue(hasNotification(advisor, NotificationType.RESULT, "Đã công bố điểm bảo vệ"));
	}

	@Test
	void danhDauDaDoc() {
		List<Notification> before = notificationService.findByRecipient(sv801);
		Notification first = before.get(0);

		notificationService.markRead(first.getId(), sv801);

		assertTrue(notificationService.findByRecipient(sv801).get(0).isRead());
		assertEquals(before.size() - 1, notificationService.countUnread(sv801));
	}

	@Test
	void khongDanhDauDuocThongBaoCuaNguoiKhac() {
		Notification ofStudent = notificationService.findByRecipient(sv801).get(0);

		assertThrows(InvalidNotificationException.class,
				() -> notificationService.markRead(ofStudent.getId(), sv802));
	}

	@Test
	void danhDauTatCaDaDoc() {
		notificationService.markAllRead(sv801);

		assertEquals(0, notificationService.countUnread(sv801));
		assertFalse(notificationService.findByRecipient(sv801).isEmpty());
	}

	@Test
	void nhacHanChiGuiChoTenChuaNopBaoCao() {
		Team teamTwo = teamService.create(period.getId(), sv802);
		Student sv805 = accountService.findStudentByCode("SV805");
		Student sv806 = accountService.findStudentByCode("SV806");
		teamService.invite(teamTwo.getId(), "SV805", sv802);
		teamService.respondInvitation(teamTwo.getId(), sv805, true, null);
		teamService.invite(teamTwo.getId(), "SV806", sv802);
		teamService.respondInvitation(teamTwo.getId(), sv806, true, null);
		registrationService.register(teamTwo.getId(), topicTwo.getId(), sv802);
		registrationService.approve(topicTwo.getId(), advisor);
		reportService.submit(teamOne.getId(), sv801,
				new MockMultipartFile("file", "bao-cao.pdf", "application/pdf", "noi dung".getBytes()));

		int sent = reminderService.remindReportDeadline(period.getId());

		assertEquals(3, sent);
		assertTrue(hasNotification(sv802, NotificationType.REMINDER, "Nhắc hạn nộp báo cáo"));
		assertFalse(hasNotification(sv801, NotificationType.REMINDER, "Nhắc hạn nộp báo cáo"));
		assertEquals(0, reminderService.remindReportDeadline(period.getId()));
	}

	@Test
	void nhacLichHoiDongGuiChoThanhVienHoiDong() {
		assertThrows(InvalidNotificationException.class, () -> reminderService.remindCouncilDate(period.getId()));

		period.setCouncilReportDate(LocalDate.now().plusDays(3));
		int sent = reminderService.remindCouncilDate(period.getId());

		assertEquals(3, sent);
		assertTrue(hasNotification(chair, NotificationType.REMINDER, "Nhắc lịch báo cáo hội đồng"));
	}

	private boolean hasNotification(project.qldt.model.account.Account recipient, NotificationType type,
			String titlePrefix) {
		return notificationService.findByRecipient(recipient).stream()
				.anyMatch(item -> item.getType() == type && item.getTitle().startsWith(titlePrefix));
	}

	private void moveToStudentPhase(RegistrationPeriod target) {
		LocalDateTime now = LocalDateTime.now();
		target.setTeacherRegisterStart(now.minusDays(10));
		target.setTeacherRegisterEnd(now.minusDays(9));
		target.setStudentRegisterStart(now.minusDays(1));
		target.setStudentRegisterEnd(now.plusDays(1));
	}

	private RegistrationPeriodRequest periodRequest() {
		RegistrationPeriodRequest request = new RegistrationPeriodRequest();
		request.setName("Đợt thông báo");
		request.setType(PeriodType.COURSE_PROJECT);
		request.setTeacherRegisterStart(LocalDateTime.now().minusDays(1));
		request.setTeacherRegisterEnd(LocalDateTime.now().plusDays(1));
		request.setStudentRegisterStart(LocalDateTime.now().plusDays(2));
		request.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));
		request.setReportDeadline(LocalDateTime.now().plusDays(10).toLocalDate());
		return request;
	}

	private TopicRequest topicRequest(Long periodId, String name) {
		TopicRequest request = new TopicRequest();
		request.setName(name);
		request.setDescription("Noi dung " + name);
		request.setPeriodId(periodId);
		return request;
	}
}