package project.qldt.service.topic;

import project.qldt.service.account.AccountService;
import project.qldt.service.notification.NotificationService;
import project.qldt.service.period.RegistrationPeriodService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.topic.TopicRequest;
import project.qldt.exception.InvalidTopicException;
import project.qldt.exception.TopicNotFoundException;
import project.qldt.model.topic.AdvisorInvitation;
import project.qldt.model.topic.InvitationStatus;
import project.qldt.model.account.Lecturer;
import project.qldt.model.notification.NotificationType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.topic.Topic;
import project.qldt.model.topic.TopicAvailability;
import project.qldt.model.topic.TopicStatus;
import project.qldt.repository.topic.TopicRepository;

@Service
@Transactional
public class TopicService {

	private final TopicRepository topicRepository;
	private final AccountService accountService;
	private final RegistrationPeriodService periodService;
	private final NotificationService notificationService;

	public TopicService(TopicRepository topicRepository, AccountService accountService,
			RegistrationPeriodService periodService, NotificationService notificationService) {
		this.topicRepository = topicRepository;
		this.accountService = accountService;
		this.periodService = periodService;
		this.notificationService = notificationService;
	}

	@Transactional(readOnly = true)
	public Topic findById(Long id) {
		return topicRepository.findById(id)
				.orElseThrow(() -> new TopicNotFoundException("Không tìm thấy đề tài có id: " + id));
	}

	@Transactional(readOnly = true)
	public List<Topic> findByStatus(TopicStatus status) {
		return topicRepository.findByStatus(status);
	}

	@Transactional(readOnly = true)
	public List<Topic> findPublished() {
		return findByStatus(TopicStatus.PUBLISHED);
	}

	@Transactional(readOnly = true)
	public List<Topic> findByLecturer(Lecturer lecturer) {
		Map<Long, Topic> merged = new LinkedHashMap<>();
		topicRepository.findByAdvisorId(lecturer.getId()).forEach(topic -> merged.put(topic.getId(), topic));
		topicRepository.findByPendingInvitationOf(lecturer.getId(), InvitationStatus.PENDING)
				.forEach(topic -> merged.put(topic.getId(), topic));
		return new ArrayList<>(merged.values());
	}

	@Transactional(readOnly = true)
	public List<Topic> findByPeriod(Long periodId) {
		return topicRepository.findByPeriodIdOrderByIdAsc(periodId);
	}

	@Transactional(readOnly = true)
	public List<Topic> search(Long periodId, TopicStatus status, TopicAvailability availability, Long advisorId,
			Long reviewerId) {
		return topicRepository.search(periodId, status, availability, advisorId, reviewerId);
	}

	@Transactional(readOnly = true)
	public List<Topic> findAvailablePublishedFor(Long periodId) {
		return topicRepository.findByStatusAndAvailabilityAndPeriodIdOrderByIdAsc(TopicStatus.PUBLISHED,
				TopicAvailability.AVAILABLE, periodId);
	}

	@Transactional(readOnly = true)
	public List<Topic> findRegisteredWithoutCouncil() {
		return topicRepository.findWithoutCouncil(TopicAvailability.REGISTERED);
	}

	@Transactional(readOnly = true)
	public List<Topic> findPendingRegistrationsOf(Long lecturerId) {
		return topicRepository.findByAdvisorIdAndAvailability(lecturerId, TopicAvailability.PENDING);
	}

	public int claimForRegistration(Long topicId) {
		return topicRepository.updateAvailabilityIfCurrent(topicId, TopicAvailability.AVAILABLE,
				TopicAvailability.PENDING);
	}

	@Transactional(readOnly = true)
	public long countAvailablePublished() {
		return topicRepository.countByStatusAndAvailability(TopicStatus.PUBLISHED, TopicAvailability.AVAILABLE);
	}

	@Transactional(readOnly = true)
	public long count() {
		return topicRepository.count();
	}

	@Transactional(readOnly = true)
	public long countByPeriod(Long periodId) {
		return topicRepository.countByPeriodId(periodId);
	}

	@Transactional(readOnly = true)
	public long countByPeriodAndStatus(Long periodId, TopicStatus status) {
		return topicRepository.countByPeriodIdAndStatus(periodId, status);
	}

	@Transactional(readOnly = true)
	public long countByPeriodAndAvailability(Long periodId, TopicAvailability availability) {
		return topicRepository.countByPeriodIdAndAvailability(periodId, availability);
	}

	@Transactional(readOnly = true)
	public Map<TopicStatus, Long> statusCounts(Long periodId) {
		Map<TopicStatus, Long> counts = new EnumMap<>(TopicStatus.class);
		for (Object[] row : topicRepository.countByStatusGrouped(periodId)) {
			counts.put((TopicStatus) row[0], ((Number) row[1]).longValue());
		}
		return counts;
	}

	@Transactional(readOnly = true)
	public List<RegistrationPeriod> findPeriodsWithApprovedTopics() {
		return topicRepository.findPeriodsByStatus(TopicStatus.APPROVED);
	}

	public Topic create(TopicRequest request, Lecturer primaryAdvisor) {
		RegistrationPeriod period = periodService.findById(request.getPeriodId());
		validateTeacherWindow(period);
		Topic topic = new Topic();
		topic.setName(request.getName());
		topic.setDescription(request.getDescription());
		topic.setPeriod(period);
		topic.setPrimaryAdvisor(primaryAdvisor);
		topic.setDepartment(primaryAdvisor.getDepartment());
		topic.getAdvisors().add(primaryAdvisor);
		return topicRepository.save(topic);
	}

	public Topic findForEdit(Long id, Lecturer actor) {
		Topic topic = findById(id);
		requirePrimaryAdvisor(topic, actor);
		requireEditable(topic);
		return topic;
	}

	public Topic update(Long id, TopicRequest request, Lecturer actor) {
		Topic topic = findById(id);
		requirePrimaryAdvisor(topic, actor);
		requireEditable(topic);
		RegistrationPeriod period = periodService.findById(request.getPeriodId());
		validateTeacherWindow(period);
		topic.setName(request.getName());
		topic.setDescription(request.getDescription());
		topic.setPeriod(period);
		topic.setStatus(TopicStatus.PENDING);
		topic.setRejectionReason(null);
		return topic;
	}

	public Topic inviteAdvisor(Long id, String lecturerCode, Lecturer actor) {
		Topic topic = findById(id);
		requirePrimaryAdvisor(topic, actor);
		requireEditable(topic);
		if (topic.getAdvisors().size() >= 2) {
			throw new InvalidTopicException("Đề tài đã có tối đa 2 giảng viên hướng dẫn");
		}
		if (topic.hasPendingAdvisorInvitation()) {
			throw new InvalidTopicException("Đang chờ phản hồi lời mời của giảng viên khác");
		}
		Lecturer invited = accountService.findLecturerByCode(lecturerCode);
		if (invited.getId().equals(topic.getPrimaryAdvisor().getId())) {
			throw new InvalidTopicException("Không thể mời chính mình làm giảng viên hướng dẫn");
		}
		if (invited.getDepartment() == null || topic.getDepartment() == null
				|| !invited.getDepartment().getId().equals(topic.getDepartment().getId())) {
			throw new InvalidTopicException("Giảng viên được mời phải cùng bộ môn với giảng viên hướng dẫn");
		}
		AdvisorInvitation invitation = new AdvisorInvitation();
		invitation.setLecturer(invited);
		topic.setInvitation(invitation);
		notificationService.send(invited, NotificationType.INVITATION, "Lời mời làm giảng viên hướng dẫn",
				topic.getPrimaryAdvisor().getFullName() + " mời bạn hướng dẫn đề tài \"" + topic.getName() + "\"",
				"/topics");
		return topic;
	}

	public Topic respondInvitation(Long id, Lecturer actor, boolean accept, String reason) {
		Topic topic = findById(id);
		validateTeacherWindow(topic.getPeriod());
		AdvisorInvitation invitation = topic.getInvitation();
		if (invitation == null || invitation.getStatus() != InvitationStatus.PENDING) {
			throw new InvalidTopicException("Không có lời mời đang chờ phản hồi");
		}
		if (!invitation.getLecturer().getId().equals(actor.getId())) {
			throw new InvalidTopicException("Bạn không phải giảng viên được mời");
		}
		if (accept) {
			invitation.setStatus(InvitationStatus.ACCEPTED);
			topic.getAdvisors().add(actor);
		} else {
			if (reason == null || reason.isBlank()) {
				throw new InvalidTopicException("Vui lòng nhập lý do từ chối");
			}
			invitation.setStatus(InvitationStatus.REJECTED);
			invitation.setResponseReason(reason);
		}
		return topic;
	}

	public Topic cancelAdvisorInvitation(Long id, Lecturer actor) {
		Topic topic = findById(id);
		requirePrimaryAdvisor(topic, actor);
		validateTeacherWindow(topic.getPeriod());
		AdvisorInvitation invitation = topic.getInvitation();
		if (invitation == null || invitation.getStatus() != InvitationStatus.PENDING) {
			throw new InvalidTopicException("Không có lời mời đang chờ để hủy");
		}
		invitation.setStatus(InvitationStatus.CANCELLED);
		return topic;
	}

	public Topic removeCoAdvisor(Long id, String lecturerCode, Lecturer actor) {
		Topic topic = findById(id);
		requirePrimaryAdvisor(topic, actor);
		requireEditable(topic);
		validateTeacherWindow(topic.getPeriod());
		if (topic.getAdvisors().size() < 2) {
			throw new InvalidTopicException("Đề tài chưa có giảng viên hướng dẫn thứ hai");
		}
		Lecturer coAdvisor = accountService.findLecturerByCode(lecturerCode);
		boolean removed = topic.getAdvisors().removeIf(advisor -> advisor.getId().equals(coAdvisor.getId()));
		if (!removed) {
			throw new InvalidTopicException("Giảng viên này không phải giảng viên hướng dẫn của đề tài");
		}
		AdvisorInvitation invitation = topic.getInvitation();
		if (invitation != null && invitation.getLecturer().getId().equals(coAdvisor.getId())) {
			invitation.setStatus(InvitationStatus.CANCELLED);
		}
		return topic;
	}

	public Topic approve(Long id) {
		Topic topic = findById(id);
		if (topic.getStatus() != TopicStatus.PENDING) {
			throw new InvalidTopicException("Chỉ duyệt được đề tài đang chờ duyệt");
		}
		topic.setStatus(TopicStatus.APPROVED);
		topic.setRejectionReason(null);
		return topic;
	}

	public Topic reject(Long id, String reason) {
		Topic topic = findById(id);
		if (topic.getStatus() != TopicStatus.PENDING) {
			throw new InvalidTopicException("Chỉ từ chối được đề tài đang chờ duyệt");
		}
		if (reason == null || reason.isBlank()) {
			throw new InvalidTopicException("Vui lòng nhập lý do từ chối");
		}
		topic.setStatus(TopicStatus.REJECTED);
		topic.setRejectionReason(reason);
		return topic;
	}

	public int publish(Long periodId) {
		periodService.findById(periodId);
		List<Topic> approved = topicRepository.findByStatusAndPeriodIdOrderByIdAsc(TopicStatus.APPROVED, periodId);
		if (approved.isEmpty()) {
			throw new InvalidTopicException("Đợt này không có đề tài nào đã duyệt để công bố");
		}
		for (Topic topic : approved) {
			topic.setStatus(TopicStatus.PUBLISHED);
			topic.setAvailability(TopicAvailability.AVAILABLE);
		}
		return approved.size();
	}

	public int rejectPeriod(Long periodId, String reason) {
		periodService.findById(periodId);
		if (reason == null || reason.isBlank()) {
			throw new InvalidTopicException("Vui lòng nhập lý do từ chối công bố");
		}
		List<Topic> approved = topicRepository.findByStatusAndPeriodIdOrderByIdAsc(TopicStatus.APPROVED, periodId);
		if (approved.isEmpty()) {
			throw new InvalidTopicException("Đợt này không có đề tài nào đã duyệt để từ chối");
		}
		for (Topic topic : approved) {
			topic.setStatus(TopicStatus.REJECTED);
			topic.setRejectionReason(reason.trim());
		}
		return approved.size();
	}

	private void validateTeacherWindow(RegistrationPeriod period) {
		LocalDateTime now = LocalDateTime.now();
		boolean before = now.isBefore(period.getTeacherRegisterStart());
		boolean after = now.isAfter(period.getTeacherRegisterEnd());
		if (before || after) {
			throw new InvalidTopicException(
					"Đợt \"" + period.getName() + "\" không trong thời gian giảng viên đăng ký đề tài");
		}
	}

	private void requirePrimaryAdvisor(Topic topic, Lecturer actor) {
		if (!topic.getPrimaryAdvisor().getId().equals(actor.getId())) {
			throw new InvalidTopicException("Chỉ giảng viên tạo đề tài mới được thực hiện thao tác này");
		}
	}

	private void requireEditable(Topic topic) {
		if (topic.getStatus() == TopicStatus.APPROVED || topic.getStatus() == TopicStatus.PUBLISHED) {
			throw new InvalidTopicException("Đề tài đã được duyệt hoặc đã công bố, không thể sửa");
		}
	}
}
