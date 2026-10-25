package project.qldt.service;
import project.qldt.service.account.AccountService;
import project.qldt.service.council.CouncilService;
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
import project.qldt.dto.topic.TopicRequest;
import project.qldt.exception.CouncilNotFoundException;
import project.qldt.exception.InvalidCouncilException;
import project.qldt.model.council.Council;
import project.qldt.model.council.CouncilRole;
import project.qldt.model.account.Lecturer;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.account.Student;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;
import project.qldt.model.council.TopicResult;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CouncilServiceExtraTest {

	private AccountService accountService;
	private RegistrationPeriodService periodService;
	private TopicService topicService;
	private TeamService teamService;
	private TopicRegistrationService registrationService;
	private CouncilService councilService;

	private Lecturer gv1;
	private Lecturer gv2;
	private Lecturer gv3;
	private Lecturer gv4;
	private Lecturer gv5;
	private Student sv1;
	private Student sv2;
	private Student sv3;
	private RegistrationPeriod period;
	private Topic topic;
	private Topic draft;

	@BeforeEach
	void setUp() {
		accountService = Beans.get(AccountService.class);
		periodService = Beans.get(RegistrationPeriodService.class);
		topicService = Beans.get(TopicService.class);
		teamService = Beans.get(TeamService.class);
		registrationService = Beans.get(TopicRegistrationService.class);
		councilService = Beans.get(CouncilService.class);

		gv1 = accountService.createLecturer("123456", "GV 1", "gv1@qldt.local", "GV901", "Công nghệ phần mềm");
		gv2 = accountService.createLecturer("123456", "GV 2", "gv2@qldt.local", "GV902", "Công nghệ phần mềm");
		gv3 = accountService.createLecturer("123456", "GV 3", "gv3@qldt.local", "GV903", "Công nghệ phần mềm");
		gv4 = accountService.createLecturer("123456", "GV 4", "gv4@qldt.local", "GV904", "Công nghệ phần mềm");
		gv5 = accountService.createLecturer("123456", "GV 5", "gv5@qldt.local", "GV905", "Công nghệ phần mềm");
		sv1 = accountService.createStudent("123456", "SV 1", "sv1@qldt.local", "SV901");
		sv2 = accountService.createStudent("123456", "SV 2", "sv2@qldt.local", "SV902");
		sv3 = accountService.createStudent("123456", "SV 3", "sv3@qldt.local", "SV903");

		RegistrationPeriodRequest pReq = new RegistrationPeriodRequest();
		pReq.setName("Đợt test");
		pReq.setType(PeriodType.COURSE_PROJECT);
		pReq.setTeacherRegisterStart(LocalDateTime.now().minusDays(1));
		pReq.setTeacherRegisterEnd(LocalDateTime.now().plusDays(1));
		pReq.setStudentRegisterStart(LocalDateTime.now().plusDays(2));
		pReq.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));
		pReq.setReportDeadline(LocalDateTime.now().plusDays(10).toLocalDate());
		period = periodService.create(pReq);

		topic = topicService.create(createTopicRequest("Đề tài 1", period.getId()), gv1);
		draft = topicService.create(createTopicRequest("Đề tài nháp", period.getId()), gv1);
		topicService.approve(topic.getId());
		topicService.publish(period.getId());

		period.setTeacherRegisterStart(LocalDateTime.now().minusDays(10));
		period.setTeacherRegisterEnd(LocalDateTime.now().minusDays(5));
		period.setStudentRegisterStart(LocalDateTime.now().minusDays(2));
		period.setStudentRegisterEnd(LocalDateTime.now().plusDays(2));

		Team team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV902", sv1);
		teamService.respondInvitation(team.getId(), sv2, true, null);
		teamService.invite(team.getId(), "SV903", sv1);
		teamService.respondInvitation(team.getId(), sv3, true, null);
		registrationService.register(team.getId(), topic.getId(), sv1);
		registrationService.approve(topic.getId(), gv1);
	}

	@Test
	void lapHoiDongChoDeTaiChuaDuyetDangKyBiChan() {
		assertThrows(InvalidCouncilException.class,
				() -> councilService.create(draft.getId(), "HD",
						List.of("GV902", "GV903", "GV904"),
						List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER)));
	}

	@Test
	void lapHoiDongSoLuongThanhVienKhongHopLe() {
		assertThrows(InvalidCouncilException.class,
				() -> councilService.create(topic.getId(), "HD",
						null,
						List.of(CouncilRole.CHAIR)));

		assertThrows(InvalidCouncilException.class,
				() -> councilService.create(topic.getId(), "HD",
						List.of("GV902", "GV903"),
						List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY)));

		Lecturer gv6 = accountService.createLecturer("123456", "GV 6", "gv6@qldt.local", "GV906", "CNPM");
		assertThrows(InvalidCouncilException.class,
				() -> councilService.create(topic.getId(), "HD",
						List.of("GV902", "GV903", "GV904", "GV905", "GV906", "GV901"),
						List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER, CouncilRole.MEMBER,
								CouncilRole.MEMBER, CouncilRole.MEMBER)));
	}

	@Test
	void lapHoiDongSaiKhopVaiTroVoiGiangVien() {
		assertThrows(InvalidCouncilException.class,
				() -> councilService.create(topic.getId(), "HD",
						List.of("GV902", "GV903", "GV904"),
						null));

		assertThrows(InvalidCouncilException.class,
				() -> councilService.create(topic.getId(), "HD",
						List.of("GV902", "GV903", "GV904"),
						List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY)));
	}

	@Test
	void lapHoiDongKhongCoDungMotChuTichVaMotThuKy() {
		assertThrows(InvalidCouncilException.class,
				() -> councilService.create(topic.getId(), "HD",
						List.of("GV902", "GV903", "GV904"),
						List.of(CouncilRole.MEMBER, CouncilRole.SECRETARY, CouncilRole.MEMBER)));

		assertThrows(InvalidCouncilException.class,
				() -> councilService.create(topic.getId(), "HD",
						List.of("GV902", "GV903", "GV904"),
						List.of(CouncilRole.CHAIR, CouncilRole.MEMBER, CouncilRole.MEMBER)));
	}

	@Test
	void lapHoiDongTenRongTuDatMacDinh() {
		Council council = councilService.create(topic.getId(), "   ",
				List.of("GV902", "GV903", "GV904"),
				List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER));

		assertEquals("Hội đồng " + council.getId(), council.getName());
	}

	@Test
	void tongHopDiemLamTronHaiChuSoThapPhan() {
		Council council = councilService.create(topic.getId(), "HD",
				List.of("GV902", "GV903", "GV904"),
				List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER));

		councilService.score(council.getId(), gv2, 8.3, null);
		councilService.score(council.getId(), gv3, 8.4, null);
		councilService.score(council.getId(), gv4, 8.4, null);

		TopicResult result = councilService.confirm(council.getId(), gv2);

		assertEquals(8.37, result.getAverageScore());
		assertEquals(1, councilService.findResultsByPeriod(period.getId()).size());
	}

	@Test
	void congBoKetQuaKhongTonTaiNemLoi() {
		assertThrows(CouncilNotFoundException.class, () -> councilService.publish(9999L));
	}

	@Test
	void timKiemHoiDongVaDeTaiSanSangLapHoiDong() {
		List<Topic> readyBefore = councilService.findTopicsReadyForCouncil();
		assertEquals(1, readyBefore.size());

		Council council = councilService.create(topic.getId(), "HD",
				List.of("GV902", "GV903", "GV904"),
				List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER));

		List<Topic> readyAfter = councilService.findTopicsReadyForCouncil();
		assertEquals(0, readyAfter.size());

		assertEquals(council.getId(), councilService.findByTopicId(topic.getId()).getId());
		assertNull(councilService.findByTopicId(9999L));
	}

	@Test
	void timKiemTheoIdKhongTonTaiNemLoi() {
		assertThrows(CouncilNotFoundException.class, () -> councilService.findById(9999L));
	}

	private TopicRequest createTopicRequest(String name, Long periodId) {
		TopicRequest req = new TopicRequest();
		req.setName(name);
		req.setDescription("Mô tả");
		req.setPeriodId(periodId);
		return req;
	}
}
