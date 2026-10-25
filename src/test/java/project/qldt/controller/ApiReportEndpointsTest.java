package project.qldt.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.dto.topic.TopicRequest;
import project.qldt.model.account.Lecturer;
import project.qldt.model.account.Student;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;
import project.qldt.repository.period.RegistrationPeriodRepository;
import project.qldt.service.account.AccountService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.report.ReportService;
import project.qldt.service.team.TeamService;
import project.qldt.service.topic.TopicRegistrationService;
import project.qldt.service.topic.TopicService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class ApiReportEndpointsTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccountService accountService;

	@Autowired
	private RegistrationPeriodService periodService;

	@Autowired
	private RegistrationPeriodRepository periodRepository;

	@Autowired
	private TopicService topicService;

	@Autowired
	private TeamService teamService;

	@Autowired
	private TopicRegistrationService registrationService;

	@Autowired
	private ReportService reportService;

	private Lecturer advisor;
	private Student leader;
	private Student member;
	private Team team;

	@BeforeEach
	void setUp() {
		advisor = accountService.createLecturer("123456", "GV API", "gvapi@qldt.local", "GV911",
				"Công nghệ phần mềm");
		accountService.createLecturer("123456", "GV API 2", "gvapi2@qldt.local", "GV912",
				"Công nghệ phần mềm");
		leader = accountService.createStudent("123456", "SV API 1", "svapi1@qldt.local", "SV911");
		member = accountService.createStudent("123456", "SV API 2", "svapi2@qldt.local", "SV912");
		Student third = accountService.createStudent("123456", "SV API 3", "svapi3@qldt.local", "SV913");

		RegistrationPeriodRequest request = new RegistrationPeriodRequest();
		request.setName("Đợt báo cáo API " + System.nanoTime());
		request.setType(PeriodType.COURSE_PROJECT);
		request.setTeacherRegisterStart(LocalDateTime.now().minusDays(1));
		request.setTeacherRegisterEnd(LocalDateTime.now().plusDays(1));
		request.setStudentRegisterStart(LocalDateTime.now().plusDays(2));
		request.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));
		request.setReportDeadline(LocalDate.now().plusDays(10));
		RegistrationPeriod period = periodService.create(request);

		TopicRequest topicRequest = new TopicRequest();
		topicRequest.setName("Đề tài báo cáo API " + System.nanoTime());
		topicRequest.setDescription("Mô tả");
		topicRequest.setPeriodId(period.getId());
		Topic topic = topicService.create(topicRequest, advisor);
		topicService.approve(topic.getId());
		topicService.publish(period.getId());

		moveToStudentPhase(period);
		team = teamService.create(period.getId(), leader);
		teamService.invite(team.getId(), "SV912", leader);
		teamService.respondInvitation(team.getId(), member, true, null);
		teamService.invite(team.getId(), "SV913", leader);
		teamService.respondInvitation(team.getId(), third, true, null);
		registrationService.register(team.getId(), topic.getId(), leader);
		registrationService.approve(topic.getId(), advisor);
	}

	@Test
	@WithMockUser(username = "SV911", roles = "STUDENT")
	void uploadAndDownloadReport() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "báo cáo.pdf", "application/pdf",
				"noi dung".getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/v1/teams/{teamId}/report", team.getId()).file(file))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.originalFileName").value("báo cáo.pdf"))
				.andExpect(jsonPath("$.version").value(1))
				.andExpect(jsonPath("$.downloadUrl").value(Matchers.containsString("/api/v1/reports/")));

		var report = reportService.findByTeam(team);
		mockMvc.perform(get("/api/v1/reports/" + report.getId() + "/file"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", Matchers.containsString("filename*=")));

		mockMvc.perform(get("/api/v1/teams/" + team.getId() + "/report"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.originalFileName").value("báo cáo.pdf"));
	}

	@Test
	@WithMockUser(username = "SV912", roles = "STUDENT")
	void memberCannotUploadReport() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "baocao.pdf", "application/pdf",
				"noi dung".getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/v1/teams/{teamId}/report", team.getId()).file(file))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_REPORT"));
	}

	@Test
	@WithMockUser(username = "SV911", roles = "STUDENT")
	void emptyReportRejected() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "baocao.pdf", "application/pdf", new byte[0]);

		mockMvc.perform(multipart("/api/v1/teams/{teamId}/report", team.getId()).file(file))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_REPORT"));
	}

	@Test
	@WithMockUser(username = "SV911", roles = "STUDENT")
	void missingFilePartRejected() throws Exception {
		mockMvc.perform(multipart("/api/v1/teams/{teamId}/report", team.getId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
	}

	@Test
	@WithMockUser(username = "GV911", roles = "LECTURER")
	void advisorViewsReport() throws Exception {
		var report = reportService.submit(team.getId(), leader,
				new MockMultipartFile("file", "baocao.pdf", "application/pdf",
						"noi dung".getBytes(StandardCharsets.UTF_8)));

		mockMvc.perform(get("/api/v1/reports/" + report.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.originalFileName").value("baocao.pdf"));

		mockMvc.perform(get("/api/v1/reports/mine"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].originalFileName").value("baocao.pdf"));
	}

	private void moveToStudentPhase(RegistrationPeriod period) {
		LocalDateTime now = LocalDateTime.now();
		period.setTeacherRegisterStart(now.minusDays(10));
		period.setTeacherRegisterEnd(now.minusDays(9));
		period.setStudentRegisterStart(now.minusDays(1));
		period.setStudentRegisterEnd(now.plusDays(1));
		periodRepository.save(period);
	}
}
