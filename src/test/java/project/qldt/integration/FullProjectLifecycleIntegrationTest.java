package project.qldt.integration;
import project.qldt.support.Beans;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import project.qldt.dto.RegistrationPeriodRequest;
import project.qldt.dto.TopicRequest;
import project.qldt.model.Council;
import project.qldt.model.CouncilRole;
import project.qldt.model.InvitationStatus;
import project.qldt.model.Lecturer;
import project.qldt.model.PeriodType;
import project.qldt.model.RegistrationPeriod;
import project.qldt.model.Report;
import project.qldt.model.Student;
import project.qldt.model.Team;
import project.qldt.model.Topic;
import project.qldt.model.TopicAvailability;
import project.qldt.model.TopicResult;
import project.qldt.model.TopicStatus;
import project.qldt.service.AccountService;
import project.qldt.service.CouncilService;
import project.qldt.service.RegistrationPeriodService;
import project.qldt.service.ReportService;
import project.qldt.service.TeamService;
import project.qldt.service.TopicRegistrationService;
import project.qldt.service.TopicService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FullProjectLifecycleIntegrationTest {

	@TempDir
	Path tempDir;

	private AccountService accountService;
	private RegistrationPeriodService periodService;
	private TopicService topicService;
	private TeamService teamService;
	private TopicRegistrationService registrationService;
	private ReportService reportService;
	private CouncilService councilService;

	@BeforeEach
	void setUp() {
		accountService = Beans.get(AccountService.class);
		periodService = Beans.get(RegistrationPeriodService.class);
		topicService = Beans.get(TopicService.class);
		teamService = Beans.get(TeamService.class);
		registrationService = Beans.get(TopicRegistrationService.class);
		reportService = Beans.get(ReportService.class);
		councilService = Beans.get(CouncilService.class);
	}

	@Test
	void quyTrinhToanDienTuTaoDotDenCongBoKetQua() {
		accountService.createAdmin("admin_t", "123456", "Admin", "admin@qldt.local");
		Lecturer gv1 = accountService.createLecturer("123456", "Trưởng khoa", "dean@qldt.local", "GV901",
				"Hệ thống thông tin");
		accountService.assignDean("GV901", true);
		Lecturer gv2 = accountService.createLecturer("123456", "GV 2", "gv2@qldt.local", "GV902", "Hệ thống thông tin");
		Lecturer gv3 = accountService.createLecturer("123456", "GV 3", "gv3@qldt.local", "GV903", "Hệ thống thông tin");
		Lecturer gv4 = accountService.createLecturer("123456", "GV 4", "gv4@qldt.local", "GV904", "Hệ thống thông tin");
		Lecturer gv5 = accountService.createLecturer("123456", "GV 5", "gv5@qldt.local", "GV905", "Hệ thống thông tin");

		Student sv1 = accountService.createStudent("123456", "SV 1", "sv1@qldt.local", "SV901");
		Student sv2 = accountService.createStudent("123456", "SV 2", "sv2@qldt.local", "SV902");
		Student sv3 = accountService.createStudent("123456", "SV 3", "sv3@qldt.local", "SV903");
		Student svOutsider = accountService.createStudent("123456", "SV Ngoai", "svout@qldt.local", "SV999");

		RegistrationPeriodRequest pReq = new RegistrationPeriodRequest();
		pReq.setName("Đợt KLTN 2026-2027");
		pReq.setType(PeriodType.GRADUATION_THESIS);
		pReq.setTeacherRegisterStart(LocalDateTime.now().minusDays(2));
		pReq.setTeacherRegisterEnd(LocalDateTime.now().plusDays(2));
		pReq.setStudentRegisterStart(LocalDateTime.now().plusDays(3));
		pReq.setStudentRegisterEnd(LocalDateTime.now().plusDays(7));
		pReq.setReviewerScoreDeadline(LocalDate.now().plusDays(10));
		pReq.setCouncilReportDate(LocalDate.now().plusDays(15));
		pReq.setReportDeadline(LocalDate.now().plusDays(9));
		RegistrationPeriod period = periodService.create(pReq);
		assertNotNull(period.getId());

		TopicRequest tReq = new TopicRequest();
		tReq.setName("Nghiên cứu ứng dụng Antigravity AI");
		tReq.setDescription("Xây dựng giải pháp quản lý đề tài thông minh");
		tReq.setPeriodId(period.getId());
		Topic topic = topicService.create(tReq, gv1);
		assertEquals(TopicStatus.PENDING, topic.getStatus());
		assertEquals("Hệ thống thông tin", topic.getDepartment());

		topicService.inviteAdvisor(topic.getId(), "GV902", gv1);
		assertEquals(InvitationStatus.PENDING, topic.getInvitation().getStatus());

		topicService.respondInvitation(topic.getId(), gv2, true, null);
		assertEquals(2, topic.getAdvisors().size());
		assertEquals(InvitationStatus.ACCEPTED, topic.getInvitation().getStatus());

		topicService.approve(topic.getId());
		assertEquals(TopicStatus.APPROVED, topic.getStatus());

		int publishedCount = topicService.publish(period.getId());
		assertEquals(1, publishedCount);
		assertEquals(TopicStatus.PUBLISHED, topic.getStatus());
		assertEquals(TopicAvailability.AVAILABLE, topic.getAvailability());

		period.setTeacherRegisterStart(LocalDateTime.now().minusDays(10));
		period.setTeacherRegisterEnd(LocalDateTime.now().minusDays(5));
		period.setStudentRegisterStart(LocalDateTime.now().minusDays(2));
		period.setStudentRegisterEnd(LocalDateTime.now().plusDays(3));

		Team team = teamService.create(period.getId(), sv1);
		assertEquals(1, team.getMembers().size());

		teamService.invite(team.getId(), "SV902", sv1);
		teamService.invite(team.getId(), "SV903", sv1);
		teamService.respondInvitation(team.getId(), sv2, true, null);
		teamService.respondInvitation(team.getId(), sv3, true, null);
		assertEquals(3, team.getMembers().size());

		registrationService.register(team.getId(), topic.getId(), sv1);
		assertEquals(TopicAvailability.PENDING, topic.getAvailability());
		assertEquals(topic.getId(), team.getTopic().getId());

		Team approvedTeam = registrationService.approve(topic.getId(), gv1);
		assertEquals(TopicAvailability.REGISTERED, approvedTeam.getTopic().getAvailability());

		MockMultipartFile fileV1 = new MockMultipartFile("file", "baocao-v1.pdf", "application/pdf",
				"noi dung v1".getBytes());
		Report reportV1 = reportService.submit(team.getId(), sv1, fileV1);
		assertEquals(1, reportV1.getVersion());
		assertEquals("baocao-v1.pdf", reportV1.getOriginalFileName());
		assertTrue(Files.exists(reportService.loadFile(reportV1)));

		MockMultipartFile fileV2 = new MockMultipartFile("file", "baocao-v2.pdf", "application/pdf",
				"noi dung v2 hoan chinh".getBytes());
		Report reportV2 = reportService.submit(team.getId(), sv1, fileV2);
		assertEquals(2, reportV2.getVersion());
		assertEquals("baocao-v2.pdf", reportV2.getOriginalFileName());

		List<Report> advisorReports = reportService.findByAdvisor(gv1);
		assertEquals(1, advisorReports.size());
		assertEquals(2, advisorReports.get(0).getVersion());

		Council council = councilService.create(topic.getId(), "Hội đồng KLTN 01",
				List.of("GV903", "GV904", "GV905"),
				List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER));
		assertEquals(3, council.getMembers().size());
		assertEquals("Hội đồng KLTN 01", council.getName());

		councilService.score(council.getId(), gv3, 8.5, "Báo cáo xuất sắc");
		councilService.score(council.getId(), gv4, 9.0, "Trả lời tốt");
		councilService.score(council.getId(), gv5, 8.0, "Cần cải thiện demo");

		TopicResult confirmed = councilService.confirm(council.getId(), gv3);
		assertEquals(8.5, confirmed.getAverageScore());
		assertFalse(confirmed.isPublished());

		TopicResult published = councilService.publish(confirmed.getId());
		assertTrue(published.isPublished());
		assertNotNull(published.getPublishedAt());

		List<TopicResult> sv1Results = councilService.findPublishedResultsFor(sv1);
		List<TopicResult> sv2Results = councilService.findPublishedResultsFor(sv2);
		List<TopicResult> sv3Results = councilService.findPublishedResultsFor(sv3);
		List<TopicResult> svOutsiderResults = councilService.findPublishedResultsFor(svOutsider);

		assertEquals(1, sv1Results.size());
		assertEquals(1, sv2Results.size());
		assertEquals(1, sv3Results.size());
		assertEquals(0, svOutsiderResults.size());

		List<TopicResult> gv1Results = councilService.findPublishedResultsOfAdvisor(gv1);
		List<TopicResult> gv2Results = councilService.findPublishedResultsOfAdvisor(gv2);
		List<TopicResult> gv3Results = councilService.findPublishedResultsOfAdvisor(gv3);

		assertEquals(1, gv1Results.size());
		assertEquals(1, gv2Results.size());
		assertEquals(0, gv3Results.size());
	}
}
