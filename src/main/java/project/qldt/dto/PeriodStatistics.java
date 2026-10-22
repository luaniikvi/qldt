package project.qldt.dto;

import project.qldt.model.RegistrationPeriod;

public record PeriodStatistics(RegistrationPeriod period, int topicCount, int publishedTopicCount,
		int registeredTopicCount, int teamCount, int reportCount, int councilCount, int publishedResultCount,
		double averageScore) {
}