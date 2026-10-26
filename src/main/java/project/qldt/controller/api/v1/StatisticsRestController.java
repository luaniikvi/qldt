package project.qldt.controller.api.v1;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import project.qldt.dto.api.response.PeriodStatisticsResponse;
import project.qldt.dto.api.response.StatisticsApiResponse;
import project.qldt.dto.api.response.StatisticsRowResponse;
import project.qldt.dto.statistics.StatisticsCharts;
import project.qldt.service.statistics.StatisticsService;

@RestController
@RequestMapping("/api/v1/statistics")
@PreAuthorize("hasRole('DEAN')")
public class StatisticsRestController {

	private final StatisticsService statisticsService;

	public StatisticsRestController(StatisticsService statisticsService) {
		this.statisticsService = statisticsService;
	}

	@GetMapping
	public StatisticsApiResponse statistics(@RequestParam Long periodId) {
		List<StatisticsRowResponse> rows = statisticsService.rows(periodId).stream()
				.map(StatisticsRowResponse::of).toList();
		return new StatisticsApiResponse(
				PeriodStatisticsResponse.of(statisticsService.summarize(periodId)), rows);
	}

	@GetMapping("/charts")
	public StatisticsCharts charts(@RequestParam Long periodId) {
		return statisticsService.charts(periodId);
	}

	@GetMapping("/export.csv")
	public ResponseEntity<byte[]> exportCsv(@RequestParam Long periodId) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename("thong-ke-dot-" + periodId + ".csv").build().toString())
				.contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
				.body(statisticsService.exportCsvWithBom(periodId));
	}

	@GetMapping("/export.xlsx")
	public ResponseEntity<byte[]> exportXlsx(@RequestParam Long periodId) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename("thong-ke-dot-" + periodId + ".xlsx").build().toString())
				.contentType(MediaType
						.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.body(statisticsService.exportXlsx(periodId));
	}
}
