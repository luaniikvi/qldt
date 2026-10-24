package project.qldt.service;
import project.qldt.service.account.AccountService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.team.TeamService;
import project.qldt.service.topic.TopicRegistrationService;
import project.qldt.service.topic.TopicService;
import project.qldt.support.Beans;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.dto.topic.TopicRequest;
import project.qldt.exception.InvalidRegistrationException;
import project.qldt.model.account.Lecturer;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.account.Student;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;
import project.qldt.model.topic.TopicAvailability;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TopicRegistrationServiceTest {

	private final AccountService accountService = Beans.get(AccountService.class);
	private final RegistrationPeriodService periodService = Beans.get(RegistrationPeriodService.class);
	private final TopicService topicService = Beans.get(TopicService.class);
	private final TeamService teamService = Beans.get(TeamService.class);
	private final TopicRegistrationService registrationService = Beans.get(TopicRegistrationService.class);

	private Lecturer gv1;
	private Lecturer gv2;
	private Student sv1;
	private Student sv2;
	private Student sv3;
	private RegistrationPeriod period;
	private Topic topic;
	private Topic draft;
	private Team team;

	@BeforeEach
	void setUp() {
		gv1 = accountService.createLecturer("123456", "GV 1", "gv1@qldt.local", "GV901", "Công nghệ phần mềm");
		gv2 = accountService.createLecturer("123456", "GV 2", "gv2@qldt.local", "GV902", "Công nghệ phần mềm");
		sv1 = accountService.createStudent("123456", "SV 1", "sv1@qldt.local", "SV901");
		sv2 = accountService.createStudent("123456", "SV 2", "sv2@qldt.local", "SV902");
		sv3 = accountService.createStudent("123456", "SV 3", "sv3@qldt.local", "SV903");
		period = periodService.create(periodRequest());
		draft = topicService.create(topicRequest(period.getId()), gv1);
		topic = topicService.create(topicRequest(period.getId()), gv1);
		topicService.approve(topic.getId());
		topicService.publish(period.getId());
		moveToStudentPhase(period);
		team = teamService.create(period.getId(), sv1);
	}

	@Test
	void dangKyDeTaiThanhCong() {
		Team result = registrationService.register(team.getId(), topic.getId(), sv1);

		assertEquals(topic.getId(), result.getTopic().getId());
		assertEquals(TopicAvailability.PENDING, topic.getAvailability());
	}

	@Test
	void chiNhomTruongMoiDuocDangKy() {
		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.register(team.getId(), topic.getId(), sv2));
	}

	@Test
	void nhomDaCoDeTaiKhongDangKyDuoc() {
		registrationService.register(team.getId(), topic.getId(), sv1);

		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.register(team.getId(), draft.getId(), sv1));
	}

	@Test
	void deTaiChuaCongBoKhongDangKyDuoc() {
		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.register(team.getId(), draft.getId(), sv1));
	}

	@Test
	void deTaiDaCoNhomDangKyThiNhomKhacKhongDangKyDuoc() {
		registrationService.register(team.getId(), topic.getId(), sv1);
		Team other = teamService.create(period.getId(), sv3);

		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.register(other.getId(), topic.getId(), sv3));
	}

	@Test
	void ngoaiThoiGianSinhVienDangKyBiChan() {
		period.setStudentRegisterStart(LocalDateTime.now().minusDays(5));
		period.setStudentRegisterEnd(LocalDateTime.now().minusDays(4));

		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.register(team.getId(), topic.getId(), sv1));
	}

	@Test
	void dotBiKhoaDangKyBiChan() {
		periodService.setRegistrationLocked(period.getId(), true);

		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.register(team.getId(), topic.getId(), sv1));
	}

	@Test
	void huyDangKyTruocKhiDuyet() {
		registrationService.register(team.getId(), topic.getId(), sv1);

		registrationService.cancel(team.getId(), sv1);

		assertNull(team.getTopic());
		assertEquals(TopicAvailability.AVAILABLE, topic.getAvailability());
	}

	@Test
	void khongHuyDuocSauKhiDuyet() {
		registrationService.register(team.getId(), topic.getId(), sv1);
		registrationService.approve(topic.getId(), gv1);

		assertThrows(InvalidRegistrationException.class, () -> registrationService.cancel(team.getId(), sv1));
	}

	@Test
	void giangVienHuongDanDuyetDangKy() {
		registrationService.register(team.getId(), topic.getId(), sv1);

		registrationService.approve(topic.getId(), gv1);

		assertEquals(TopicAvailability.REGISTERED, topic.getAvailability());
	}

	@Test
	void tuChoiDangKyCanLyDoVaTraDeTaiVeTrangThaiBan() {
		registrationService.register(team.getId(), topic.getId(), sv1);

		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.reject(topic.getId(), gv1, " "));

		registrationService.reject(topic.getId(), gv1, "Đề tài không phù hợp");

		assertNull(team.getTopic());
		assertEquals(TopicAvailability.AVAILABLE, topic.getAvailability());
		assertEquals("Đề tài không phù hợp", team.getRegistrationRejectionReason());
	}

	@Test
	void giangVienKhongPhaiGvhdKhongDuocDuyet() {
		registrationService.register(team.getId(), topic.getId(), sv1);

		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.approve(topic.getId(), gv2));
	}

	@Test
	void chiMotLanGianhDuocDeTai() {
		assertEquals(1, topicService.claimForRegistration(topic.getId()));
		assertEquals(0, topicService.claimForRegistration(topic.getId()));
	}

	@Test
	void dangKyKhiDeTaiDaBiGianhThiBaoLoi() {
		assertEquals(1, topicService.claimForRegistration(topic.getId()));

		Team other = teamService.create(period.getId(), sv2);
		InvalidRegistrationException ex = assertThrows(InvalidRegistrationException.class,
				() -> registrationService.register(other.getId(), topic.getId(), sv2));
		assertEquals("Đề tài đã có nhóm đăng ký", ex.getMessage());
	}

	private void moveToStudentPhase(RegistrationPeriod period) {
		LocalDateTime now = LocalDateTime.now();
		period.setTeacherRegisterStart(now.minusDays(10));
		period.setTeacherRegisterEnd(now.minusDays(9));
		period.setStudentRegisterStart(now.minusDays(1));
		period.setStudentRegisterEnd(now.plusDays(1));
	}

	private RegistrationPeriodRequest periodRequest() {
		RegistrationPeriodRequest request = new RegistrationPeriodRequest();
		request.setName("Đợt đăng ký");
		request.setType(PeriodType.COURSE_PROJECT);
		request.setTeacherRegisterStart(LocalDateTime.now().minusDays(1));
		request.setTeacherRegisterEnd(LocalDateTime.now().plusDays(1));
		request.setStudentRegisterStart(LocalDateTime.now().plusDays(2));
		request.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));
		request.setReportDeadline(LocalDateTime.now().plusDays(10).toLocalDate());
		return request;
	}

	private TopicRequest topicRequest(Long periodId) {
		TopicRequest request = new TopicRequest();
		request.setName("Đề tài mẫu");
		request.setDescription("Nội dung đề tài");
		request.setPeriodId(periodId);
		return request;
	}
}
