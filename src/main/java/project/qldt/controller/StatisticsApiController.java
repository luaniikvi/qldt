package project.qldt.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import project.qldt.dto.StatisticsResponse;
import project.qldt.service.StatisticsService;

@RestController
@RequestMapping("/api/statistics")
@PreAuthorize("hasRole('DEAN')")
public class StatisticsApiController {

	private final StatisticsService statisticsService;

	public StatisticsApiController(StatisticsService statisticsService) {
		this.statisticsService = statisticsService;
	}

	@GetMapping
	public StatisticsResponse statistics(@RequestParam Long periodId) {
		return new StatisticsResponse(statisticsService.summarize(periodId), statisticsService.rows(periodId));
	}
}