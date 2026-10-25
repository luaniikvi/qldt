package project.qldt.dto.api.response;

import java.time.LocalDateTime;

import project.qldt.model.announcement.Announcement;

public record AnnouncementResponse(Long id, String title, String content, LocalDateTime createdAt) {

	public static AnnouncementResponse of(Announcement announcement) {
		return new AnnouncementResponse(announcement.getId(), announcement.getTitle(), announcement.getContent(),
				announcement.getCreatedAt());
	}
}
