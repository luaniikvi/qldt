package project.qldt.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.account.AccountRequest;
import project.qldt.dto.account.ChangePasswordRequest;
import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.dto.topic.TopicRequest;
import project.qldt.exception.AccountNotFoundException;
import project.qldt.exception.CouncilNotFoundException;
import project.qldt.exception.InvalidAccountException;
import project.qldt.exception.InvalidCouncilException;
import project.qldt.exception.InvalidRegistrationException;
import project.qldt.exception.InvalidRegistrationPeriodException;
import project.qldt.exception.InvalidReportException;
import project.qldt.exception.InvalidTeamException;
import project.qldt.exception.InvalidTopicException;
import project.qldt.exception.ReportNotFoundException;
import project.qldt.exception.TeamNotFoundException;
import project.qldt.exception.TopicNotFoundException;
import project.qldt.model.account.AccountType;
import project.qldt.model.account.Lecturer;
import project.qldt.model.account.Student;
import project.qldt.model.council.Council;
import project.qldt.model.council.CouncilRole;
import project.qldt.model.council.TopicResult;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.report.Report;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;
import project.qldt.service.account.AccountService;
import project.qldt.service.council.CouncilService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.report.ReportService;
import project.qldt.service.reviewer.ReviewerService;
import project.qldt.service.team.TeamService;
import project.qldt.service.topic.TopicRegistrationService;
import project.qldt.service.topic.TopicService;
import project.qldt.support.Beans;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ComprehensiveBusinessRuleEdgeCasesTest {

	private AccountService accountService;
	private RegistrationPeriodService periodService;
	private TopicService topicService;
	private TeamService teamService;
	private TopicRegistrationService registrationService;
	private CouncilService councilService;
	private ReviewerService reviewerService;
	private ReportService reportService;

	private RegistrationPeriod thesisPeriod;
	private Lecturer gvCnpm1;
	private Lecturer gvCnpm2;
	private Lecturer gvHttt;
	private Student sv1;
	private Student sv2;
	private Student sv3;
	private Student sv4;
	private Student sv5;
	private Student sv6;

	@BeforeEach
	void setUp() {
		accountService = Beans.get(AccountService.class);
		periodService = Beans.get(RegistrationPeriodService.class);
		topicService = Beans.get(TopicService.class);
		teamService = Beans.get(TeamService.class);
		registrationService = Beans.get(TopicRegistrationService.class);
		councilService = Beans.get(CouncilService.class);
		reviewerService = Beans.get(ReviewerService.class);
		reportService = Beans.get(ReportService.class);

		String stamp = String.valueOf(System.nanoTime());
		gvCnpm1 = accountService.createLecturer("123456", "GV CNPM 1 " + stamp, "gv1" + stamp + "@test.local",
				"GVCP1" + stamp, "CNPM");
		gvCnpm2 = accountService.createLecturer("123456", "GV CNPM 2 " + stamp, "gv2" + stamp + "@test.local",
				"GVCP2" + stamp, "CNPM");
		gvHttt = accountService.createLecturer("123456", "GV HTTT " + stamp, "gv3" + stamp + "@test.local",
				"GVHT" + stamp, "HTTT");

		sv1 = accountService.createStudent("123456", "SV 1 " + stamp, "sv1" + stamp + "@test.local", "SVEDGE1" + stamp);
		sv2 = accountService.createStudent("123456", "SV 2 " + stamp, "sv2" + stamp + "@test.local", "SVEDGE2" + stamp);
		sv3 = accountService.createStudent("123456", "SV 3 " + stamp, "sv3" + stamp + "@test.local", "SVEDGE3" + stamp);
		sv4 = accountService.createStudent("123456", "SV 4 " + stamp, "sv4" + stamp + "@test.local", "SVEDGE4" + stamp);
		sv5 = accountService.createStudent("123456", "SV 5 " + stamp, "sv5" + stamp + "@test.local", "SVEDGE5" + stamp);
		sv6 = accountService.createStudent("123456", "SV 6 " + stamp, "sv6" + stamp + "@test.local", "SVEDGE6" + stamp);

		LocalDateTime now = LocalDateTime.now();
		RegistrationPeriodRequest req = new RegistrationPeriodRequest();
		req.setName("Đợt KLTN Edge Cases " + stamp);
		req.setType(PeriodType.GRADUATION_THESIS);
		req.setTeacherRegisterStart(now.minusDays(2));
		req.setTeacherRegisterEnd(now.plusDays(2));
		req.setStudentRegisterStart(now.plusDays(3));
		req.setStudentRegisterEnd(now.plusDays(10));
		req.setReportDeadline(LocalDate.now().plusDays(15));
		req.setReviewerScoreDeadline(LocalDate.now().plusDays(20));
		req.setCouncilReportDate(LocalDate.now().plusDays(25));
		thesisPeriod = periodService.create(req);
	}

	private void moveToStudentPhase(RegistrationPeriod period) {
		LocalDateTime now = LocalDateTime.now();
		period.setTeacherRegisterStart(now.minusDays(10));
		period.setTeacherRegisterEnd(now.minusDays(9));
		period.setStudentRegisterStart(now.minusDays(1));
		period.setStudentRegisterEnd(now.plusDays(1));
	}

	private Team createTeamWithThreeMembers(RegistrationPeriod period, Student leader, Student m1, Student m2) {
		Team team = teamService.create(period.getId(), leader);
		teamService.invite(team.getId(), m1.getStudentCode(), leader);
		teamService.respondInvitation(team.getId(), m1, true, null);
		teamService.invite(team.getId(), m2.getStudentCode(), leader);
		teamService.respondInvitation(team.getId(), m2, true, null);
		return team;
	}

	// ==================== 1. TEAM SERVICE EDGE CASES ====================

	@Test
	@DisplayName("Team: Sinh viên đã thuộc nhóm không được tạo nhóm mới trong cùng đợt")
	void team_StudentAlreadyInTeam_CannotCreateAnother() {
		teamService.create(thesisPeriod.getId(), sv1);
		assertThrows(InvalidTeamException.class, () -> teamService.create(thesisPeriod.getId(), sv1));
	}

	@Test
	@DisplayName("Team: Nhóm trưởng không thể mời chính mình")
	void team_CannotInviteSelf() {
		moveToStudentPhase(thesisPeriod);
		Team team = teamService.create(thesisPeriod.getId(), sv1);
		assertThrows(InvalidTeamException.class, () -> teamService.invite(team.getId(), sv1.getStudentCode(), sv1));
	}

	@Test
	@DisplayName("Team: Không thể mời sinh viên đã là thành viên trong nhóm")
	void team_CannotInviteExistingMember() {
		moveToStudentPhase(thesisPeriod);
		Team team = teamService.create(thesisPeriod.getId(), sv1);
		teamService.invite(team.getId(), sv2.getStudentCode(), sv1);
		teamService.respondInvitation(team.getId(), sv2, true, null);

		assertThrows(InvalidTeamException.class, () -> teamService.invite(team.getId(), sv2.getStudentCode(), sv1));
	}

	@Test
	@DisplayName("Team: Không thể mời sinh viên đang có lời mời pending")
	void team_CannotInvitePendingStudent() {
		moveToStudentPhase(thesisPeriod);
		Team team = teamService.create(thesisPeriod.getId(), sv1);
		teamService.invite(team.getId(), sv2.getStudentCode(), sv1);

		assertThrows(InvalidTeamException.class, () -> teamService.invite(team.getId(), sv2.getStudentCode(), sv1));
	}

	@Test
	@DisplayName("Team: Không thể mời quá sĩ số tối đa 5 người (bao gồm cả lời mời đang chờ)")
	void team_CannotExceedMaxMembersWithInvitations() {
		moveToStudentPhase(thesisPeriod);
		Team team = teamService.create(thesisPeriod.getId(), sv1);
		teamService.invite(team.getId(), sv2.getStudentCode(), sv1);
		teamService.invite(team.getId(), sv3.getStudentCode(), sv1);
		teamService.invite(team.getId(), sv4.getStudentCode(), sv1);
		teamService.invite(team.getId(), sv5.getStudentCode(), sv1);

		assertThrows(InvalidTeamException.class, () -> teamService.invite(team.getId(), sv6.getStudentCode(), sv1));
	}

	@Test
	@DisplayName("Team: Từ chối lời mời vào nhóm mà không có lý do bị ném ngoại lệ")
	void team_RejectInvitationWithoutReason_Fails() {
		moveToStudentPhase(thesisPeriod);
		Team team = teamService.create(thesisPeriod.getId(), sv1);
		teamService.invite(team.getId(), sv2.getStudentCode(), sv1);

		assertThrows(InvalidTeamException.class, () -> teamService.respondInvitation(team.getId(), sv2, false, ""));
		assertThrows(InvalidTeamException.class, () -> teamService.respondInvitation(team.getId(), sv2, false, null));
	}

	@Test
	@DisplayName("Team: Chuyển quyền nhóm trưởng cho sinh viên không phải thành viên ném ngoại lệ")
	void team_TransferLeaderToNonMember_Fails() {
		moveToStudentPhase(thesisPeriod);
		Team team = teamService.create(thesisPeriod.getId(), sv1);
		assertThrows(InvalidTeamException.class,
				() -> teamService.transferLeader(team.getId(), sv2.getStudentCode(), sv1));
	}

	@Test
	@DisplayName("Team: Nhóm trưởng không được rời nhóm khi chưa chuyển quyền")
	void team_LeaderCannotLeaveWithoutTransferring() {
		moveToStudentPhase(thesisPeriod);
		Team team = teamService.create(thesisPeriod.getId(), sv1);
		assertThrows(InvalidTeamException.class, () -> teamService.leave(team.getId(), sv1));
	}

	@Test
	@DisplayName("Team: Người không phải thành viên rời nhóm ném ngoại lệ")
	void team_NonMemberCannotLeave() {
		moveToStudentPhase(thesisPeriod);
		Team team = teamService.create(thesisPeriod.getId(), sv1);
		assertThrows(InvalidTeamException.class, () -> teamService.leave(team.getId(), sv2));
	}

	@Test
	@DisplayName("Team: Nhóm trưởng không thể tự xóa mình khỏi nhóm")
	void team_LeaderCannotRemoveSelf() {
		moveToStudentPhase(thesisPeriod);
		Team team = teamService.create(thesisPeriod.getId(), sv1);
		assertThrows(InvalidTeamException.class,
				() -> teamService.removeMember(team.getId(), sv1.getStudentCode(), sv1));
	}

	@Test
	@DisplayName("Team: Hủy lời mời không tồn tại ném ngoại lệ")
	void team_CancelNonExistentInvitation_Fails() {
		moveToStudentPhase(thesisPeriod);
		Team team = teamService.create(thesisPeriod.getId(), sv1);
		assertThrows(InvalidTeamException.class,
				() -> teamService.cancelInvitation(team.getId(), sv2.getStudentCode(), sv1));
	}

	@Test
	@DisplayName("Team: Tìm nhóm theo ID không tồn tại ném TeamNotFoundException")
	void team_FindByIdNotFound() {
		assertThrows(TeamNotFoundException.class, () -> teamService.findById(999999L));
	}

	// ==================== 2. TOPIC SERVICE EDGE CASES ====================

	@Test
	@DisplayName("Topic: Không thể mời chính mình làm giảng viên đồng hướng dẫn")
	void topic_CannotInviteSelfAsCoAdvisor() {
		LocalDateTime now = LocalDateTime.now();
		RegistrationPeriodRequest pr = new RegistrationPeriodRequest();
		pr.setName("Đợt test topic edge " + System.nanoTime());
		pr.setType(PeriodType.COURSE_PROJECT);
		pr.setTeacherRegisterStart(now.minusDays(1));
		pr.setTeacherRegisterEnd(now.plusDays(2));
		pr.setStudentRegisterStart(now.plusDays(3));
		pr.setStudentRegisterEnd(now.plusDays(10));
		pr.setReportDeadline(LocalDate.now().plusDays(20));
		RegistrationPeriod p = periodService.create(pr);

		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài test");
		tr.setDescription("Mô tả");
		tr.setPeriodId(p.getId());
		Topic topic = topicService.create(tr, gvCnpm1);

		assertThrows(InvalidTopicException.class,
				() -> topicService.inviteAdvisor(topic.getId(), gvCnpm1.getLecturerCode(), gvCnpm1));
	}

	@Test
	@DisplayName("Topic: Giảng viên được mời đồng hướng dẫn khác bộ môn bị ném ngoại lệ")
	void topic_CannotInviteAdvisorFromDifferentDepartment() {
		LocalDateTime now = LocalDateTime.now();
		RegistrationPeriodRequest pr = new RegistrationPeriodRequest();
		pr.setName("Đợt test dept edge " + System.nanoTime());
		pr.setType(PeriodType.COURSE_PROJECT);
		pr.setTeacherRegisterStart(now.minusDays(1));
		pr.setTeacherRegisterEnd(now.plusDays(2));
		pr.setStudentRegisterStart(now.plusDays(3));
		pr.setStudentRegisterEnd(now.plusDays(10));
		pr.setReportDeadline(LocalDate.now().plusDays(20));
		RegistrationPeriod p = periodService.create(pr);

		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài test dept");
		tr.setDescription("Mô tả");
		tr.setPeriodId(p.getId());
		Topic topic = topicService.create(tr, gvCnpm1);

		assertThrows(InvalidTopicException.class,
				() -> topicService.inviteAdvisor(topic.getId(), gvHttt.getLecturerCode(), gvCnpm1));
	}

	@Test
	@DisplayName("Topic: Từ chối lời mời đồng hướng dẫn không có lý do ném ngoại lệ")
	void topic_RejectAdvisorInvitationWithoutReason_Fails() {
		LocalDateTime now = LocalDateTime.now();
		RegistrationPeriodRequest pr = new RegistrationPeriodRequest();
		pr.setName("Đợt test reject inv " + System.nanoTime());
		pr.setType(PeriodType.COURSE_PROJECT);
		pr.setTeacherRegisterStart(now.minusDays(1));
		pr.setTeacherRegisterEnd(now.plusDays(2));
		pr.setStudentRegisterStart(now.plusDays(3));
		pr.setStudentRegisterEnd(now.plusDays(10));
		pr.setReportDeadline(LocalDate.now().plusDays(20));
		RegistrationPeriod p = periodService.create(pr);

		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài test inv");
		tr.setDescription("Mô tả");
		tr.setPeriodId(p.getId());
		Topic topic = topicService.create(tr, gvCnpm1);
		topicService.inviteAdvisor(topic.getId(), gvCnpm2.getLecturerCode(), gvCnpm1);

		assertThrows(InvalidTopicException.class,
				() -> topicService.respondInvitation(topic.getId(), gvCnpm2, false, "   "));
	}

	@Test
	@DisplayName("Topic: Không thể xóa GV đồng hướng dẫn khi đề tài chỉ có 1 GV")
	void topic_RemoveCoAdvisorWhenOnlyOne_Fails() {
		LocalDateTime now = LocalDateTime.now();
		RegistrationPeriodRequest pr = new RegistrationPeriodRequest();
		pr.setName("Đợt test remove advisor " + System.nanoTime());
		pr.setType(PeriodType.COURSE_PROJECT);
		pr.setTeacherRegisterStart(now.minusDays(1));
		pr.setTeacherRegisterEnd(now.plusDays(2));
		pr.setStudentRegisterStart(now.plusDays(3));
		pr.setStudentRegisterEnd(now.plusDays(10));
		pr.setReportDeadline(LocalDate.now().plusDays(20));
		RegistrationPeriod p = periodService.create(pr);

		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài test one advisor");
		tr.setDescription("Mô tả");
		tr.setPeriodId(p.getId());
		Topic topic = topicService.create(tr, gvCnpm1);

		assertThrows(InvalidTopicException.class,
				() -> topicService.removeCoAdvisor(topic.getId(), gvCnpm2.getLecturerCode(), gvCnpm1));
	}

	@Test
	@DisplayName("Topic: Không thể sửa đề tài khi đã được duyệt hoặc đã công bố")
	void topic_CannotEditApprovedOrPublishedTopic() {
		LocalDateTime now = LocalDateTime.now();
		RegistrationPeriodRequest pr = new RegistrationPeriodRequest();
		pr.setName("Đợt test edit topic " + System.nanoTime());
		pr.setType(PeriodType.COURSE_PROJECT);
		pr.setTeacherRegisterStart(now.minusDays(1));
		pr.setTeacherRegisterEnd(now.plusDays(2));
		pr.setStudentRegisterStart(now.plusDays(3));
		pr.setStudentRegisterEnd(now.plusDays(10));
		pr.setReportDeadline(LocalDate.now().plusDays(20));
		RegistrationPeriod p = periodService.create(pr);

		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài duyệt rồi");
		tr.setDescription("Mô tả");
		tr.setPeriodId(p.getId());
		Topic topic = topicService.create(tr, gvCnpm1);
		topicService.approve(topic.getId());

		TopicRequest editReq = new TopicRequest();
		editReq.setName("Tên mới");
		editReq.setDescription("Mô tả mới");
		editReq.setPeriodId(p.getId());

		assertThrows(InvalidTopicException.class, () -> topicService.update(topic.getId(), editReq, gvCnpm1));
		assertThrows(InvalidTopicException.class, () -> topicService.findForEdit(topic.getId(), gvCnpm1));
	}

	// ==================== 3. TOPIC REGISTRATION EDGE CASES ====================

	@Test
	@DisplayName("Registration: Nhóm đã có đề tài không thể đăng ký thêm đề tài khác")
	void registration_TeamAlreadyHasTopic_CannotRegisterAnother() {
		TopicRequest tr1 = new TopicRequest();
		tr1.setName("Đề tài 1");
		tr1.setDescription("Mô tả 1");
		tr1.setPeriodId(thesisPeriod.getId());
		Topic topic1 = topicService.create(tr1, gvCnpm1);
		topicService.approve(topic1.getId());

		TopicRequest tr2 = new TopicRequest();
		tr2.setName("Đề tài 2");
		tr2.setDescription("Mô tả 2");
		tr2.setPeriodId(thesisPeriod.getId());
		Topic topic2 = topicService.create(tr2, gvCnpm1);
		topicService.approve(topic2.getId());

		topicService.publish(thesisPeriod.getId());

		moveToStudentPhase(thesisPeriod);
		Team team = createTeamWithThreeMembers(thesisPeriod, sv1, sv2, sv3);
		registrationService.register(team.getId(), topic1.getId(), sv1);

		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.register(team.getId(), topic2.getId(), sv1));
	}

	@Test
	@DisplayName("Registration: Đăng ký đề tài chưa được công bố ném ngoại lệ")
	void registration_UnpublishedTopic_Fails() {
		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài chưa công bố");
		tr.setDescription("Mô tả");
		tr.setPeriodId(thesisPeriod.getId());
		Topic topic = topicService.create(tr, gvCnpm1);
		topicService.approve(topic.getId());
		// Không publish

		moveToStudentPhase(thesisPeriod);
		Team team = createTeamWithThreeMembers(thesisPeriod, sv1, sv2, sv3);
		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.register(team.getId(), topic.getId(), sv1));
	}

	@Test
	@DisplayName("Registration: Thành viên không phải nhóm trưởng đăng ký đề tài ném ngoại lệ")
	void registration_NonLeaderCannotRegister() {
		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài nhóm trưởng");
		tr.setDescription("Mô tả");
		tr.setPeriodId(thesisPeriod.getId());
		Topic topic = topicService.create(tr, gvCnpm1);
		topicService.approve(topic.getId());
		topicService.publish(thesisPeriod.getId());

		moveToStudentPhase(thesisPeriod);
		Team team = createTeamWithThreeMembers(thesisPeriod, sv1, sv2, sv3);

		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.register(team.getId(), topic.getId(), sv2));
	}

	@Test
	@DisplayName("Registration: Đăng ký đề tài khi nhóm có dưới 3 thành viên ném ngoại lệ")
	void registration_UnderThreeMembers_Fails() {
		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài dưới 3 SV");
		tr.setDescription("Mô tả");
		tr.setPeriodId(thesisPeriod.getId());
		Topic topic = topicService.create(tr, gvCnpm1);
		topicService.approve(topic.getId());
		topicService.publish(thesisPeriod.getId());

		moveToStudentPhase(thesisPeriod);
		Team team = teamService.create(thesisPeriod.getId(), sv1);
		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.register(team.getId(), topic.getId(), sv1));
	}

	@Test
	@DisplayName("Registration: Không thể hủy đăng ký khi đề tài đã được duyệt đăng ký")
	void registration_CannotCancelApprovedRegistration() {
		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài đã duyệt đk");
		tr.setDescription("Mô tả");
		tr.setPeriodId(thesisPeriod.getId());
		Topic topic = topicService.create(tr, gvCnpm1);
		topicService.approve(topic.getId());
		topicService.publish(thesisPeriod.getId());

		moveToStudentPhase(thesisPeriod);
		Team team = createTeamWithThreeMembers(thesisPeriod, sv1, sv2, sv3);
		registrationService.register(team.getId(), topic.getId(), sv1);
		registrationService.approve(topic.getId(), gvCnpm1);

		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.cancel(team.getId(), sv1));
	}

	@Test
	@DisplayName("Registration: Giảng viên không phải GVHD duyệt đăng ký ném ngoại lệ")
	void registration_NonAdvisorCannotApprove() {
		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài non-advisor");
		tr.setDescription("Mô tả");
		tr.setPeriodId(thesisPeriod.getId());
		Topic topic = topicService.create(tr, gvCnpm1);
		topicService.approve(topic.getId());
		topicService.publish(thesisPeriod.getId());

		moveToStudentPhase(thesisPeriod);
		Team team = createTeamWithThreeMembers(thesisPeriod, sv1, sv2, sv3);
		registrationService.register(team.getId(), topic.getId(), sv1);

		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.approve(topic.getId(), gvCnpm2));
	}

	@Test
	@DisplayName("Registration: Từ chối đăng ký đề tài không có lý do ném ngoại lệ")
	void registration_RejectWithoutReason_Fails() {
		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài reject no reason");
		tr.setDescription("Mô tả");
		tr.setPeriodId(thesisPeriod.getId());
		Topic topic = topicService.create(tr, gvCnpm1);
		topicService.approve(topic.getId());
		topicService.publish(thesisPeriod.getId());

		moveToStudentPhase(thesisPeriod);
		Team team = createTeamWithThreeMembers(thesisPeriod, sv1, sv2, sv3);
		registrationService.register(team.getId(), topic.getId(), sv1);

		assertThrows(InvalidRegistrationException.class,
				() -> registrationService.reject(topic.getId(), gvCnpm1, "  "));
	}

	// ==================== 4. COUNCIL & SCORING EDGE CASES ====================

	@Test
	@DisplayName("Council: Không thể lập hội đồng cho đề tài chưa được duyệt đăng ký")
	void council_TopicNotRegistered_CannotCreateCouncil() {
		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài chưa ai đăng ký");
		tr.setDescription("Mô tả");
		tr.setPeriodId(thesisPeriod.getId());
		Topic topic = topicService.create(tr, gvCnpm1);
		topicService.approve(topic.getId());
		topicService.publish(thesisPeriod.getId());

		assertThrows(InvalidCouncilException.class, () -> councilService.create(topic.getId(), "Hội đồng 1",
				List.of("GV001", "GV002", "GV003"),
				List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER)));
	}

	@Test
	@DisplayName("Council: GVHD không được tham gia hội đồng của đề tài mình")
	void council_AdvisorCannotBeMemberOfCouncil() {
		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài council advisor test");
		tr.setDescription("Mô tả");
		tr.setPeriodId(thesisPeriod.getId());
		Topic topic = topicService.create(tr, gvCnpm1);
		topicService.approve(topic.getId());
		topicService.publish(thesisPeriod.getId());

		moveToStudentPhase(thesisPeriod);
		Team team = createTeamWithThreeMembers(thesisPeriod, sv1, sv2, sv3);
		registrationService.register(team.getId(), topic.getId(), sv1);
		registrationService.approve(topic.getId(), gvCnpm1);

		assertThrows(InvalidCouncilException.class, () -> councilService.create(topic.getId(), "Hội đồng 1",
				List.of(gvCnpm1.getLecturerCode(), "GV002", "GV003"),
				List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER)));
	}

	@Test
	@DisplayName("Council: Hội đồng phải có đúng 1 Chủ tịch và 1 Thư ký")
	void council_MustHaveOneChairAndOneSecretary() {
		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài council roles test");
		tr.setDescription("Mô tả");
		tr.setPeriodId(thesisPeriod.getId());
		Topic topic = topicService.create(tr, gvCnpm1);
		topicService.approve(topic.getId());
		topicService.publish(thesisPeriod.getId());

		moveToStudentPhase(thesisPeriod);
		Team team = createTeamWithThreeMembers(thesisPeriod, sv1, sv2, sv3);
		registrationService.register(team.getId(), topic.getId(), sv1);
		registrationService.approve(topic.getId(), gvCnpm1);

		// 2 Chủ tịch, 0 Thư ký
		assertThrows(InvalidCouncilException.class, () -> councilService.create(topic.getId(), "Hội đồng 1",
				List.of("GV002", "GV003", "GV004"),
				List.of(CouncilRole.CHAIR, CouncilRole.CHAIR, CouncilRole.MEMBER)));
	}

	@Test
	@DisplayName("Council: Điểm số ngoài khoảng 0-10 hoặc có hơn 1 chữ số thập phân bị ném ngoại lệ")
	void council_InvalidScoreFormatOrRange() {
		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài score validate test");
		tr.setDescription("Mô tả");
		tr.setPeriodId(thesisPeriod.getId());
		Topic topic = topicService.create(tr, gvCnpm1);
		topicService.approve(topic.getId());
		topicService.publish(thesisPeriod.getId());

		moveToStudentPhase(thesisPeriod);
		Team team = createTeamWithThreeMembers(thesisPeriod, sv1, sv2, sv3);
		registrationService.register(team.getId(), topic.getId(), sv1);
		registrationService.approve(topic.getId(), gvCnpm1);

		// Phân công GVPB và chấm điểm
		reviewerService.assign(topic.getId(), gvHttt.getLecturerCode());
		reviewerService.score(topic.getId(), gvHttt, 8.0, "Đồng ý bảo vệ");

		Council council = councilService.create(topic.getId(), "Hội đồng test",
				List.of("GV002", "GV003", "GV004"),
				List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER));

		Lecturer chair = accountService.findLecturerByCode("GV002");

		assertThrows(InvalidCouncilException.class, () -> councilService.score(council.getId(), chair, -1.0, ""));
		assertThrows(InvalidCouncilException.class, () -> councilService.score(council.getId(), chair, 10.5, ""));
		assertThrows(InvalidCouncilException.class, () -> councilService.score(council.getId(), chair, 8.55, ""));
	}

	// ==================== 5. REPORT SERVICE EDGE CASES ====================

	@Test
	@DisplayName("Report: Nộp tệp rỗng hoặc null ném ngoại lệ")
	void report_SubmitEmptyFile_Fails() {
		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài report test");
		tr.setDescription("Mô tả");
		tr.setPeriodId(thesisPeriod.getId());
		Topic topic = topicService.create(tr, gvCnpm1);
		topicService.approve(topic.getId());
		topicService.publish(thesisPeriod.getId());

		moveToStudentPhase(thesisPeriod);
		Team team = createTeamWithThreeMembers(thesisPeriod, sv1, sv2, sv3);
		registrationService.register(team.getId(), topic.getId(), sv1);
		registrationService.approve(topic.getId(), gvCnpm1);

		MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
		assertThrows(InvalidReportException.class, () -> reportService.submit(team.getId(), sv1, emptyFile));
		assertThrows(InvalidReportException.class, () -> reportService.submit(team.getId(), sv1, null));
	}

	@Test
	@DisplayName("Report: Nộp tệp vượt quá 10MB ném ngoại lệ")
	void report_SubmitExceeds10MB_Fails() {
		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài report size test");
		tr.setDescription("Mô tả");
		tr.setPeriodId(thesisPeriod.getId());
		Topic topic = topicService.create(tr, gvCnpm1);
		topicService.approve(topic.getId());
		topicService.publish(thesisPeriod.getId());

		moveToStudentPhase(thesisPeriod);
		Team team = createTeamWithThreeMembers(thesisPeriod, sv1, sv2, sv3);
		registrationService.register(team.getId(), topic.getId(), sv1);
		registrationService.approve(topic.getId(), gvCnpm1);

		byte[] largeBytes = new byte[(int) (11 * 1024 * 1024)]; // 11MB
		MockMultipartFile largeFile = new MockMultipartFile("file", "large.pdf", "application/pdf", largeBytes);
		assertThrows(InvalidReportException.class, () -> reportService.submit(team.getId(), sv1, largeFile));
	}

	@Test
	@DisplayName("Report: Giảng viên không hướng dẫn tải báo cáo ném ngoại lệ")
	void report_NonAdvisorCannotFindReport() {
		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài report adv test");
		tr.setDescription("Mô tả");
		tr.setPeriodId(thesisPeriod.getId());
		Topic topic = topicService.create(tr, gvCnpm1);
		topicService.approve(topic.getId());
		topicService.publish(thesisPeriod.getId());

		moveToStudentPhase(thesisPeriod);
		Team team = createTeamWithThreeMembers(thesisPeriod, sv1, sv2, sv3);
		registrationService.register(team.getId(), topic.getId(), sv1);
		registrationService.approve(topic.getId(), gvCnpm1);

		MockMultipartFile file = new MockMultipartFile("file", "baocao.pdf", "application/pdf", "demo".getBytes());
		Report report = reportService.submit(team.getId(), sv1, file);

		assertThrows(InvalidReportException.class, () -> reportService.findForLecturer(report.getId(), gvCnpm2));
	}

	// ==================== 6. ACCOUNT SERVICE EDGE CASES ====================

	@Test
	@DisplayName("Account: Tạo tài khoản với mật khẩu < 6 ký tự ném ngoại lệ")
	void account_CreateAccount_ShortPassword_Fails() {
		AccountRequest req = new AccountRequest();
		req.setCode("TESTACC1");
		req.setPassword("12345");
		req.setConfirmPassword("12345");
		req.setFullName("Nguyễn Văn A");
		req.setType(AccountType.STUDENT);

		assertThrows(InvalidAccountException.class, () -> accountService.createAccount(req));
	}

	@Test
	@DisplayName("Account: Tạo tài khoản với mật khẩu xác nhận không khớp ném ngoại lệ")
	void account_CreateAccount_MismatchedConfirmPassword_Fails() {
		AccountRequest req = new AccountRequest();
		req.setCode("TESTACC2");
		req.setPassword("123456");
		req.setConfirmPassword("654321");
		req.setFullName("Nguyễn Văn B");
		req.setType(AccountType.STUDENT);

		assertThrows(InvalidAccountException.class, () -> accountService.createAccount(req));
	}

	@Test
	@DisplayName("Account: Đổi mật khẩu với mật khẩu cũ không đúng ném ngoại lệ")
	void account_ChangePassword_WrongCurrentPassword_Fails() {
		ChangePasswordRequest req = new ChangePasswordRequest();
		req.setCurrentPassword("sai_pass");
		req.setNewPassword("newpass123");
		req.setConfirmPassword("newpass123");

		assertThrows(InvalidAccountException.class, () -> accountService.changePassword(sv1.getUsername(), req));
	}

	@Test
	@DisplayName("Account: Đổi mật khẩu với mật khẩu mới trùng mật khẩu cũ ném ngoại lệ")
	void account_ChangePassword_SamePassword_Fails() {
		ChangePasswordRequest req = new ChangePasswordRequest();
		req.setCurrentPassword("123456");
		req.setNewPassword("123456");
		req.setConfirmPassword("123456");

		assertThrows(InvalidAccountException.class, () -> accountService.changePassword(sv1.getUsername(), req));
	}

	@Test
	@DisplayName("Account: Gán chức danh Trưởng khoa cho sinh viên ném ngoại lệ")
	void account_SetDeanForStudent_Fails() {
		assertThrows(InvalidAccountException.class, () -> accountService.setDean(sv1.getId(), true));
	}

	// ==================== 7. REGISTRATION PERIOD SERVICE EDGE CASES ====================

	@Test
	@DisplayName("Period: Đợt có thời gian kết thúc trước thời gian bắt đầu ném ngoại lệ")
	void period_TeacherEndBeforeStart_Fails() {
		LocalDateTime now = LocalDateTime.now();
		RegistrationPeriodRequest req = new RegistrationPeriodRequest();
		req.setName("Đợt sai thời gian");
		req.setType(PeriodType.COURSE_PROJECT);
		req.setTeacherRegisterStart(now.plusDays(5));
		req.setTeacherRegisterEnd(now.plusDays(2));
		req.setStudentRegisterStart(now.plusDays(6));
		req.setStudentRegisterEnd(now.plusDays(10));
		req.setReportDeadline(LocalDate.now().plusDays(20));

		assertThrows(InvalidRegistrationPeriodException.class, () -> periodService.create(req));
	}

	@Test
	@DisplayName("Period: Đợt KLTN thiếu hạn nộp điểm GVPB ném ngoại lệ")
	void period_ThesisWithoutReviewerDeadline_Fails() {
		LocalDateTime now = LocalDateTime.now();
		RegistrationPeriodRequest req = new RegistrationPeriodRequest();
		req.setName("Đợt KLTN thiếu GVPB deadline");
		req.setType(PeriodType.GRADUATION_THESIS);
		req.setTeacherRegisterStart(now.minusDays(5));
		req.setTeacherRegisterEnd(now.minusDays(3));
		req.setStudentRegisterStart(now.minusDays(2));
		req.setStudentRegisterEnd(now.plusDays(5));
		req.setReportDeadline(LocalDate.now().plusDays(10));
		req.setReviewerScoreDeadline(null);
		req.setCouncilReportDate(LocalDate.now().plusDays(20));

		assertThrows(InvalidRegistrationPeriodException.class, () -> periodService.create(req));
	}

	@Test
	@DisplayName("Period: Xóa đợt đã có đề tài hoặc nhóm sinh viên ném ngoại lệ")
	void period_DeletePeriodWithTopicsOrTeams_Fails() {
		TopicRequest tr = new TopicRequest();
		tr.setName("Đề tài khóa đợt");
		tr.setDescription("Mô tả");
		tr.setPeriodId(thesisPeriod.getId());
		topicService.create(tr, gvCnpm1);

		assertThrows(InvalidRegistrationPeriodException.class,
				() -> periodService.deleteById(thesisPeriod.getId()));
	}
}
