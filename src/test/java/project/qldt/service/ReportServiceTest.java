package project.qldt.service;
import project.qldt.service.account.AccountService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.report.ReportService;
import project.qldt.service.team.TeamService;
import project.qldt.service.topic.TopicRegistrationService;
import project.qldt.service.topic.TopicService;
import project.qldt.support.Beans;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.dto.topic.TopicRequest;
import project.qldt.exception.InvalidReportException;
import project.qldt.model.account.Lecturer;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.report.Report;
import project.qldt.model.account.Student;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportServiceTest {

	private final AccountService accountService = Beans.get(AccountService.class);
	private final RegistrationPeriodService periodService = Beans.get(RegistrationPeriodService.class);
	private final TopicService topicService = Beans.get(TopicService.class);
	private final TeamService teamService = Beans.get(TeamService.class);
	private final TopicRegistrationService registrationService = Beans.get(TopicRegistrationService.class);

	@TempDir
	Path tempDir;

	private ReportService reportService;
	private Lecturer gv1;
	private Lecturer gv2;
	private Student sv1;
	private Student sv2;
	private Student sv3;
	private Team team;

	@BeforeEach
	void setUp() {
		reportService = Beans.get(ReportService.class);
		gv1 = accountService.createLecturer("123456", "GV 1", "gv1@qldt.local", "GV901", "Công nghệ phần mềm");
		gv2 = accountService.createLecturer("123456", "GV 2", "gv2@qldt.local", "GV902", "Công nghệ phần mềm");
		sv1 = accountService.createStudent("123456", "SV 1", "sv1@qldt.local", "SV901");
		sv2 = accountService.createStudent("123456", "SV 2", "sv2@qldt.local", "SV902");
		sv3 = accountService.createStudent("123456", "SV 3", "sv3@qldt.local", "SV903");
		RegistrationPeriod period = periodService.create(periodRequest());
		Topic topic = topicService.create(topicRequest(period.getId()), gv1);
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

	@Test
	void nopBaoCaoThanhCong() {
		Report report = reportService.submit(team.getId(), sv1, file("baocao.pdf", "noi dung"));

		assertEquals(1, report.getVersion());
		assertEquals("baocao.pdf", report.getOriginalFileName());
		assertNotNull(report.getSubmittedAt());
		assertTrue(reportService.prepareDownload(report).resource().exists());
	}

	@Test
	void nopLaiCapNhatPhienBan() {
		reportService.submit(team.getId(), sv1, file("baocao.pdf", "v1"));
		Report report = reportService.submit(team.getId(), sv1, file("baocao-v2.pdf", "v2"));

		assertEquals(2, report.getVersion());
		assertEquals("baocao-v2.pdf", report.getOriginalFileName());
		assertEquals(1, reportService.findByAdvisor(gv1).size());
	}

	@Test
	void chiNhomTruongMoiDuocNop() {
		assertThrows(InvalidReportException.class,
				() -> reportService.submit(team.getId(), sv2, file("baocao.pdf", "noi dung")));
	}

	@Test
	void chuaDuyetDeTaiThiKhongNopDuoc() {
		Student sv4 = accountService.createStudent("123456", "SV 4", "sv4@qldt.local", "SV904");
		Team other = teamService.create(team.getPeriod().getId(), sv4);

		assertThrows(InvalidReportException.class,
				() -> reportService.submit(other.getId(), sv4, file("baocao.pdf", "noi dung")));
	}

	@Test
	void quaHanNopBiChan() {
		team.getPeriod().setReportDeadline(java.time.LocalDate.now().minusDays(1));

		assertThrows(InvalidReportException.class,
				() -> reportService.submit(team.getId(), sv1, file("baocao.pdf", "noi dung")));
	}

	@Test
	void nopDuocTepBatKyDinhDang() {
		Report report = reportService.submit(team.getId(), sv1, file("baocao.txt", "noi dung"));

		assertEquals("baocao.txt", report.getOriginalFileName());
		assertTrue(reportService.prepareDownload(report).resource().exists());
	}

	@Test
	void fileRongBiChan() {
		assertThrows(InvalidReportException.class,
				() -> reportService.submit(team.getId(), sv1, file("baocao.pdf", "")));
	}

	@Test
	void giangVienHuongDanThayBaoCao() {
		reportService.submit(team.getId(), sv1, file("baocao.pdf", "noi dung"));

		assertEquals(1, reportService.findByAdvisor(gv1).size());
		assertEquals(0, reportService.findByAdvisor(gv2).size());
	}

	private MockMultipartFile file(String name, String content) {
		return new MockMultipartFile("file", name, "application/pdf", content.getBytes());
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