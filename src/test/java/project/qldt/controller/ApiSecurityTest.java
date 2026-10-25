package project.qldt.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ApiSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("Không xác thực gọi API trả 401 JSON, không redirect /login")
	void unauthenticatedReturnsJson401() throws Exception {
		mockMvc.perform(get("/api/v1/topics"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.path").value("/api/v1/topics"));
	}

	@Test
	@DisplayName("HTTP Basic xác thực thành công và không tạo session")
	void basicAuthWorksWithoutSession() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me").with(httpBasic("admin", "123456")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("admin"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(header().doesNotExist("Set-Cookie"));
	}

	@Test
	@WithMockUser(username = "SV001", roles = "STUDENT")
	@DisplayName("Sinh viên gọi API quản trị tài khoản bị 403 JSON")
	void studentCannotListAccounts() throws Exception {
		mockMvc.perform(get("/api/v1/accounts"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	@DisplayName("ADMIN lấy danh sách tài khoản, lọc theo loại và từ khóa")
	void adminListsAccounts() throws Exception {
		mockMvc.perform(get("/api/v1/accounts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].password").doesNotExist());

		mockMvc.perform(get("/api/v1/accounts").param("type", "LECTURER").param("q", "GV001"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].username").value("GV001"))
				.andExpect(jsonPath("$[0].type").value("LECTURER"));
	}
}
