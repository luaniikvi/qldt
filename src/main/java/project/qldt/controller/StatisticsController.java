package project.qldt.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import project.qldt.model.RegistrationPeriod;
import project.qldt.service.StatisticsService;

@Controller
@RequestMapping("/dean/statistics")
@PreAuthorize("hasRole('DEAN')")
public class StatisticsController {

	private final StatisticsService statisticsService;

	public StatisticsController(StatisticsService statisticsService) {
		this.statisticsService = statisticsService;
	}

	@GetMapping
	public String index(@RequestParam(required = false) Long periodId, Model model) {
		List<RegistrationPeriod> periods = statisticsService.periods();
		model.addAttribute("periods", periods);
		Long selected = periodId;
		if (selected == null && !periods.isEmpty()) {
			selected = periods.get(0).getId();
		}
		if (selected != null) {
			model.addAttribute("selectedPeriodId", selected);
			model.addAttribute("statistics", statisticsService.summarize(selected));
			model.addAttribute("rows", statisticsService.rows(selected));
		}
		return "statistics/index";
	}

	@GetMapping("/export")
	public ResponseEntity<byte[]> export(@RequestParam Long periodId) {
		byte[] content = statisticsService.exportCsv(periodId).getBytes(StandardCharsets.UTF_8);
		byte[] body = new byte[content.length + 3];
		body[0] = (byte) 0xEF;
		body[1] = (byte) 0xBB;
		body[2] = (byte) 0xBF;
		System.arraycopy(content, 0, body, 3, content.length);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename("thong-ke-dot-" + periodId + ".csv").build()
								.toString())
				.contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
				.body(body);
	}
}