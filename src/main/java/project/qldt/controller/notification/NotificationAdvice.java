package project.qldt.controller.notification;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import project.qldt.exception.AccountNotFoundException;
import project.qldt.model.account.Account;
import project.qldt.model.account.Lecturer;
import project.qldt.model.account.Role;
import project.qldt.model.account.Student;
import project.qldt.service.account.AccountService;
import project.qldt.service.notification.NotificationService;

@ControllerAdvice
public class NotificationAdvice {

	private final AccountService accountService;
	private final NotificationService notificationService;

	public NotificationAdvice(AccountService accountService, NotificationService notificationService) {
		this.accountService = accountService;
		this.notificationService = notificationService;
	}

	@ModelAttribute
	public void populateUserContext(Authentication authentication, Model model) {
		Authentication auth = resolveAuthentication(authentication);
		Account account = loadAccount(auth);
		model.addAttribute("currentUserAccount", account);
		model.addAttribute("unreadNotifications", account == null ? 0L : notificationService.countUnread(account));
		model.addAttribute("currentUserCode", codeOf(account, auth));
		model.addAttribute("currentUserRoleName", roleNameOf(account, auth));
		model.addAttribute("currentUserInitials", initialsOf(account, auth));
	}

	private Account loadAccount(Authentication auth) {
		if (auth == null) {
			return null;
		}
		try {
			return accountService.findByUsername(auth.getName());
		} catch (AccountNotFoundException ex) {
			return null;
		}
	}

	private String codeOf(Account account, Authentication auth) {
		if (account instanceof Student student) {
			return student.getStudentCode();
		}
		if (account instanceof Lecturer lecturer) {
			return lecturer.getLecturerCode();
		}
		if (account != null && account.getUsername() != null) {
			return account.getUsername();
		}
		return auth != null && auth.getName() != null ? auth.getName() : "";
	}

	private String roleNameOf(Account account, Authentication auth) {
		if (account != null) {
			if (account.getRoles().contains(Role.ADMIN)) {
				return "Quản trị viên";
			}
			if (account.isDean()) {
				return "Trưởng khoa";
			}
			if (account.getRoles().contains(Role.LECTURER)) {
				return "Giảng viên";
			}
			if (account.getRoles().contains(Role.STUDENT)) {
				return "Sinh viên";
			}
		}
		if (auth == null) {
			return "Khách";
		}
		if (hasRole(auth, "ROLE_ADMIN") || "admin".equalsIgnoreCase(auth.getName())) {
			return "Quản trị viên";
		}
		if (hasRole(auth, "ROLE_DEAN")) {
			return "Trưởng khoa";
		}
		if (hasRole(auth, "ROLE_LECTURER")) {
			return "Giảng viên";
		}
		if (hasRole(auth, "ROLE_STUDENT")) {
			return "Sinh viên";
		}
		return "Thành viên";
	}

	private String initialsOf(Account account, Authentication auth) {
		if (account != null && account.getFullName() != null && !account.getFullName().isBlank()) {
			String[] words = account.getFullName().trim().split("\\s+");
			if (words.length == 1) {
				return words[0].substring(0, Math.min(2, words[0].length())).toUpperCase();
			}
			return (words[0].substring(0, 1) + words[words.length - 1].substring(0, 1)).toUpperCase();
		}
		if (auth == null) {
			return "U";
		}
		if (hasRole(auth, "ROLE_ADMIN") || "admin".equalsIgnoreCase(auth.getName())) {
			return "AD";
		}
		if (hasRole(auth, "ROLE_STUDENT")) {
			return "SV";
		}
		return "GV";
	}

	private boolean hasRole(Authentication auth, String role) {
		return auth.getAuthorities() != null
				&& auth.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
	}

	private Authentication resolveAuthentication(Authentication authentication) {
		if (authentication != null && authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken)) {
			return authentication;
		}
		Authentication contextAuth = SecurityContextHolder.getContext().getAuthentication();
		if (contextAuth != null && contextAuth.isAuthenticated()
				&& !(contextAuth instanceof AnonymousAuthenticationToken)) {
			return contextAuth;
		}
		return null;
	}
}
