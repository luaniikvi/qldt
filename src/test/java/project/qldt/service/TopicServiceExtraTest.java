package project.qldt.service;
import project.qldt.support.Beans;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import project.qldt.dto.RegistrationPeriodRequest;
import project.qldt.dto.TopicRequest;
import project.qldt.exception.AccountNotFoundException;
import project.qldt.exception.InvalidTopicException;
import project.qldt.exception.RegistrationPeriodNotFoundException;
import project.qldt.exception.TopicNotFoundException;
import project.qldt.model.InvitationStatus;
import project.qldt.model.Lecturer;
import project.qldt.model.PeriodType;
import project.qldt.model.RegistrationPeriod;
import project.qldt.model.Topic;
import project.qldt.model.TopicAvailability;
import project.qldt.model.TopicStatus;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TopicServiceExtraTest {

	private AccountService accountService;
	private RegistrationPeriodService periodService;
	private TopicService topicService;

	private Lecturer gv1;
	private Lecturer gv2;
	private Lecturer gvOther;
	private RegistrationPeriod period;

	@BeforeEach
	void setUp() {
		accountService = Beans.get(AccountService.class);
		periodService = Beans.get(RegistrationPeriodService.class);
		topicService = Beans.get(TopicService.class);

		gv1 = accountService.createLecturer("123456", "GV 1", "gv1@qldt.local", "GV901", "Công nghệ phần mềm");
		gv2 = accountService.createLecturer("123456", "GV 2", "gv2@qldt.local", "GV902", "Công nghệ phần mềm");
		gvOther = accountService.createLecturer("123456", "GV Khac", "other@qldt.local", "GV999", "Hệ thống thông tin");

		RegistrationPeriodRequest pReq = new RegistrationPeriodRequest();
		pReq.setName("Đợt test");
		pReq.setType(PeriodType.COURSE);
		pReq.setTeacherRegisterStart(LocalDateTime.now().minusDays(1));
		pReq.setTeacherRegisterEnd(LocalDateTime.now().plusDays(2));
		pReq.setStudentRegisterStart(LocalDateTime.now().plusDays(3));
		pReq.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));
		pReq.setReportDeadline(LocalDateTime.now().plusDays(10).toLocalDate());
		period = periodService.create(pReq);
	}

	@Test
	void taoDeTaiVoiDotKhongTonTaiNemLoi() {
		TopicRequest req = createTopicRequest("Đề tài 1", 9999L);
		assertThrows(RegistrationPeriodNotFoundException.class, () -> topicService.create(req, gv1));
	}

	@Test
	void findForEditKiemTraQuyenVaTrangThai() {
		Topic topic = topicService.create(createTopicRequest("Đề tài 1", period.getId()), gv1);

		assertThrows(InvalidTopicException.class, () -> topicService.findForEdit(topic.getId(), gv2));

		Topic editable = topicService.findForEdit(topic.getId(), gv1);
		assertNotNull(editable);

		topicService.approve(topic.getId());
		assertThrows(InvalidTopicException.class, () -> topicService.findForEdit(topic.getId(), gv1));
	}

	@Test
	void capNhatDeTaiBiTuChoiSeTroVePendingVaXoaLyDo() {
		Topic topic = topicService.create(createTopicRequest("Đề tài 1", period.getId()), gv1);
		topicService.reject(topic.getId(), "Lý do chưa rõ");
		assertEquals(TopicStatus.REJECTED, topic.getStatus());
		assertEquals("Lý do chưa rõ", topic.getRejectionReason());

		TopicRequest updateReq = createTopicRequest("Đề tài 1 Đã Sửa", period.getId());
		Topic updated = topicService.update(topic.getId(), updateReq, gv1);

		assertEquals(TopicStatus.PENDING, updated.getStatus());
		assertNull(updated.getRejectionReason());
		assertEquals("Đề tài 1 Đã Sửa", updated.getName());
	}

	@Test
	void capNhatDeTaiKhiKhongPhaiNguoiTaoHoacDaDuyetBiChan() {
		Topic topic = topicService.create(createTopicRequest("Đề tài 1", period.getId()), gv1);
		TopicRequest updateReq = createTopicRequest("Đề tài Sửa", period.getId());

		assertThrows(InvalidTopicException.class, () -> topicService.update(topic.getId(), updateReq, gv2));

		topicService.approve(topic.getId());
		assertThrows(InvalidTopicException.class, () -> topicService.update(topic.getId(), updateReq, gv1));
	}

	@Test
	void moiGiangVienMaKhongTonTaiNemLoi() {
		Topic topic = topicService.create(createTopicRequest("Đề tài 1", period.getId()), gv1);

		assertThrows(AccountNotFoundException.class,
				() -> topicService.inviteAdvisor(topic.getId(), "GV_NOT_FOUND", gv1));
	}

	@Test
	void moiGiangVienKhiKhongPhaiNguoiTaoHoacDaDuyetBiChan() {
		Topic topic = topicService.create(createTopicRequest("Đề tài 1", period.getId()), gv1);

		assertThrows(InvalidTopicException.class,
				() -> topicService.inviteAdvisor(topic.getId(), "GV902", gv2));

		topicService.approve(topic.getId());
		assertThrows(InvalidTopicException.class,
				() -> topicService.inviteAdvisor(topic.getId(), "GV902", gv1));
	}

	@Test
	void moiGiangVienKhiDangCoLoiMoiChoBiChan() {
		Topic topic = topicService.create(createTopicRequest("Đề tài 1", period.getId()), gv1);
		topicService.inviteAdvisor(topic.getId(), "GV902", gv1);

		Lecturer gv3 = accountService.createLecturer("123456", "GV 3", "gv3@qldt.local", "GV903", "Công nghệ phần mềm");
		assertThrows(InvalidTopicException.class, () -> topicService.inviteAdvisor(topic.getId(), "GV903", gv1));
	}

	@Test
	void phanHoiLoiMoiKhiKhongCoLoiMoiHoacSaiNguoiBiChan() {
		Topic topic = topicService.create(createTopicRequest("Đề tài 1", period.getId()), gv1);

		assertThrows(InvalidTopicException.class,
				() -> topicService.respondInvitation(topic.getId(), gv2, true, null));

		topicService.inviteAdvisor(topic.getId(), "GV902", gv1);

		assertThrows(InvalidTopicException.class,
				() -> topicService.respondInvitation(topic.getId(), gvOther, true, null));
	}

	@Test
	void huyLoiMoiVaMoiLai() {
		Topic topic = topicService.create(createTopicRequest("Đề tài 1", period.getId()), gv1);
		topicService.inviteAdvisor(topic.getId(), "GV902", gv1);

		assertThrows(InvalidTopicException.class, () -> topicService.cancelAdvisorInvitation(topic.getId(), gv2));

		topicService.cancelAdvisorInvitation(topic.getId(), gv1);
		assertEquals(InvitationStatus.CANCELLED, topic.getInvitation().getStatus());

		assertThrows(InvalidTopicException.class, () -> topicService.cancelAdvisorInvitation(topic.getId(), gv1));

		Lecturer gv3 = accountService.createLecturer("123456", "GV 3", "gv3@qldt.local", "GV903", "Công nghệ phần mềm");
		topicService.inviteAdvisor(topic.getId(), "GV903", gv1);
		assertEquals(InvitationStatus.PENDING, topic.getInvitation().getStatus());
		assertEquals("GV903", topic.getInvitation().getLecturer().getLecturerCode());
	}

	@Test
	void xoaGiangVienHuongDanThuHaiKiemTraRangBuoc() {
		Topic topic = topicService.create(createTopicRequest("Đề tài 1", period.getId()), gv1);

		assertThrows(InvalidTopicException.class, () -> topicService.removeCoAdvisor(topic.getId(), "GV902", gv1));

		topicService.inviteAdvisor(topic.getId(), "GV902", gv1);
		topicService.respondInvitation(topic.getId(), gv2, true, null);
		assertEquals(2, topic.getAdvisors().size());

		assertThrows(InvalidTopicException.class, () -> topicService.removeCoAdvisor(topic.getId(), "GV902", gv2));

		Lecturer gv3 = accountService.createLecturer("123456", "GV 3", "gv3@qldt.local", "GV903", "Công nghệ phần mềm");
		assertThrows(InvalidTopicException.class, () -> topicService.removeCoAdvisor(topic.getId(), "GV903", gv1));

		topicService.removeCoAdvisor(topic.getId(), "GV902", gv1);
		assertEquals(1, topic.getAdvisors().size());
		assertEquals(InvitationStatus.CANCELLED, topic.getInvitation().getStatus());
	}

	@Test
	void duyetVaTuChoiChiApDungKhiPending() {
		Topic topic = topicService.create(createTopicRequest("Đề tài 1", period.getId()), gv1);
		topicService.approve(topic.getId());

		assertThrows(InvalidTopicException.class, () -> topicService.approve(topic.getId()));
		assertThrows(InvalidTopicException.class, () -> topicService.reject(topic.getId(), "Ly do"));
	}

	@Test
	void congBoKhiKhongCoDeTaiDaDuyetNemLoi() {
		topicService.create(createTopicRequest("Đề tài 1", period.getId()), gv1);

		assertThrows(InvalidTopicException.class, () -> topicService.publish(period.getId()));
	}

	@Test
	void danhSachDotCoDeTaiDaDuyetVaDeTaiChoPhepDangKy() {
		Topic topic = topicService.create(createTopicRequest("Đề tài 1", period.getId()), gv1);
		assertEquals(0, topicService.findPeriodsWithApprovedTopics().size());

		topicService.approve(topic.getId());
		List<RegistrationPeriod> periods = topicService.findPeriodsWithApprovedTopics();
		assertEquals(1, periods.size());
		assertEquals(period.getId(), periods.get(0).getId());

		assertEquals(0, topicService.findAvailablePublished().size());

		topicService.publish(period.getId());
		List<Topic> available = topicService.findAvailablePublished();
		assertEquals(1, available.size());
		assertEquals(topic.getId(), available.get(0).getId());
	}

	@Test
	void danhSachDeTaiCuaGiangVienGomLoiMoiDangCho() {
		Topic topic = topicService.create(createTopicRequest("Đề tài 1", period.getId()), gv1);
		topicService.inviteAdvisor(topic.getId(), "GV902", gv1);

		List<Topic> gv1Topics = topicService.findByLecturer(gv1);
		List<Topic> gv2Topics = topicService.findByLecturer(gv2);
		List<Topic> otherTopics = topicService.findByLecturer(gvOther);

		assertEquals(1, gv1Topics.size());
		assertEquals(1, gv2Topics.size());
		assertEquals(0, otherTopics.size());
	}

	@Test
	void timKiemTheoIdKhongTonTaiNemLoi() {
		assertThrows(TopicNotFoundException.class, () -> topicService.findById(9999L));
	}

	private TopicRequest createTopicRequest(String name, Long periodId) {
		TopicRequest req = new TopicRequest();
		req.setName(name);
		req.setDescription("Mô tả");
		req.setPeriodId(periodId);
		return req;
	}
}
