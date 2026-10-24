package project.qldt.controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.dto.topic.TopicRequest;
import project.qldt.model.account.Lecturer;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.account.Student;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;
import project.qldt.service.account.AccountService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.team.TeamService;
import project.qldt.service.topic.TopicRegistrationService;
import project.qldt.service.topic.TopicService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class TeamControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccountService accountService;

	@Autowired
	private RegistrationPeriodService periodService;

	@Autowired
	private TopicService topicService;

	@Autowired
	private TeamService teamService;

	@Autowired
	private TopicRegistrationService registrationService;

	private RegistrationPeriod period;
	private Topic topic;
	private Student sv1;
	private Student sv2;
	private Lecturer gv1;
	private Team team;

	@BeforeEach
	void setUp() {
		sv1 = accountService.findStudentByCode("SV001");
		sv2 = accountService.findStudentByCode("SV002");
		gv1 = accountService.findLecturerByCode("GV001");

		RegistrationPeriodRequest pReq = new RegistrationPeriodRequest();
		pReq.setName("Đợt Team Controller Test");
		pReq.setType(PeriodType.COURSE_PROJECT);
		pReq.setTeacherRegisterStart(LocalDateTime.now().minusDays(1));
		pReq.setTeacherRegisterEnd(LocalDateTime.now().plusDays(1));
		pReq.setStudentRegisterStart(LocalDateTime.now().plusDays(2));
		pReq.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));
		pReq.setReportDeadline(LocalDate.now().plusDays(10));
		period = periodService.create(pReq);

		TopicRequest tReq = new TopicRequest();
		tReq.setName("Đề tài Team Test");
		tReq.setDescription("Mô tả");
		tReq.setPeriodId(period.getId());
		topic = topicService.create(tReq, gv1);
		topicService.approve(topic.getId());
		topicService.publish(period.getId());

		period.setTeacherRegisterStart(LocalDateTime.now().minusDays(10));
		period.setTeacherRegisterEnd(LocalDateTime.now().minusDays(5));
		period.setStudentRegisterStart(LocalDateTime.now().minusDays(2));
		period.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));

		team = teamService.create(period.getId(), sv1);
	}

	@Test
	@WithMockUser(username = "SV001", roles = "STUDENT")
	void xemDanhSachNhom() throws Exception {
		mockMvc.perform(get("/teams"))
				.andExpect(status().isOk())
				.andExpect(view().name("teams/list"))
				.andExpect(model().attributeExists("teams"));
	}

	@Test
	@WithMockUser(username = "SV001", roles = "STUDENT")
	void xemFormTaoNhom() throws Exception {
		mockMvc.perform(get("/teams/new"))
				.andExpect(status().isOk())
				.andExpect(view().name("teams/form"))
				.andExpect(model().attributeExists("team"))
				.andExpect(model().attributeExists("periods"));
	}

	@Test
	@WithMockUser(username = "SV001", roles = "STUDENT")
	void moiThanhVienVaHuyLoiMoi() throws Exception {
		mockMvc.perform(post("/teams/" + team.getId() + "/invite")
				.with(csrf())
				.param("studentCode", "SV002"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/teams"))
				.andExpect(flash().attributeExists("message"));

		mockMvc.perform(post("/teams/" + team.getId() + "/invitation/cancel")
				.with(csrf())
				.param("studentCode", "SV002"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/teams"))
				.andExpect(flash().attributeExists("message"));
	}

	@Test
	@WithMockUser(username = "SV002", roles = "STUDENT")
	void chapNhanLoiMoiVaoNhom() throws Exception {
		teamService.invite(team.getId(), "SV002", sv1);

		mockMvc.perform(post("/teams/" + team.getId() + "/invitation")
				.with(csrf())
				.param("accept", "true"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/teams"))
				.andExpect(flash().attributeExists("message"));
	}

	@Test
	@WithMockUser(username = "SV001", roles = "STUDENT")
	void dangKyDeTaiVaHuyDangKy() throws Exception {
		mockMvc.perform(get("/teams/" + team.getId() + "/register"))
				.andExpect(status().isOk())
				.andExpect(view().name("teams/register"))
				.andExpect(model().attributeExists("team"))
				.andExpect(model().attributeExists("topics"));

		mockMvc.perform(post("/teams/" + team.getId() + "/register")
				.with(csrf())
				.param("topicId", String.valueOf(topic.getId())))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/teams"))
				.andExpect(flash().attributeExists("message"));

		mockMvc.perform(post("/teams/" + team.getId() + "/registration/cancel")
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/teams"))
				.andExpect(flash().attributeExists("message"));
	}

	@Test
	@WithMockUser(username = "GV001", roles = "LECTURER")
	void giangVienDuyetDangKyDeTai() throws Exception {
		registrationService.register(team.getId(), topic.getId(), sv1);

		mockMvc.perform(get("/registrations"))
				.andExpect(status().isOk())
				.andExpect(view().name("registrations/list"))
				.andExpect(model().attributeExists("registrations"));

		mockMvc.perform(post("/registrations/" + topic.getId() + "/approve")
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/registrations"))
				.andExpect(flash().attributeExists("message"));
	}

	@Test
	@WithMockUser(username = "SV001", roles = "STUDENT")
	void nopBaoCaoDeTai() throws Exception {
		registrationService.register(team.getId(), topic.getId(), sv1);
		registrationService.approve(topic.getId(), gv1);

		MockMultipartFile file = new MockMultipartFile("file", "baocao.pdf", "application/pdf", "noi dung".getBytes());

		mockMvc.perform(multipart("/teams/" + team.getId() + "/report")
				.file(file)
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/teams"))
				.andExpect(flash().attributeExists("message"));
	}
}
