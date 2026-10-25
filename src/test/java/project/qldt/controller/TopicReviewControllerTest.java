package project.qldt.controller;

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
import project.qldt.dto.topic.TopicRequest;
import project.qldt.model.account.Lecturer;
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
class TopicReviewControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccountService accountService;

	@Autowired
	private RegistrationPeriodService periodService;

	@Autowired
	private TopicService topicService;

	private RegistrationPeriod period;
	private Lecturer gv;
	private Topic topic;

	@BeforeEach
	void setUp() {
		String stamp = String.valueOf(System.nanoTime());
		gv = accountService.createLecturer("123456", "GV TopicReview", "gvtr" + stamp + "@test.local", "GVTR" + stamp,
				"CNPM");

		LocalDateTime now = LocalDateTime.now();
		RegistrationPeriodRequest req = new RegistrationPeriodRequest();
		req.setName("Đợt test TopicReview " + stamp);
		req.setType(PeriodType.COURSE_PROJECT);
		req.setTeacherRegisterStart(now.minusDays(5));
		req.setTeacherRegisterEnd(now.plusDays(2));
		req.setStudentRegisterStart(now.plusDays(3));
		req.setStudentRegisterEnd(now.plusDays(10));
		req.setReportDeadline(LocalDate.now().plusDays(20));
		period = periodService.create(req);

		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài chờ duyệt " + stamp);
		tr.setDescription("Mô tả");
		tr.setPeriodId(period.getId());
		topic = topicService.create(tr, gv);
	}

	@Test
	@WithMockUser(username = "dean", roles = "DEAN")
	@DisplayName("Trưởng khoa xem danh sách đề tài chờ duyệt thành công")
	void deanCanViewPendingTopics() throws Exception {
		mockMvc.perform(get("/dean/topics"))
				.andExpect(status().isOk())
				.andExpect(view().name("topics/pending"))
				.andExpect(model().attributeExists("topics"))
				.andExpect(model().attributeExists("periods"));
	}

	@Test
	@WithMockUser(username = "dean", roles = "DEAN")
	@DisplayName("Trưởng khoa duyệt đề tài thành công")
	void deanApproveTopic_Success() throws Exception {
		mockMvc.perform(post("/dean/topics/" + topic.getId() + "/approve")
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dean/topics"))
				.andExpect(flash().attributeExists("message"));
	}

	@Test
	@WithMockUser(username = "dean", roles = "DEAN")
	@DisplayName("Trưởng khoa từ chối đề tài có lý do thành công")
	void deanRejectTopic_Success() throws Exception {
		mockMvc.perform(post("/dean/topics/" + topic.getId() + "/reject")
				.with(csrf())
				.param("reason", "Đề tài trùng lặp với năm trước"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dean/topics"))
				.andExpect(flash().attributeExists("message"));
	}

	@Test
	@WithMockUser(username = "dean", roles = "DEAN")
	@DisplayName("Trưởng khoa từ chối đề tài không có lý do bị báo lỗi")
	void deanRejectTopic_EmptyReason_Fails() throws Exception {
		mockMvc.perform(post("/dean/topics/" + topic.getId() + "/reject")
				.with(csrf())
				.param("reason", ""))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dean/topics"))
				.andExpect(flash().attributeExists("error"));
	}

	@Test
	@WithMockUser(username = "dean", roles = "DEAN")
	@DisplayName("Trưởng khoa công bố các đề tài đã duyệt của đợt thành công")
	void deanPublishTopics_Success() throws Exception {
		topicService.approve(topic.getId());
		mockMvc.perform(post("/dean/topics/publish/" + period.getId())
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dean/topics"))
				.andExpect(flash().attributeExists("message"));
	}

	@Test
	@WithMockUser(username = "dean", roles = "DEAN")
	@DisplayName("Trưởng khoa công bố đợt không có đề tài nào đã duyệt bị báo lỗi")
	void deanPublishTopics_NoApproved_Fails() throws Exception {
		// Chưa duyệt đề tài nào
		mockMvc.perform(post("/dean/topics/publish/" + period.getId())
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dean/topics"))
				.andExpect(flash().attributeExists("error"));
	}

	@Test
	@WithMockUser(username = "dean", roles = "DEAN")
	@DisplayName("Trưởng khoa từ chối công bố các đề tài của đợt thành công kèm lý do")
	void deanRejectPeriod_Success() throws Exception {
		topicService.approve(topic.getId());
		mockMvc.perform(post("/dean/topics/reject-period/" + period.getId())
				.param("reason", "Chất lượng đề tài cần nâng cao thêm")
				.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dean/topics"))
				.andExpect(flash().attributeExists("message"));
	}
}
