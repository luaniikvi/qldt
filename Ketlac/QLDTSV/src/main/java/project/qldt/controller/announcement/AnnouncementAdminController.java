package project.qldt.controller.announcement;

import org.springframework.security.access.prepost.PreAuthorize;
import project.qldt.exception.AnnouncementNotFoundException;
import project.qldt.exception.InvalidAnnouncementException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import project.qldt.model.announcement.Announcement;
import project.qldt.service.announcement.AnnouncementService;

@Controller
@RequestMapping("/admin/announcements")
@PreAuthorize("hasRole('ADMIN')")
public class AnnouncementAdminController {

	private final AnnouncementService announcementService;

	public AnnouncementAdminController(AnnouncementService announcementService) {
		this.announcementService = announcementService;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("announcements", announcementService.findAll());
		return "admin/announcements/list";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		model.addAttribute("announcement", new Announcement());
		return "admin/announcements/form";
	}

	@PostMapping
	public String create(@RequestParam String title,
			@RequestParam String content,
			RedirectAttributes redirectAttributes) {
		try {
			announcementService.create(title, content);
			redirectAttributes.addFlashAttribute("message", "Đã đăng thông báo thành công");
			return "redirect:/admin/announcements";
		} catch (InvalidAnnouncementException | AnnouncementNotFoundException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			return "redirect:/admin/announcements/new";
		}
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		model.addAttribute("announcement", announcementService.findById(id));
		return "admin/announcements/form";
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id,
			@RequestParam String title,
			@RequestParam String content,
			RedirectAttributes redirectAttributes) {
		try {
			announcementService.update(id, title, content);
			redirectAttributes.addFlashAttribute("message", "Đã cập nhật thông báo thành công");
			return "redirect:/admin/announcements";
		} catch (InvalidAnnouncementException | AnnouncementNotFoundException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			return "redirect:/admin/announcements/" + id + "/edit";
		}
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			announcementService.deleteById(id);
			redirectAttributes.addFlashAttribute("message", "Đã xóa thông báo");
		} catch (AnnouncementNotFoundException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/announcements";
	}
}
