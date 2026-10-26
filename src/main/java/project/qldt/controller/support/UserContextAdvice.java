package project.qldt.controller.support;

import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import project.qldt.dto.account.CurrentUserView;
import project.qldt.security.CurrentAuthentication;
import project.qldt.service.account.CurrentUserService;

@ControllerAdvice
public class UserContextAdvice {

	private final CurrentUserService currentUserService;

	public UserContextAdvice(CurrentUserService currentUserService) {
		this.currentUserService = currentUserService;
	}

	@ModelAttribute
	public void populateUserContext(Authentication authentication, Model model) {
		Authentication auth = CurrentAuthentication.resolve(authentication);
		CurrentUserView user = currentUserService.currentUser(CurrentAuthentication.usernameOf(auth),
				CurrentAuthentication.authoritiesOf(auth));
		model.addAttribute("currentUserAccount", user.account());
		model.addAttribute("unreadNotifications", user.unreadNotifications());
		model.addAttribute("currentUserCode", user.code());
		model.addAttribute("currentUserRoleName", user.roleName());
		model.addAttribute("currentUserInitials", user.initials());
	}
}
