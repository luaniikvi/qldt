package project.qldt.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import project.qldt.model.Account;
import project.qldt.service.AccountService;
import project.qldt.service.NotificationService;

@ControllerAdvice
public class NotificationAdvice {

	private final AccountService accountService;
	private final NotificationService notificationService;

	public NotificationAdvice(AccountService accountService, NotificationService notificationService) {
		this.accountService = accountService;
		this.notificationService = notificationService;
	}

	@ModelAttribute("unreadNotifications")
	public long unreadNotifications(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return 0;
		}
		Account account = accountService.findByUsername(authentication.getName());
		return notificationService.countUnread(account);
	}
}