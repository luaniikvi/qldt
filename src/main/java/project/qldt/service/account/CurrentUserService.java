package project.qldt.service.account;

import java.util.Collection;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.account.CurrentUserView;
import project.qldt.exception.AccountNotFoundException;
import project.qldt.model.account.Account;
import project.qldt.model.account.Role;
import project.qldt.service.notification.NotificationService;

@Service
@Transactional(readOnly = true)
public class CurrentUserService {

	private static final String ROLE_ADMIN = "ROLE_ADMIN";
	private static final String ROLE_DEAN = "ROLE_DEAN";
	private static final String ROLE_LECTURER = "ROLE_LECTURER";
	private static final String ROLE_STUDENT = "ROLE_STUDENT";

	private final AccountService accountService;
	private final NotificationService notificationService;

	public CurrentUserService(AccountService accountService, NotificationService notificationService) {
		this.accountService = accountService;
		this.notificationService = notificationService;
	}

	public CurrentUserView currentUser(String username, Collection<String> authorities) {
		Account account = findAccountOrNull(username);
		return new CurrentUserView(account,
				account == null ? 0L : notificationService.countUnread(account),
				loginCodeOf(account, username),
				roleNameOf(account, username, authorities),
				initialsOf(account, username, authorities));
	}

	public Account findAccountOrNull(String username) {
		if (username == null || username.isBlank()) {
			return null;
		}
		try {
			return accountService.findByUsername(username);
		} catch (AccountNotFoundException ex) {
			return null;
		}
	}

	private String loginCodeOf(Account account, String username) {
		String code = accountService.loginCodeOf(account);
		if (!code.isBlank()) {
			return code;
		}
		return username == null ? "" : username;
	}

	private String roleNameOf(Account account, String username, Collection<String> authorities) {
		if (account != null) {
			return accountService.roleLabel(account);
		}
		if (username == null) {
			return "Khách";
		}
		Role role = roleOf(username, authorities);
		return role == null ? "Thành viên" : accountService.roleLabel(role);
	}

	private String initialsOf(Account account, String username, Collection<String> authorities) {
		if (account != null && account.getFullName() != null && !account.getFullName().isBlank()) {
			return initialsFromFullName(account.getFullName());
		}
		if (username == null) {
			return "U";
		}
		if (isAdmin(username, authorities)) {
			return "AD";
		}
		return hasRole(authorities, ROLE_STUDENT) ? "SV" : "GV";
	}

	private Role roleOf(String username, Collection<String> authorities) {
		if (isAdmin(username, authorities)) {
			return Role.ADMIN;
		}
		if (hasRole(authorities, ROLE_DEAN)) {
			return Role.DEAN;
		}
		if (hasRole(authorities, ROLE_LECTURER)) {
			return Role.LECTURER;
		}
		return hasRole(authorities, ROLE_STUDENT) ? Role.STUDENT : null;
	}

	private String initialsFromFullName(String fullName) {
		String[] words = fullName.trim().split("\\s+");
		if (words.length == 1) {
			return words[0].substring(0, Math.min(2, words[0].length())).toUpperCase(Locale.ROOT);
		}
		return (words[0].substring(0, 1) + words[words.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
	}

	private boolean isAdmin(String username, Collection<String> authorities) {
		return "admin".equalsIgnoreCase(username) || hasRole(authorities, ROLE_ADMIN);
	}

	private boolean hasRole(Collection<String> authorities, String role) {
		return authorities != null && authorities.contains(role);
	}
}
