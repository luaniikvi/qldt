package project.qldt.dto.api.response;

import java.util.List;

import project.qldt.dto.api.common.TopicSummary;
import project.qldt.model.council.Council;
import project.qldt.model.council.TopicResult;

public record CouncilResponse(Long id, String name, TopicSummary topic, List<CouncilMemberResponse> members,
		double averageScore, boolean fullyScored, TopicResultResponse result) {

	public static CouncilResponse of(Council council, TopicResult result) {
		return new CouncilResponse(council.getId(), council.getName(), TopicSummary.of(council.getTopic()),
				council.getMembers().stream().map(CouncilMemberResponse::of).toList(), council.averageScore(),
				council.isFullyScored(), result == null ? null : TopicResultResponse.of(result));
	}
}
