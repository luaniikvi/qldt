package project.qldt.dto.api.response;

import java.time.LocalDateTime;

import project.qldt.model.notification.Notification;
import project.qldt.model.notification.NotificationType;

public record NotificationResponse(Long id, NotificationType type, String title, String content, String link,
		LocalDateTime createdAt, boolean read) {

	public static NotificationResponse of(Notification notification) {
		return new NotificationResponse(notification.getId(), notification.getType(), notification.getTitle(),
				notification.getContent(), notification.getLink(), notification.getCreatedAt(),
				notification.isRead());
	}
}
