package project.qldt.controller.reviewer;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import project.qldt.exception.AccountNotFoundException;
import project.qldt.exception.InvalidTopicException;
import project.qldt.service.account.AccountService;
import project.qldt.service.reviewer.ReviewerService;

@Controller
@RequestMapping("/dean/reviewers")
@PreAuthorize("hasRole('DEAN')")
public class ReviewerAdminController {

	private final ReviewerService reviewerService;
	private final AccountService accountService;

	public ReviewerAdminController(ReviewerService reviewerService, AccountService accountService) {
		this.reviewerService = reviewerService;
		this.accountService = accountService;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("topics", reviewerService.findAssignable());
		model.addAttribute("lecturers", accountService.findAllLecturers());
		return "reviewer/admin";
	}

	@PostMapping
	public String assign(@RequestParam Long topicId, @RequestParam String lecturerCode,
			RedirectAttributes redirectAttributes) {
		try {
			reviewerService.assign(topicId, lecturerCode);
			redirectAttributes.addFlashAttribute("message", "Đã phân công giảng viên phản biện");
		} catch (InvalidTopicException | AccountNotFoundException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/dean/reviewers";
	}
}
