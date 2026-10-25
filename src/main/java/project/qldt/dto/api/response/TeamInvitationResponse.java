package project.qldt.dto.api.response;

import project.qldt.dto.api.common.AccountSummary;
import project.qldt.model.team.TeamInvitation;
import project.qldt.model.topic.InvitationStatus;

public record TeamInvitationResponse(Long id, AccountSummary student, InvitationStatus status,
		String responseReason) {

	public static TeamInvitationResponse of(TeamInvitation invitation) {
		return new TeamInvitationResponse(invitation.getId(), AccountSummary.of(invitation.getStudent()),
				invitation.getStatus(), invitation.getResponseReason());
	}
}
