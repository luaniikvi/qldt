package project.qldt.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import project.qldt.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

	long countByRecipientIdAndReadFalse(Long recipientId);

	boolean existsByRecipientIdAndTitle(Long recipientId, String title);
}