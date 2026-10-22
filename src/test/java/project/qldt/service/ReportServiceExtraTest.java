package project.qldt.service;
import project.qldt.support.Beans;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import project.qldt.dto.RegistrationPeriodRequest;
import project.qldt.dto.TopicRequest;
import project.qldt.exception.InvalidReportException;
import project.qldt.exception.ReportNotFoundException;
import project.qldt.model.Lecturer;
import project.qldt.model.PeriodType;
import project.qldt.model.RegistrationPeriod;
import project.qldt.model.Report;
import project.qldt.model.Student;
import project.qldt.model.Team;
import project.qldt.model.Topic;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportServiceExtraTest {

	private AccountService accountService;
	private RegistrationPeriodService periodService;
	private TopicService topicService;
	private TeamService teamService;
	private TopicRegistrationService registrationService;
	private ReportService reportService;

	@TempDir
	Path tempDir;

	private Lecturer gv1;
	private Student sv1;
	private Student sv2;
	private RegistrationPeriod period;
	private Team team;

	@BeforeEach
	void setUp() {
		accountService = Beans.get(AccountService.class);
		periodService = Beans.get(RegistrationPeriodService.class);
		topicService = Beans.get(TopicService.class);
		teamService = Beans.get(TeamService.class);
		registrationService = Beans.get(TopicRegistrationService.class);
		reportService = Beans.get(ReportService.class);

		gv1 = accountService.createLecturer("123456", "GV 1", "gv1@qldt.local", "GV901", "Công nghệ phần mềm");
		sv1 = accountService.createStudent("123456", "SV 1", "sv1@qldt.local", "SV901");
		sv2 = accountService.createStudent("123456", "SV 2", "sv2@qldt.local", "SV902");

		RegistrationPeriodRequest pReq = new RegistrationPeriodRequest();
		pReq.setName("Đợt test");
		pReq.setType(PeriodType.COURSE);
		pReq.setTeacherRegisterStart(LocalDateTime.now().minusDays(1));
		pReq.setTeacherRegisterEnd(LocalDateTime.now().plusDays(1));
		pReq.setStudentRegisterStart(LocalDateTime.now().plusDays(2));
		pReq.setStudentRegisterEnd(LocalDateTime.now().plusDays(10));
		pReq.setReportDeadline(LocalDate.now().plusDays(20));
		period = periodService.create(pReq);

		Topic topic = topicService.create(createTopicRequest("Đề tài 1", period.getId()), gv1);
		topicService.approve(topic.getId());
		topicService.publish(period.getId());

		period.setTeacherRegisterStart(LocalDateTime.now().minusDays(10));
		period.setTeacherRegisterEnd(LocalDateTime.now().minusDays(5));
		period.setStudentRegisterStart(LocalDateTime.now().minusDays(2));
		period.setStudentRegisterEnd(LocalDateTime.now().plusDays(2));
		period.setReportDeadline(LocalDate.now().plusDays(5));

		team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV902", sv1);
		teamService.respondInvitation(team.getId(), sv2, true, null);
		registrationService.register(team.getId(), topic.getId(), sv1);
		registrationService.approve(topic.getId(), gv1);
	}

	@Test
	void nopBaoCaoQuaHanBiChan() {
		period.setReportDeadline(LocalDate.now().minusDays(1));

		MockMultipartFile file = new MockMultipartFile("file", "bc.pdf", "application/pdf", "noidung".getBytes());
		assertThrows(InvalidReportException.class, () -> reportService.submit(team.getId(), sv1, file));
	}

	@Test
	void nopBaoCaoDungNgayHanHopLe() {
		period.setReportDeadline(LocalDate.now());

		MockMultipartFile file = new MockMultipartFile("file", "bc.pdf", "application/pdf", "noidung".getBytes());
		Report report = reportService.submit(team.getId(), sv1, file);

		assertNotNull(report);
		assertEquals("bc.pdf", report.getOriginalFileName());
	}

	@Test
	void tepBaoCaoRongHoacNullBiChan() {
		assertThrows(InvalidReportException.class, () -> reportService.submit(team.getId(), sv1, null));

		MockMultipartFile emptyFile = new MockMultipartFile("file", "bc.pdf", "application/pdf", new byte[0]);
		assertThrows(InvalidReportException.class, () -> reportService.submit(team.getId(), sv1, emptyFile));
	}

	@Test
	void tepBaoCaoQua10MbBiChan() {
		byte[] largeBytes = new byte[10 * 1024 * 1024 + 1];
		MockMultipartFile largeFile = new MockMultipartFile("file", "bc.pdf", "application/pdf", largeBytes);

		assertThrows(InvalidReportException.class, () -> reportService.submit(team.getId(), sv1, largeFile));
	}

	@Test
	void chapNhanMoiDinhDangTep() {
		MockMultipartFile txtFile = new MockMultipartFile("file", "bc.txt", "text/plain", "abc".getBytes());
		Report txtReport = reportService.submit(team.getId(), sv1, txtFile);
		assertEquals("bc.txt", txtReport.getOriginalFileName());
		assertTrue(Files.exists(reportService.loadFile(txtReport)));

		MockMultipartFile zipFile = new MockMultipartFile("file", "bc.zip", "application/zip", "abc".getBytes());
		assertEquals("bc.zip", reportService.submit(team.getId(), sv1, zipFile).getOriginalFileName());

		MockMultipartFile noExtFile = new MockMultipartFile("file", "bc", "application/octet-stream",
				"abc".getBytes());
		assertEquals("bc", reportService.submit(team.getId(), sv1, noExtFile).getOriginalFileName());
	}

	@Test
	void dinhDangDocVaDocxHopLe() {
		MockMultipartFile docFile = new MockMultipartFile("file", "bc.doc", "application/msword", "doc".getBytes());
		Report report1 = reportService.submit(team.getId(), sv1, docFile);
		assertEquals("bc.doc", report1.getOriginalFileName());

		MockMultipartFile docxFile = new MockMultipartFile("file", "bc.docx", "application/vnd.openxmlformats",
				"docx".getBytes());
		Report report2 = reportService.submit(team.getId(), sv1, docxFile);
		assertEquals("bc.docx", report2.getOriginalFileName());
		assertEquals(2, report2.getVersion());
	}

	@Test
	void timKiemTheoIdKhongTonTaiNemLoi() {
		assertThrows(ReportNotFoundException.class, () -> reportService.findById(9999L));
	}

	@Test
	void loadFileTraVeDuongDanChinhXac() {
		MockMultipartFile file = new MockMultipartFile("file", "bc.pdf", "application/pdf", "noidung".getBytes());
		Report report = reportService.submit(team.getId(), sv1, file);

		Path path = reportService.loadFile(report);
		assertTrue(Files.exists(path));
		assertEquals("team-" + team.getId() + ".pdf", path.getFileName().toString());
	}

	private TopicRequest createTopicRequest(String name, Long periodId) {
		TopicRequest req = new TopicRequest();
		req.setName(name);
		req.setDescription("Mô tả");
		req.setPeriodId(periodId);
		return req;
	}
}
