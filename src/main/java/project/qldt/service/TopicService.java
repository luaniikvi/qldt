package project.qldt.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.TopicRequest;
import project.qldt.exception.InvalidTopicException;
import project.qldt.exception.TopicNotFoundException;
import project.qldt.model.AdvisorInvitation;
import project.qldt.model.InvitationStatus;
import project.qldt.model.Lecturer;
import project.qldt.model.RegistrationPeriod;
import project.qldt.model.Topic;
import project.qldt.model.TopicAvailability;
import project.qldt.model.TopicStatus;
import project.qldt.repository.TopicRepository;

@Service
@Transactional
public class TopicService {

	private final TopicRepository topicRepository;
	private final AccountService accountService;
	private final RegistrationPeriodService periodService;

	public TopicService(TopicRepository topicRepository, AccountService accountService,
			RegistrationPeriodService periodService) {
		this.topicRepository = topicRepository;
		this.accountService = accountService;
		this.periodService = periodService;
	}

	@Transactional(readOnly = true)
	public List<Topic> findAll() {
		return topicRepository.findAll();
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
	public List<Topic> findAvailablePublished() {
		return topicRepository.findByStatusAndAvailability(TopicStatus.PUBLISHED, TopicAvailability.AVAILABLE);
	}

	@Transactional(readOnly = true)
	public List<Topic> findByLecturer(Lecturer lecturer) {
		List<Topic> topics = new ArrayList<>();
		for (Topic topic : topicRepository.findAll()) {
			if (isAdvisor(topic, lecturer) || hasPendingInvitation(topic, lecturer)) {
				topics.add(topic);
			}
		}
		return topics;
	}

	@Transactional(readOnly = true)
	public List<RegistrationPeriod> findPeriodsWithApprovedTopics() {
		Set<Long> periodIds = new LinkedHashSet<>();
		List<RegistrationPeriod> periods = new ArrayList<>();
		for (Topic topic : topicRepository.findAll()) {
			if (topic.getStatus() == TopicStatus.APPROVED && periodIds.add(topic.getPeriod().getId())) {
				periods.add(topic.getPeriod());
			}
		}
		return periods;
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
		if (topic.getInvitation() != null && topic.getInvitation().getStatus() == InvitationStatus.PENDING) {
			throw new InvalidTopicException("Đang chờ phản hồi lời mời của giảng viên khác");
		}
		Lecturer invited = accountService.findLecturerByCode(lecturerCode);
		if (invited.getId().equals(topic.getPrimaryAdvisor().getId())) {
			throw new InvalidTopicException("Không thể mời chính mình làm giảng viên hướng dẫn");
		}
		if (!invited.getDepartment().equals(topic.getDepartment())) {
			throw new InvalidTopicException("Giảng viên được mời phải cùng bộ môn với giảng viên hướng dẫn");
		}
		AdvisorInvitation invitation = new AdvisorInvitation();
		invitation.setLecturer(invited);
		topic.setInvitation(invitation);
		return topic;
	}

	public Topic respondInvitation(Long id, Lecturer actor, boolean accept, String reason) {
		Topic topic = findById(id);
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
		int count = 0;
		for (Topic topic : topicRepository.findAll()) {
			if (topic.getPeriod().getId().equals(periodId) && topic.getStatus() == TopicStatus.APPROVED) {
				topic.setStatus(TopicStatus.PUBLISHED);
				topic.setAvailability(TopicAvailability.AVAILABLE);
				count++;
			}
		}
		if (count == 0) {
			throw new InvalidTopicException("Đợt này không có đề tài nào đã duyệt để công bố");
		}
		return count;
	}

	private boolean isAdvisor(Topic topic, Lecturer lecturer) {
		return topic.getAdvisors().stream().anyMatch(advisor -> advisor.getId().equals(lecturer.getId()));
	}

	private boolean hasPendingInvitation(Topic topic, Lecturer lecturer) {
		return topic.getInvitation() != null
				&& topic.getInvitation().getStatus() == InvitationStatus.PENDING
				&& topic.getInvitation().getLecturer().getId().equals(lecturer.getId());
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
