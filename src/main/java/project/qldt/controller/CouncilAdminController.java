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

import project.qldt.dto.CouncilView;
import project.qldt.exception.InvalidCouncilException;
import project.qldt.model.Council;
import project.qldt.model.CouncilRole;
import project.qldt.service.AccountService;
import project.qldt.service.CouncilService;

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
		List<CouncilView> views = new ArrayList<>();
		for (Council council : councilService.findAll()) {
			boolean fullyScored = council.getMembers().stream().allMatch(member -> member.getScore() != null);
			views.add(
					new CouncilView(council, null, councilService.findResultByCouncilId(council.getId()), fullyScored));
		}
		model.addAttribute("councils", views);
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
			List<String> codes = new ArrayList<>();
			List<CouncilRole> parsedRoles = new ArrayList<>();
			if (lecturerCodes != null) {
				for (int i = 0; i < lecturerCodes.size(); i++) {
					String code = lecturerCodes.get(i);
					if (code == null || code.isBlank()) {
						continue;
					}
					codes.add(code.trim());
					parsedRoles.add(parseRole(roles, i));
				}
			}
			Council council = councilService.create(topicId, name, codes, parsedRoles);
			redirectAttributes.addFlashAttribute("message", "Đã lập " + council.getName());
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/dean/councils";
	}

	@PostMapping("/results/{id}/publish")
	public String publish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			councilService.publish(id);
			redirectAttributes.addFlashAttribute("message", "Đã công bố kết quả");
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/dean/councils";
	}

	private CouncilRole parseRole(List<String> roles, int index) {
		if (roles == null || roles.size() <= index || roles.get(index).isBlank()) {
			return CouncilRole.MEMBER;
		}
		try {
			return CouncilRole.valueOf(roles.get(index));
		} catch (IllegalArgumentException ex) {
			throw new InvalidCouncilException("Vai trò trong hội đồng không hợp lệ");
		}
	}
}