package project.qldt.controller.report;

import java.nio.charset.StandardCharsets;

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

import project.qldt.exception.InvalidReportException;
import project.qldt.exception.ReportNotFoundException;
import project.qldt.model.account.Account;
import project.qldt.model.account.Lecturer;
import project.qldt.model.report.Report;
import project.qldt.service.account.AccountService;
import project.qldt.service.report.ReportService;

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
		ReportService.ReportFile file = reportService
				.prepareDownload(reportService.findForLecturer(id, currentLecturer(authentication)));
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(file.resource());
	}

	@GetMapping("/{id}/view")
	public ResponseEntity<Resource> view(@PathVariable Long id, Authentication authentication) {
		ReportService.ReportFile file = reportService
				.prepareDownload(reportService.findForLecturer(id, currentLecturer(authentication)));
		MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
		String lower = file.fileName().toLowerCase();
		if (lower.endsWith(".pdf")) {
			mediaType = MediaType.APPLICATION_PDF;
		}
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
						.filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
				.contentType(mediaType)
				.body(file.resource());
	}

	@GetMapping("/topic/{topicId}/view")
	public ResponseEntity<Resource> viewByTopic(@PathVariable Long topicId, Authentication authentication) {
		Report report = reportService.findByTopicId(topicId);
		if (report == null) {
			throw new ReportNotFoundException("Nhóm chưa nộp báo cáo");
		}
		return view(report.getId(), authentication);
	}

	@GetMapping("/topic/{topicId}/download")
	public ResponseEntity<Resource> downloadByTopic(@PathVariable Long topicId, Authentication authentication) {
		Report report = reportService.findByTopicId(topicId);
		if (report == null) {
			throw new ReportNotFoundException("Nhóm chưa nộp báo cáo");
		}
		return download(report.getId(), authentication);
	}

	@ExceptionHandler({ReportNotFoundException.class, AccessDeniedException.class, InvalidReportException.class})
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