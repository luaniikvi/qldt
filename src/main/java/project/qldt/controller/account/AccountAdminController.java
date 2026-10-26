package project.qldt.controller.account;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import project.qldt.dto.account.AccountRequest;
import project.qldt.exception.AccountNotFoundException;
import project.qldt.exception.InvalidAccountException;
import project.qldt.model.account.AccountType;
import project.qldt.security.CurrentAuthentication;
import project.qldt.service.account.AccountService;
import project.qldt.service.department.DepartmentService;

@Controller
@RequestMapping("/admin/accounts")
@PreAuthorize("hasRole('ADMIN')")
public class AccountAdminController {

	private final AccountService accountService;
	private final DepartmentService departmentService;

	public AccountAdminController(AccountService accountService, DepartmentService departmentService) {
		this.accountService = accountService;
		this.departmentService = departmentService;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("accounts", accountService.findAll());
		return "admin/accounts/list";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		return form(model, new AccountRequest());
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("account") AccountRequest request,
			BindingResult result,
			Model model) {
		if (result.hasErrors()) {
			return form(model, request);
		}
		try {
			accountService.createAccount(request);
		} catch (InvalidAccountException ex) {
			result.reject("invalid", ex.getMessage());
			return form(model, request);
		}
		return "redirect:/admin/accounts";
	}

	@PostMapping("/{id}/lock")
	public String lock(@PathVariable Long id) {
		accountService.setEnabled(id, false);
		return "redirect:/admin/accounts";
	}

	@PostMapping("/{id}/unlock")
	public String unlock(@PathVariable Long id) {
		accountService.setEnabled(id, true);
		return "redirect:/admin/accounts";
	}

	@PostMapping("/{id}/dean")
	public String setDean(@PathVariable Long id,
			@RequestParam boolean dean,
			RedirectAttributes redirectAttributes) {
		try {
			accountService.setDean(id, dean);
		} catch (InvalidAccountException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/accounts";
	}

	@PostMapping("/{id}/reset-password")
	public String resetPassword(@PathVariable Long id,
			@RequestParam String password,
			RedirectAttributes redirectAttributes) {
		try {
			accountService.resetPassword(id, password);
			redirectAttributes.addFlashAttribute("message", "Đã đặt lại mật khẩu");
		} catch (InvalidAccountException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/accounts";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			String currentUsername = CurrentAuthentication.usernameOf(authentication);
			accountService.deleteAccount(id, currentUsername);
			redirectAttributes.addFlashAttribute("message", "Đã xóa tài khoản thành công");
		} catch (InvalidAccountException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/accounts";
	}

	@ExceptionHandler(AccountNotFoundException.class)
	public String handleNotFound(AccountNotFoundException ex, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("error", ex.getMessage());
		return "redirect:/admin/accounts";
	}

	private String form(Model model, AccountRequest request) {
		model.addAttribute("account", request);
		model.addAttribute("accountTypes", AccountType.values());
		model.addAttribute("departments", departmentService.findAll());
		return "admin/accounts/form";
	}
}
