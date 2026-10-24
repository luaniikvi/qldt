package project.qldt.controller;

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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import project.qldt.model.account.Account;
import project.qldt.model.account.Lecturer;
import project.qldt.model.account.Student;
import project.qldt.service.account.AccountService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class AccountAdminControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccountService accountService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void xemDanhSachTaiKhoan() throws Exception {
		mockMvc.perform(get("/admin/accounts"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/accounts/list"))
				.andExpect(model().attributeExists("accounts"));
	}

	@Test
	void xemFormTaoTaiKhoan() throws Exception {
		mockMvc.perform(get("/admin/accounts/new"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/accounts/form"))
				.andExpect(model().attributeExists("account"))
				.andExpect(model().attributeExists("accountTypes"));
	}

	@Test
	void taoTaiKhoanHopLeChuyenHuong() throws Exception {
		mockMvc.perform(post("/admin/accounts")
				.with(csrf())
				.param("type", "LECTURER")
				.param("code", "GV_MOCK_1")
				.param("fullName", "Giang Vien Mock")
				.param("email", "gvmock@qldt.local")
				.param("password", "123456")
				.param("confirmPassword", "123456")
				.param("department", "Công nghệ phần mềm"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/accounts"));

		Account account = accountService.findByUsername("GV_MOCK_1");
		assertTrue(account instanceof Lecturer);
	}

	@Test
	void taoTaiKhoanLoiValidationTraVeForm() throws Exception {
		mockMvc.perform(post("/admin/accounts")
				.with(csrf())
				.param("type", "LECTURER")
				.param("code", "")
				.param("fullName", "")
				.param("email", "khong_phai_email")
				.param("password", "123"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/accounts/form"))
				.andExpect(model().hasErrors());
	}

	@Test
	void khoaVaMoKhoaTaiKhoan() throws Exception {
		Student sv = accountService.createStudent("123456", "SV Toggle", "svtoggle@qldt.local", "SV_TOGGLE");

		mockMvc.perform(post("/admin/accounts/" + sv.getId() + "/lock")
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/accounts"));

		assertFalse(accountService.findById(sv.getId()).isEnabled());

		mockMvc.perform(post("/admin/accounts/" + sv.getId() + "/unlock")
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/accounts"));

		assertTrue(accountService.findById(sv.getId()).isEnabled());
	}

	@Test
	void boNhiemDeanChoGiangVien() throws Exception {
		Lecturer gv = accountService.createLecturer("123456", "GV Dean", "dean2@qldt.local", "GV_DEAN_2", "CNPM");

		mockMvc.perform(post("/admin/accounts/" + gv.getId() + "/dean")
				.with(csrf())
				.param("dean", "true"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/accounts"));

		assertTrue(accountService.findLecturerByCode("GV_DEAN_2").isDean());
	}

	@Test
	void datLaiMatKhauHopLeVaKhongHopLe() throws Exception {
		Student sv = accountService.createStudent("123456", "SV Pass", "svpass@qldt.local", "SV_PASS");

		mockMvc.perform(post("/admin/accounts/" + sv.getId() + "/reset-password")
				.with(csrf())
				.param("password", "newpassword123"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/accounts"))
				.andExpect(flash().attributeExists("message"));

		assertTrue(passwordEncoder.matches("newpassword123", accountService.findById(sv.getId()).getPassword()));

		mockMvc.perform(post("/admin/accounts/" + sv.getId() + "/reset-password")
				.with(csrf())
				.param("password", "123"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/accounts"))
				.andExpect(flash().attributeExists("error"));
	}
}
