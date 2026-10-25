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
import project.qldt.model.account.Student;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;
import project.qldt.service.account.AccountService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.reviewer.ReviewerService;
import project.qldt.service.team.TeamService;
import project.qldt.service.topic.TopicRegistrationService;
import project.qldt.service.topic.TopicService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class ReviewerControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccountService accountService;

	@Autowired
	private RegistrationPeriodService periodService;

	@Autowired
	private TopicService topicService;

	@Autowired
	private TeamService teamService;

	@Autowired
	private TopicRegistrationService registrationService;

	@Autowired
	private ReviewerService reviewerService;

	private RegistrationPeriod period;
	private Lecturer gv1;
	private Lecturer gvReviewer;
	private Student sv;
	private Topic topic;

	@BeforeEach
	void setUp() {
		String stamp = String.valueOf(System.nanoTime());
		gv1 = accountService.createLecturer("123456", "GV HD", "gvhd" + stamp + "@test.local", "GVHD_RC" + stamp,
				"CNPM");
		gvReviewer = accountService.createLecturer("123456", "GV PB", "gvpb" + stamp + "@test.local", "GVPB_RC" + stamp,
				"CNPM");
		sv = accountService.createStudent("123456", "SV Test", "svt" + stamp + "@test.local", "SVT_RC" + stamp);
		Student sv2 = accountService.createStudent("123456", "SV Test 2", "svt2" + stamp + "@test.local", "SVT_RC2" + stamp);
		Student sv3 = accountService.createStudent("123456", "SV Test 3", "svt3" + stamp + "@test.local", "SVT_RC3" + stamp);

		LocalDateTime now = LocalDateTime.now();
		RegistrationPeriodRequest req = new RegistrationPeriodRequest();
		req.setName("Đợt KLTN ReviewerCtrl " + stamp);
		req.setType(PeriodType.GRADUATION_THESIS);
		req.setTeacherRegisterStart(now.minusDays(2));
		req.setTeacherRegisterEnd(now.plusDays(2));
		req.setStudentRegisterStart(now.plusDays(3));
		req.setStudentRegisterEnd(now.plusDays(10));
		req.setReportDeadline(LocalDate.now().plusDays(15));
		req.setReviewerScoreDeadline(LocalDate.now().plusDays(20));
		req.setCouncilReportDate(LocalDate.now().plusDays(25));
		period = periodService.create(req);

		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài KLTN ReviewerCtrl " + stamp);
		tr.setDescription("Mô tả");
		tr.setPeriodId(period.getId());
		topic = topicService.create(tr, gv1);
		topicService.approve(topic.getId());
		topicService.publish(period.getId());

		period.setTeacherRegisterStart(now.minusDays(10));
		period.setTeacherRegisterEnd(now.minusDays(9));
		period.setStudentRegisterStart(now.minusDays(1));
		period.setStudentRegisterEnd(now.plusDays(1));

		Team team = teamService.create(period.getId(), sv);
		teamService.invite(team.getId(), sv2.getStudentCode(), sv);
		teamService.respondInvitation(team.getId(), sv2, true, null);
		teamService.invite(team.getId(), sv3.getStudentCode(), sv);
		teamService.respondInvitation(team.getId(), sv3, true, null);
		registrationService.register(team.getId(), topic.getId(), sv);
		registrationService.approve(topic.getId(), gv1);

		reviewerService.assign(topic.getId(), gvReviewer.getLecturerCode());
	}

	@Test
	@DisplayName("Giảng viên xem danh sách đề tài được phân công phản biện")
	void lecturerCanViewReviewerTopics() throws Exception {
		mockMvc.perform(get("/reviewer/topics")
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
						.user(gvReviewer.getUsername()).roles("LECTURER")))
				.andExpect(status().isOk())
				.andExpect(view().name("reviewer/topics"))
				.andExpect(model().attributeExists("topics"))
				.andExpect(model().attributeExists("reports"));
	}

	@Test
	@DisplayName("GVPB nhập điểm phản biện hợp lệ thành công")
	void reviewerSubmitScore_Success() throws Exception {
		mockMvc.perform(post("/reviewer/topics/" + topic.getId() + "/score")
				.with(csrf())
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
						.user(gvReviewer.getUsername()).roles("LECTURER"))
				.param("score", "8.5")
				.param("comment", "Báo cáo tốt"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/reviewer/topics"))
				.andExpect(flash().attributeExists("message"));
	}

	@Test
	@DisplayName("GVPB nhập điểm không hợp lệ (> 10) bị báo lỗi")
	void reviewerSubmitScore_InvalidScore_Fails() throws Exception {
		mockMvc.perform(post("/reviewer/topics/" + topic.getId() + "/score")
				.with(csrf())
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
						.user(gvReviewer.getUsername()).roles("LECTURER"))
				.param("score", "11.5")
				.param("comment", "Điểm vượt ngưỡng"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/reviewer/topics"))
				.andExpect(flash().attributeExists("error"));
	}

	@Test
	@DisplayName("Người dùng đăng nhập xem trang bảng điểm phản biện thành công")
	void authenticatedUserCanViewReviewerScores() throws Exception {
		mockMvc.perform(get("/reviewer-scores")
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
						.user(sv.getUsername()).roles("STUDENT")))
				.andExpect(status().isOk())
				.andExpect(view().name("reviewer/scores"))
				.andExpect(model().attributeExists("topics"))
				.andExpect(model().attributeExists("isDean"));
	}
}
