package project.qldt.service;
import project.qldt.service.account.AccountService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.topic.TopicService;
import project.qldt.support.Beans;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.dto.topic.TopicRequest;
import project.qldt.exception.InvalidTopicException;
import project.qldt.model.topic.InvitationStatus;
import project.qldt.model.account.Lecturer;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.topic.Topic;
import project.qldt.model.topic.TopicAvailability;
import project.qldt.model.topic.TopicStatus;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TopicServiceTest {

	private final AccountService accountService = Beans.get(AccountService.class);
	private final RegistrationPeriodService periodService = Beans.get(RegistrationPeriodService.class);
	private final TopicService topicService = Beans.get(TopicService.class);

	private Lecturer gv1;
	private Lecturer gv2;
	private RegistrationPeriod period;

	@BeforeEach
	void setUp() {
		gv1 = accountService.createLecturer("123456", "GV 1", "gv1@qldt.local", "GV901", "Công nghệ phần mềm");
		gv2 = accountService.createLecturer("123456", "GV 2", "gv2@qldt.local", "GV902", "Công nghệ phần mềm");
		period = periodService.create(periodRequest(LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1)));
	}

	@Test
	void taoDeTaiLayBoMonCuaNguoiTao() {
		Topic topic = topicService.create(topicRequest(period.getId()), gv1);

		assertEquals("Công nghệ phần mềm", topic.getDepartment().getName());
		assertEquals(1, topic.getAdvisors().size());
		assertEquals(TopicStatus.PENDING, topic.getStatus());
		assertEquals(TopicAvailability.AVAILABLE, topic.getAvailability());
	}

	@Test
	void ngoaiThoiGianGiangVienDangKyBiChan() {
		RegistrationPeriod past = periodService.create(
				periodRequest(LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(5)));

		assertThrows(InvalidTopicException.class, () -> topicService.create(topicRequest(past.getId()), gv1));
	}

	@Test
	void moiGiangVienCungBoMon() {
		Topic topic = topicService.create(topicRequest(period.getId()), gv1);

		topicService.inviteAdvisor(topic.getId(), "GV902", gv1);

		assertEquals(InvitationStatus.PENDING, topic.getInvitation().getStatus());
		assertEquals(1, topic.getAdvisors().size());
	}

	@Test
	void moiGiangVienKhacBoMonBiChan() {
		accountService.createLecturer("123456", "GV 3", "gv3@qldt.local", "GV903", "Hệ thống thông tin");
		Topic topic = topicService.create(topicRequest(period.getId()), gv1);

		assertThrows(InvalidTopicException.class, () -> topicService.inviteAdvisor(topic.getId(), "GV903", gv1));
	}

	@Test
	void moiChinhMinhBiChan() {
		Topic topic = topicService.create(topicRequest(period.getId()), gv1);

		assertThrows(InvalidTopicException.class, () -> topicService.inviteAdvisor(topic.getId(), "GV901", gv1));
	}

	@Test
	void giangVienDuocMoiDongYThiThanhGvhdThuHai() {
		Topic topic = topicService.create(topicRequest(period.getId()), gv1);
		topicService.inviteAdvisor(topic.getId(), "GV902", gv1);

		topicService.respondInvitation(topic.getId(), gv2, true, null);

		assertEquals(2, topic.getAdvisors().size());
		assertEquals(InvitationStatus.ACCEPTED, topic.getInvitation().getStatus());
	}

	@Test
	void giangVienDuocMoiTuChoiCanNhapLyDo() {
		Topic topic = topicService.create(topicRequest(period.getId()), gv1);
		topicService.inviteAdvisor(topic.getId(), "GV902", gv1);

		assertThrows(InvalidTopicException.class,
				() -> topicService.respondInvitation(topic.getId(), gv2, false, " "));

		topicService.respondInvitation(topic.getId(), gv2, false, "Bận việc");

		assertEquals(InvitationStatus.REJECTED, topic.getInvitation().getStatus());
		assertEquals("Bận việc", topic.getInvitation().getResponseReason());
		assertEquals(1, topic.getAdvisors().size());
	}

	@Test
	void toiDaHaiGiangVienHuongDan() {
		Topic topic = topicService.create(topicRequest(period.getId()), gv1);
		topicService.inviteAdvisor(topic.getId(), "GV902", gv1);
		topicService.respondInvitation(topic.getId(), gv2, true, null);

		assertThrows(InvalidTopicException.class, () -> topicService.inviteAdvisor(topic.getId(), "GV903", gv1));
	}

	@Test
	void chiNguoiTaoMoiDuocSuaDeTai() {
		Topic topic = topicService.create(topicRequest(period.getId()), gv1);

		assertThrows(InvalidTopicException.class,
				() -> topicService.update(topic.getId(), topicRequest(period.getId()), gv2));
	}

	@Test
	void duyetVaCongBoDeTai() {
		Topic topic = topicService.create(topicRequest(period.getId()), gv1);

		topicService.approve(topic.getId());
		assertEquals(TopicStatus.APPROVED, topic.getStatus());

		assertEquals(1, topicService.publish(period.getId()));
		assertEquals(TopicStatus.PUBLISHED, topic.getStatus());
		assertEquals(TopicAvailability.AVAILABLE, topic.getAvailability());
		assertTrue(topicService.findPublished().contains(topic));
	}

	@Test
	void tuChoiCanNhapLyDo() {
		Topic topic = topicService.create(topicRequest(period.getId()), gv1);

		assertThrows(InvalidTopicException.class, () -> topicService.reject(topic.getId(), " "));

		topicService.reject(topic.getId(), "Nội dung chưa rõ");
		assertEquals(TopicStatus.REJECTED, topic.getStatus());
		assertEquals("Nội dung chưa rõ", topic.getRejectionReason());
	}

	@Test
	void suaDeTaiBiTuChoiDuaVeChoDuyet() {
		Topic topic = topicService.create(topicRequest(period.getId()), gv1);
		topicService.reject(topic.getId(), "Bổ sung nội dung");

		topicService.update(topic.getId(), topicRequest(period.getId()), gv1);

		assertEquals(TopicStatus.PENDING, topic.getStatus());
		assertNull(topic.getRejectionReason());
	}

	@Test
	void huyLoiMoiGvhdVaMoiLai() {
		Topic topic = topicService.create(topicRequest(period.getId()), gv1);
		topicService.inviteAdvisor(topic.getId(), "GV902", gv1);

		topicService.cancelAdvisorInvitation(topic.getId(), gv1);

		assertEquals(InvitationStatus.CANCELLED, topic.getInvitation().getStatus());

		topicService.inviteAdvisor(topic.getId(), "GV902", gv1);
		assertEquals(InvitationStatus.PENDING, topic.getInvitation().getStatus());
	}

	@Test
	void chiNguoiTaoMoiDuocHuyLoiMoiGvhd() {
		Topic topic = topicService.create(topicRequest(period.getId()), gv1);
		topicService.inviteAdvisor(topic.getId(), "GV902", gv1);

		assertThrows(InvalidTopicException.class,
				() -> topicService.cancelAdvisorInvitation(topic.getId(), gv2));
	}

	@Test
	void xoaGiangVienHuongDanThuHai() {
		Topic topic = topicService.create(topicRequest(period.getId()), gv1);
		topicService.inviteAdvisor(topic.getId(), "GV902", gv1);
		topicService.respondInvitation(topic.getId(), gv2, true, null);

		topicService.removeCoAdvisor(topic.getId(), "GV902", gv1);

		assertEquals(1, topic.getAdvisors().size());
		assertEquals(InvitationStatus.CANCELLED, topic.getInvitation().getStatus());
	}

	@Test
	void chiNguoiTaoMoiDuocXoaGvhd() {
		Topic topic = topicService.create(topicRequest(period.getId()), gv1);
		topicService.inviteAdvisor(topic.getId(), "GV902", gv1);
		topicService.respondInvitation(topic.getId(), gv2, true, null);

		assertThrows(InvalidTopicException.class,
				() -> topicService.removeCoAdvisor(topic.getId(), "GV902", gv2));
	}

	private RegistrationPeriodRequest periodRequest(LocalDateTime teacherStart, LocalDateTime teacherEnd) {
		RegistrationPeriodRequest request = new RegistrationPeriodRequest();
		request.setName("Đợt giảng viên");
		request.setType(PeriodType.COURSE_PROJECT);
		request.setTeacherRegisterStart(teacherStart);
		request.setTeacherRegisterEnd(teacherEnd);
		request.setStudentRegisterStart(teacherEnd.plusDays(1));
		request.setStudentRegisterEnd(teacherEnd.plusDays(5));
		request.setReportDeadline(teacherEnd.plusDays(6).toLocalDate());
		return request;
	}

	private TopicRequest topicRequest(Long periodId) {
		TopicRequest request = new TopicRequest();
		request.setName("Xây dựng hệ thống quản lý");
		request.setDescription("Nội dung đề tài");
		request.setPeriodId(periodId);
		return request;
	}
}
