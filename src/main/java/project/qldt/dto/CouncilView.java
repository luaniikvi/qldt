package project.qldt.dto;

import project.qldt.model.Council;
import project.qldt.model.CouncilMember;
import project.qldt.model.TopicResult;

public record CouncilView(Council council, CouncilMember myMember, TopicResult result, boolean fullyScored) {
}