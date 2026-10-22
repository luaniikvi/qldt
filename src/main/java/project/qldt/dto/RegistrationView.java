package project.qldt.dto;

import project.qldt.model.Team;
import project.qldt.model.Topic;

public record RegistrationView(Topic topic, Team team) {
}
