package project.qldt.controller.statistics;

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

import project.qldt.model.period.RegistrationPeriod;
import project.qldt.service.statistics.StatisticsService;

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
		Long selected = statisticsService.resolvePeriodId(periodId);
		if (selected != null) {
			model.addAttribute("selectedPeriodId", selected);
			model.addAttribute("statistics", statisticsService.summarize(selected));
			model.addAttribute("rows", statisticsService.rows(selected));
			model.addAttribute("charts", statisticsService.charts(selected));
		}
		return "statistics/index";
	}

	@GetMapping("/export-xlsx")
	public ResponseEntity<byte[]> exportXlsx(@RequestParam Long periodId) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename("thong-ke-dot-" + periodId + ".xlsx").build().toString())
				.contentType(MediaType
						.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.body(statisticsService.exportXlsx(periodId));
	}

	@GetMapping("/export")
	public ResponseEntity<byte[]> export(@RequestParam Long periodId) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename("thong-ke-dot-" + periodId + ".csv").build()
								.toString())
				.contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
				.body(statisticsService.exportCsvWithBom(periodId));
	}
}