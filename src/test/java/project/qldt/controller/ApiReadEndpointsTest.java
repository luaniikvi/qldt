package project.qldt.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.service.account.AccountService;
import project.qldt.service.period.RegistrationPeriodService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class ApiReadEndpointsTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistrationPeriodService periodService;

	@Autowired
	private AccountService accountService;

	private RegistrationPeriod period;

	@BeforeEach
	void setUp() {
		LocalDateTime now = LocalDateTime.now();
		RegistrationPeriodRequest request = new RegistrationPeriodRequest();
		request.setName("Đợt API đọc " + System.nanoTime());
		request.setType(PeriodType.COURSE_PROJECT);
		request.setTeacherRegisterStart(now.minusDays(5));
		request.setTeacherRegisterEnd(now.minusDays(3));
		request.setStudentRegisterStart(now.minusDays(2));
		request.setStudentRegisterEnd(now.plusDays(5));
		request.setReportDeadline(LocalDate.now().plusDays(10));
		period = periodService.create(request);
	}

	@Test
	@WithMockUser(username = "GV001", roles = "LECTURER")
	@DisplayName("Giảng viên lấy thông tin cá nhân và bộ môn")
	void lecturerReadsOwnProfileAndDepartments() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("GV001"))
				.andExpect(jsonPath("$.roles[0]").value("LECTURER"));

		mockMvc.perform(get("/api/v1/departments"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].code").exists());
	}

	@Test
	@WithMockUser(username = "SV001", roles = "STUDENT")
	@DisplayName("Đọc danh sách đợt, đề tài, thông báo, thông báo cá nhân")
	void studentReadsCommonResources() throws Exception {
		mockMvc.perform(get("/api/v1/periods"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());

		mockMvc.perform(get("/api/v1/periods/" + period.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(period.getId()));

		mockMvc.perform(get("/api/v1/topics").param("periodId", String.valueOf(period.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());

		mockMvc.perform(get("/api/v1/announcements").param("limit", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());

		mockMvc.perform(get("/api/v1/notifications"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());

		mockMvc.perform(get("/api/v1/notifications/unread-count"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unread").isNumber());
	}

	@Test
	@WithMockUser(username = "GV001", roles = {"LECTURER", "DEAN"})
	@DisplayName("DEAN xem thống kê và xuất CSV/xlsx qua API")
	void deanReadsStatistics() throws Exception {
		mockMvc.perform(get("/api/v1/statistics").param("periodId", String.valueOf(period.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.summary.topicCount").isNumber())
				.andExpect(jsonPath("$.rows").isArray());

		mockMvc.perform(get("/api/v1/statistics/charts").param("periodId",
				String.valueOf(period.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.statusBars").isArray());

		mockMvc.perform(get("/api/v1/statistics/export.csv").param("periodId",
				String.valueOf(period.getId())))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", Matchers.containsString(".csv")));

		mockMvc.perform(get("/api/v1/statistics/export.xlsx").param("periodId",
				String.valueOf(period.getId())))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", Matchers.containsString(".xlsx")));
	}

	@Test
	@WithMockUser(username = "GV001", roles = {"LECTURER", "DEAN"})
	@DisplayName("DEAN xem nhóm và đăng ký theo đợt sau khi được gán vai trò trưởng khoa")
	void deanListsTeamsByPeriod() throws Exception {
		accountService.assignDean("GV001", true);

		mockMvc.perform(get("/api/v1/teams").param("periodId", String.valueOf(period.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());

		mockMvc.perform(get("/api/v1/registrations").param("periodId", String.valueOf(period.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	@WithMockUser(username = "SV001", roles = "STUDENT")
	@DisplayName("Sinh viên xem danh sách nhóm của mình")
	void studentListsOwnTeams() throws Exception {
		mockMvc.perform(get("/api/v1/teams"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	@WithMockUser(username = "SV001", roles = "STUDENT")
	@DisplayName("Lấy đề tài không tồn tại trả 404 NOT_FOUND")
	void topicNotFound() throws Exception {
		mockMvc.perform(get("/api/v1/topics/999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("NOT_FOUND"));
	}
}
