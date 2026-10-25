package project.qldt.controller.api.v1;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import project.qldt.dto.api.response.BulkActionResponse;
import project.qldt.dto.api.response.NotificationResponse;
import project.qldt.dto.api.response.UnreadCountResponse;
import project.qldt.model.account.Account;
import project.qldt.service.account.AccountService;
import project.qldt.service.notification.NotificationService;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationRestController extends ApiControllerSupport {

	private final NotificationService notificationService;

	public NotificationRestController(NotificationService notificationService, AccountService accountService) {
		super(accountService);
		this.notificationService = notificationService;
	}

	@GetMapping
	public List<NotificationResponse> list(Authentication authentication) {
		return notificationService.findByRecipient(currentAccount(authentication)).stream()
				.map(NotificationResponse::of).toList();
	}

	@GetMapping("/unread-count")
	public UnreadCountResponse unreadCount(Authentication authentication) {
		return new UnreadCountResponse(notificationService.countUnread(currentAccount(authentication)));
	}

	@PatchMapping("/{id}/read")
	public NotificationResponse markRead(@PathVariable Long id, Authentication authentication) {
		Account account = currentAccount(authentication);
		return NotificationResponse.of(notificationService.markRead(id, account));
	}

	@PostMapping("/read-all")
	public BulkActionResponse markAllRead(Authentication authentication) {
		return new BulkActionResponse(notificationService.markAllRead(currentAccount(authentication)));
	}
}
