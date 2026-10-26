package project.qldt.controller.api.v1;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import project.qldt.dto.api.response.ReportResponse;
import project.qldt.model.report.Report;
import project.qldt.service.account.AccountService;
import project.qldt.service.report.ReportService;

@RestController
@RequestMapping("/api/v1")
public class ReportRestController extends ApiControllerSupport {

	private final ReportService reportService;

	public ReportRestController(ReportService reportService, AccountService accountService) {
		super(accountService);
		this.reportService = reportService;
	}

	@PreAuthorize("hasRole('STUDENT')")
	@PostMapping("/teams/{teamId}/report")
	public ResponseEntity<ReportResponse> submit(@PathVariable Long teamId,
			@RequestPart("file") MultipartFile file, Authentication authentication) {
		Report report = reportService.submit(teamId, currentStudent(authentication), file);
		return ResponseEntity.status(201).body(ReportResponse.of(report));
	}

	@PreAuthorize("hasRole('STUDENT')")
	@GetMapping("/teams/{teamId}/report")
	public ReportResponse byTeam(@PathVariable Long teamId, Authentication authentication) {
		return ReportResponse.of(reportService.findForTeamMember(teamId, currentStudent(authentication)));
	}

	@PreAuthorize("hasRole('DEAN')")
	@GetMapping("/reports")
	public List<ReportResponse> byPeriod(@RequestParam Long periodId) {
		return reportService.findByPeriod(periodId).stream().map(ReportResponse::of).toList();
	}

	@PreAuthorize("hasRole('LECTURER')")
	@GetMapping("/reports/mine")
	public List<ReportResponse> mine(Authentication authentication) {
		return reportService.findByAdvisor(currentLecturer(authentication)).stream().map(ReportResponse::of)
				.toList();
	}

	@GetMapping("/reports/{id}")
	public ReportResponse getOne(@PathVariable Long id, Authentication authentication) {
		return ReportResponse.of(reportService.findVisible(id, currentAccount(authentication)));
	}

	@GetMapping("/reports/{id}/file")
	public ResponseEntity<Resource> download(@PathVariable Long id, Authentication authentication) {
		ReportService.ReportFile file = reportService
				.prepareDownload(reportService.findVisible(id, currentAccount(authentication)));
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
				.contentType(file.mediaType())
				.body(file.resource());
	}
}
