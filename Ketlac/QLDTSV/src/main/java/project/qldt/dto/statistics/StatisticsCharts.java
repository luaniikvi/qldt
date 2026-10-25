package project.qldt.dto.statistics;

import java.util.List;

public record StatisticsCharts(List<StatisticsChart> statusBars, List<StatisticsChart> progressBars,
		List<StatisticsChart> scoreBars) {
}