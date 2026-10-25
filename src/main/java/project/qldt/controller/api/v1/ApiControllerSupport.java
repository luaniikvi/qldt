package project.qldt.controller.api.v1;

import org.springframework.security.access.AccessDeniedException;
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
		Account account = currentAccount(authentication);
		if (account instanceof Lecturer lecturer) {
			return lecturer;
		}
		throw new AccessDeniedException("Chỉ giảng viên mới sử dụng được chức năng này");
	}

	protected Student currentStudent(Authentication authentication) {
		Account account = currentAccount(authentication);
		if (account instanceof Student student) {
			return student;
		}
		throw new AccessDeniedException("Chỉ sinh viên mới sử dụng được chức năng này");
	}
}
