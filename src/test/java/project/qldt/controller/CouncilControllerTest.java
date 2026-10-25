package project.qldt.controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.dto.topic.TopicRequest;
import project.qldt.model.council.Council;
import project.qldt.model.council.CouncilRole;
import project.qldt.model.account.Lecturer;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.account.Student;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;
import project.qldt.model.council.TopicResult;
import project.qldt.service.account.AccountService;
import project.qldt.service.council.CouncilService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.team.TeamService;
import project.qldt.service.topic.TopicRegistrationService;
import project.qldt.service.topic.TopicService;

@SpringBootTest(classes = project.qldt.QldtApplication.class)
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class CouncilControllerTest {

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

	@Autowired
	private CouncilService councilService;

	@Autowired
	private project.qldt.service.report.ReportService reportService;

	private Topic topic;
	private Council council;

	@BeforeEach
	void setUp() {
		Lecturer gv1 = accountService.findLecturerByCode("GV001");
		Student sv1 = accountService.findStudentByCode("SV001");
		Student sv2 = accountService.findStudentByCode("SV002");
		Student sv3 = accountService.findStudentByCode("SV003");

		RegistrationPeriodRequest pReq = new RegistrationPeriodRequest();
		pReq.setName("Đợt Council Controller Test");
		pReq.setType(PeriodType.COURSE_PROJECT);
		pReq.setTeacherRegisterStart(LocalDateTime.now().minusDays(1));
		pReq.setTeacherRegisterEnd(LocalDateTime.now().plusDays(1));
		pReq.setStudentRegisterStart(LocalDateTime.now().plusDays(2));
		pReq.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));
		pReq.setReportDeadline(LocalDate.now().plusDays(10));
		RegistrationPeriod period = periodService.create(pReq);

		TopicRequest tReq = new TopicRequest();
		tReq.setName("Đề tài Council Test");
		tReq.setDescription("Mô tả");
		tReq.setPeriodId(period.getId());
		topic = topicService.create(tReq, gv1);
		topicService.approve(topic.getId());
		topicService.publish(period.getId());

		period.setTeacherRegisterStart(LocalDateTime.now().minusDays(10));
		period.setTeacherRegisterEnd(LocalDateTime.now().minusDays(5));
		period.setStudentRegisterStart(LocalDateTime.now().minusDays(2));
		period.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));

		Team team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV002", sv1);
		teamService.respondInvitation(team.getId(), sv2, true, null);
		teamService.invite(team.getId(), "SV003", sv1);
		teamService.respondInvitation(team.getId(), sv3, true, null);
		registrationService.register(team.getId(), topic.getId(), sv1);
		registrationService.approve(topic.getId(), gv1);

		council = councilService.create(topic.getId(), "Hội đồng Test",
				List.of("GV002", "GV003", "GV004"),
				List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER));
	}

	@Test
	@WithMockUser(username = "GV001", roles = {"LECTURER", "DEAN"})
	void deanXemVaLapHoiDong() throws Exception {
		mockMvc.perform(get("/dean/councils"))
				.andExpect(status().isOk())
				.andExpect(view().name("councils/dean"))
				.andExpect(model().attributeExists("councils"))
				.andExpect(model().attributeExists("topics"))
				.andExpect(model().attributeExists("lecturers"));
	}

	@Test
	@WithMockUser(username = "GV002", roles = "LECTURER")
	void thanhVienXemVaNhapDiemHoiDong() throws Exception {
		mockMvc.perform(get("/councils"))
				.andExpect(status().isOk())
				.andExpect(view().name("councils/list"))
				.andExpect(model().attributeExists("councils"));

		mockMvc.perform(post("/councils/" + council.getId() + "/score")
				.with(csrf())
				.param("score", "8.5")
				.param("comment", "Báo cáo tốt"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/councils"))
				.andExpect(flash().attributeExists("message"));
	}

	@Test
	@WithMockUser(username = "GV002", roles = "LECTURER")
	void chuTichTongHopDiem() throws Exception {
		Lecturer gv2 = accountService.findLecturerByCode("GV002");
		Lecturer gv3 = accountService.findLecturerByCode("GV003");
		Lecturer gv4 = accountService.findLecturerByCode("GV004");

		councilService.score(council.getId(), gv2, 8.0, null);
		councilService.score(council.getId(), gv3, 8.5, null);
		councilService.score(council.getId(), gv4, 9.0, null);

		mockMvc.perform(post("/councils/" + council.getId() + "/confirm")
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/councils"))
				.andExpect(flash().attributeExists("message"));
	}

	@Test
	@WithMockUser(username = "GV001", roles = {"LECTURER", "DEAN"})
	void deanCongBoDiem() throws Exception {
		Lecturer gv2 = accountService.findLecturerByCode("GV002");
		Lecturer gv3 = accountService.findLecturerByCode("GV003");
		Lecturer gv4 = accountService.findLecturerByCode("GV004");

		councilService.score(council.getId(), gv2, 8.0, null);
		councilService.score(council.getId(), gv3, 8.5, null);
		councilService.score(council.getId(), gv4, 9.0, null);
		TopicResult result = councilService.confirm(council.getId(), gv2);

		mockMvc.perform(post("/dean/councils/results/" + result.getId() + "/publish")
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dean/councils"))
				.andExpect(flash().attributeExists("message"));
	}

	@Test
	@WithMockUser(username = "SV001", roles = "STUDENT")
	void sinhVienXemKetQuaBaoVe() throws Exception {
		mockMvc.perform(get("/results"))
				.andExpect(status().isOk())
				.andExpect(view().name("results/list"))
				.andExpect(model().attributeExists("results"));
	}

	@Test
	@WithMockUser(username = "GV001", roles = "LECTURER")
	void giangVienHuongDanXemKetQuaBaoVe() throws Exception {
		mockMvc.perform(get("/results"))
				.andExpect(status().isOk())
				.andExpect(view().name("results/list"))
				.andExpect(model().attributeExists("results"));
	}

	@Test
	@WithMockUser(username = "GV002", roles = "LECTURER")
	void thanhVienHoiDongXemDuocBaoCaoDeTai() throws Exception {
		Student sv1 = accountService.findStudentByCode("SV001");
		Team team = teamService.findByTopicId(topic.getId());
		org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
				"file", "baocao_hoidong.pdf", "application/pdf", "noi dung bao cao hoi dong".getBytes());
		reportService.submit(team.getId(), sv1, file);

		mockMvc.perform(get("/councils"))
				.andExpect(status().isOk())
				.andExpect(view().name("councils/list"))
				.andExpect(model().attributeExists("councils"));

		mockMvc.perform(get("/reports/topic/" + topic.getId() + "/view"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inline")));
	}
}
