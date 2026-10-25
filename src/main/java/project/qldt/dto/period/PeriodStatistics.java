package project.qldt.dto.period;

import project.qldt.model.period.RegistrationPeriod;

public record PeriodStatistics(RegistrationPeriod period, int topicCount, int publishedTopicCount,
		int registeredTopicCount, int teamCount, int reportCount, int councilCount, int publishedResultCount,
		double averageScore) {
}