package project.qldt.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.exception.CouncilNotFoundException;
import project.qldt.exception.InvalidCouncilException;
import project.qldt.model.Council;
import project.qldt.model.CouncilMember;
import project.qldt.model.CouncilRole;
import project.qldt.model.Lecturer;
import project.qldt.model.NotificationType;
import project.qldt.model.Student;
import project.qldt.model.Team;
import project.qldt.model.Topic;
import project.qldt.model.TopicAvailability;
import project.qldt.model.TopicResult;
import project.qldt.repository.CouncilRepository;
import project.qldt.repository.TopicResultRepository;

@Service
@Transactional
public class CouncilService {

	private final CouncilRepository councilRepository;
	private final TopicResultRepository resultRepository;
	private final AccountService accountService;
	private final TopicService topicService;
	private final TeamService teamService;
	private final NotificationService notificationService;

	public CouncilService(CouncilRepository councilRepository, TopicResultRepository resultRepository,
			AccountService accountService, TopicService topicService, TeamService teamService,
			NotificationService notificationService) {
		this.councilRepository = councilRepository;
		this.resultRepository = resultRepository;
		this.accountService = accountService;
		this.topicService = topicService;
		this.teamService = teamService;
		this.notificationService = notificationService;
	}

	@Transactional(readOnly = true)
	public List<Council> findAll() {
		return councilRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Council findById(Long id) {
		return councilRepository.findById(id)
				.orElseThrow(() -> new CouncilNotFoundException("Không tìm thấy hội đồng có id: " + id));
	}

	@Transactional(readOnly = true)
	public Council findByTopicId(Long topicId) {
		return councilRepository.findByTopicId(topicId).orElse(null);
	}

	public List<Topic> findTopicsReadyForCouncil() {
		List<Topic> topics = new ArrayList<>();
		for (Topic topic : topicService.findAll()) {
			if (topic.getAvailability() == TopicAvailability.REGISTERED && findByTopicId(topic.getId()) == null) {
				topics.add(topic);
			}
		}
		return topics;
	}

	public List<Council> findByLecturer(Lecturer lecturer) {
		List<Council> list = new ArrayList<>();
		for (Council council : councilRepository.findAll()) {
			boolean isMember = council.getMembers().stream()
					.anyMatch(member -> member.getLecturer().getId().equals(lecturer.getId()));
			if (isMember) {
				list.add(council);
			}
		}
		return list;
	}

	public Council create(Long topicId, String name, List<String> lecturerCodes, List<CouncilRole> roles) {
		Topic topic = topicService.findById(topicId);
		if (topic.getAvailability() != TopicAvailability.REGISTERED) {
			throw new InvalidCouncilException("Chỉ lập hội đồng cho đề tài đã được duyệt đăng ký");
		}
		if (findByTopicId(topicId) != null) {
			throw new InvalidCouncilException("Đề tài này đã có hội đồng");
		}
		List<String> codes = lecturerCodes == null ? new ArrayList<>() : lecturerCodes;
		if (codes.size() < 3 || codes.size() > 5) {
			throw new InvalidCouncilException("Hội đồng phải có từ 3 đến 5 giảng viên");
		}
		if (roles == null || roles.size() != codes.size()) {
			throw new InvalidCouncilException("Số vai trò phải khớp với số giảng viên");
		}
		long chairs = roles.stream().filter(role -> role == CouncilRole.CHAIR).count();
		long secretaries = roles.stream().filter(role -> role == CouncilRole.SECRETARY).count();
		if (chairs != 1 || secretaries != 1) {
			throw new InvalidCouncilException("Hội đồng phải có đúng 1 chủ tịch và 1 thư ký");
		}
		Council council = new Council();
		boolean defaultName = name == null || name.isBlank();
		council.setName(defaultName ? "Hội đồng" : name.trim());
		council.setTopic(topic);
		List<Long> usedIds = new ArrayList<>();
		for (int i = 0; i < codes.size(); i++) {
			Lecturer lecturer = accountService.findLecturerByCode(codes.get(i));
			if (usedIds.contains(lecturer.getId())) {
				throw new InvalidCouncilException("Giảng viên bị trùng trong hội đồng");
			}
			boolean isAdvisor = topic.getAdvisors().stream()
					.anyMatch(advisor -> advisor.getId().equals(lecturer.getId()));
			if (isAdvisor) {
				throw new InvalidCouncilException("Giảng viên hướng dẫn không được tham gia hội đồng của đề tài mình");
			}
			usedIds.add(lecturer.getId());
			CouncilMember member = new CouncilMember();
			member.setCouncil(council);
			member.setLecturer(lecturer);
			member.setRole(roles.get(i));
			council.getMembers().add(member);
		}
		Council saved = councilRepository.save(council);
		if (defaultName) {
			saved.setName("Hội đồng " + saved.getId());
		}
		return saved;
	}

	public Council score(Long councilId, Lecturer actor, double score, String comment) {
		Council council = findById(councilId);
		CouncilMember member = council.getMembers().stream()
				.filter(item -> item.getLecturer().getId().equals(actor.getId()))
				.findFirst()
				.orElseThrow(() -> new InvalidCouncilException("Bạn không phải thành viên của hội đồng này"));
		if (score < 0 || score > 10) {
			throw new InvalidCouncilException("Điểm phải nằm trong khoảng 0 đến 10");
		}
		if (Math.abs(score * 10 - Math.rint(score * 10)) > 0.0000001) {
			throw new InvalidCouncilException("Điểm chỉ được có 1 chữ số thập phân");
		}
		TopicResult result = resultOf(council);
		if (result != null && result.isPublished()) {
			throw new InvalidCouncilException("Kết quả đã công bố, không thể sửa điểm");
		}
		member.setScore(score);
		member.setComment(comment);
		member.setScoredAt(LocalDateTime.now());
		return council;
	}

	public TopicResult confirm(Long councilId, Lecturer actor) {
		Council council = findById(councilId);
		boolean isChair = council.getMembers().stream()
				.anyMatch(member -> member.getLecturer().getId().equals(actor.getId())
						&& member.getRole() == CouncilRole.CHAIR);
		if (!isChair) {
			throw new InvalidCouncilException("Chỉ chủ tịch hội đồng mới được tổng hợp điểm");
		}
		TopicResult result = resultOf(council);
		if (result != null && result.isPublished()) {
			throw new InvalidCouncilException("Kết quả đã công bố, không thể tổng hợp lại");
		}
		double sum = 0;
		for (CouncilMember member : council.getMembers()) {
			if (member.getScore() == null) {
				throw new InvalidCouncilException("Còn thành viên chưa nhập điểm");
			}
			sum += member.getScore();
		}
		double average = Math.round((sum / council.getMembers().size()) * 100.0) / 100.0;
		if (result == null) {
			result = new TopicResult();
			result.setCouncil(council);
		}
		result.setAverageScore(average);
		return resultRepository.save(result);
	}

	public TopicResult publish(Long resultId) {
		TopicResult result = resultRepository.findById(resultId)
				.orElseThrow(() -> new CouncilNotFoundException("Không tìm thấy kết quả có id: " + resultId));
		if (result.isPublished()) {
			throw new InvalidCouncilException("Kết quả đã được công bố");
		}
		result.setPublished(true);
		result.setPublishedAt(LocalDateTime.now());
		Council council = result.getCouncil();
		Team team = teamService.findByTopicId(council.getTopic().getId());
		if (team != null) {
			for (Student member : team.getMembers()) {
				notificationService.send(member, NotificationType.RESULT, "Đã công bố điểm bảo vệ",
						"Đề tài \"" + council.getTopic().getName() + "\" đạt " + result.getAverageScore() + " điểm",
						"/results");
			}
		}
		for (Lecturer advisor : council.getTopic().getAdvisors()) {
			notificationService.send(advisor, NotificationType.RESULT, "Đã công bố điểm bảo vệ",
					"Đề tài \"" + council.getTopic().getName() + "\" đạt " + result.getAverageScore() + " điểm",
					"/results");
		}
		return resultRepository.save(result);
	}

	@Transactional(readOnly = true)
	public List<TopicResult> findAllResults() {
		return resultRepository.findAll();
	}

	@Transactional(readOnly = true)
	public TopicResult findResultByCouncilId(Long councilId) {
		return resultRepository.findByCouncilId(councilId).orElse(null);
	}

	public List<TopicResult> findPublishedResultsFor(Student student) {
		List<TopicResult> list = new ArrayList<>();
		for (TopicResult result : resultRepository.findAll()) {
			if (!result.isPublished()) {
				continue;
			}
			Team team = teamService.findByTopicId(result.getCouncil().getTopic().getId());
			boolean inTeam = team != null && team.getMembers().stream()
					.anyMatch(member -> member.getId().equals(student.getId()));
			if (inTeam) {
				list.add(result);
			}
		}
		return list;
	}

	public List<TopicResult> findPublishedResultsOfAdvisor(Lecturer lecturer) {
		List<TopicResult> list = new ArrayList<>();
		for (TopicResult result : resultRepository.findAll()) {
			boolean isAdvisor = result.getCouncil().getTopic().getAdvisors().stream()
					.anyMatch(advisor -> advisor.getId().equals(lecturer.getId()));
			if (result.isPublished() && isAdvisor) {
				list.add(result);
			}
		}
		return list;
	}

	private TopicResult resultOf(Council council) {
		return findResultByCouncilId(council.getId());
	}
}