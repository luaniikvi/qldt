package project.qldt.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.AccessDeniedException;
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

import project.qldt.dto.TopicRequest;
import project.qldt.exception.AccountNotFoundException;
import project.qldt.exception.InvalidTopicException;
import project.qldt.exception.TopicNotFoundException;
import project.qldt.model.Account;
import project.qldt.model.Lecturer;
import project.qldt.service.AccountService;
import project.qldt.service.RegistrationPeriodService;
import project.qldt.service.TopicService;

@Controller
@RequestMapping("/topics")
@PreAuthorize("hasRole('LECTURER')")
public class TopicController {

	private final TopicService topicService;
	private final RegistrationPeriodService periodService;
	private final AccountService accountService;

	public TopicController(TopicService topicService, RegistrationPeriodService periodService,
			AccountService accountService) {
		this.topicService = topicService;
		this.periodService = periodService;
		this.accountService = accountService;
	}

	@GetMapping
	public String list(Authentication authentication, Model model) {
		model.addAttribute("topics", topicService.findByLecturer(currentLecturer(authentication)));
		model.addAttribute("currentUser", authentication.getName());
		return "topics/list";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		return form(model, new TopicRequest(), null);
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("topic") TopicRequest request,
			BindingResult result,
			Authentication authentication,
			Model model) {
		if (result.hasErrors()) {
			return form(model, request, null);
		}
		try {
			topicService.create(request, currentLecturer(authentication));
		} catch (InvalidTopicException ex) {
			result.reject("invalid", ex.getMessage());
			return form(model, request, null);
		}
		return "redirect:/topics";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Authentication authentication, Model model) {
		return form(model, TopicRequest.from(topicService.findForEdit(id, currentLecturer(authentication))), id);
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id,
			@Valid @ModelAttribute("topic") TopicRequest request,
			BindingResult result,
			Authentication authentication,
			Model model) {
		if (result.hasErrors()) {
			return form(model, request, id);
		}
		try {
			topicService.update(id, request, currentLecturer(authentication));
		} catch (InvalidTopicException ex) {
			result.reject("invalid", ex.getMessage());
			return form(model, request, id);
		}
		return "redirect:/topics";
	}

	@PostMapping("/{id}/invite")
	public String invite(@PathVariable Long id,
			@RequestParam String lecturerCode,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			topicService.inviteAdvisor(id, lecturerCode, currentLecturer(authentication));
			redirectAttributes.addFlashAttribute("message", "Đã gửi lời mời hướng dẫn");
		} catch (InvalidTopicException | AccountNotFoundException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/topics";
	}

	@PostMapping("/{id}/invitation")
	public String respondInvitation(@PathVariable Long id,
			@RequestParam boolean accept,
			@RequestParam(required = false) String reason,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			topicService.respondInvitation(id, currentLecturer(authentication), accept, reason);
			redirectAttributes.addFlashAttribute("message",
					accept ? "Đã đồng ý lời mời hướng dẫn" : "Đã từ chối lời mời hướng dẫn");
		} catch (InvalidTopicException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/topics";
	}

	@PostMapping("/{id}/invitation/cancel")
	public String cancelInvitation(@PathVariable Long id,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			topicService.cancelAdvisorInvitation(id, currentLecturer(authentication));
			redirectAttributes.addFlashAttribute("message", "Đã hủy lời mời hướng dẫn");
		} catch (InvalidTopicException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/topics";
	}

	@PostMapping("/{id}/advisor/remove")
	public String removeCoAdvisor(@PathVariable Long id,
			@RequestParam String lecturerCode,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			topicService.removeCoAdvisor(id, lecturerCode, currentLecturer(authentication));
			redirectAttributes.addFlashAttribute("message", "Đã xóa giảng viên hướng dẫn khỏi đề tài");
		} catch (InvalidTopicException | AccountNotFoundException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/topics";
	}

	@ExceptionHandler(TopicNotFoundException.class)
	public String handleNotFound(TopicNotFoundException ex, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("error", ex.getMessage());
		return "redirect:/topics";
	}

	private String form(Model model, TopicRequest request, Long topicId) {
		model.addAttribute("topic", request);
		model.addAttribute("topicId", topicId);
		model.addAttribute("periods", periodService.findAll());
		return "topics/form";
	}

	private Lecturer currentLecturer(Authentication authentication) {
		Account account = accountService.findByUsername(authentication.getName());
		if (account instanceof Lecturer lecturer) {
			return lecturer;
		}
		throw new AccessDeniedException("Chỉ giảng viên mới sử dụng được chức năng này");
	}
}
