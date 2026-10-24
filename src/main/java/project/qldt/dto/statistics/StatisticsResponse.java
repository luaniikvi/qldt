package project.qldt.dto.statistics;

import project.qldt.dto.period.PeriodStatistics;
import java.util.List;

public record StatisticsResponse(PeriodStatistics summary, List<TopicStatisticsRow> rows) {
}