package project.qldt.dto.api.response;

import project.qldt.dto.api.common.AccountSummary;
import project.qldt.model.topic.AdvisorInvitation;
import project.qldt.model.topic.InvitationStatus;

public record AdvisorInvitationResponse(Long id, AccountSummary lecturer, InvitationStatus status,
		String responseReason) {

	public static AdvisorInvitationResponse of(AdvisorInvitation invitation) {
		return invitation == null
				? null
				: new AdvisorInvitationResponse(invitation.getId(), AccountSummary.of(invitation.getLecturer()),
						invitation.getStatus(), invitation.getResponseReason());
	}
}
