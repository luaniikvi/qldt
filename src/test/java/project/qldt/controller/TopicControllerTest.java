package project.qldt.controller;
import project.qldt.support.Beans;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import project.qldt.dto.RegistrationPeriodRequest;
import project.qldt.dto.TopicRequest;
import project.qldt.model.Lecturer;
import project.qldt.model.PeriodType;
import project.qldt.model.RegistrationPeriod;
import project.qldt.model.Topic;
import project.qldt.service.AccountService;
import project.qldt.service.RegistrationPeriodService;
import project.qldt.service.TopicService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class TopicControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccountService accountService;

	@Autowired
	private RegistrationPeriodService periodService;

	@Autowired
	private TopicService topicService;

	private RegistrationPeriod period;
	private Topic topic;
	private Lecturer gv1;

	@BeforeEach
	void setUp() {
		gv1 = accountService.findLecturerByCode("GV001");

		RegistrationPeriodRequest pReq = new RegistrationPeriodRequest();
		pReq.setName("Đợt Topic Controller Test");
		pReq.setType(PeriodType.COURSE);
		pReq.setTeacherRegisterStart(LocalDateTime.now().minusDays(2));
		pReq.setTeacherRegisterEnd(LocalDateTime.now().plusDays(2));
		pReq.setStudentRegisterStart(LocalDateTime.now().plusDays(3));
		pReq.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));
		pReq.setReportDeadline(LocalDate.now().plusDays(10));
		period = periodService.create(pReq);

		TopicRequest tReq = new TopicRequest();
		tReq.setName("Đề tài Controller Test");
		tReq.setDescription("Mô tả");
		tReq.setPeriodId(period.getId());
		topic = topicService.create(tReq, gv1);
	}

	@Test
	@WithMockUser(username = "GV001", roles = "LECTURER")
	void xemDanhSachDeTai() throws Exception {
		mockMvc.perform(get("/topics"))
				.andExpect(status().isOk())
				.andExpect(view().name("topics/list"))
				.andExpect(model().attributeExists("topics"));
	}

	@Test
	@WithMockUser(username = "GV001", roles = "LECTURER")
	void xemFormTaoDeTai() throws Exception {
		mockMvc.perform(get("/topics/new"))
				.andExpect(status().isOk())
				.andExpect(view().name("topics/form"))
				.andExpect(model().attributeExists("topic"))
				.andExpect(model().attributeExists("periods"));
	}

	@Test
	@WithMockUser(username = "GV001", roles = "LECTURER")
	void taoDeTaiHopLe() throws Exception {
		mockMvc.perform(post("/topics")
				.with(csrf())
				.param("name", "Đề tài Mới Qua Controller")
				.param("description", "Mô tả chi tiết")
				.param("periodId", String.valueOf(period.getId())))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/topics"));
	}

	@Test
	@WithMockUser(username = "GV001", roles = "LECTURER")
	void taoDeTaiLoiValidation() throws Exception {
		mockMvc.perform(post("/topics")
				.with(csrf())
				.param("name", "")
				.param("description", ""))
				.andExpect(status().isOk())
				.andExpect(view().name("topics/form"))
				.andExpect(model().hasErrors());
	}

	@Test
	@WithMockUser(username = "GV001", roles = "LECTURER")
	void moiGiangVienHuongDanVaHuyLoiMoi() throws Exception {
		mockMvc.perform(post("/topics/" + topic.getId() + "/invite")
				.with(csrf())
				.param("lecturerCode", "GV002"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/topics"))
				.andExpect(flash().attributeExists("message"));

		mockMvc.perform(post("/topics/" + topic.getId() + "/invitation/cancel")
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/topics"))
				.andExpect(flash().attributeExists("message"));
	}

	@Test
	@WithMockUser(username = "GV002", roles = "LECTURER")
	void phanHoiLoiMoiHuongDan() throws Exception {
		topicService.inviteAdvisor(topic.getId(), "GV002", gv1);

		mockMvc.perform(post("/topics/" + topic.getId() + "/invitation")
				.with(csrf())
				.param("accept", "true"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/topics"))
				.andExpect(flash().attributeExists("message"));
	}

	@Test
	@WithMockUser(username = "GV001", roles = {"LECTURER", "DEAN"})
	void deanDuyetVaCongBoDeTai() throws Exception {
		mockMvc.perform(get("/dean/topics"))
				.andExpect(status().isOk())
				.andExpect(view().name("topics/pending"))
				.andExpect(model().attributeExists("topics"));

		mockMvc.perform(post("/dean/topics/" + topic.getId() + "/approve")
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dean/topics"))
				.andExpect(flash().attributeExists("message"));

		mockMvc.perform(post("/dean/topics/publish/" + period.getId())
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dean/topics"))
				.andExpect(flash().attributeExists("message"));
	}

	@Test
	@WithMockUser(username = "SV001", roles = "STUDENT")
	void xemDanhSachDeTaiDaCongBo() throws Exception {
		mockMvc.perform(get("/published-topics"))
				.andExpect(status().isOk())
				.andExpect(view().name("topics/published"))
				.andExpect(model().attributeExists("topics"));
	}
}
