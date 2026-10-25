package project.qldt.repository.notification;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import project.qldt.model.notification.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

	long countByRecipientIdAndReadFalse(Long recipientId);

	boolean existsByRecipientIdAndTitle(Long recipientId, String title);
}