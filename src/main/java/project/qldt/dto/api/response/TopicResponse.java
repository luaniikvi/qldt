package project.qldt.dto.api.response;

import java.time.LocalDateTime;
import java.util.List;

import project.qldt.dto.api.common.AccountSummary;
import project.qldt.dto.api.common.DepartmentResponse;
import project.qldt.dto.api.common.PeriodSummary;
import project.qldt.model.topic.Topic;
import project.qldt.model.topic.TopicAvailability;
import project.qldt.model.topic.TopicStatus;

public record TopicResponse(Long id, String name, String description, PeriodSummary period,
		DepartmentResponse department, AccountSummary primaryAdvisor, List<AccountSummary> advisors,
		AccountSummary reviewer, Double reviewerScore, String reviewerComment, LocalDateTime reviewerScoredAt,
		TopicStatus status, TopicAvailability availability, String rejectionReason,
		AdvisorInvitationResponse invitation) {

	public static TopicResponse of(Topic topic) {
		return new TopicResponse(topic.getId(), topic.getName(), topic.getDescription(),
				PeriodSummary.of(topic.getPeriod()), DepartmentResponse.of(topic.getDepartment()),
				AccountSummary.of(topic.getPrimaryAdvisor()),
				topic.getAdvisors().stream().map(AccountSummary::of).toList(),
				AccountSummary.of(topic.getReviewer()), topic.getReviewerScore(), topic.getReviewerComment(),
				topic.getReviewerScoredAt(), topic.getStatus(), topic.getAvailability(),
				topic.getRejectionReason(), AdvisorInvitationResponse.of(topic.getInvitation()));
	}
}
