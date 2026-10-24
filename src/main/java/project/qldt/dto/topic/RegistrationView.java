package project.qldt.dto.topic;

import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;

public record RegistrationView(Topic topic, Team team) {
}
