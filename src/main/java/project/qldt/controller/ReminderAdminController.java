package project.qldt.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import project.qldt.exception.InvalidNotificationException;
import project.qldt.service.ReminderService;

@Controller
@RequestMapping("/dean/reminders")
@PreAuthorize("hasRole('DEAN')")
public class ReminderAdminController {

	private final ReminderService reminderService;

	public ReminderAdminController(ReminderService reminderService) {
		this.reminderService = reminderService;
	}

	@GetMapping
	public String index(Model model) {
		model.addAttribute("periods", reminderService.periods());
		return "reminders/index";
	}

	@PostMapping("/{periodId}/report-deadline")
	public String remindReportDeadline(@PathVariable Long periodId, RedirectAttributes redirectAttributes) {
		try {
			int sent = reminderService.remindReportDeadline(periodId);
			redirectAttributes.addFlashAttribute("message", "Đã gửi " + sent + " thông báo nhắc hạn nộp báo cáo");
		} catch (InvalidNotificationException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/dean/reminders";
	}

	@PostMapping("/{periodId}/council-date")
	public String remindCouncilDate(@PathVariable Long periodId, RedirectAttributes redirectAttributes) {
		try {
			int sent = reminderService.remindCouncilDate(periodId);
			redirectAttributes.addFlashAttribute("message", "Đã gửi " + sent + " thông báo nhắc lịch báo cáo");
		} catch (InvalidNotificationException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/dean/reminders";
	}
}