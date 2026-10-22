package project.qldt.controller;
import project.qldt.support.Beans;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import project.qldt.model.PeriodType;
import project.qldt.model.RegistrationPeriod;
import project.qldt.service.RegistrationPeriodService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
@WithMockUser(username = "GV001", roles = {"LECTURER", "DEAN"})
class RegistrationPeriodControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistrationPeriodService periodService;

	private RegistrationPeriod testPeriod;

	@BeforeEach
	void setUp() {
		RegistrationPeriodRequest req = new RegistrationPeriodRequest();
		req.setName("Đợt Controller Test");
		req.setType(PeriodType.COURSE);
		req.setTeacherRegisterStart(LocalDateTime.now().minusDays(5));
		req.setTeacherRegisterEnd(LocalDateTime.now().minusDays(1));
		req.setStudentRegisterStart(LocalDateTime.now());
		req.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));
		req.setReportDeadline(LocalDate.now().plusDays(10));
		testPeriod = periodService.create(req);
	}

	@Test
	void xemDanhSachDot() throws Exception {
		mockMvc.perform(get("/periods"))
				.andExpect(status().isOk())
				.andExpect(view().name("periods/list"))
				.andExpect(model().attributeExists("periods"));
	}

	@Test
	void xemFormTaoDot() throws Exception {
		mockMvc.perform(get("/periods/new"))
				.andExpect(status().isOk())
				.andExpect(view().name("periods/form"))
				.andExpect(model().attributeExists("period"))
				.andExpect(model().attributeExists("periodTypes"));
	}

	@Test
	void taoDotHopLe() throws Exception {
		mockMvc.perform(post("/periods")
				.with(csrf())
				.param("name", "Đợt Mới Test")
				.param("type", "COURSE")
				.param("teacherRegisterStart", "2027-01-01T08:00")
				.param("teacherRegisterEnd", "2027-01-10T17:00")
				.param("studentRegisterStart", "2027-01-11T08:00")
				.param("studentRegisterEnd", "2027-01-20T17:00")
				.param("reportDeadline", "2027-01-30"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/periods"));
	}

	@Test
	void taoDotLoiValidationTraVeForm() throws Exception {
		mockMvc.perform(post("/periods")
				.with(csrf())
				.param("name", "")
				.param("type", "COURSE"))
				.andExpect(status().isOk())
				.andExpect(view().name("periods/form"))
				.andExpect(model().hasErrors());
	}

	@Test
	void xemFormSuaDot() throws Exception {
		mockMvc.perform(get("/periods/" + testPeriod.getId() + "/edit"))
				.andExpect(status().isOk())
				.andExpect(view().name("periods/form"))
				.andExpect(model().attributeExists("period"));
	}

	@Test
	void suaDotHopLe() throws Exception {
		mockMvc.perform(post("/periods/" + testPeriod.getId())
				.with(csrf())
				.param("name", "Đợt Controller Test Sửa")
				.param("type", "COURSE")
				.param("teacherRegisterStart", "2027-01-01T08:00")
				.param("teacherRegisterEnd", "2027-01-10T17:00")
				.param("studentRegisterStart", "2027-01-11T08:00")
				.param("studentRegisterEnd", "2027-01-20T17:00")
				.param("reportDeadline", "2027-01-30"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/periods"));
	}

	@Test
	void khoaVaMoKhoaDotDangKy() throws Exception {
		mockMvc.perform(post("/periods/" + testPeriod.getId() + "/lock")
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/periods"));

		assertTrue(periodService.findById(testPeriod.getId()).isRegistrationLocked());

		mockMvc.perform(post("/periods/" + testPeriod.getId() + "/unlock")
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/periods"));

		assertFalse(periodService.findById(testPeriod.getId()).isRegistrationLocked());
	}

	@Test
	void xoaDotDangKy() throws Exception {
		RegistrationPeriodRequest req = new RegistrationPeriodRequest();
		req.setName("Đợt Sẽ Xóa");
		req.setType(PeriodType.COURSE);
		req.setTeacherRegisterStart(LocalDateTime.now().minusDays(5));
		req.setTeacherRegisterEnd(LocalDateTime.now().minusDays(1));
		req.setStudentRegisterStart(LocalDateTime.now());
		req.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));
		req.setReportDeadline(LocalDate.now().plusDays(10));
		RegistrationPeriod toDelete = periodService.create(req);

		mockMvc.perform(post("/periods/" + toDelete.getId() + "/delete")
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/periods"));
	}

	@Test
	void suaDotKhongTonTaiChuyenHuong() throws Exception {
		mockMvc.perform(get("/periods/99999/edit"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/periods"))
				.andExpect(flash().attributeExists("error"));
	}
}
