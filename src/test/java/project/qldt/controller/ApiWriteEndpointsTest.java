package project.qldt.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import project.qldt.dto.topic.TopicRequest;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.topic.Topic;
import project.qldt.service.account.AccountService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.topic.TopicService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class ApiWriteEndpointsTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistrationPeriodService periodService;

	@Autowired
	private TopicService topicService;

	@Autowired
	private AccountService accountService;

	private RegistrationPeriod period;

	@BeforeEach
	void setUp() {
		LocalDateTime now = LocalDateTime.now();
		RegistrationPeriodRequest request = new RegistrationPeriodRequest();
		request.setName("Đợt API ghi " + System.nanoTime());
		request.setType(PeriodType.COURSE_PROJECT);
		request.setTeacherRegisterStart(now.minusDays(1));
		request.setTeacherRegisterEnd(now.plusDays(1));
		request.setStudentRegisterStart(now.plusDays(2));
		request.setStudentRegisterEnd(now.plusDays(5));
		request.setReportDeadline(LocalDate.now().plusDays(10));
		period = periodService.create(request);
	}

	@Test
	@WithMockUser(username = "GV001", roles = "LECTURER")
	@DisplayName("Giảng viên tạo đề tài trong thời gian đăng ký")
	void lecturerCreatesTopic() throws Exception {
		String json = """
				{"name":"Đề tài API","description":"Mô tả đề tài","periodId":%d}
				""".formatted(period.getId());

		mockMvc.perform(post("/api/v1/topics").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Đề tài API"))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.primaryAdvisor.username").value("GV001"));
	}

	@Test
	@WithMockUser(username = "SV001", roles = "STUDENT")
	@DisplayName("Sinh viên không được tạo đề tài")
	void studentCannotCreateTopic() throws Exception {
		String json = """
				{"name":"Đề tài cấm","description":"Mô tả","periodId":%d}
				""".formatted(period.getId());

		mockMvc.perform(post("/api/v1/topics").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "GV001", roles = {"LECTURER", "DEAN"})
	@DisplayName("DEAN duyệt đề tài qua PATCH approval")
	void deanApprovesTopic() throws Exception {
		TopicRequest request = new TopicRequest();
		request.setName("Đề tài chờ duyệt " + System.nanoTime());
		request.setDescription("Mô tả");
		request.setPeriodId(period.getId());
		Topic topic = topicService.create(request, accountService.findLecturerByCode("GV001"));

		mockMvc.perform(patch("/api/v1/topics/" + topic.getId() + "/approval")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"approved\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"));
	}

	@Test
	@WithMockUser(username = "GV001", roles = {"LECTURER", "DEAN"})
	@DisplayName("Duyệt đề tài thiếu trường approved trả 400 VALIDATION_ERROR")
	void approvalRequiresApprovedField() throws Exception {
		TopicRequest request = new TopicRequest();
		request.setName("Đề tài thiếu approved " + System.nanoTime());
		request.setDescription("Mô tả");
		request.setPeriodId(period.getId());
		Topic topic = topicService.create(request, accountService.findLecturerByCode("GV001"));

		mockMvc.perform(patch("/api/v1/topics/" + topic.getId() + "/approval")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	@DisplayName("ADMIN tạo tài khoản, response không chứa mật khẩu")
	void adminCreatesAccount() throws Exception {
		String code = "SV" + (System.nanoTime() % 100000);
		String json = """
				{"type":"STUDENT","code":"%s","fullName":"Sinh viên API","email":"svapi@qldt.local",
				 "password":"123456","confirmPassword":"123456"}
				""".formatted(code);

		mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value(code))
				.andExpect(jsonPath("$.type").value("STUDENT"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.studentCode").value(code));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	@DisplayName("Tạo tài khoản thiếu dữ liệu trả 400 VALIDATION_ERROR")
	void createAccountValidationFails() throws Exception {
		mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"type\":\"STUDENT\",\"fullName\":\"Thiếu mã\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors").isArray());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	@DisplayName("ADMIN tạo và xóa thông báo")
	void adminCreatesAndDeletesAnnouncement() throws Exception {
		String response = mockMvc.perform(post("/api/v1/announcements")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Thông báo API\",\"content\":\"Nội dung\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("Thông báo API"))
				.andReturn().getResponse().getContentAsString();
		long id = Long.parseLong(response.replaceAll(".*\"id\":(\\d+).*", "$1"));

		mockMvc.perform(get("/api/v1/announcements/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Thông báo API"));

		mockMvc.perform(delete("/api/v1/announcements/" + id))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/announcements/" + id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("NOT_FOUND"));
	}

	@Test
	@WithMockUser(username = "SV001", roles = "STUDENT")
	@DisplayName("Sinh viên không được tạo thông báo")
	void studentCannotCreateAnnouncement() throws Exception {
		mockMvc.perform(post("/api/v1/announcements").contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Cấm\",\"content\":\"Nội dung\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	@DisplayName("Sai Content-Type trả 415")
	void unsupportedMediaType() throws Exception {
		mockMvc.perform(post("/api/v1/announcements").contentType(MediaType.TEXT_PLAIN)
				.content("noi dung"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.error").value("UNSUPPORTED_MEDIA_TYPE"));
	}

	@Test
	@WithMockUser(username = "SV001", roles = "STUDENT")
	@DisplayName("Nhóm trưởng tạo nhóm, mời và sinh viên khác chấp nhận lời mời")
	void teamInvitationFlow() throws Exception {
		mockMvc.perform(post("/api/v1/teams").contentType(MediaType.APPLICATION_JSON)
				.content("{\"periodId\":" + period.getId() + "}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.leader.username").value("SV001"))
				.andExpect(jsonPath("$.members.length()").value(1));
	}
}
