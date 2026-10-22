package project.qldt.dto;

import project.qldt.model.Topic;

public record TopicStatisticsRow(Topic topic, String advisorNames, String teamLeader, int memberCount,
		String reportFileName, String councilName, Double score, boolean published) {
}