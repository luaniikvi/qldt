package project.qldt.dto.api.response;

import java.util.List;

import project.qldt.dto.api.common.AccountSummary;
import project.qldt.dto.api.common.PeriodSummary;
import project.qldt.dto.api.common.TopicSummary;
import project.qldt.model.report.Report;
import project.qldt.model.team.Team;
import project.qldt.model.topic.InvitationStatus;

public record TeamResponse(Long id, PeriodSummary period, AccountSummary leader, List<AccountSummary> members,
		List<TeamInvitationResponse> pendingInvitations, TopicSummary topic, String registrationRejectionReason,
		ReportResponse report) {

	public static TeamResponse of(Team team, Report report) {
		return new TeamResponse(team.getId(), PeriodSummary.of(team.getPeriod()),
				AccountSummary.of(team.getLeader()), team.getMembers().stream().map(AccountSummary::of).toList(),
				team.getInvitations().stream()
						.filter(invitation -> invitation.getStatus() == InvitationStatus.PENDING)
						.map(TeamInvitationResponse::of).toList(),
				TopicSummary.of(team.getTopic()), team.getRegistrationRejectionReason(),
				report == null ? null : ReportResponse.of(report));
	}
}
