package project.qldt.service.announcement;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.exception.AnnouncementNotFoundException;
import project.qldt.exception.InvalidAnnouncementException;
import project.qldt.model.announcement.Announcement;
import project.qldt.repository.announcement.AnnouncementRepository;

@Service
@Transactional
public class AnnouncementService {

	private final AnnouncementRepository announcementRepository;

	public AnnouncementService(AnnouncementRepository announcementRepository) {
		this.announcementRepository = announcementRepository;
	}

	@Transactional(readOnly = true)
	public List<Announcement> findAll() {
		return announcementRepository.findAllByOrderByCreatedAtAsc();
	}

	@Transactional(readOnly = true)
	public List<Announcement> findRecent() {
		return announcementRepository.findTop5ByOrderByCreatedAtDesc();
	}

	@Transactional(readOnly = true)
	public long count() {
		return announcementRepository.count();
	}

	@Transactional(readOnly = true)
	public Announcement findById(Long id) {
		return announcementRepository.findById(id)
				.orElseThrow(() -> new AnnouncementNotFoundException("Không tìm thấy thông báo có id: " + id));
	}

	public Announcement create(String title, String content) {
		validate(title, content);
		Announcement announcement = new Announcement();
		announcement.setTitle(title.trim());
		announcement.setContent(content.trim());
		announcement.setCreatedAt(LocalDateTime.now());
		return announcementRepository.save(announcement);
	}

	public Announcement update(Long id, String title, String content) {
		Announcement announcement = findById(id);
		validate(title, content);
		announcement.setTitle(title.trim());
		announcement.setContent(content.trim());
		return announcementRepository.save(announcement);
	}

	public void deleteById(Long id) {
		announcementRepository.deleteById(findById(id).getId());
	}

	private void validate(String title, String content) {
		if (title == null || title.isBlank()) {
			throw new InvalidAnnouncementException("Tiêu đề thông báo không được để trống");
		}
		if (content == null || content.isBlank()) {
			throw new InvalidAnnouncementException("Nội dung thông báo không được để trống");
		}
	}
}
