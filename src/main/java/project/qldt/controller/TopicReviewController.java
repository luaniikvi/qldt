package project.qldt.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import project.qldt.exception.InvalidTopicException;
import project.qldt.exception.TopicNotFoundException;
import project.qldt.model.TopicStatus;
import project.qldt.service.TopicService;

@Controller
@RequestMapping("/dean/topics")
@PreAuthorize("hasRole('DEAN')")
public class TopicReviewController {

	private final TopicService topicService;

	public TopicReviewController(TopicService topicService) {
		this.topicService = topicService;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("topics", topicService.findByStatus(TopicStatus.PENDING));
		model.addAttribute("periods", topicService.findPeriodsWithApprovedTopics());
		return "topics/pending";
	}

	@PostMapping("/{id}/approve")
	public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			topicService.approve(id);
			redirectAttributes.addFlashAttribute("message", "Đã duyệt đề tài");
		} catch (InvalidTopicException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/dean/topics";
	}

	@PostMapping("/{id}/reject")
	public String reject(@PathVariable Long id,
			@RequestParam(required = false) String reason,
			RedirectAttributes redirectAttributes) {
		try {
			topicService.reject(id, reason);
			redirectAttributes.addFlashAttribute("message", "Đã từ chối đề tài");
		} catch (InvalidTopicException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/dean/topics";
	}

	@PostMapping("/publish/{periodId}")
	public String publish(@PathVariable Long periodId, RedirectAttributes redirectAttributes) {
		try {
			int count = topicService.publish(periodId);
			redirectAttributes.addFlashAttribute("message", "Đã công bố " + count + " đề tài");
		} catch (InvalidTopicException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/dean/topics";
	}

	@ExceptionHandler(TopicNotFoundException.class)
	public String handleNotFound(TopicNotFoundException ex, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("error", ex.getMessage());
		return "redirect:/dean/topics";
	}
}
