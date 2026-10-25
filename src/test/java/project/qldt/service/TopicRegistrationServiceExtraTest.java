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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.dto.topic.RegistrationView;
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
class TopicRegistrationServiceExtraTest {

	private AccountService accountService;
	private RegistrationPeriodService periodService;
	private TopicService topicService;
	private TeamService teamService;
	private TopicRegistrationService registrationService;

	private Lecturer gv1;
	private Lecturer gv2;
	private Lecturer gvOther;
	private Student sv1;
	private Student sv2;
	private Student sv3;
	private RegistrationPeriod period;
	private Topic topic;
	private Team team;

	@BeforeEach
	void setUp() {
		accountService = Beans.get(AccountService.class);
		periodService = Beans.get(RegistrationPeriodService.class);
		topicService = Beans.get(TopicService.class);
		teamService = Beans.get(TeamService.class);
		registrationService = Beans.get(TopicRegistrationService.class);

		gv1 = accountService.createLecturer("123456", "GV 1", "gv1@qldt.local", "GV901", "Công nghệ phần mềm");
		gv2 = accountService.createLecturer("123456", "GV 2", "gv2@qldt.local", "GV902", "Công nghệ phần mềm");
		gvOther = accountService.createLecturer("123456", "GV Khac", "other@qldt.local", "GV999", "Hệ thống thông tin");
		sv1 = accountService.createStudent("123456", "SV 1", "sv1@qldt.local", "SV901");
		sv2 = accountService.createStudent("123456", "SV 2", "sv2@qldt.local", "SV902");
		sv3 = accountService.createStudent("123456", "SV 3", "sv3@qldt.local", "SV903");

		RegistrationPeriodRequest pReq = new RegistrationPeriodRequest();
		pReq.setName("Đợt test");
		pReq.setType(PeriodType.COURSE_PROJECT);
		pReq.setTeacherRegisterStart(LocalDateTime.now().minusDays(1));
		pReq.setTeacherRegisterEnd(LocalDateTime.now().plusDays(1));
		pReq.setStudentRegisterStart(LocalDateTime.now().plusDays(2));
		pReq.setStudentRegisterEnd(LocalDateTime.now().plusDays(10));
		pReq.setReportDeadline(LocalDateTime.now().plusDays(20).toLocalDate());
		period = periodService.create(pReq);

		topic = topicService.create(createTopicRequest("Đề tài 1", period.getId()), gv1);
		topicService.approve(topic.getId());
		topicService.publish(period.getId());

		period.setTeacherRegisterStart(LocalDateTime.now().minusDays(10));
		period.setTeacherRegisterEnd(LocalDateTime.now().minusDays(5));
		period.setStudentRegisterStart(LocalDateTime.now().minusDays(1));
		period.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));
		period.setReportDeadline(LocalDateTime.now().plusDays(10).toLocalDate());

		team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV902", sv1);
		teamService.respondInvitation(team.getId(), sv2, true, null);
		teamService.invite(team.getId(), "SV903", sv1);
		teamService.respondInvitation(team.getId(), sv3, true, null);
	}

	@Test
	void dangKyKhiDotBiKhoaBiChan() {
		periodService.setRegistrationLocked(period.getId(), true);

		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.register(team.getId(), topic.getId(), sv1));
	}

	@Test
	void dangKyTruocHoacSauThoiGianSinhVienDangKyBiChan() {
		period.setStudentRegisterStart(LocalDateTime.now().plusDays(1));
		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.register(team.getId(), topic.getId(), sv1));

		period.setStudentRegisterStart(LocalDateTime.now().minusDays(5));
		period.setStudentRegisterEnd(LocalDateTime.now().minusDays(1));
		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.register(team.getId(), topic.getId(), sv1));
	}

	@Test
	void dangKyDeTaiCuaDotKhacBiChan() {
		RegistrationPeriodRequest otherPReq = new RegistrationPeriodRequest();
		otherPReq.setName("Đợt khác");
		otherPReq.setType(PeriodType.COURSE_PROJECT);
		otherPReq.setTeacherRegisterStart(LocalDateTime.now().minusDays(1));
		otherPReq.setTeacherRegisterEnd(LocalDateTime.now().plusDays(1));
		otherPReq.setStudentRegisterStart(LocalDateTime.now().plusDays(2));
		otherPReq.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));
		otherPReq.setReportDeadline(LocalDateTime.now().plusDays(10).toLocalDate());
		RegistrationPeriod otherPeriod = periodService.create(otherPReq);

		Topic otherTopic = topicService.create(createTopicRequest("Đề tài đợt khác", otherPeriod.getId()), gv1);
		topicService.approve(otherTopic.getId());
		topicService.publish(otherPeriod.getId());

		otherPeriod.setTeacherRegisterStart(LocalDateTime.now().minusDays(10));
		otherPeriod.setTeacherRegisterEnd(LocalDateTime.now().minusDays(5));
		otherPeriod.setStudentRegisterStart(LocalDateTime.now().minusDays(1));
		otherPeriod.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));

		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.register(team.getId(), otherTopic.getId(), sv1));
	}

	@Test
	void huyDangKyKiemTraRangBuoc() {
		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.cancel(team.getId(), sv1));

		registrationService.register(team.getId(), topic.getId(), sv1);

		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.cancel(team.getId(), sv2));

		registrationService.approve(topic.getId(), gv1);
		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.cancel(team.getId(), sv1));
	}

	@Test
	void duyetDangKyKiemTraRangBuoc() {
		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.approve(topic.getId(), gv1));

		registrationService.register(team.getId(), topic.getId(), sv1);

		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.approve(topic.getId(), gvOther));

		Team approvedTeam = registrationService.approve(topic.getId(), gv1);
		assertNotNull(approvedTeam);
		assertEquals(TopicAvailability.REGISTERED, topic.getAvailability());
	}

	@Test
	void tuChoiDangKyKiemTraRangBuocVaCapNhatLyDo() {
		registrationService.register(team.getId(), topic.getId(), sv1);

		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.reject(topic.getId(), gvOther, "Lý do"));
		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.reject(topic.getId(), gv1, "  "));

		Team rejectedTeam = registrationService.reject(topic.getId(), gv1, "Yêu cầu bổ sung kiến thức");

		assertEquals(TopicAvailability.AVAILABLE, topic.getAvailability());
		assertNull(rejectedTeam.getTopic());
		assertEquals("Yêu cầu bổ sung kiến thức", rejectedTeam.getRegistrationRejectionReason());
	}

	@Test
	void danhSachChoDuyetCuaGiangVienVaDeTaiCoSanCuaNhom() {
		List<Topic> available = registrationService.findAvailableTopicsFor(team);
		assertEquals(1, available.size());

		List<RegistrationView> pendingBefore = registrationService.findPendingForAdvisor(gv1);
		assertEquals(0, pendingBefore.size());

		registrationService.register(team.getId(), topic.getId(), sv1);

		List<RegistrationView> pendingAfter = registrationService.findPendingForAdvisor(gv1);
		assertEquals(1, pendingAfter.size());
		assertEquals(team.getId(), pendingAfter.get(0).team().getId());
		assertEquals(topic.getId(), pendingAfter.get(0).topic().getId());

		List<RegistrationView> otherPending = registrationService.findPendingForAdvisor(gvOther);
		assertEquals(0, otherPending.size());
	}

	@Test
	void dangKyKhiChuaDuBaThanhVienBiChan() {
		Student svX = accountService.createStudent("123456", "SV X", "svx@qldt.local", "SV999EXTRA");
		Team understaffed = teamService.create(period.getId(), svX);
		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.register(understaffed.getId(), topic.getId(), svX));
	}

	private TopicRequest createTopicRequest(String name, Long periodId) {
		TopicRequest req = new TopicRequest();
		req.setName(name);
		req.setDescription("Mô tả");
		req.setPeriodId(periodId);
		return req;
	}
}
