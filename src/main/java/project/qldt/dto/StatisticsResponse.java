package project.qldt.dto;

import java.util.List;

public record StatisticsResponse(PeriodStatistics summary, List<TopicStatisticsRow> rows) {
}