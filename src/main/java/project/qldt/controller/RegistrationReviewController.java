package project.qldt.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import project.qldt.exception.InvalidRegistrationException;
import project.qldt.exception.TopicNotFoundException;
import project.qldt.model.Account;
import project.qldt.model.Lecturer;
import project.qldt.service.AccountService;
import project.qldt.service.TopicRegistrationService;

@Controller
@RequestMapping("/registrations")
@PreAuthorize("hasRole('LECTURER')")
public class RegistrationReviewController {

	private final TopicRegistrationService registrationService;
	private final AccountService accountService;

	public RegistrationReviewController(TopicRegistrationService registrationService,
			AccountService accountService) {
		this.registrationService = registrationService;
		this.accountService = accountService;
	}

	@GetMapping
	public String list(Authentication authentication, Model model) {
		model.addAttribute("registrations",
				registrationService.findPendingForAdvisor(currentLecturer(authentication)));
		return "registrations/list";
	}

	@PostMapping("/{topicId}/approve")
	public String approve(@PathVariable Long topicId,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			registrationService.approve(topicId, currentLecturer(authentication));
			redirectAttributes.addFlashAttribute("message", "Đã duyệt đăng ký đề tài");
		} catch (InvalidRegistrationException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/registrations";
	}

	@PostMapping("/{topicId}/reject")
	public String reject(@PathVariable Long topicId,
			@RequestParam(required = false) String reason,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			registrationService.reject(topicId, currentLecturer(authentication), reason);
			redirectAttributes.addFlashAttribute("message", "Đã từ chối đăng ký đề tài");
		} catch (InvalidRegistrationException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/registrations";
	}

	@ExceptionHandler(TopicNotFoundException.class)
	public String handleNotFound(TopicNotFoundException ex, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("error", ex.getMessage());
		return "redirect:/registrations";
	}

	private Lecturer currentLecturer(Authentication authentication) {
		Account account = accountService.findByUsername(authentication.getName());
		if (account instanceof Lecturer lecturer) {
			return lecturer;
		}
		throw new AccessDeniedException("Chỉ giảng viên mới sử dụng được chức năng này");
	}
}
