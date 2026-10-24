package project.qldt.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import project.qldt.service.email.EmailService;

class EmailServiceTest {

	private JavaMailSender mailSender;
	private EmailService emailService;

	@BeforeEach
	void setUp() {
		mailSender = mock(JavaMailSender.class);
		emailService = new EmailService(mailSender);
		emailService.setSenderEmail("noreply@school.edu.vn");
	}

	@Test
	void sendResultAnnouncementSuccess() {
		emailService.sendResultAnnouncement("sv01@school.edu.vn", "Nguyen Van A", "He Thong AI", 9.5);
		verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
	}

	@Test
	void sendResultAnnouncementSkipWhenRecipientEmpty() {
		emailService.sendResultAnnouncement(null, "Nguyen Van A", "He Thong AI", 9.5);
		emailService.sendResultAnnouncement("   ", "Nguyen Van A", "He Thong AI", 9.5);
		verify(mailSender, never()).send(any(SimpleMailMessage.class));
	}

	@Test
	void sendResultAnnouncementSkipWhenSenderEmpty() {
		emailService.setSenderEmail("");
		emailService.sendResultAnnouncement("sv01@school.edu.vn", "Nguyen Van A", "He Thong AI", 9.5);
		verify(mailSender, never()).send(any(SimpleMailMessage.class));
	}

	@Test
	void sendResultAnnouncementSkipWhenDisabled() {
		emailService.setMailEnabled(false);
		emailService.sendResultAnnouncement("sv01@school.edu.vn", "Nguyen Van A", "He Thong AI", 9.5);
		verify(mailSender, never()).send(any(SimpleMailMessage.class));
	}

	@Test
	void sendResultAnnouncementSkipWhenMailSenderNull() {
		EmailService nullSenderService = new EmailService(null);
		nullSenderService.setSenderEmail("noreply@school.edu.vn");
		assertDoesNotThrow(() -> nullSenderService.sendResultAnnouncement("sv01@school.edu.vn", "Nguyen Van A", "He Thong AI", 9.5));
	}

	@Test
	void sendResultAnnouncementHandlesExceptionGracefully() {
		doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));
		assertDoesNotThrow(() -> emailService.sendResultAnnouncement("sv01@school.edu.vn", "Nguyen Van A", "He Thong AI", 9.5));
	}
}
