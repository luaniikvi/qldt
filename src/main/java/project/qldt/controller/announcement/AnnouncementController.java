package project.qldt.controller.announcement;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import project.qldt.service.announcement.AnnouncementService;

@Controller
@RequestMapping("/announcements")
@PreAuthorize("isAuthenticated()")
public class AnnouncementController {

	private final AnnouncementService announcementService;

	public AnnouncementController(AnnouncementService announcementService) {
		this.announcementService = announcementService;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("announcements", announcementService.findAll());
		return "announcements/list";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		model.addAttribute("announcement", announcementService.findById(id));
		return "announcements/detail";
	}
}
