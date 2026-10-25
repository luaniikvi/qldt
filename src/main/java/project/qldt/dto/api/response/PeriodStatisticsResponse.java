package project.qldt.dto.api.response;

import project.qldt.dto.api.common.PeriodSummary;
import project.qldt.dto.period.PeriodStatistics;

public record PeriodStatisticsResponse(PeriodSummary period, int topicCount, int publishedTopicCount,
		int registeredTopicCount, int teamCount, int reportCount, int councilCount, int publishedResultCount,
		double averageScore) {

	public static PeriodStatisticsResponse of(PeriodStatistics statistics) {
		return new PeriodStatisticsResponse(PeriodSummary.of(statistics.period()), statistics.topicCount(),
				statistics.publishedTopicCount(), statistics.registeredTopicCount(), statistics.teamCount(),
				statistics.reportCount(), statistics.councilCount(), statistics.publishedResultCount(),
				statistics.averageScore());
	}
}
