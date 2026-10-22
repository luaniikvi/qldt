package project.qldt.controller;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import project.qldt.exception.ReportNotFoundException;
import project.qldt.model.Account;
import project.qldt.model.Lecturer;
import project.qldt.model.Report;
import project.qldt.service.AccountService;
import project.qldt.service.ReportService;

@Controller
@RequestMapping("/reports")
@PreAuthorize("hasRole('LECTURER')")
public class ReportController {

	private final ReportService reportService;
	private final AccountService accountService;

	public ReportController(ReportService reportService, AccountService accountService) {
		this.reportService = reportService;
		this.accountService = accountService;
	}

	@GetMapping
	public String list(Authentication authentication, Model model) {
		model.addAttribute("reports", reportService.findByAdvisor(currentLecturer(authentication)));
		return "reports/list";
	}

	@GetMapping("/{id}/download")
	public ResponseEntity<Resource> download(@PathVariable Long id, Authentication authentication) {
		Report report = reportService.findById(id);
		Lecturer lecturer = currentLecturer(authentication);
		boolean isAdvisor = report.getTeam().getTopic() != null
				&& report.getTeam().getTopic().getAdvisors().stream()
						.anyMatch(advisor -> advisor.getId().equals(lecturer.getId()));
		if (!isAdvisor) {
			throw new AccessDeniedException("Bạn không phải giảng viên hướng dẫn của đề tài này");
		}
		Path path = reportService.loadFile(report);
		if (!Files.exists(path)) {
			throw new ReportNotFoundException("Tệp báo cáo không còn trên máy chủ");
		}
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(report.getOriginalFileName()).build().toString())
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(new FileSystemResource(path));
	}

	@ExceptionHandler({ReportNotFoundException.class, AccessDeniedException.class})
	public String handleError(RuntimeException ex, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("error", ex.getMessage());
		return "redirect:/reports";
	}

	private Lecturer currentLecturer(Authentication authentication) {
		Account account = accountService.findByUsername(authentication.getName());
		if (account instanceof Lecturer lecturer) {
			return lecturer;
		}
		throw new AccessDeniedException("Chỉ giảng viên mới sử dụng được chức năng này");
	}
}