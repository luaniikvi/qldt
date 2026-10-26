package project.qldt.controller.notification;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import project.qldt.exception.InvalidNotificationException;
import project.qldt.exception.NotificationNotFoundException;
import project.qldt.model.account.Account;
import project.qldt.service.account.AccountService;
import project.qldt.service.notification.NotificationService;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

	private final NotificationService notificationService;
	private final AccountService accountService;

	public NotificationController(NotificationService notificationService, AccountService accountService) {
		this.notificationService = notificationService;
		this.accountService = accountService;
	}

	@GetMapping
	public String list(Authentication authentication, Model model) {
		Account account = accountService.findByUsername(authentication.getName());
		model.addAttribute("notifications", notificationService.findByRecipient(account));
		model.addAttribute("unreadCount", notificationService.countUnread(account));
		return "notifications/list";
	}

	@PostMapping("/{id}/read")
	public String markRead(@PathVariable Long id, Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			notificationService.markRead(id, accountService.findByUsername(authentication.getName()));
			redirectAttributes.addFlashAttribute("message", "Đã đánh dấu đã đọc");
		} catch (InvalidNotificationException | NotificationNotFoundException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/notifications";
	}

	@PostMapping("/read-all")
	public String markAllRead(Authentication authentication, RedirectAttributes redirectAttributes) {
		int count = notificationService.markAllRead(accountService.findByUsername(authentication.getName()));
		redirectAttributes.addFlashAttribute("message", "Đã đánh dấu đã đọc " + count + " thông báo");
		return "redirect:/notifications";
	}

}