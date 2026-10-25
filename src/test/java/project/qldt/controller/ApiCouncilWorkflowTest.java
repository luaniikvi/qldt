package project.qldt.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.dto.topic.TopicRequest;
import project.qldt.model.account.Lecturer;
import project.qldt.model.account.Student;
import project.qldt.model.council.Council;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;
import project.qldt.repository.period.RegistrationPeriodRepository;
import project.qldt.service.account.AccountService;
import project.qldt.service.council.CouncilService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.team.TeamService;
import project.qldt.service.topic.TopicRegistrationService;
import project.qldt.service.topic.TopicService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class ApiCouncilWorkflowTest {

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
	private CouncilService councilService;

	private Lecturer advisor;
	private Student leader;
	private Topic topic;
	private Team team;

	@BeforeEach
	void setUp() {
		advisor = accountService.createLecturer("123456", "GV Hướng dẫn", "gvhd@qldt.local", "GV921",
				"Công nghệ phần mềm");
		accountService.createLecturer("123456", "GV Chủ tịch", "gvct@qldt.local", "GV922",
				"Công nghệ phần mềm");
		accountService.createLecturer("123456", "GV Thư ký", "gvtk@qldt.local", "GV923",
				"Công nghệ phần mềm");
		accountService.createLecturer("123456", "GV Thành viên", "gvtv@qldt.local", "GV924",
				"Công nghệ phần mềm");
		accountService.createLecturer("123456", "GV Phản biện", "gvpb@qldt.local", "GV926",
				"Công nghệ phần mềm");
		accountService.assignDean("GV921", true);

		leader = accountService.createStudent("123456", "SV Hội đồng 1", "svhd1@qldt.local", "SV921");
		Student member2 = accountService.createStudent("123456", "SV Hội đồng 2", "svhd2@qldt.local", "SV922");
		Student member3 = accountService.createStudent("123456", "SV Hội đồng 3", "svhd3@qldt.local", "SV923");

		RegistrationPeriodRequest request = new RegistrationPeriodRequest();
		request.setName("Đợt hội đồng API " + System.nanoTime());
		request.setType(PeriodType.GRADUATION_THESIS);
		request.setTeacherRegisterStart(LocalDateTime.now().minusDays(1));
		request.setTeacherRegisterEnd(LocalDateTime.now().plusDays(1));
		request.setStudentRegisterStart(LocalDateTime.now().plusDays(2));
		request.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));
		request.setReviewerScoreDeadline(LocalDate.now().plusDays(12));
		request.setCouncilReportDate(LocalDate.now().plusDays(15));
		request.setReportDeadline(LocalDate.now().plusDays(10));
		RegistrationPeriod period = periodService.create(request);

		TopicRequest topicRequest = new TopicRequest();
		topicRequest.setName("Đề tài hội đồng API " + System.nanoTime());
		topicRequest.setDescription("Mô tả");
		topicRequest.setPeriodId(period.getId());
		topic = topicService.create(topicRequest, advisor);
		topicService.approve(topic.getId());
		topicService.publish(period.getId());

		moveToStudentPhase(period);
		team = teamService.create(period.getId(), leader);
		teamService.invite(team.getId(), "SV922", leader);
		teamService.respondInvitation(team.getId(), member2, true, null);
		teamService.invite(team.getId(), "SV923", leader);
		teamService.respondInvitation(team.getId(), member3, true, null);
		registrationService.register(team.getId(), topic.getId(), leader);
		registrationService.approve(topic.getId(), advisor);
	}

	@Test
	@DisplayName("Luồng phản biện, hội đồng, công bố kết quả qua API")
	void councilWorkflow() throws Exception {
		mockMvc.perform(put("/api/v1/reviewer/topics/{id}/reviewer", topic.getId())
				.with(httpBasic("GV921", "123456"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"lecturerCode\":\"GV926\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reviewer.username").value("GV926"));

		mockMvc.perform(post("/api/v1/reviewer/topics/{id}/score", topic.getId())
				.with(httpBasic("GV926", "123456"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"score\":8.5,\"comment\":\"Tốt\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reviewerScore").value(8.5));

		String body = """
				{"topicId":%d,"name":"Hội đồng API","members":[
				  {"lecturerCode":"GV922","role":"CHAIR"},
				  {"lecturerCode":"GV923","role":"SECRETARY"},
				  {"lecturerCode":"GV924","role":"MEMBER"}]}
				""".formatted(topic.getId());

		mockMvc.perform(post("/api/v1/councils").with(httpBasic("GV921", "123456"))
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Hội đồng API"))
				.andExpect(jsonPath("$.members.length()").value(3));

		Council council = councilService.findByTopicId(topic.getId());
		score(council.getId(), "GV922", 7.0);
		score(council.getId(), "GV923", 8.0);
		score(council.getId(), "GV924", 9.0);

		mockMvc.perform(post("/api/v1/councils/{id}/confirmation", council.getId())
				.with(httpBasic("GV922", "123456")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.averageScore").value(8.0));

		mockMvc.perform(post("/api/v1/results/{id}/publish",
				councilService.findResultByCouncilId(council.getId()).getId())
				.with(httpBasic("GV921", "123456")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.published").value(true))
				.andExpect(jsonPath("$.finalScore").value(8.2));

		mockMvc.perform(get("/api/v1/results/mine").with(httpBasic("SV921", "123456")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].published").value(true));
	}

	@Test
	@DisplayName("Sinh viên không được lập hội đồng")
	void studentCannotCreateCouncil() throws Exception {
		String body = """
				{"topicId":%d,"members":[
				  {"lecturerCode":"GV922","role":"CHAIR"},
				  {"lecturerCode":"GV923","role":"SECRETARY"},
				  {"lecturerCode":"GV924","role":"MEMBER"}]}
				""".formatted(topic.getId());

		mockMvc.perform(post("/api/v1/councils").with(httpBasic("SV921", "123456"))
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
	}

	private void score(Long councilId, String username, double value) throws Exception {
		mockMvc.perform(post("/api/v1/councils/{id}/scores", councilId)
				.with(httpBasic(username, "123456"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"score\":" + value + ",\"comment\":\"Nhận xét\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullyScored").exists());
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
