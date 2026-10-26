package project.qldt.controller.topic;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import project.qldt.exception.InvalidRegistrationException;
import project.qldt.service.topic.TopicRegistrationService;

@Controller
@RequestMapping("/dean/registrations")
@PreAuthorize("hasRole('DEAN')")
public class DeanRegistrationController {

	private final TopicRegistrationService registrationService;

	public DeanRegistrationController(TopicRegistrationService registrationService) {
		this.registrationService = registrationService;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("registrations", registrationService.findRegistrationsForDean());
		return "registrations/dean";
	}

	@PostMapping("/{topicId}/cancel")
	public String cancel(@PathVariable Long topicId, @RequestParam String reason,
			RedirectAttributes redirectAttributes) {
		try {
			registrationService.cancelByDean(topicId, reason);
			redirectAttributes.addFlashAttribute("message", "Đã hủy đăng ký đề tài");
		} catch (InvalidRegistrationException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/dean/registrations";
	}
}