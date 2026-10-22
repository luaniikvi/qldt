package project.qldt.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.RegistrationPeriodRequest;
import project.qldt.dto.TopicRequest;
import project.qldt.exception.InvalidRegistrationException;
import project.qldt.model.Lecturer;
import project.qldt.model.Notification;
import project.qldt.model.NotificationType;
import project.qldt.model.PeriodType;
import project.qldt.model.RegistrationPeriod;
import project.qldt.model.Student;
import project.qldt.model.Team;
import project.qldt.model.Topic;
import project.qldt.model.TopicAvailability;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RegistrationCancelTest {

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
	private NotificationService notificationService;

	private Lecturer advisor;
	private Student sv1;
	private RegistrationPeriod period;
	private Topic topic;
	private Topic topicNoTeam;
	private Team team;

	@BeforeEach
	void setUp() {
		advisor = accountService.createLecturer("123456", "GV 811", "gv811@qldt.local", "GV811", "CNPM");
		sv1 = accountService.createStudent("123456", "SV 811", "sv811@qldt.local", "SV811");

		RegistrationPeriodRequest periodRequest = new RegistrationPeriodRequest();
		periodRequest.setName("Đợt hủy đăng ký");
		periodRequest.setType(PeriodType.COURSE);
		periodRequest.setTeacherRegisterStart(LocalDateTime.now().minusDays(1));
		periodRequest.setTeacherRegisterEnd(LocalDateTime.now().plusDays(1));
		periodRequest.setStudentRegisterStart(LocalDateTime.now().plusDays(2));
		periodRequest.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));
		periodRequest.setReportDeadline(LocalDateTime.now().plusDays(10).toLocalDate());
		period = periodService.create(periodRequest);

		TopicRequest topicRequest = new TopicRequest();
		topicRequest.setName("De tai huy dang ky");
		topicRequest.setDescription("Noi dung");
		topicRequest.setPeriodId(period.getId());
		topic = topicService.create(topicRequest, advisor);
		topicService.approve(topic.getId());
		topicService.publish(period.getId());
		topicNoTeam = topicService.create(topicRequest, advisor);

		LocalDateTime now = LocalDateTime.now();
		period.setTeacherRegisterStart(now.minusDays(10));
		period.setTeacherRegisterEnd(now.minusDays(9));
		period.setStudentRegisterStart(now.minusDays(1));
		period.setStudentRegisterEnd(now.plusDays(1));

		team = teamService.create(period.getId(), sv1);
		registrationService.register(team.getId(), topic.getId(), sv1);
	}

	@Test
	void deanHuyDangKyDangChoDuyet() {
		registrationService.cancelByDean(topic.getId(), "Nhóm đăng ký nhầm đề tài");

		assertEquals(TopicAvailability.AVAILABLE, topic.getAvailability());
		assertNull(team.getTopic());
		assertEquals("Nhóm đăng ký nhầm đề tài", team.getRegistrationRejectionReason());
	}

	@Test
	void deanHuyDangKyDaDuyet() {
		registrationService.approve(topic.getId(), advisor);
		assertEquals(TopicAvailability.REGISTERED, topic.getAvailability());

		registrationService.cancelByDean(topic.getId(), "Trưởng khoa can thiệp");

		assertEquals(TopicAvailability.AVAILABLE, topic.getAvailability());
		assertNull(team.getTopic());
	}

	@Test
	void deanHuyGuiThongBaoChoSinhVienVaGiangVien() {
		registrationService.cancelByDean(topic.getId(), "Xử lý ngoại lệ");

		assertTrue(hasNotification(sv1, "Trưởng khoa hủy đăng ký đề tài"));
		assertTrue(hasNotification(advisor, "Trưởng khoa hủy đăng ký đề tài"));
	}

	@Test
	void deanHuyKhongNhapLyDoBiChan() {
		assertThrows(InvalidRegistrationException.class, () -> registrationService.cancelByDean(topic.getId(), "  "));
	}

	@Test
	void deanHuyKhiDeTaiChuaCoNhomBiChan() {
		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.cancelByDean(topicNoTeam.getId(), "Thử hủy"));
	}

	private boolean hasNotification(project.qldt.model.Account recipient, String title) {
		return notificationService.findByRecipient(recipient).stream()
				.anyMatch(item -> item.getType() == NotificationType.REJECTION && item.getTitle().equals(title));
	}
}