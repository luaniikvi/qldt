package project.qldt.controller.council;

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

import project.qldt.exception.CouncilNotFoundException;
import project.qldt.exception.InvalidCouncilException;
import project.qldt.model.council.Council;
import project.qldt.model.council.CouncilRole;
import project.qldt.service.account.AccountService;
import project.qldt.service.council.CouncilService;

@Controller
@RequestMapping("/dean/councils")
@PreAuthorize("hasRole('DEAN')")
public class CouncilAdminController {

	private final CouncilService councilService;
	private final AccountService accountService;

	public CouncilAdminController(CouncilService councilService, AccountService accountService) {
		this.councilService = councilService;
		this.accountService = accountService;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("councils", councilService.findAllViews());
		model.addAttribute("topics", councilService.findTopicsReadyForCouncil());
		model.addAttribute("lecturers", accountService.findAllLecturers());
		model.addAttribute("roles", CouncilRole.values());
		return "councils/dean";
	}

	@PostMapping
	public String create(@RequestParam Long topicId, @RequestParam(required = false) String name,
			@RequestParam(name = "lecturerCode", required = false) List<String> lecturerCodes,
			@RequestParam(name = "role", required = false) List<String> roles,
			RedirectAttributes redirectAttributes) {
		try {
			Council council = councilService.createFromInput(topicId, name, lecturerCodes, roles);
			redirectAttributes.addFlashAttribute("message", "Đã lập " + council.getName());
		} catch (InvalidCouncilException | CouncilNotFoundException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			redirectAttributes.addFlashAttribute("formTopicId", topicId);
			redirectAttributes.addFlashAttribute("formName", name);
			redirectAttributes.addFlashAttribute("formLecturerCodes", lecturerCodes);
			redirectAttributes.addFlashAttribute("formRoles", roles);
		}
		return "redirect:/dean/councils";
	}

	@PostMapping("/results/{id}/publish")
	public String publish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			councilService.publish(id);
			redirectAttributes.addFlashAttribute("message", "Đã công bố kết quả");
		} catch (InvalidCouncilException | CouncilNotFoundException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/dean/councils";
	}
}
