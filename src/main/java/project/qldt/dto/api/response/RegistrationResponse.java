package project.qldt.dto.api.response;

import project.qldt.dto.topic.RegistrationView;
import project.qldt.model.report.Report;

public record RegistrationResponse(TopicResponse topic, TeamResponse team) {

	public static RegistrationResponse of(RegistrationView view, Report report) {
		return new RegistrationResponse(TopicResponse.of(view.topic()), TeamResponse.of(view.team(), report));
	}
}
