package project.qldt.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.service.account.AccountService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class AccountSettingsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccountService accountService;

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void testGetChangePasswordPage() throws Exception {
		mockMvc.perform(get("/account/change-password"))
				.andExpect(status().isOk())
				.andExpect(view().name("account/change-password"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void testPostChangePasswordSuccess() throws Exception {
		mockMvc.perform(post("/account/change-password")
				.with(csrf())
				.param("currentPassword", "123456")
				.param("newPassword", "newpass123")
				.param("confirmPassword", "newpass123"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/account/change-password"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void testPostChangePasswordWrongCurrent() throws Exception {
		mockMvc.perform(post("/account/change-password")
				.with(csrf())
				.param("currentPassword", "wrongpass")
				.param("newPassword", "newpass123")
				.param("confirmPassword", "newpass123"))
				.andExpect(status().isOk())
				.andExpect(view().name("account/change-password"));
	}
}
