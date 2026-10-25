package project.qldt.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
import project.qldt.service.account.AccountService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.team.TeamService;
import project.qldt.service.topic.TopicRegistrationService;
import project.qldt.service.topic.TopicService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class DeanRegistrationControllerTest {

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
	private Lecturer gv;
	private Student sv;
	private Topic topic;
	private Team team;

	@BeforeEach
	void setUp() {
		String stamp = String.valueOf(System.nanoTime());
		gv = accountService.createLecturer("123456", "GV DeanReg", "gvdr" + stamp + "@test.local", "GVDR" + stamp,
				"CNPM");
		sv = accountService.createStudent("123456", "SV DeanReg", "svdr" + stamp + "@test.local", "SVDR" + stamp);
		Student sv2 = accountService.createStudent("123456", "SV DeanReg 2", "svdr2" + stamp + "@test.local", "SVDR2" + stamp);
		Student sv3 = accountService.createStudent("123456", "SV DeanReg 3", "svdr3" + stamp + "@test.local", "SVDR3" + stamp);

		LocalDateTime now = LocalDateTime.now();
		RegistrationPeriodRequest req = new RegistrationPeriodRequest();
		req.setName("Đợt test DeanReg " + stamp);
		req.setType(PeriodType.COURSE_PROJECT);
		req.setTeacherRegisterStart(now.minusDays(2));
		req.setTeacherRegisterEnd(now.plusDays(2));
		req.setStudentRegisterStart(now.plusDays(3));
		req.setStudentRegisterEnd(now.plusDays(10));
		req.setReportDeadline(LocalDate.now().plusDays(15));
		period = periodService.create(req);

		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài DeanReg " + stamp);
		tr.setDescription("Mô tả");
		tr.setPeriodId(period.getId());
		topic = topicService.create(tr, gv);
		topicService.approve(topic.getId());
		topicService.publish(period.getId());

		period.setTeacherRegisterStart(now.minusDays(10));
		period.setTeacherRegisterEnd(now.minusDays(9));
		period.setStudentRegisterStart(now.minusDays(1));
		period.setStudentRegisterEnd(now.plusDays(1));

		team = teamService.create(period.getId(), sv);
		teamService.invite(team.getId(), sv2.getStudentCode(), sv);
		teamService.respondInvitation(team.getId(), sv2, true, null);
		teamService.invite(team.getId(), sv3.getStudentCode(), sv);
		teamService.respondInvitation(team.getId(), sv3, true, null);
		registrationService.register(team.getId(), topic.getId(), sv);
	}

	@Test
	@WithMockUser(username = "dean", roles = "DEAN")
	@DisplayName("Trưởng khoa xem danh sách đăng ký đề tài thành công")
	void deanCanViewRegistrationList() throws Exception {
		mockMvc.perform(get("/dean/registrations"))
				.andExpect(status().isOk())
				.andExpect(view().name("registrations/dean"))
				.andExpect(model().attributeExists("registrations"));
	}

	@Test
	@WithMockUser(username = "sv001", roles = "STUDENT")
	@DisplayName("Sinh viên truy cập /dean/registrations bị 403 Forbidden")
	void studentCannotAccessDeanRegistrations() throws Exception {
		mockMvc.perform(get("/dean/registrations"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "dean", roles = "DEAN")
	@DisplayName("Trưởng khoa hủy đăng ký đề tài kèm lý do thành công")
	void deanCancelRegistration_Success() throws Exception {
		mockMvc.perform(post("/dean/registrations/" + topic.getId() + "/cancel")
				.with(csrf())
				.param("reason", "Nhóm không đạt yêu cầu tiên quyết"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dean/registrations"))
				.andExpect(flash().attributeExists("message"));
	}

	@Test
	@WithMockUser(username = "dean", roles = "DEAN")
	@DisplayName("Trưởng khoa hủy đăng ký đề tài với lý do rỗng bị báo lỗi")
	void deanCancelRegistration_EmptyReason_Fails() throws Exception {
		mockMvc.perform(post("/dean/registrations/" + topic.getId() + "/cancel")
				.with(csrf())
				.param("reason", "   "))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dean/registrations"))
				.andExpect(flash().attributeExists("error"));
	}
}
