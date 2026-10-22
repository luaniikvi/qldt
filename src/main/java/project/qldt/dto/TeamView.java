package project.qldt.dto;

import project.qldt.model.InvitationStatus;
import project.qldt.model.Report;
import project.qldt.model.Student;
import project.qldt.model.Team;

public record TeamView(Team team, boolean leader, boolean member, boolean invited, Report report) {

	public static TeamView of(Team team, Student student, Report report) {
		boolean leader = team.getLeader().getId().equals(student.getId());
		boolean member = team.getMembers().stream()
				.anyMatch(memberOfTeam -> memberOfTeam.getId().equals(student.getId()));
		boolean invited = team.getInvitations().stream()
				.anyMatch(invitation -> invitation.getStatus() == InvitationStatus.PENDING
						&& invitation.getStudent().getId().equals(student.getId()));
		return new TeamView(team, leader, member, invited, report);
	}
}