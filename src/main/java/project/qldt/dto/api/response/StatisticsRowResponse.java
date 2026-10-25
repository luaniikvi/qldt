package project.qldt.dto.api.response;

import project.qldt.dto.statistics.TopicStatisticsRow;

public record StatisticsRowResponse(Long topicId, String topicName, String advisorNames, String teamLeader,
		int memberCount, String reportFileName, String councilName, Double score, boolean published) {

	public static StatisticsRowResponse of(TopicStatisticsRow row) {
		return new StatisticsRowResponse(row.topic().getId(), row.topic().getName(), row.advisorNames(),
				row.teamLeader(), row.memberCount(), row.reportFileName(), row.councilName(), row.score(),
				row.published());
	}
}
