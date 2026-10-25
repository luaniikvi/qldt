package project.qldt.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

	private static final Logger log = LoggerFactory.getLogger(EmailService.class);

	private final JavaMailSender mailSender;

	@Value("${app.mail.enabled:true}")
	private boolean mailEnabled = true;

	@Value("${app.mail.from-email:${spring.mail.username:}}")
	private String senderEmail;

	@Autowired
	public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	public void setSenderEmail(String senderEmail) {
		this.senderEmail = senderEmail;
	}

	public void setMailEnabled(boolean mailEnabled) {
		this.mailEnabled = mailEnabled;
	}

	@Async 
	public void sendResultAnnouncement(String recipientEmail, String studentName, String topicName, Double finalScore) {
		if (!mailEnabled || mailSender == null || recipientEmail == null || recipientEmail.isBlank()) {
			log.info("Bỏ qua gửi email công bố kết quả cho: {}", recipientEmail);
			return;
		}

		if (senderEmail == null || senderEmail.isBlank()) {
			log.info("Chưa cấu hình tài khoản email người gửi, bỏ qua gửi email thực tế cho: {}", recipientEmail);
			return;
		}

		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(senderEmail);
			message.setTo(recipientEmail);
			message.setSubject("[QLDT] Công bố kết quả bảo vệ đề tài: " + topicName);
			message.setText("Xin chào " + studentName + ",\n\n"
					+ "Hội đồng bảo vệ đã hoàn tất chấm điểm và công bố kết quả chính thức cho đề tài:\n"
					+ "Đề tài: " + topicName + "\n"
					+ "Điểm tổng kết: " + finalScore + "/10\n\n"
					+ "Bạn có thể đăng nhập vào hệ thống Quản lý đề tài để xem chi tiết kết quả và nhận xét.\n\n"
					+ "Trân trọng,\n"
					+ "Ban Quản lý Đề tài");

			mailSender.send(message);
			log.info("Đã gửi email công bố kết quả thành công tới: {}", recipientEmail);
		} catch (Exception ex) {
			log.error("Không thể gửi email tới {}: {}", recipientEmail, ex.getMessage());
		}
	}
}
