package project.qldt.controller;
import project.qldt.support.Beans;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class SecurityAccessTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void trangChuVaLoginCongKhai() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/home"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/login"))
				.andExpect(status().isOk());
	}

	@Test
	void chuaDangNhapVaoTrangBaoMatBiChuyenHuongLogin() throws Exception {
		mockMvc.perform(get("/admin/accounts").header("Accept", "text/html"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		mockMvc.perform(get("/periods").header("Accept", "text/html"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		mockMvc.perform(get("/topics").header("Accept", "text/html"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		mockMvc.perform(get("/teams").header("Accept", "text/html"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		mockMvc.perform(get("/councils").header("Accept", "text/html"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	void chuaDangNhapGoiKhongPhaiHtmlNhan401() throws Exception {
		mockMvc.perform(get("/admin/accounts"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/periods"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(username = "SV001", roles = "STUDENT")
	void sinhVienKhongVaoDuocQuanTriTaiKhoan() throws Exception {
		mockMvc.perform(get("/admin/accounts"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "GV002", roles = "LECTURER")
	void giangVienKhongVaoDuocQuanTriTaiKhoan() throws Exception {
		mockMvc.perform(get("/admin/accounts"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminVaoDuocQuanTriTaiKhoan() throws Exception {
		mockMvc.perform(get("/admin/accounts"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "SV001", roles = "STUDENT")
	void sinhVienKhongVaoDuocTaoDot() throws Exception {
		mockMvc.perform(get("/periods/new"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "GV002", roles = "LECTURER")
	void giangVienThuongKhongVaoDuocTaoDot() throws Exception {
		mockMvc.perform(get("/periods/new"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "GV001", roles = {"LECTURER", "DEAN"})
	void truongKhoaVaoDuocTaoDot() throws Exception {
		mockMvc.perform(get("/periods/new"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "SV001", roles = "STUDENT")
	void sinhVienKhongVaoDuocDeTaiCuaGiangVien() throws Exception {
		mockMvc.perform(get("/topics"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "GV002", roles = "LECTURER")
	void giangVienVaoDuocDeTaiCuaMinh() throws Exception {
		mockMvc.perform(get("/topics"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "GV002", roles = "LECTURER")
	void giangVienKhongVaoDuocNhomSinhVien() throws Exception {
		mockMvc.perform(get("/teams"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "SV001", roles = "STUDENT")
	void sinhVienVaoDuocNhomCuaMinh() throws Exception {
		mockMvc.perform(get("/teams"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "GV002", roles = "LECTURER")
	void giangVienKhongVaoDuocDuyetDeTaiCuaDean() throws Exception {
		mockMvc.perform(get("/dean/topics"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "GV001", roles = {"LECTURER", "DEAN"})
	void truongKhoaVaoDuocDuyetDeTai() throws Exception {
		mockMvc.perform(get("/dean/topics"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "SV001", roles = "STUDENT")
	void sinhVienKhongVaoDuocBaoCaoCuaGiangVien() throws Exception {
		mockMvc.perform(get("/reports"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "GV002", roles = "LECTURER")
	void giangVienVaoDuocBaoCao() throws Exception {
		mockMvc.perform(get("/reports"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void postKhongCoCsrfBiTuChoi() throws Exception {
		mockMvc.perform(post("/admin/accounts")
				.param("type", "STUDENT")
				.param("code", "SV888")
				.param("fullName", "SV")
				.param("email", "sv@qldt.local")
				.param("password", "123456"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void postCoCsrfDuocChapNhan() throws Exception {
		mockMvc.perform(post("/admin/accounts")
				.with(csrf())
				.param("type", "STUDENT")
				.param("code", "SV888")
				.param("fullName", "Sinh Vien Csrf")
				.param("email", "sv888@qldt.local")
				.param("password", "123456"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("/admin/accounts*"));
	}
}
