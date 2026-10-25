package project.qldt.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.service.period.RegistrationPeriodService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class RegistrationPeriodRestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistrationPeriodService periodService;

	private RegistrationPeriod samplePeriod;

	@BeforeEach
	void setUp() {
		LocalDateTime now = LocalDateTime.now();
		RegistrationPeriodRequest request = new RegistrationPeriodRequest();
		request.setName("Đợt REST Test " + System.nanoTime());
		request.setType(PeriodType.COURSE_PROJECT);
		request.setTeacherRegisterStart(now.minusDays(5));
		request.setTeacherRegisterEnd(now.minusDays(3));
		request.setStudentRegisterStart(now.minusDays(2));
		request.setStudentRegisterEnd(now.plusDays(5));
		request.setReportDeadline(LocalDate.now().plusDays(10));
		samplePeriod = periodService.create(request);
	}

	@Test
	@WithMockUser(username = "sv001", roles = "STUDENT")
	@DisplayName("GET /api/v1/periods lấy danh sách đợt thành công")
	void getPeriods_Success() throws Exception {
		mockMvc.perform(get("/api/v1/periods"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	@WithMockUser(username = "sv001", roles = "STUDENT")
	@DisplayName("GET /api/v1/periods/{id} lấy chi tiết đợt tồn tại")
	void getPeriodById_Success() throws Exception {
		mockMvc.perform(get("/api/v1/periods/" + samplePeriod.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(samplePeriod.getId()))
				.andExpect(jsonPath("$.name").value(samplePeriod.getName()));
	}

	@Test
	@WithMockUser(username = "sv001", roles = "STUDENT")
	@DisplayName("GET /api/v1/periods/{id} với ID không tồn tại trả về 404 NOT_FOUND")
	void getPeriodById_NotFound() throws Exception {
		mockMvc.perform(get("/api/v1/periods/999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("NOT_FOUND"));
	}

	@Test
	@WithMockUser(username = "dean", roles = "DEAN")
	@DisplayName("POST /api/v1/periods với quyền DEAN tạo đợt thành công")
	void createPeriod_AsDean_Success() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		String json = """
				{
					"name": "Đợt tạo mới qua API",
					"type": "COURSE_PROJECT",
					"teacherRegisterStart": "%s",
					"teacherRegisterEnd": "%s",
					"studentRegisterStart": "%s",
					"studentRegisterEnd": "%s",
					"reportDeadline": "%s"
				}
				""".formatted(now.plusDays(1), now.plusDays(3), now.plusDays(4), now.plusDays(10),
				LocalDate.now().plusDays(20));

		mockMvc.perform(post("/api/v1/periods")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Đợt tạo mới qua API"));
	}

	@Test
	@WithMockUser(username = "sv001", roles = "STUDENT")
	@DisplayName("POST /api/v1/periods với quyền STUDENT bị 403 Forbidden")
	void createPeriod_AsStudent_Forbidden() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		String json = """
				{
					"name": "Đợt test cấm",
					"type": "COURSE_PROJECT",
					"teacherRegisterStart": "%s",
					"teacherRegisterEnd": "%s",
					"studentRegisterStart": "%s",
					"studentRegisterEnd": "%s",
					"reportDeadline": "%s"
				}
				""".formatted(
				now.plusDays(1),
				now.plusDays(2),
				now.plusDays(3),
				now.plusDays(4),
				now.plusDays(5).toLocalDate());

		mockMvc.perform(post("/api/v1/periods")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "dean", roles = "DEAN")
	@DisplayName("POST /api/v1/periods với ngày sai thứ tự trả về 400 Bad Request")
	void createPeriod_InvalidDates_BadRequest() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		String json = """
				{
					"name": "Đợt ngày sai",
					"type": "COURSE_PROJECT",
					"teacherRegisterStart": "%s",
					"teacherRegisterEnd": "%s",
					"studentRegisterStart": "%s",
					"studentRegisterEnd": "%s",
					"reportDeadline": "%s"
				}
				""".formatted(now.plusDays(5), now.plusDays(2), now.plusDays(6), now.plusDays(10),
				LocalDate.now().plusDays(20));

		mockMvc.perform(post("/api/v1/periods")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_PERIOD"));
	}

	@Test
	@WithMockUser(username = "dean", roles = "DEAN")
	@DisplayName("PUT /api/v1/periods/{id} với quyền DEAN cập nhật thành công")
	void updatePeriod_AsDean_Success() throws Exception {
		LocalDateTime now = LocalDateTime.now();
		String json = """
				{
					"name": "Tên đợt đã cập nhật",
					"type": "COURSE_PROJECT",
					"teacherRegisterStart": "%s",
					"teacherRegisterEnd": "%s",
					"studentRegisterStart": "%s",
					"studentRegisterEnd": "%s",
					"reportDeadline": "%s"
				}
				""".formatted(now.minusDays(5), now.minusDays(3), now.minusDays(2), now.plusDays(5),
				LocalDate.now().plusDays(15));

		mockMvc.perform(put("/api/v1/periods/" + samplePeriod.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Tên đợt đã cập nhật"));
	}

	@Test
	@WithMockUser(username = "dean", roles = "DEAN")
	@DisplayName("DELETE /api/v1/periods/{id} với quyền DEAN xóa đợt thành công")
	void deletePeriod_AsDean_Success() throws Exception {
		mockMvc.perform(delete("/api/v1/periods/" + samplePeriod.getId()))
				.andExpect(status().isNoContent());
	}

	@Test
	@WithMockUser(username = "gv001", roles = "LECTURER")
	@DisplayName("DELETE /api/v1/periods/{id} với quyền LECTURER bị 403 Forbidden")
	void deletePeriod_AsLecturer_Forbidden() throws Exception {
		mockMvc.perform(delete("/api/v1/periods/" + samplePeriod.getId()))
				.andExpect(status().isForbidden());
	}
}
