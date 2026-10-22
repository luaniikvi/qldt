package project.qldt.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import project.qldt.dto.RegistrationView;
import project.qldt.exception.InvalidRegistrationException;
import project.qldt.model.Team;
import project.qldt.model.Topic;
import project.qldt.service.TeamService;
import project.qldt.service.TopicRegistrationService;
import project.qldt.service.TopicService;

@Controller
@RequestMapping("/dean/registrations")
@PreAuthorize("hasRole('DEAN')")
public class DeanRegistrationController {

	private final TopicService topicService;
	private final TeamService teamService;
	private final TopicRegistrationService registrationService;

	public DeanRegistrationController(TopicService topicService, TeamService teamService,
			TopicRegistrationService registrationService) {
		this.topicService = topicService;
		this.teamService = teamService;
		this.registrationService = registrationService;
	}

	@GetMapping
	public String list(Model model) {
		List<RegistrationView> registrations = new ArrayList<>();
		for (Topic topic : topicService.findAll()) {
			Team team = teamService.findByTopicId(topic.getId());
			if (team != null) {
				registrations.add(new RegistrationView(topic, team));
			}
		}
		model.addAttribute("registrations", registrations);
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