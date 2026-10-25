package project.qldt.controller.reviewer;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import project.qldt.exception.InvalidTopicException;
import project.qldt.model.account.Account;
import project.qldt.model.account.Lecturer;
import project.qldt.model.topic.Topic;
import project.qldt.service.account.AccountService;
import project.qldt.service.report.ReportService;
import project.qldt.service.reviewer.ReviewerService;
import java.util.List;

@Controller
public class ReviewerController {

	private final ReviewerService reviewerService;
	private final AccountService accountService;
	private final ReportService reportService;

	public ReviewerController(ReviewerService reviewerService, AccountService accountService,
			ReportService reportService) {
		this.reviewerService = reviewerService;
		this.accountService = accountService;
		this.reportService = reportService;
	}

	@GetMapping("/reviewer/topics")
	@PreAuthorize("hasRole('LECTURER')")
	public String myTopics(Authentication authentication, Model model) {
		Lecturer lecturer = currentLecturer(authentication);
		List<Topic> topics = reviewerService.findAssigned(lecturer);
		model.addAttribute("topics", topics);
		List<Long> topicIds = topics.stream().map(Topic::getId).toList();
		model.addAttribute("reports", reportService.findReportsByTopicIds(topicIds));
		return "reviewer/topics";
	}

	@PostMapping("/reviewer/topics/{id}/score")
	@PreAuthorize("hasRole('LECTURER')")
	public String score(@PathVariable Long id, @RequestParam double score,
			@RequestParam(required = false) String comment, Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			reviewerService.score(id, currentLecturer(authentication), score, comment);
			redirectAttributes.addFlashAttribute("message", "Đã gửi điểm phản biện");
		} catch (InvalidTopicException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/reviewer/topics";
	}

	@GetMapping("/reviewer-scores")
	public String scores(Authentication authentication, Model model) {
		Account account = accountService.findByUsername(authentication.getName());
		boolean dean = account instanceof Lecturer lecturer && lecturer.isDean();
		model.addAttribute("topics", reviewerService.findVisibleScores(account));
		model.addAttribute("isDean", dean);
		return "reviewer/scores";
	}

	private Lecturer currentLecturer(Authentication authentication) {
		Account account = accountService.findByUsername(authentication.getName());
		if (account instanceof Lecturer lecturer) {
			return lecturer;
		}
		throw new AccessDeniedException("Chỉ giảng viên mới sử dụng được chức năng này");
	}
}
