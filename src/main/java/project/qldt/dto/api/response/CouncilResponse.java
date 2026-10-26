package project.qldt.dto.api.response;

import java.util.List;

import project.qldt.dto.api.common.TopicSummary;
import project.qldt.dto.council.CouncilView;
import project.qldt.model.council.Council;

public record CouncilResponse(Long id, String name, TopicSummary topic, List<CouncilMemberResponse> members,
		double averageScore, boolean fullyScored, TopicResultResponse result) {

	public static CouncilResponse of(CouncilView view) {
		Council council = view.council();
		return new CouncilResponse(council.getId(), council.getName(), TopicSummary.of(council.getTopic()),
				council.getMembers().stream().map(CouncilMemberResponse::of).toList(), view.averageScore(),
				view.fullyScored(), view.result() == null ? null : TopicResultResponse.of(view.result()));
	}
}
