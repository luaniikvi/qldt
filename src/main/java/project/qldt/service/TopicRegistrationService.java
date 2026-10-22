package project.qldt.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.RegistrationView;
import project.qldt.exception.InvalidRegistrationException;
import project.qldt.model.Lecturer;
import project.qldt.model.NotificationType;
import project.qldt.model.RegistrationPeriod;
import project.qldt.model.Student;
import project.qldt.model.Team;
import project.qldt.model.Topic;
import project.qldt.model.TopicAvailability;
import project.qldt.model.TopicStatus;

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
		List<Topic> topics = new ArrayList<>();
		for (Topic topic : topicService.findAvailablePublished()) {
			if (topic.getPeriod().getId().equals(team.getPeriod().getId())) {
				topics.add(topic);
			}
		}
		return topics;
	}

	public Team register(Long teamId, Long topicId, Student actor) {
		Team team = teamService.findById(teamId);
		requireLeader(team, actor);
		if (team.getTopic() != null) {
			throw new InvalidRegistrationException("Nhóm đã đăng ký một đề tài");
		}
		validateStudentWindow(team.getPeriod());
		Topic topic = topicService.findById(topicId);
		if (!topic.getPeriod().getId().equals(team.getPeriod().getId())) {
			throw new InvalidRegistrationException("Đề tài không thuộc đợt đăng ký của nhóm");
		}
		if (topic.getStatus() != TopicStatus.PUBLISHED) {
			throw new InvalidRegistrationException("Đề tài chưa được công bố");
		}
		if (topic.getAvailability() != TopicAvailability.AVAILABLE) {
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
		if (topic.getAvailability() != TopicAvailability.PENDING) {
			throw new InvalidRegistrationException("Đề tài đã được duyệt, không thể hủy đăng ký");
		}
		topic.setAvailability(TopicAvailability.AVAILABLE);
		team.setTopic(null);
		return team;
	}

	public Team approve(Long topicId, Lecturer actor) {
		Topic topic = topicService.findById(topicId);
		requireAdvisor(topic, actor);
		if (topic.getAvailability() != TopicAvailability.PENDING) {
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
		if (topic.getAvailability() != TopicAvailability.PENDING) {
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

	public List<RegistrationView> findPendingForAdvisor(Lecturer lecturer) {
		List<RegistrationView> views = new ArrayList<>();
		for (Topic topic : topicService.findAll()) {
			boolean isAdvisor = topic.getAdvisors().stream()
					.anyMatch(advisor -> advisor.getId().equals(lecturer.getId()));
			if (isAdvisor && topic.getAvailability() == TopicAvailability.PENDING) {
				Team team = teamService.findByTopicId(topic.getId());
				if (team != null) {
					views.add(new RegistrationView(topic, team));
				}
			}
		}
		return views;
	}

	private void requireLeader(Team team, Student actor) {
		if (!team.getLeader().getId().equals(actor.getId())) {
			throw new InvalidRegistrationException(
					"Chỉ nhóm trưởng mới được đăng ký hoặc hủy đăng ký đề tài");
		}
	}

	private void requireAdvisor(Topic topic, Lecturer actor) {
		boolean isAdvisor = topic.getAdvisors().stream()
				.anyMatch(advisor -> advisor.getId().equals(actor.getId()));
		if (!isAdvisor) {
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
