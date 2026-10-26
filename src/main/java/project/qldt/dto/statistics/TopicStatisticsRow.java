package project.qldt.dto.statistics;

import project.qldt.model.topic.Topic;

public record TopicStatisticsRow(Topic topic, String advisorNames, String teamLeader, int memberCount,
		String reportFileName, String councilName, Double score, boolean published, boolean requiresReviewerScore) {
}