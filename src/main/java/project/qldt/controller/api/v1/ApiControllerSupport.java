package project.qldt.controller.api.v1;

import org.springframework.security.core.Authentication;

import project.qldt.model.account.Account;
import project.qldt.model.account.Lecturer;
import project.qldt.model.account.Student;
import project.qldt.service.account.AccountService;

public abstract class ApiControllerSupport {

	protected final AccountService accountService;

	protected ApiControllerSupport(AccountService accountService) {
		this.accountService = accountService;
	}

	protected Account currentAccount(Authentication authentication) {
		return accountService.findByUsername(authentication.getName());
	}

	protected Lecturer currentLecturer(Authentication authentication) {
		return accountService.requireLecturer(authentication.getName());
	}

	protected Student currentStudent(Authentication authentication) {
		return accountService.requireStudent(authentication.getName());
	}
}
