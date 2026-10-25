package project.qldt.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.exception.AnnouncementNotFoundException;
import project.qldt.exception.InvalidAnnouncementException;
import project.qldt.model.announcement.Announcement;
import project.qldt.service.announcement.AnnouncementService;
import project.qldt.support.Beans;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnnouncementServiceTest {

	private AnnouncementService announcementService;

	@BeforeEach
	void setUp() {
		announcementService = Beans.get(AnnouncementService.class);
	}

	@Test
	@DisplayName("Tạo thông báo hợp lệ thành công")
	void createAnnouncement_Success() {
		Announcement announcement = announcementService.create("Thông báo nộp đồ án", "Hạn chót là ngày 30/12");
		assertNotNull(announcement.getId());
		assertEquals("Thông báo nộp đồ án", announcement.getTitle());
		assertEquals("Hạn chót là ngày 30/12", announcement.getContent());
		assertNotNull(announcement.getCreatedAt());
	}

	@Test
	@DisplayName("Tạo thông báo với tiêu đề null hoặc rỗng ném ngoại lệ")
	void createAnnouncement_InvalidTitle() {
		InvalidAnnouncementException ex1 = assertThrows(InvalidAnnouncementException.class,
				() -> announcementService.create(null, "Nội dung"));
		assertTrue(ex1.getMessage().contains("Tiêu đề"));

		InvalidAnnouncementException ex2 = assertThrows(InvalidAnnouncementException.class,
				() -> announcementService.create("   ", "Nội dung"));
		assertTrue(ex2.getMessage().contains("Tiêu đề"));
	}

	@Test
	@DisplayName("Tạo thông báo với nội dung null hoặc rỗng ném ngoại lệ")
	void createAnnouncement_InvalidContent() {
		InvalidAnnouncementException ex1 = assertThrows(InvalidAnnouncementException.class,
				() -> announcementService.create("Tiêu đề", null));
		assertTrue(ex1.getMessage().contains("Nội dung"));

		InvalidAnnouncementException ex2 = assertThrows(InvalidAnnouncementException.class,
				() -> announcementService.create("Tiêu đề", "   "));
		assertTrue(ex2.getMessage().contains("Nội dung"));
	}

	@Test
	@DisplayName("Tìm kiếm thông báo theo ID thành công")
	void findById_Success() {
		Announcement saved = announcementService.create("Tiêu đề 1", "Nội dung 1");
		Announcement found = announcementService.findById(saved.getId());
		assertNotNull(found);
		assertEquals(saved.getId(), found.getId());
		assertEquals("Tiêu đề 1", found.getTitle());
	}

	@Test
	@DisplayName("Tìm kiếm thông báo theo ID không tồn tại ném ngoại lệ")
	void findById_NotFound() {
		assertThrows(AnnouncementNotFoundException.class, () -> announcementService.findById(999999L));
	}

	@Test
	@DisplayName("Cập nhật thông báo hợp lệ thành công")
	void updateAnnouncement_Success() {
		Announcement saved = announcementService.create("Tiêu đề gốc", "Nội dung gốc");
		Announcement updated = announcementService.update(saved.getId(), "Tiêu đề mới", "Nội dung mới");
		assertEquals("Tiêu đề mới", updated.getTitle());
		assertEquals("Nội dung mới", updated.getContent());
	}

	@Test
	@DisplayName("Cập nhật thông báo với dữ liệu rỗng ném ngoại lệ")
	void updateAnnouncement_InvalidData() {
		Announcement saved = announcementService.create("Tiêu đề gốc", "Nội dung gốc");
		assertThrows(InvalidAnnouncementException.class,
				() -> announcementService.update(saved.getId(), "", "Nội dung mới"));
		assertThrows(InvalidAnnouncementException.class,
				() -> announcementService.update(saved.getId(), "Tiêu đề mới", ""));
	}

	@Test
	@DisplayName("Xóa thông báo theo ID thành công")
	void deleteById_Success() {
		Announcement saved = announcementService.create("Tiêu đề cần xóa", "Nội dung cần xóa");
		announcementService.deleteById(saved.getId());
		assertThrows(AnnouncementNotFoundException.class, () -> announcementService.findById(saved.getId()));
	}

	@Test
	@DisplayName("Liệt kê tất cả và lấy 5 thông báo gần nhất")
	void findAllAndRecent() {
		for (int i = 1; i <= 7; i++) {
			announcementService.create("Thông báo " + i, "Nội dung " + i);
		}
		List<Announcement> all = announcementService.findAll();
		assertTrue(all.size() >= 7);

		List<Announcement> recent = announcementService.findRecent();
		assertTrue(recent.size() <= 5);
		assertTrue(announcementService.count() >= 7);
	}
}