package project.qldt.controller.account;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import project.qldt.dto.account.ChangePasswordRequest;
import project.qldt.exception.InvalidAccountException;
import project.qldt.security.CurrentAuthentication;
import project.qldt.service.account.AccountService;

@Controller
@RequestMapping("/account/change-password")
public class AccountSettingsController {

	private final AccountService accountService;

	public AccountSettingsController(AccountService accountService) {
		this.accountService = accountService;
	}

	@GetMapping
	public String form(Model model) {
		if (!model.containsAttribute("passwordForm")) {
			model.addAttribute("passwordForm", new ChangePasswordRequest());
		}
		return "account/change-password";
	}

	@PostMapping
	public String changePassword(@Valid @ModelAttribute("passwordForm") ChangePasswordRequest request,
			BindingResult bindingResult,
			Authentication authentication,
			RedirectAttributes redirectAttributes,
			Model model) {
		if (CurrentAuthentication.resolve(authentication) == null) {
			return "redirect:/login";
		}
		if (bindingResult.hasErrors()) {
			return "account/change-password";
		}
		try {
			accountService.changePassword(authentication.getName(), request);
			redirectAttributes.addFlashAttribute("message", "Đổi mật khẩu thành công!");
			return "redirect:/account/change-password";
		} catch (InvalidAccountException ex) {
			bindingResult.reject("invalid", ex.getMessage());
			return "account/change-password";
		}
	}
}
