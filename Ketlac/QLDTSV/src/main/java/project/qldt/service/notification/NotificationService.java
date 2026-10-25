package project.qldt.service.notification;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.exception.InvalidNotificationException;
import project.qldt.exception.NotificationNotFoundException;
import project.qldt.model.account.Account;
import project.qldt.model.notification.Notification;
import project.qldt.model.notification.NotificationType;
import project.qldt.repository.notification.NotificationRepository;

@Service
@Transactional
public class NotificationService {

	private final NotificationRepository notificationRepository;

	public NotificationService(NotificationRepository notificationRepository) {
		this.notificationRepository = notificationRepository;
	}

	public Notification send(Account recipient, NotificationType type, String title, String content, String link) {
		Notification notification = new Notification();
		notification.setRecipient(recipient);
		notification.setType(type);
		notification.setTitle(title);
		notification.setContent(content);
		notification.setLink(link);
		notification.setCreatedAt(LocalDateTime.now());
		return notificationRepository.save(notification);
	}

	@Transactional(readOnly = true)
	public List<Notification> findByRecipient(Account recipient) {
		return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipient.getId());
	}

	@Transactional(readOnly = true)
	public long countUnread(Account recipient) {
		return notificationRepository.countByRecipientIdAndReadFalse(recipient.getId());
	}

	@Transactional(readOnly = true)
	public boolean exists(Account recipient, String title) {
		return notificationRepository.existsByRecipientIdAndTitle(recipient.getId(), title);
	}

	public Notification markRead(Long id, Account actor) {
		Notification notification = notificationRepository.findById(id)
				.orElseThrow(() -> new NotificationNotFoundException("Không tìm thấy thông báo có id: " + id));
		if (!notification.getRecipient().getId().equals(actor.getId())) {
			throw new InvalidNotificationException("Bạn không phải người nhận thông báo này");
		}
		notification.setRead(true);
		return notificationRepository.save(notification);
	}

	public int markAllRead(Account actor) {
		List<Notification> notifications = findByRecipient(actor);
		notifications.forEach(notification -> notification.setRead(true));
		notificationRepository.saveAll(notifications);
		return notifications.size();
	}
}