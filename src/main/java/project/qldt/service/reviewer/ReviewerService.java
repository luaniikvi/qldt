package project.qldt.service.reviewer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.exception.InvalidTopicException;
import project.qldt.model.account.Account;
import project.qldt.model.account.Admin;
import project.qldt.model.account.Lecturer;
import project.qldt.model.account.Student;
import project.qldt.service.common.ScoreRules;
import project.qldt.model.council.Council;
import project.qldt.model.council.TopicResult;
import project.qldt.model.notification.NotificationType;
import project.qldt.model.period.PeriodType;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;
import project.qldt.model.topic.TopicAvailability;
import project.qldt.repository.topic.TopicRepository;
import project.qldt.service.account.AccountService;
import project.qldt.service.council.CouncilService;
import project.qldt.service.notification.NotificationService;
import project.qldt.service.team.TeamService;
import project.qldt.service.topic.TopicService;

@Service
@Transactional
public class ReviewerService {

	private final TopicRepository topicRepository;
	private final AccountService accountService;
	private final CouncilService councilService;
	private final TeamService teamService;
	private final NotificationService notificationService;
	private final TopicService topicService;

	public ReviewerService(TopicRepository topicRepository, AccountService accountService,
			CouncilService councilService, TeamService teamService, NotificationService notificationService,
			TopicService topicService) {
		this.topicRepository = topicRepository;
		this.accountService = accountService;
		this.councilService = councilService;
		this.teamService = teamService;
		this.notificationService = notificationService;
		this.topicService = topicService;
	}

	@Transactional(readOnly = true)
	public List<Topic> findAssignable() {
		return topicRepository.findForReviewer(TopicAvailability.REGISTERED,
				List.of(PeriodType.SPECIALIZED_PROJECT, PeriodType.GRADUATION_THESIS));
	}

	@Transactional(readOnly = true)
	public List<Topic> findAssigned(Lecturer reviewer) {
		return topicRepository.findByReviewerIdOrderByIdDesc(reviewer.getId());
	}

	@Transactional(readOnly = true)
	public Topic findTopic(Long topicId) {
		return topicRepository.findById(topicId)
				.orElseThrow(() -> new InvalidTopicException("Không tìm thấy đề tài có id: " + topicId));
	}

	public Topic assign(Long topicId, String lecturerCode) {
		Topic topic = findTopic(topicId);
		if (!topicService.isRegistered(topic)) {
			throw new InvalidTopicException("Chỉ phân công phản biện cho đề tài đã được duyệt đăng ký");
		}
		if (!topicService.requiresReviewerScore(topic)) {
			throw new InvalidTopicException("Đợt này không yêu cầu điểm giảng viên phản biện");
		}
		if (topic.getReviewerScore() != null) {
			throw new InvalidTopicException("Đề tài đã được phản biện chấm điểm, không thể đổi giảng viên phản biện");
		}
		Lecturer lecturer = accountService.findLecturerByCode(lecturerCode);
		if (topicService.isAdvisorOf(topic, lecturer.getId())) {
			throw new InvalidTopicException("Giảng viên hướng dẫn không thể làm giảng viên phản biện");
		}
		topic.setReviewer(lecturer);
		notificationService.send(lecturer, NotificationType.INVITATION, "Phân công giảng viên phản biện",
				"Bạn được phân công phản biện đề tài \"" + topic.getName() + "\"", "/reviewer/topics");
		return topicRepository.save(topic);
	}

	public Topic score(Long topicId, Lecturer reviewer, double score, String comment) {
		Topic topic = findTopic(topicId);
		if (topic.getReviewer() == null || !topic.getReviewer().getId().equals(reviewer.getId())) {
			throw new InvalidTopicException("Bạn không phải giảng viên phản biện của đề tài này");
		}
		Council council = councilService.findByTopicId(topic.getId());
		if (council != null) {
			TopicResult result = councilService.findResultByCouncilId(council.getId());
			if (result != null && result.isPublished()) {
				throw new InvalidTopicException("Kết quả đã công bố, không thể sửa điểm phản biện");
			}
			if (result != null) {
				throw new InvalidTopicException("Hội đồng đã tổng hợp điểm, không thể sửa điểm phản biện");
			}
		}
		ScoreRules.validate(score).ifPresent(message -> {
			throw new InvalidTopicException(message);
		});
		LocalDate deadline = topic.getPeriod().getReviewerScoreDeadline();
		if (deadline != null && LocalDate.now().isAfter(deadline)) {
			throw new InvalidTopicException("Đã quá hạn nộp điểm phản biện");
		}
		topic.setReviewerScore(score);
		topic.setReviewerComment(comment);
		topic.setReviewerScoredAt(LocalDateTime.now());
		Topic saved = topicRepository.save(topic);
		notifyScore(saved);
		return saved;
	}

	@Transactional(readOnly = true)
	public boolean canViewAllScores(Account viewer) {
		return viewer instanceof Admin || (viewer instanceof Lecturer lecturer && accountService.isDean(lecturer));
	}

	@Transactional(readOnly = true)
	public List<Topic> findVisibleScores(Account viewer) {
		if (canViewAllScores(viewer)) {
			return topicRepository.findByReviewerScoreIsNotNullOrderByIdDesc();
		}
		if (viewer instanceof Lecturer lecturer) {
			return topicRepository.findReviewedByReviewerId(lecturer.getId());
		}
		if (viewer instanceof Student student) {
			return topicRepository.findReviewedOfStudent(student.getId());
		}
		return List.of();
	}

	private void notifyScore(Topic topic) {
		String title = "Đã có điểm giảng viên phản biện";
		String content = "Đề tài \"" + topic.getName() + "\" được phản biện " + topic.getReviewerScore() + " điểm";
		for (Lecturer advisor : topic.getAdvisors()) {
			notificationService.send(advisor, NotificationType.RESULT, title, content, "/reviewer-scores");
		}
		Team team = teamService.findByTopicId(topic.getId());
		if (team != null) {
			for (Student member : team.getMembers()) {
				notificationService.send(member, NotificationType.RESULT, title, content, "/reviewer-scores");
			}
		}
	}
}
