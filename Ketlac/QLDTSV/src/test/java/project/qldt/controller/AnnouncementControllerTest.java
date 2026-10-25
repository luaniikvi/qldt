package project.qldt.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.model.announcement.Announcement;
import project.qldt.service.announcement.AnnouncementService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class AnnouncementControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AnnouncementService announcementService;

	@Test
	@WithMockUser(username = "sv001", roles = "STUDENT")
	@DisplayName("Sinh viên xem danh sách thông báo thành công")
	void userCanViewAnnouncementList() throws Exception {
		announcementService.create("Thông báo kỳ thi", "Nội dung kỳ thi");
		mockMvc.perform(get("/announcements"))
				.andExpect(status().isOk())
				.andExpect(view().name("announcements/list"))
				.andExpect(model().attributeExists("announcements"));
	}

	@Test
	@WithMockUser(username = "sv001", roles = "STUDENT")
	@DisplayName("Sinh viên xem chi tiết thông báo thành công")
	void userCanViewAnnouncementDetail() throws Exception {
		Announcement a = announcementService.create("Thông báo chi tiết", "Nội dung chi tiết");
		mockMvc.perform(get("/announcements/" + a.getId()))
				.andExpect(status().isOk())
				.andExpect(view().name("announcements/detail"))
				.andExpect(model().attributeExists("announcement"));
	}

	@Test
	@DisplayName("Người dùng chưa đăng nhập xem thông báo bị chặn 401 Unauthorized")
	void anonymousCannotViewAnnouncements() throws Exception {
		mockMvc.perform(get("/announcements"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	@DisplayName("Admin xem danh sách quản lý thông báo")
	void adminCanViewAdminAnnouncementList() throws Exception {
		mockMvc.perform(get("/admin/announcements"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/announcements/list"))
				.andExpect(model().attributeExists("announcements"));
	}

	@Test
	@WithMockUser(username = "sv001", roles = "STUDENT")
	@DisplayName("Sinh viên truy cập trang quản lý thông báo bị 403 Forbidden")
	void studentCannotAccessAdminAnnouncements() throws Exception {
		mockMvc.perform(get("/admin/announcements"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	@DisplayName("Admin xem form tạo thông báo mới")
	void adminCanViewCreateForm() throws Exception {
		mockMvc.perform(get("/admin/announcements/new"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/announcements/form"))
				.andExpect(model().attributeExists("announcement"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	@DisplayName("Admin đăng thông báo hợp lệ thành công")
	void adminCreateAnnouncement_Success() throws Exception {
		mockMvc.perform(post("/admin/announcements")
				.with(csrf())
				.param("title", "Tiêu đề mới")
				.param("content", "Nội dung mới"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/announcements"))
				.andExpect(flash().attributeExists("message"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	@DisplayName("Admin đăng thông báo tiêu đề trống bị báo lỗi và redirect về form new")
	void adminCreateAnnouncement_EmptyTitle() throws Exception {
		mockMvc.perform(post("/admin/announcements")
				.with(csrf())
				.param("title", "")
				.param("content", "Nội dung hợp lệ"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/announcements/new"))
				.andExpect(flash().attributeExists("error"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	@DisplayName("Admin xem form sửa thông báo")
	void adminCanViewEditForm() throws Exception {
		Announcement a = announcementService.create("Cần sửa", "Nội dung cũ");
		mockMvc.perform(get("/admin/announcements/" + a.getId() + "/edit"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/announcements/form"))
				.andExpect(model().attributeExists("announcement"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	@DisplayName("Admin cập nhật thông báo thành công")
	void adminUpdateAnnouncement_Success() throws Exception {
		Announcement a = announcementService.create("Tiêu đề", "Nội dung");
		mockMvc.perform(post("/admin/announcements/" + a.getId())
				.with(csrf())
				.param("title", "Tiêu đề đã sửa")
				.param("content", "Nội dung đã sửa"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/announcements"))
				.andExpect(flash().attributeExists("message"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	@DisplayName("Admin cập nhật thông báo với nội dung rỗng báo lỗi")
	void adminUpdateAnnouncement_EmptyContent() throws Exception {
		Announcement a = announcementService.create("Tiêu đề", "Nội dung");
		mockMvc.perform(post("/admin/announcements/" + a.getId())
				.with(csrf())
				.param("title", "Tiêu đề hợp lệ")
				.param("content", ""))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/announcements/" + a.getId() + "/edit"))
				.andExpect(flash().attributeExists("error"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	@DisplayName("Admin xóa thông báo thành công")
	void adminDeleteAnnouncement_Success() throws Exception {
		Announcement a = announcementService.create("Tiêu đề xóa", "Nội dung xóa");
		mockMvc.perform(post("/admin/announcements/" + a.getId() + "/delete")
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/announcements"))
				.andExpect(flash().attributeExists("message"));
	}
}
