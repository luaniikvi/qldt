package project.qldt.controller.council;

import java.util.List;

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

import project.qldt.exception.CouncilNotFoundException;
import project.qldt.exception.InvalidCouncilException;
import project.qldt.model.account.Account;
import project.qldt.model.account.Lecturer;
import project.qldt.service.account.AccountService;
import project.qldt.service.council.CouncilService;

@Controller
@RequestMapping("/councils")
@PreAuthorize("hasRole('LECTURER')")
public class CouncilController {

	private final CouncilService councilService;
	private final AccountService accountService;

	public CouncilController(CouncilService councilService, AccountService accountService) {
		this.councilService = councilService;
		this.accountService = accountService;
	}

	@GetMapping
	public String list(Authentication authentication, Model model) {
		model.addAttribute("councils", councilService.findViewsFor(currentLecturer(authentication)));
		return "councils/list";
	}

	@PostMapping("/{id}/score")
	public String score(@PathVariable Long id, @RequestParam double score,
			@RequestParam(required = false) String comment,
			Authentication authentication, RedirectAttributes redirectAttributes) {
		try {
			councilService.score(id, currentLecturer(authentication), score, comment);
			redirectAttributes.addFlashAttribute("message", "Đã lưu điểm của bạn");
		} catch (InvalidCouncilException | CouncilNotFoundException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/councils";
	}

	@PostMapping("/{id}/confirm")
	public String confirm(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
		try {
			councilService.confirm(id, currentLecturer(authentication));
			redirectAttributes.addFlashAttribute("message", "Đã tổng hợp điểm, chờ trưởng khoa công bố");
		} catch (InvalidCouncilException | CouncilNotFoundException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/councils";
	}

	@ExceptionHandler(AccessDeniedException.class)
	public String handleDenied(AccessDeniedException ex, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("error", ex.getMessage());
		return "redirect:/home";
	}

	private Lecturer currentLecturer(Authentication authentication) {
		Account account = accountService.findByUsername(authentication.getName());
		if (account instanceof Lecturer lecturer) {
			return lecturer;
		}
		throw new AccessDeniedException("Chỉ giảng viên mới sử dụng được chức năng này");
	}
}