package project.qldt.controller;
import project.qldt.support.Beans;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import project.qldt.dto.RegistrationPeriodRequest;
import project.qldt.dto.TopicRequest;
import project.qldt.model.Lecturer;
import project.qldt.model.PeriodType;
import project.qldt.model.RegistrationPeriod;
import project.qldt.model.Report;
import project.qldt.model.Student;
import project.qldt.model.Team;
import project.qldt.model.Topic;
import project.qldt.service.AccountService;
import project.qldt.service.RegistrationPeriodService;
import project.qldt.service.ReportService;
import project.qldt.service.TeamService;
import project.qldt.service.TopicRegistrationService;
import project.qldt.service.TopicService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class ReportControllerTest {

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
	private ReportService reportService;

	private Report report;

	@BeforeEach
	void setUp() {
		Lecturer gv1 = accountService.findLecturerByCode("GV001");
		Student sv1 = accountService.findStudentByCode("SV001");

		RegistrationPeriodRequest pReq = new RegistrationPeriodRequest();
		pReq.setName("Đợt Report Controller Test");
		pReq.setType(PeriodType.COURSE);
		pReq.setTeacherRegisterStart(LocalDateTime.now().minusDays(1));
		pReq.setTeacherRegisterEnd(LocalDateTime.now().plusDays(1));
		pReq.setStudentRegisterStart(LocalDateTime.now().plusDays(2));
		pReq.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));
		pReq.setReportDeadline(LocalDate.now().plusDays(10));
		RegistrationPeriod period = periodService.create(pReq);

		TopicRequest tReq = new TopicRequest();
		tReq.setName("Đề tài Report Test");
		tReq.setDescription("Mô tả");
		tReq.setPeriodId(period.getId());
		Topic topic = topicService.create(tReq, gv1);
		topicService.approve(topic.getId());
		topicService.publish(period.getId());

		period.setTeacherRegisterStart(LocalDateTime.now().minusDays(10));
		period.setTeacherRegisterEnd(LocalDateTime.now().minusDays(5));
		period.setStudentRegisterStart(LocalDateTime.now().minusDays(2));
		period.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));

		Team team = teamService.create(period.getId(), sv1);
		registrationService.register(team.getId(), topic.getId(), sv1);
		registrationService.approve(topic.getId(), gv1);

		MockMultipartFile file = new MockMultipartFile("file", "baocao_download.pdf", "application/pdf",
				"noi dung tai ve".getBytes());
		report = reportService.submit(team.getId(), sv1, file);
	}

	@Test
	@WithMockUser(username = "GV001", roles = "LECTURER")
	void giangVienXemDanhSachBaoCao() throws Exception {
		mockMvc.perform(get("/reports"))
				.andExpect(status().isOk())
				.andExpect(view().name("reports/list"))
				.andExpect(model().attributeExists("reports"));
	}

	@Test
	@WithMockUser(username = "GV001", roles = "LECTURER")
	void giangVienHuongDanTaiBaoCao() throws Exception {
		mockMvc.perform(get("/reports/" + report.getId() + "/download"))
				.andExpect(status().isOk())
				.andExpect(header().exists("Content-Disposition"));
	}

	@Test
	@WithMockUser(username = "GV002", roles = "LECTURER")
	void giangVienKhongPhaiHuongDanTaiBaoCaoBiChan() throws Exception {
		mockMvc.perform(get("/reports/" + report.getId() + "/download"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/reports"))
				.andExpect(flash().attributeExists("error"));
	}

	@Test
	@WithMockUser(username = "GV001", roles = "LECTURER")
	void taiBaoCaoKhongTonTaiChuyenHuong() throws Exception {
		mockMvc.perform(get("/reports/99999/download"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/reports"))
				.andExpect(flash().attributeExists("error"));
	}
}
