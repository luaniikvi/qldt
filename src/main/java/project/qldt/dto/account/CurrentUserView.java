package project.qldt.dto.account;

import project.qldt.model.account.Account;

public record CurrentUserView(Account account, long unreadNotifications, String code, String roleName,
		String initials) {
}
