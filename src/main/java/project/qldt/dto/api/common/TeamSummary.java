package project.qldt.dto.api.common;

import project.qldt.model.team.Team;

public record TeamSummary(Long id, Long periodId, Long leaderId, String leaderName, int memberCount, Long topicId,
		String topicName) {

	public static TeamSummary of(Team team) {
		return team == null
				? null
				: new TeamSummary(team.getId(), team.getPeriod() == null ? null : team.getPeriod().getId(),
						team.getLeader() == null ? null : team.getLeader().getId(),
						team.getLeader() == null ? null : team.getLeader().getFullName(),
						team.getMembers() == null ? 0 : team.getMembers().size(),
						team.getTopic() == null ? null : team.getTopic().getId(),
						team.getTopic() == null ? null : team.getTopic().getName());
	}
}
