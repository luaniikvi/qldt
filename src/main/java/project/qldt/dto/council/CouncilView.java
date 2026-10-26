package project.qldt.dto.council;

import project.qldt.model.council.Council;
import project.qldt.model.council.CouncilMember;
import project.qldt.model.council.TopicResult;
import project.qldt.model.report.Report;

public record CouncilView(Council council, CouncilMember myMember, TopicResult result, boolean fullyScored,
		double averageScore, boolean requiresReviewerScore, Report report) {
}