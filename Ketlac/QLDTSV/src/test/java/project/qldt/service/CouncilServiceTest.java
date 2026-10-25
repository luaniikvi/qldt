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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.dto.topic.TopicRequest;
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
class CouncilServiceTest {

	@org.springframework.beans.factory.annotation.Autowired
	private AccountService accountService;
	@org.springframework.beans.factory.annotation.Autowired
	private RegistrationPeriodService periodService;
	@org.springframework.beans.factory.annotation.Autowired
	private TopicService topicService;
	@org.springframework.beans.factory.annotation.Autowired
	private TeamService teamService;
	@org.springframework.beans.factory.annotation.Autowired
	private TopicRegistrationService registrationService;
	@org.springframework.beans.factory.annotation.Autowired
	private CouncilService councilService;

	private Lecturer gv1;
	private Lecturer gv2;
	private Lecturer gv3;
	private Lecturer gv4;
	private Student sv1;
	private Student sv2;
	private Student sv3;
	private Team team;
	private Topic topic;

	@BeforeEach
	void setUp() {
		gv1 = accountService.createLecturer("123456", "GV 1", "gv1@qldt.local", "GV901", "Công nghệ phần mềm");
		gv2 = accountService.createLecturer("123456", "GV 2", "gv2@qldt.local", "GV902", "Công nghệ phần mềm");
		gv3 = accountService.createLecturer("123456", "GV 3", "gv3@qldt.local", "GV903", "Công nghệ phần mềm");
		gv4 = accountService.createLecturer("123456", "GV 4", "gv4@qldt.local", "GV904", "Công nghệ phần mềm");
		sv1 = accountService.createStudent("123456", "SV 1", "sv1@qldt.local", "SV901");
		sv2 = accountService.createStudent("123456", "SV 2", "sv2@qldt.local", "SV902");
		sv3 = accountService.createStudent("123456", "SV 3", "sv3@qldt.local", "SV903");
		RegistrationPeriod period = periodService.create(periodRequest());
		topic = topicService.create(topicRequest(period.getId()), gv1);
		topicService.approve(topic.getId());
		topicService.publish(period.getId());
		moveToStudentPhase(period);
		team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV902", sv1);
		teamService.respondInvitation(team.getId(), sv2, true, null);
		teamService.invite(team.getId(), "SV903", sv1);
		teamService.respondInvitation(team.getId(), sv3, true, null);
		registrationService.register(team.getId(), topic.getId(), sv1);
		registrationService.approve(topic.getId(), gv1);
	}

	private Council createCouncil() {
		return councilService.create(topic.getId(), "Hội đồng 1", List.of("GV902", "GV903", "GV904"),
				List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER));
	}

	private void scoreAll(double first, double second, double third) {
		Council council = councilService.findByTopicId(topic.getId());
		councilService.score(council.getId(), gv2, first, "Nhận xét 1");
		councilService.score(council.getId(), gv3, second, "Nhận xét 2");
		councilService.score(council.getId(), gv4, third, "Nhận xét 3");
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

	@Test
	void lapHoiDongHopLe() {
		Council council = createCouncil();

		assertEquals(3, council.getMembers().size());
		assertEquals("Hội đồng 1", council.getName());
		assertEquals(1, councilService.findByLecturer(gv2).size());
		assertEquals(0, councilService.findByLecturer(gv1).size());
	}

	@Test
	void hoiDongPhaiCo3Den5GiangVien() {
		assertThrows(InvalidCouncilException.class, () -> councilService.create(topic.getId(), "Hội đồng",
				List.of("GV902", "GV903"), List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY)));
	}

	@Test
	void phaiCoDungMotChuTichVaMotThuKy() {
		assertThrows(InvalidCouncilException.class, () -> councilService.create(topic.getId(), "Hội đồng",
				List.of("GV902", "GV903", "GV904"),
				List.of(CouncilRole.CHAIR, CouncilRole.CHAIR, CouncilRole.MEMBER)));
	}

	@Test
	void giangVienHuongDanKhongDuocVaoHoiDong() {
		assertThrows(InvalidCouncilException.class, () -> councilService.create(topic.getId(), "Hội đồng",
				List.of("GV901", "GV903", "GV904"),
				List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER)));
	}

	@Test
	void giangVienKhongDuocTrungTrongHoiDong() {
		assertThrows(InvalidCouncilException.class, () -> councilService.create(topic.getId(), "Hội đồng",
				List.of("GV902", "GV902", "GV903"),
				List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER)));
	}

	@Test
	void moiDeTaiChiCoMotHoiDong() {
		createCouncil();

		assertThrows(InvalidCouncilException.class, this::createCouncil);
	}

	@Test
	void chiThanhVienMoiDuocNhapDiem() {
		Council council = createCouncil();
		Lecturer outsider = accountService.createLecturer("123456", "GV 5", "gv5@qldt.local", "GV905",
				"Công nghệ phần mềm");

		assertThrows(InvalidCouncilException.class,
				() -> councilService.score(council.getId(), outsider, 8.0, null));
	}

	@Test
	void diemPhaiTrongKhoang0Den10() {
		Council council = createCouncil();

		assertThrows(InvalidCouncilException.class, () -> councilService.score(council.getId(), gv2, 11, null));
		assertThrows(InvalidCouncilException.class, () -> councilService.score(council.getId(), gv2, -1, null));
	}

	@Test
	void diemChiCoMotChuSoThapPhan() {
		Council council = createCouncil();

		assertThrows(InvalidCouncilException.class, () -> councilService.score(council.getId(), gv2, 7.25, null));
	}

	@Test
	void chiChuTichDuocTongHopDiem() {
		Council council = createCouncil();
		scoreAll(8.0, 7.5, 8.5);

		assertThrows(InvalidCouncilException.class, () -> councilService.confirm(council.getId(), gv3));
	}

	@Test
	void chuaDuDiemThiKhongTongHopDuoc() {
		Council council = createCouncil();
		councilService.score(council.getId(), gv2, 8.0, null);

		assertThrows(InvalidCouncilException.class, () -> councilService.confirm(council.getId(), gv2));
	}

	@Test
	void chuTichTongHopDiemTrungBinh() {
		Council council = createCouncil();
		scoreAll(8.0, 7.5, 8.5);

		TopicResult result = councilService.confirm(council.getId(), gv2);

		assertEquals(8.0, result.getAverageScore());
		assertEquals(false, result.isPublished());
	}

	@Test
	void chiSinhVienTrongNhomMoiThayKetQua() {
		Council council = createCouncil();
		scoreAll(8.0, 8.0, 8.0);
		TopicResult confirmed = councilService.confirm(council.getId(), gv2);

		assertEquals(0, councilService.findPublishedResultsFor(sv1).size());

		councilService.publish(confirmed.getId());

		assertEquals(1, councilService.findPublishedResultsFor(sv1).size());
		assertEquals(1, councilService.findPublishedResultsFor(sv2).size());
		assertEquals(1, councilService.findPublishedResultsFor(sv3).size());
		Student svOutsider = accountService.createStudent("123456", "SV Outsider", "svout@qldt.local", "SV999OUT");
		assertEquals(0, councilService.findPublishedResultsFor(svOutsider).size());
		assertEquals(1, councilService.findPublishedResultsOfAdvisor(gv1).size());
		assertEquals(0, councilService.findPublishedResultsOfAdvisor(gv2).size());
	}

	@Test
	void khongSuaDiemSauKhiCongBo() {
		Council council = createCouncil();
		scoreAll(8.0, 8.0, 8.0);
		TopicResult confirmed = councilService.confirm(council.getId(), gv2);
		councilService.publish(confirmed.getId());

		assertThrows(InvalidCouncilException.class, () -> councilService.score(council.getId(), gv2, 9.0, null));
		assertThrows(InvalidCouncilException.class, () -> councilService.confirm(council.getId(), gv2));
	}

	@Test
	void khongCongBoHaiLan() {
		Council council = createCouncil();
		scoreAll(8.0, 8.0, 8.0);
		TopicResult confirmed = councilService.confirm(council.getId(), gv2);
		councilService.publish(confirmed.getId());

		assertThrows(InvalidCouncilException.class, () -> councilService.publish(confirmed.getId()));
	}

}
