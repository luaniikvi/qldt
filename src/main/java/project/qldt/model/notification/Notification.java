package project.qldt.model.notification;

import project.qldt.model.account.Account;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "recipient_id", nullable = false)
	private Account recipient;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NotificationType type;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, length = 500)
	private String content;

	@Column(length = 200)
	private String link;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "is_read", nullable = false)
	private boolean read;
}