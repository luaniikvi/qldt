package project.qldt.service.team;

import project.qldt.service.account.AccountService;
import project.qldt.service.notification.NotificationService;
import project.qldt.service.period.RegistrationPeriodService;
import java.util.ArrayList;
import java.util.Collection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.exception.InvalidTeamException;
import project.qldt.exception.TeamNotFoundException;
import project.qldt.model.topic.InvitationStatus;
import project.qldt.model.notification.NotificationType;
import project.qldt.dto.team.TeamView;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.report.Report;
import project.qldt.model.account.Student;
import project.qldt.model.team.Team;
import project.qldt.model.team.TeamInvitation;
import project.qldt.repository.account.StudentRepository;
import project.qldt.repository.report.ReportRepository;
import project.qldt.repository.team.TeamRepository;

@Service
@Transactional
public class TeamService {

	public static final int MIN_MEMBERS = 3;
	public static final int MAX_MEMBERS = 5;

	private final TeamRepository teamRepository;
	private final AccountService accountService;
	private final RegistrationPeriodService periodService;
	private final NotificationService notificationService;
	private final ReportRepository reportRepository;
	private final StudentRepository studentRepository;

	public TeamService(TeamRepository teamRepository, AccountService accountService,
			RegistrationPeriodService periodService, NotificationService notificationService,
			ReportRepository reportRepository, StudentRepository studentRepository) {
		this.teamRepository = teamRepository;
		this.accountService = accountService;
		this.periodService = periodService;
		this.notificationService = notificationService;
		this.reportRepository = reportRepository;
		this.studentRepository = studentRepository;
	}

	@Transactional(readOnly = true)
	public Team findById(Long id) {
		return teamRepository.findById(id)
				.orElseThrow(() -> new TeamNotFoundException("Không tìm thấy nhóm có id: " + id));
	}

	@Transactional(readOnly = true)
	public List<Team> findByStudent(Student student) {
		return teamRepository.findInvolvedOf(student.getId(), InvitationStatus.PENDING);
	}

	@Transactional(readOnly = true)
	public List<Team> findByPeriod(Long periodId) {
		return teamRepository.findByPeriodIdOrderByIdAsc(periodId);
	}

	@Transactional(readOnly = true)
	public List<Team> findByTopicIds(Collection<Long> topicIds) {
		return topicIds.isEmpty() ? List.of() : teamRepository.findByTopicIdIn(topicIds);
	}

	@Transactional(readOnly = true)
	public List<Team> findAllWithTopic() {
		return teamRepository.findAllWithTopic();
	}

	@Transactional(readOnly = true)
	public long count() {
		return teamRepository.count();
	}

	@Transactional(readOnly = true)
	public long countByPeriod(Long periodId) {
		return teamRepository.countByPeriodId(periodId);
	}

	@Transactional(readOnly = true)
	public List<Team> findUnreportedByPeriod(Long periodId) {
		return teamRepository.findUnreportedByPeriod(periodId);
	}

	@Transactional(readOnly = true)
	public Team findForLeader(Long teamId, Student actor) {
		Team team = findById(teamId);
		requireLeader(team, actor);
		return team;
	}

	@Transactional(readOnly = true)
	public List<TeamView> findViewsFor(Student student) {
		List<Team> teams = teamRepository.findInvolvedOf(student.getId(), InvitationStatus.PENDING);
		if (teams.isEmpty()) {
			return List.of();
		}
		Map<Long, Report> reports = new HashMap<>();
		for (Report report : reportRepository.findByTeamIdIn(teams.stream().map(Team::getId).toList())) {
			reports.put(report.getTeam().getId(), report);
		}
		List<TeamView> views = new ArrayList<>();
		for (Team team : teams) {
			views.add(TeamView.of(team, student, reports.get(team.getId())));
		}
		return views;
	}

	@Transactional(readOnly = true)
	public Team findByTopicId(Long topicId) {
		return teamRepository.findByTopicId(topicId).orElse(null);
	}

	public Team create(Long periodId, Student leader) {
		RegistrationPeriod period = periodService.findById(periodId);
		Student lockedLeader = studentRepository.findByIdForUpdate(leader.getId())
				.orElseThrow(() -> new InvalidTeamException("Không tìm thấy sinh viên"));
		ensureNotInTeam(period, lockedLeader);
		Team team = new Team();
		team.setPeriod(period);
		team.setLeader(lockedLeader);
		team.getMembers().add(lockedLeader);
		try {
			return teamRepository.saveAndFlush(team);
		} catch (DataIntegrityViolationException ex) {
			throw new InvalidTeamException("Bạn đã có nhóm trong đợt này");
		}
	}

	public Team invite(Long id, String studentCode, Student actor) {
		Team team = lockTeam(id);
		requireLeader(team, actor);
		Student invited = accountService.findStudentByCode(studentCode);
		if (invited.getId().equals(actor.getId())) {
			throw new InvalidTeamException("Không thể mời chính mình");
		}
		if (team.isMember(invited.getId())) {
			throw new InvalidTeamException("Sinh viên này đã ở trong nhóm");
		}
		if (team.hasPendingInvitation(invited.getId())) {
			throw new InvalidTeamException("Sinh viên này đã được mời và đang chờ phản hồi");
		}
		if (team.getMembers().size() + team.pendingInvitationCount() >= MAX_MEMBERS) {
			throw new InvalidTeamException("Nhóm đã đạt sĩ số tối đa " + MAX_MEMBERS + " người (bao gồm lời mời đang chờ)");
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
		Team team = lockTeam(id);
		TeamInvitation invitation = team.pendingInvitationOf(actor.getId());
		if (invitation == null) {
			throw new InvalidTeamException("Không có lời mời đang chờ phản hồi");
		}
		if (accept) {
			if (team.getMembers().size() >= MAX_MEMBERS) {
				throw new InvalidTeamException("Nhóm đã đủ " + MAX_MEMBERS + " thành viên");
			}
			studentRepository.findByIdForUpdate(actor.getId());
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
		Team team = lockTeam(id);
		requireLeader(team, actor);
		Student newLeader = accountService.findStudentByCode(studentCode);
		if (!team.isMember(newLeader.getId())) {
			throw new InvalidTeamException("Chỉ có thể chuyển quyền cho thành viên trong nhóm");
		}
		team.setLeader(newLeader);
		return team;
	}

	public Team leave(Long id, Student actor) {
		Team team = lockTeam(id);
		if (team.isLeader(actor.getId())) {
			throw new InvalidTeamException("Nhóm trưởng phải chuyển quyền cho thành viên khác trước khi rời nhóm");
		}
		boolean removed = team.getMembers().removeIf(member -> member.getId().equals(actor.getId()));
		if (!removed) {
			throw new InvalidTeamException("Bạn không phải thành viên của nhóm");
		}
		return team;
	}

	public Team cancelInvitation(Long id, String studentCode, Student actor) {
		Team team = lockTeam(id);
		requireLeader(team, actor);
		Student student = accountService.findStudentByCode(studentCode);
		TeamInvitation invitation = team.pendingInvitationOf(student.getId());
		if (invitation == null) {
			throw new InvalidTeamException("Không có lời mời đang chờ để hủy");
		}
		invitation.setStatus(InvitationStatus.CANCELLED);
		return team;
	}

	public Team removeMember(Long id, String studentCode, Student actor) {
		Team team = lockTeam(id);
		requireLeader(team, actor);
		Student member = accountService.findStudentByCode(studentCode);
		if (team.isLeader(member.getId())) {
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

	private Team lockTeam(Long id) {
		return teamRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new TeamNotFoundException("Không tìm thấy id: " + id));
	}

	private void ensureNotInTeam(RegistrationPeriod period, Student student) {
		if (teamRepository.existsByPeriodIdAndMembersId(period.getId(), student.getId())) {
			throw new InvalidTeamException(
					"Sinh viên " + student.getStudentCode() + " đã thuộc một nhóm trong đợt này");
		}
	}

	private void requireLeader(Team team, Student actor) {
		if (!team.isLeader(actor.getId())) {
			throw new InvalidTeamException("Chỉ nhóm trưởng mới được thực hiện thao tác này");
		}
	}
}
