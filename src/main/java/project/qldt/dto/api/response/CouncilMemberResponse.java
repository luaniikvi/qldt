package project.qldt.dto.api.response;

import java.time.LocalDateTime;

import project.qldt.dto.api.common.AccountSummary;
import project.qldt.model.council.CouncilMember;
import project.qldt.model.council.CouncilRole;

public record CouncilMemberResponse(Long id, AccountSummary lecturer, CouncilRole role, Double score, String comment,
		LocalDateTime scoredAt) {

	public static CouncilMemberResponse of(CouncilMember member) {
		return new CouncilMemberResponse(member.getId(), AccountSummary.of(member.getLecturer()), member.getRole(),
				member.getScore(), member.getComment(), member.getScoredAt());
	}
}
