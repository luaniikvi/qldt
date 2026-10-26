package project.qldt.dto.team;

import project.qldt.model.report.Report;
import project.qldt.model.team.Team;

public record TeamView(Team team, boolean leader, boolean member, boolean invited, Report report) {
}