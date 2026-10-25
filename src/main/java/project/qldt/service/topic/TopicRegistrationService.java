package project.qldt.service.topic;

import project.qldt.service.notification.NotificationService;
import project.qldt.service.team.TeamService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.topic.RegistrationView;
import project.qldt.exception.InvalidRegistrationException;
import project.qldt.model.account.Lecturer;
import project.qldt.model.notification.NotificationType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.account.Student;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;
import project.qldt.model.topic.TopicAvailability;

@Service
@Transactional
public class TopicRegistrationService {

	private final TeamService teamService;
	private final TopicService topicService;
	private final NotificationService notificationService;

	public TopicRegistrationService(TeamService teamService, TopicService topicService,
			NotificationService notificationService) {
		this.teamService = teamService;
		this.topicService = topicService;
		this.notificationService = notificationService;
	}

	public List<Topic> findAvailableTopicsFor(Team team) {
		return topicService.findAvailablePublishedFor(team.getPeriod().getId());
	}

	public Team register(Long teamId, Long topicId, Student actor) {
		Team team = teamService.findById(teamId);
		requireLeader(team, actor);
		if (team.getMembers().size() < TeamService.MIN_MEMBERS) {
			throw new InvalidRegistrationException(
					"Nhóm phải có tối thiểu " + TeamService.MIN_MEMBERS + " thành viên mới được đăng ký đề tài");
		}
		if (team.getMembers().size() > TeamService.MAX_MEMBERS) {
			throw new InvalidRegistrationException(
					"Nhóm chỉ được có tối đa " + TeamService.MAX_MEMBERS + " thành viên");
		}
		if (team.getTopic() != null) {
			throw new InvalidRegistrationException("Nhóm đã đăng ký một đề tài");
		}
		validateStudentWindow(team.getPeriod());
		Topic topic = topicService.findById(topicId);
		if (!topic.getPeriod().getId().equals(team.getPeriod().getId())) {
			throw new InvalidRegistrationException("Đề tài không thuộc đợt đăng ký của nhóm");
		}
		if (!topic.isPublished()) {
			throw new InvalidRegistrationException("Đề tài chưa được công bố");
		}
		if (!topic.isAvailable()) {
			throw new InvalidRegistrationException("Đề tài đã có nhóm đăng ký");
		}
		if (topicService.claimForRegistration(topicId) == 0) {
			throw new InvalidRegistrationException("Đề tài đã có nhóm đăng ký");
		}
		topic.setAvailability(TopicAvailability.PENDING);
		team.setTopic(topic);
		team.setRegistrationRejectionReason(null);
		for (Lecturer advisor : topic.getAdvisors()) {
			notificationService.send(advisor, NotificationType.APPROVAL, "Đăng ký đề tài mới",
					"Nhóm của " + team.getLeader().getFullName() + " đã đăng ký đề tài \"" + topic.getName() + "\"",
					"/registrations");
		}
		return team;
	}

	public Team cancel(Long teamId, Student actor) {
		Team team = teamService.findById(teamId);
		requireLeader(team, actor);
		Topic topic = team.getTopic();
		if (topic == null) {
			throw new InvalidRegistrationException("Nhóm chưa đăng ký đề tài");
		}
		if (!topic.isWaitingApproval()) {
			throw new InvalidRegistrationException("Đề tài đã được duyệt, không thể hủy đăng ký");
		}
		topic.setAvailability(TopicAvailability.AVAILABLE);
		team.setTopic(null);
		return team;
	}

	public Team approve(Long topicId, Lecturer actor) {
		Topic topic = topicService.findById(topicId);
		requireAdvisor(topic, actor);
		if (!topic.isWaitingApproval()) {
			throw new InvalidRegistrationException("Không có đăng ký nào đang chờ duyệt");
		}
		Team team = teamService.findByTopicId(topicId);
		topic.setAvailability(TopicAvailability.REGISTERED);
		if (team != null) {
			notificationService.send(team.getLeader(), NotificationType.APPROVAL, "Đăng ký đề tài được duyệt",
					"Đề tài \"" + topic.getName() + "\" đã được duyệt đăng ký", "/teams");
		}
		return team;
	}

	public Team reject(Long topicId, Lecturer actor, String reason) {
		Topic topic = topicService.findById(topicId);
		requireAdvisor(topic, actor);
		if (!topic.isWaitingApproval()) {
			throw new InvalidRegistrationException("Không có đăng ký nào đang chờ duyệt");
		}
		if (reason == null || reason.isBlank()) {
			throw new InvalidRegistrationException("Vui lòng nhập lý do từ chối");
		}
		Team team = teamService.findByTopicId(topicId);
		topic.setAvailability(TopicAvailability.AVAILABLE);
		if (team != null) {
			team.setTopic(null);
			team.setRegistrationRejectionReason(reason);
			notificationService.send(team.getLeader(), NotificationType.REJECTION, "Đăng ký đề tài bị từ chối",
					"Đề tài \"" + topic.getName() + "\" bị từ chối: " + reason, "/teams");
		}
		return team;
	}

	public Team cancelByDean(Long topicId, String reason) {
		Topic topic = topicService.findById(topicId);
		if (reason == null || reason.isBlank()) {
			throw new InvalidRegistrationException("Vui lòng nhập lý do hủy đăng ký");
		}
		Team team = teamService.findByTopicId(topicId);
		if (team == null) {
			throw new InvalidRegistrationException("Đề tài này chưa có nhóm đăng ký");
		}
		topic.setAvailability(TopicAvailability.AVAILABLE);
		team.setTopic(null);
		team.setRegistrationRejectionReason(reason);
		for (Student member : team.getMembers()) {
			notificationService.send(member, NotificationType.REJECTION, "Trưởng khoa hủy đăng ký đề tài",
					"Đăng ký đề tài \"" + topic.getName() + "\" đã bị hủy: " + reason, "/teams");
		}
		for (Lecturer advisor : topic.getAdvisors()) {
			notificationService.send(advisor, NotificationType.REJECTION, "Trưởng khoa hủy đăng ký đề tài",
					"Đăng ký đề tài \"" + topic.getName() + "\" đã bị hủy: " + reason, "/registrations");
		}
		return team;
	}

	@Transactional(readOnly = true)
	public List<RegistrationView> findPendingForAdvisor(Lecturer lecturer) {
		List<Topic> topics = topicService.findPendingRegistrationsOf(lecturer.getId());
		if (topics.isEmpty()) {
			return List.of();
		}
		Map<Long, Team> teams = new HashMap<>();
		for (Team team : teamService.findByTopicIds(topics.stream().map(Topic::getId).toList())) {
			if (team.getTopic() != null) {
				teams.put(team.getTopic().getId(), team);
			}
		}
		List<RegistrationView> views = new ArrayList<>();
		for (Topic topic : topics) {
			Team team = teams.get(topic.getId());
			if (team != null) {
				views.add(new RegistrationView(topic, team));
			}
		}
		return views;
	}

	@Transactional(readOnly = true)
	public List<RegistrationView> findRegistrationsForDean() {
		return findRegistrationsForDean(null);
	}

	@Transactional(readOnly = true)
	public List<RegistrationView> findRegistrationsForDean(Long periodId) {
		List<Team> teams = periodId == null
				? teamService.findAllWithTopic()
				: teamService.findWithTopicByPeriod(periodId);
		List<RegistrationView> views = new ArrayList<>();
		for (Team team : teams) {
			views.add(new RegistrationView(team.getTopic(), team));
		}
		return views;
	}

	private void requireLeader(Team team, Student actor) {
		if (!team.isLeader(actor.getId())) {
			throw new InvalidRegistrationException(
					"Chỉ nhóm trưởng mới được đăng ký hoặc hủy đăng ký đề tài");
		}
	}

	private void requireAdvisor(Topic topic, Lecturer actor) {
		if (!topic.isAdvisorOf(actor.getId())) {
			throw new InvalidRegistrationException("Chỉ giảng viên hướng dẫn của đề tài mới được duyệt");
		}
	}

	private void validateStudentWindow(RegistrationPeriod period) {
		if (period.isRegistrationLocked()) {
			throw new InvalidRegistrationException("Đợt \"" + period.getName() + "\" đã bị khóa đăng ký");
		}
		LocalDateTime now = LocalDateTime.now();
		boolean before = now.isBefore(period.getStudentRegisterStart());
		boolean after = now.isAfter(period.getStudentRegisterEnd());
		if (before || after) {
			throw new InvalidRegistrationException(
					"Đợt \"" + period.getName() + "\" không trong thời gian sinh viên đăng ký");
		}
	}
}
