package project.qldt.dto.team;

import project.qldt.model.report.Report;
import project.qldt.model.account.Student;
import project.qldt.model.team.Team;

public record TeamView(Team team, boolean leader, boolean member, boolean invited, Report report) {

	public static TeamView of(Team team, Student student, Report report) {
		return new TeamView(team, team.isLeader(student.getId()), team.isMember(student.getId()),
				team.hasPendingInvitation(student.getId()), report);
	}
}