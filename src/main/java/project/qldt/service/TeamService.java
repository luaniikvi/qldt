package project.qldt.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.exception.InvalidTeamException;
import project.qldt.exception.TeamNotFoundException;
import project.qldt.model.InvitationStatus;
import project.qldt.model.NotificationType;
import project.qldt.model.RegistrationPeriod;
import project.qldt.model.Student;
import project.qldt.model.Team;
import project.qldt.model.TeamInvitation;
import project.qldt.repository.TeamRepository;

@Service
@Transactional
public class TeamService {

	private static final int MAX_MEMBERS = 3;

	private final TeamRepository teamRepository;
	private final AccountService accountService;
	private final RegistrationPeriodService periodService;
	private final NotificationService notificationService;

	public TeamService(TeamRepository teamRepository, AccountService accountService,
			RegistrationPeriodService periodService, NotificationService notificationService) {
		this.teamRepository = teamRepository;
		this.accountService = accountService;
		this.periodService = periodService;
		this.notificationService = notificationService;
	}

	@Transactional(readOnly = true)
	public List<Team> findAll() {
		return teamRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Team findById(Long id) {
		return teamRepository.findById(id)
				.orElseThrow(() -> new TeamNotFoundException("Không tìm thấy nhóm có id: " + id));
	}

	@Transactional(readOnly = true)
	public List<Team> findByStudent(Student student) {
		List<Team> teams = new ArrayList<>();
		for (Team team : teamRepository.findAll()) {
			if (isMember(team, student) || hasPendingInvitation(team, student)) {
				teams.add(team);
			}
		}
		return teams;
	}

	@Transactional(readOnly = true)
	public Team findByTopicId(Long topicId) {
		return teamRepository.findByTopicId(topicId).orElse(null);
	}

	public Team create(Long periodId, Student leader) {
		RegistrationPeriod period = periodService.findById(periodId);
		ensureNotInTeam(period, leader);
		Team team = new Team();
		team.setPeriod(period);
		team.setLeader(leader);
		team.getMembers().add(leader);
		return teamRepository.save(team);
	}

	public Team invite(Long id, String studentCode, Student actor) {
		Team team = findById(id);
		requireLeader(team, actor);
		Student invited = accountService.findStudentByCode(studentCode);
		if (invited.getId().equals(actor.getId())) {
			throw new InvalidTeamException("Không thể mời chính mình");
		}
		if (isMember(team, invited)) {
			throw new InvalidTeamException("Sinh viên này đã ở trong nhóm");
		}
		if (hasPendingInvitation(team, invited)) {
			throw new InvalidTeamException("Sinh viên này đã được mời và đang chờ phản hồi");
		}
		if (team.getMembers().size() + pendingInvitationCount(team) >= MAX_MEMBERS) {
			throw new InvalidTeamException("Nhóm đã đủ " + MAX_MEMBERS + " thành viên (kể cả lời mời đang chờ)");
		}
		ensureNotInTeam(team.getPeriod(), invited);
		TeamInvitation invitation = new TeamInvitation();
		invitation.setTeam(team);
		invitation.setStudent(invited);
		team.getInvitations().add(invitation);
		notificationService.send(invited, NotificationType.INVITATION, "Lời mời vào nhóm",
				actor.getFullName() + " mời bạn vào nhóm của " + team.getLeader().getFullName(), "/teams");
		return team;
	}

	public Team respondInvitation(Long id, Student actor, boolean accept, String reason) {
		Team team = findById(id);
		TeamInvitation invitation = findPendingInvitation(team, actor);
		if (invitation == null) {
			throw new InvalidTeamException("Không có lời mời đang chờ phản hồi");
		}
		if (accept) {
			if (team.getMembers().size() >= MAX_MEMBERS) {
				throw new InvalidTeamException("Nhóm đã đủ " + MAX_MEMBERS + " thành viên");
			}
			ensureNotInTeam(team.getPeriod(), actor);
			invitation.setStatus(InvitationStatus.ACCEPTED);
			team.getMembers().add(actor);
		} else {
			if (reason == null || reason.isBlank()) {
				throw new InvalidTeamException("Vui lòng nhập lý do từ chối");
			}
			invitation.setStatus(InvitationStatus.REJECTED);
			invitation.setResponseReason(reason);
		}
		notificationService.send(team.getLeader(), NotificationType.INVITATION,
				accept ? "Lời mời vào nhóm đã được chấp nhận" : "Lời mời vào nhóm bị từ chối",
				actor.getFullName() + (accept ? " đã đồng ý" : " đã từ chối") + " lời mời vào nhóm", "/teams");
		return team;
	}

	public Team transferLeader(Long id, String studentCode, Student actor) {
		Team team = findById(id);
		requireLeader(team, actor);
		Student newLeader = accountService.findStudentByCode(studentCode);
		boolean isMember = team.getMembers().stream()
				.anyMatch(member -> member.getId().equals(newLeader.getId()));
		if (!isMember) {
			throw new InvalidTeamException("Chỉ có thể chuyển quyền cho thành viên trong nhóm");
		}
		team.setLeader(newLeader);
		return team;
	}

	public Team leave(Long id, Student actor) {
		Team team = findById(id);
		if (team.getLeader().getId().equals(actor.getId())) {
			throw new InvalidTeamException("Nhóm trưởng phải chuyển quyền cho thành viên khác trước khi rời nhóm");
		}
		boolean removed = team.getMembers().removeIf(member -> member.getId().equals(actor.getId()));
		if (!removed) {
			throw new InvalidTeamException("Bạn không phải thành viên của nhóm");
		}
		return team;
	}

	public Team cancelInvitation(Long id, String studentCode, Student actor) {
		Team team = findById(id);
		requireLeader(team, actor);
		Student student = accountService.findStudentByCode(studentCode);
		TeamInvitation invitation = findPendingInvitation(team, student);
		if (invitation == null) {
			throw new InvalidTeamException("Không có lời mời đang chờ để hủy");
		}
		invitation.setStatus(InvitationStatus.CANCELLED);
		return team;
	}

	public Team removeMember(Long id, String studentCode, Student actor) {
		Team team = findById(id);
		requireLeader(team, actor);
		Student member = accountService.findStudentByCode(studentCode);
		if (team.getLeader().getId().equals(member.getId())) {
			throw new InvalidTeamException("Nhóm trưởng không thể tự xóa mình khỏi nhóm");
		}
		boolean removed = team.getMembers().removeIf(item -> item.getId().equals(member.getId()));
		if (!removed) {
			throw new InvalidTeamException("Sinh viên này không phải thành viên của nhóm");
		}
		team.getInvitations().stream()
				.filter(invitation -> invitation.getStudent().getId().equals(member.getId())
						&& invitation.getStatus() == InvitationStatus.ACCEPTED)
				.forEach(invitation -> invitation.setStatus(InvitationStatus.CANCELLED));
		return team;
	}

	private boolean isMember(Team team, Student student) {
		return team.getMembers().stream().anyMatch(member -> member.getId().equals(student.getId()));
	}

	private boolean hasPendingInvitation(Team team, Student student) {
		return findPendingInvitation(team, student) != null;
	}

	private TeamInvitation findPendingInvitation(Team team, Student student) {
		return team.getInvitations().stream()
				.filter(invitation -> invitation.getStatus() == InvitationStatus.PENDING)
				.filter(invitation -> invitation.getStudent().getId().equals(student.getId()))
				.findFirst()
				.orElse(null);
	}

	private long pendingInvitationCount(Team team) {
		return team.getInvitations().stream()
				.filter(invitation -> invitation.getStatus() == InvitationStatus.PENDING)
				.count();
	}

	private void ensureNotInTeam(RegistrationPeriod period, Student student) {
		for (Team team : teamRepository.findAll()) {
			if (team.getPeriod().getId().equals(period.getId()) && isMember(team, student)) {
				throw new InvalidTeamException(
						"Sinh viên " + student.getStudentCode() + " đã thuộc một nhóm trong đợt này");
			}
		}
	}

	private void requireLeader(Team team, Student actor) {
		if (!team.getLeader().getId().equals(actor.getId())) {
			throw new InvalidTeamException("Chỉ nhóm trưởng mới được thực hiện thao tác này");
		}
	}
}
