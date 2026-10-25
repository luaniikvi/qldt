package project.qldt.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class StatisticsApiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistrationPeriodService periodService;

	private RegistrationPeriod period;

	@BeforeEach
	void setUp() {
		LocalDateTime now = LocalDateTime.now();
		RegistrationPeriodRequest req = new RegistrationPeriodRequest();
		req.setName("Đợt thống kê API test " + System.nanoTime());
		req.setType(PeriodType.COURSE_PROJECT);
		req.setTeacherRegisterStart(now.minusDays(5));
		req.setTeacherRegisterEnd(now.minusDays(3));
		req.setStudentRegisterStart(now.minusDays(2));
		req.setStudentRegisterEnd(now.plusDays(5));
		req.setReportDeadline(LocalDate.now().plusDays(10));
		period = periodService.create(req);
	}

	@Test
	@WithMockUser(username = "dean", roles = "DEAN")
	@DisplayName("GET /api/statistics với quyền DEAN trả về 200 OK và cấu trúc dữ liệu thống kê")
	void getStatistics_AsDean_Success() throws Exception {
		mockMvc.perform(get("/api/statistics")
				.param("periodId", String.valueOf(period.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.summary").exists())
				.andExpect(jsonPath("$.summary.topicCount").isNumber())
				.andExpect(jsonPath("$.rows").isArray());
	}

	@Test
	@WithMockUser(username = "gv001", roles = "LECTURER")
	@DisplayName("GET /api/statistics với quyền LECTURER bị 403 Forbidden")
	void getStatistics_AsLecturer_Forbidden() throws Exception {
		mockMvc.perform(get("/api/statistics")
				.param("periodId", String.valueOf(period.getId())))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "sv001", roles = "STUDENT")
	@DisplayName("GET /api/statistics với quyền STUDENT bị 403 Forbidden")
	void getStatistics_AsStudent_Forbidden() throws Exception {
		mockMvc.perform(get("/api/statistics")
				.param("periodId", String.valueOf(period.getId())))
				.andExpect(status().isForbidden());
	}
}
