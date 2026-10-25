package project.qldt.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.dto.topic.TopicRequest;
import project.qldt.exception.InvalidCouncilException;
import project.qldt.exception.InvalidTopicException;
import project.qldt.model.account.Lecturer;
import project.qldt.model.account.Student;
import project.qldt.model.council.Council;
import project.qldt.model.council.CouncilRole;
import project.qldt.model.council.TopicResult;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;
import project.qldt.repository.topic.TopicRepository;
import project.qldt.service.account.AccountService;
import project.qldt.service.council.CouncilService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.reviewer.ReviewerService;
import project.qldt.service.team.TeamService;
import project.qldt.service.topic.TopicRegistrationService;
import project.qldt.service.topic.TopicService;
import project.qldt.support.Beans;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReviewerServiceTest {

	private final AccountService accountService = Beans.get(AccountService.class);
	private final RegistrationPeriodService periodService = Beans.get(RegistrationPeriodService.class);
	private final TopicService topicService = Beans.get(TopicService.class);
	private final TeamService teamService = Beans.get(TeamService.class);
	private final TopicRegistrationService registrationService = Beans.get(TopicRegistrationService.class);
	private final CouncilService councilService = Beans.get(CouncilService.class);
	private final ReviewerService reviewerService = Beans.get(ReviewerService.class);
	private final TopicRepository topicRepository = Beans.get(TopicRepository.class);

	private Lecturer gvhd;
	private Lecturer gvpb;
	private Lecturer gv3;
	private Lecturer gv4;
	private Lecturer gv5;
	private Student sv1;
	private Student sv2;
	private Student sv3;
	private Topic topic;

	@BeforeEach
	void setUp() {
		gvhd = accountService.createLecturer("123456", "GV HD", "gvhd@qldt.local", "GV801", "Công nghệ phần mềm");
		gvpb = accountService.createLecturer("123456", "GV PB", "gvpb@qldt.local", "GV802", "Công nghệ phần mềm");
		gv3 = accountService.createLecturer("123456", "GV 3", "gv3@qldt.local", "GV803", "Công nghệ phần mềm");
		gv4 = accountService.createLecturer("123456", "GV 4", "gv4@qldt.local", "GV804", "Công nghệ phần mềm");
		gv5 = accountService.createLecturer("123456", "GV 5", "gv5@qldt.local", "GV805", "Công nghệ phần mềm");
		sv1 = accountService.createStudent("123456", "SV 1", "sv1@qldt.local", "SV801");
		sv2 = accountService.createStudent("123456", "SV 2", "sv2@qldt.local", "SV802");
		sv3 = accountService.createStudent("123456", "SV 3", "sv3@qldt.local", "SV803");
		RegistrationPeriod period = periodService.create(periodRequest());
		topic = topicService.create(topicRequest(period.getId()), gvhd);
		topicService.approve(topic.getId());
		topicService.publish(period.getId());
		moveToStudentPhase(period);
		Team team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV802", sv1);
		teamService.respondInvitation(team.getId(), sv2, true, null);
		teamService.invite(team.getId(), "SV803", sv1);
		teamService.respondInvitation(team.getId(), sv3, true, null);
		registrationService.register(team.getId(), topic.getId(), sv1);
		registrationService.approve(topic.getId(), gvhd);
	}

	@Test
	void phanCongVaChamDiemPhanBienLuuDuLieu() {
		reviewerService.assign(topic.getId(), "GV802");
		assertEquals(gvpb.getId(), topic.getReviewer().getId());
		assertEquals(1, reviewerService.findAssignable().size());

		reviewerService.score(topic.getId(), gvpb, 8.5, "Nội dung tốt");
		Topic saved = topicRepository.findById(topic.getId()).orElseThrow();
		assertEquals(8.5, saved.getReviewerScore());
		assertEquals("Nội dung tốt", saved.getReviewerComment());
		assertNotNull(saved.getReviewerScoredAt());
		assertEquals(1, reviewerService.findVisibleScores(gvpb).size());
	}

	@Test
	void chiTruongKhoaVaGiangVienPhanBienMoiThayDiem() {
		reviewerService.assign(topic.getId(), "GV802");
		reviewerService.score(topic.getId(), gvpb, 8.0, "Đạt");
		councilService.create(topic.getId(), "Hội đồng KLTN", List.of("GV803", "GV804", "GV805"),
				List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER));
		assertEquals(1, reviewerService.findVisibleScores(gvpb).size());
		assertTrue(reviewerService.findVisibleScores(gv3).isEmpty());
		gv4.getExtraRoles().add(project.qldt.model.account.Role.DEAN);
		assertEquals(1, reviewerService.findVisibleScores(gv4).size());
	}

	@Test
	void khongThePhanCongChinhGiangVienHuongDan() {
		InvalidTopicException ex = assertThrows(InvalidTopicException.class,
				() -> reviewerService.assign(topic.getId(), "GV801"));
		assertTrue(ex.getMessage().contains("hướng dẫn"));
	}

	@Test
	void diemPhanBienPhaiTrongKhoang0Den10() {
		reviewerService.assign(topic.getId(), "GV802");
		assertThrows(InvalidTopicException.class, () -> reviewerService.score(topic.getId(), gvpb, 11.0, null));
	}

	@Test
	void diemPhanBienChiDuocMotChuSoThapPhan() {
		reviewerService.assign(topic.getId(), "GV802");
		assertThrows(InvalidTopicException.class, () -> reviewerService.score(topic.getId(), gvpb, 8.55, null));
	}

	@Test
	void chiGiangVienDuocPhanCongMoiChamDuoc() {
		reviewerService.assign(topic.getId(), "GV802");
		assertThrows(InvalidTopicException.class, () -> reviewerService.score(topic.getId(), gv3, 8.0, null));
	}

	@Test
	void diemTongKetTheoTrongSoPhanBien40VaHoiDong60() {
		reviewerService.assign(topic.getId(), "GV802");
		reviewerService.score(topic.getId(), gvpb, 9.0, "Rất tốt");

		Council council = councilService.create(topic.getId(), "Hội đồng KLTN",
				List.of("GV803", "GV804", "GV805"),
				List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER));
		councilService.score(council.getId(), gv3, 8.5, "Tốt");
		councilService.score(council.getId(), gv4, 9.0, "Tốt");
		councilService.score(council.getId(), gv5, 8.0, "Khá");

		TopicResult result = councilService.confirm(council.getId(), gv3);
		assertEquals(8.5, result.getAverageScore());
		TopicResult published = councilService.publish(result.getId());
		assertEquals(8.7, published.getFinalScore(), 0.0001);
	}

	@Test
	void hoiDongChuaTheChamKhiPhanBienChuaCham() {
		Council council = councilService.create(topic.getId(), "Hội đồng KLTN",
				List.of("GV803", "GV804", "GV805"),
				List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER));
		assertThrows(InvalidCouncilException.class, () -> councilService.score(council.getId(), gv3, 8.5, null));
	}

	@Test
	void sinhVienTrongNhomThayDiemPhanBien() {
		reviewerService.assign(topic.getId(), "GV802");
		reviewerService.score(topic.getId(), gvpb, 8.0, "Đạt");
		assertEquals(1, reviewerService.findVisibleScores(sv1).size());
	}

	@Test
	void giangVienNgoaiKhongThayDiemPhanBien() {
		reviewerService.assign(topic.getId(), "GV802");
		reviewerService.score(topic.getId(), gvpb, 8.0, "Đạt");
		assertTrue(reviewerService.findVisibleScores(gv3).isEmpty());
	}

	@Test
	void suaDiemThanhVienSauKhiTongHopThiCapNhatLaiDiemTrungBinh() {
		reviewerService.assign(topic.getId(), "GV802");
		reviewerService.score(topic.getId(), gvpb, 9.0, "Tốt");

		Council council = councilService.create(topic.getId(), "Hội đồng KLTN",
				List.of("GV803", "GV804", "GV805"),
				List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER));
		councilService.score(council.getId(), gv3, 10.0, "Xuất sắc");
		councilService.score(council.getId(), gv4, 8.5, "Tốt");
		councilService.score(council.getId(), gv5, 4.5, "Yếu");

		TopicResult confirmed = councilService.confirm(council.getId(), gv3);
		assertEquals(7.67, confirmed.getAverageScore(), 0.001);

		councilService.score(council.getId(), gv5, 6.5, "Cập nhật sau phản biện");
		TopicResult updated = councilService.findResultByCouncilId(council.getId());
		assertEquals(8.33, updated.getAverageScore(), 0.001);

		TopicResult published = councilService.publish(updated.getId());
		assertEquals(8.6, published.getFinalScore(), 0.001);
	}

	@Test
	void suaDiemPhanBienSauKhiCongBoBiChan() {
		reviewerService.assign(topic.getId(), "GV802");
		reviewerService.score(topic.getId(), gvpb, 9.0, "Tốt");

		Council council = councilService.create(topic.getId(), "Hội đồng KLTN",
				List.of("GV803", "GV804", "GV805"),
				List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER));
		councilService.score(council.getId(), gv3, 8.5, "Tốt");
		councilService.score(council.getId(), gv4, 8.5, "Tốt");
		councilService.score(council.getId(), gv5, 8.5, "Tốt");
		TopicResult result = councilService.confirm(council.getId(), gv3);
		councilService.publish(result.getId());

		assertThrows(InvalidTopicException.class, () -> reviewerService.score(topic.getId(), gvpb, 7.0, null));
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
		request.setName("Đợt KLTN");
		request.setType(PeriodType.GRADUATION_THESIS);
		request.setTeacherRegisterStart(LocalDateTime.now().minusDays(1));
		request.setTeacherRegisterEnd(LocalDateTime.now().plusDays(1));
		request.setStudentRegisterStart(LocalDateTime.now().plusDays(2));
		request.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));
		request.setReviewerScoreDeadline(LocalDate.now().plusDays(8));
		request.setCouncilReportDate(LocalDate.now().plusDays(12));
		request.setReportDeadline(LocalDate.now().plusDays(6));
		return request;
	}

	private TopicRequest topicRequest(Long periodId) {
		TopicRequest request = new TopicRequest();
		request.setName("Đề tài phản biện");
		request.setDescription("Nội dung đề tài");
		request.setPeriodId(periodId);
		return request;
	}
}
