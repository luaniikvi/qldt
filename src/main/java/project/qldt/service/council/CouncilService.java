package project.qldt.service.council;

import project.qldt.service.account.AccountService;
import project.qldt.service.email.EmailService;
import project.qldt.service.notification.NotificationService;
import project.qldt.service.team.TeamService;
import project.qldt.service.topic.TopicService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.council.CouncilView;
import project.qldt.exception.CouncilNotFoundException;
import project.qldt.exception.InvalidCouncilException;
import project.qldt.model.council.Council;
import project.qldt.model.council.CouncilMember;
import project.qldt.model.council.CouncilRole;
import project.qldt.model.account.Lecturer;
import project.qldt.model.common.ScoreRules;
import project.qldt.model.notification.NotificationType;
import project.qldt.model.report.Report;
import project.qldt.model.account.Student;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;
import project.qldt.model.council.TopicResult;
import project.qldt.repository.council.CouncilRepository;
import project.qldt.repository.council.TopicResultRepository;
import project.qldt.repository.report.ReportRepository;

@Service
@Transactional
public class CouncilService {

	private final CouncilRepository councilRepository;
	private final TopicResultRepository resultRepository;
	private final AccountService accountService;
	private final TopicService topicService;
	private final TeamService teamService;
	private final NotificationService notificationService;
	private final ReportRepository reportRepository;
	private final EmailService emailService;

	public CouncilService(CouncilRepository councilRepository, TopicResultRepository resultRepository,
			AccountService accountService, TopicService topicService, TeamService teamService,
			NotificationService notificationService, ReportRepository reportRepository,
			EmailService emailService) {
		this.councilRepository = councilRepository;
		this.resultRepository = resultRepository;
		this.accountService = accountService;
		this.topicService = topicService;
		this.teamService = teamService;
		this.notificationService = notificationService;
		this.reportRepository = reportRepository;
		this.emailService = emailService;
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
		return topicService.findRegisteredWithoutCouncil();
	}

	public List<Council> findByLecturer(Lecturer lecturer) {
		return councilRepository.findWithMembersByMemberId(lecturer.getId());
	}

	@Transactional(readOnly = true)
	public List<Council> findByPeriod(Long periodId) {
		return councilRepository.findByPeriodId(periodId);
	}

	@Transactional(readOnly = true)
	public List<TopicResult> findResultsByPeriod(Long periodId) {
		return resultRepository.findByPeriodId(periodId);
	}

	@Transactional(readOnly = true)
	public long count() {
		return councilRepository.count();
	}

	@Transactional(readOnly = true)
	public long countByPeriod(Long periodId) {
		return councilRepository.countByPeriodId(periodId);
	}

	@Transactional(readOnly = true)
	public long countPublishedResults(Long periodId) {
		return resultRepository.countPublishedByPeriod(periodId);
	}

	@Transactional(readOnly = true)
	public double averagePublishedScore(Long periodId) {
		return resultRepository.averagePublishedScoreByPeriod(periodId);
	}

	@Transactional(readOnly = true)
	public Map<String, Double> scoreChart(Long periodId) {
		Map<String, Double> rows = new LinkedHashMap<>();
		for (Object[] row : resultRepository.scoreRowsByPeriod(periodId)) {
			rows.put((String) row[0], ((Number) row[1]).doubleValue());
		}
		return rows;
	}

	@Transactional(readOnly = true)
	public List<CouncilView> findAllViews() {
		List<Council> councils = councilRepository.findAllWithMembers();
		Map<Long, TopicResult> results = resultsByCouncilId(councils);
		Map<Long, Report> reports = reportsByTopicId(councils);
		List<CouncilView> views = new ArrayList<>();
		for (Council council : councils) {
			Report report = council.getTopic() != null ? reports.get(council.getTopic().getId()) : null;
			views.add(new CouncilView(council, null, results.get(council.getId()), council.isFullyScored(), report));
		}
		return views;
	}

	@Transactional(readOnly = true)
	public List<CouncilView> findViewsFor(Lecturer lecturer) {
		List<Council> councils = councilRepository.findWithMembersByMemberId(lecturer.getId());
		Map<Long, TopicResult> results = resultsByCouncilId(councils);
		Map<Long, Report> reports = reportsByTopicId(councils);
		List<CouncilView> views = new ArrayList<>();
		for (Council council : councils) {
			CouncilMember mine = council.memberOf(lecturer.getId());
			Report report = council.getTopic() != null ? reports.get(council.getTopic().getId()) : null;
			views.add(new CouncilView(council, mine, results.get(council.getId()), council.isFullyScored(), report));
		}
		return views;
	}

	private Map<Long, Report> reportsByTopicId(List<Council> councils) {
		if (councils.isEmpty()) {
			return Map.of();
		}
		List<Long> topicIds = councils.stream()
				.filter(c -> c.getTopic() != null)
				.map(c -> c.getTopic().getId())
				.toList();
		if (topicIds.isEmpty()) {
			return Map.of();
		}
		List<Report> reports = reportRepository.findByTopicIds(topicIds);
		Map<Long, Report> map = new HashMap<>();
		for (Report r : reports) {
			if (r.getTeam() != null && r.getTeam().getTopic() != null) {
				map.put(r.getTeam().getTopic().getId(), r);
			}
		}
		return map;
	}

	private Map<Long, TopicResult> resultsByCouncilId(List<Council> councils) {
		if (councils.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = councils.stream().map(Council::getId).toList();
		Map<Long, TopicResult> results = new HashMap<>();
		for (TopicResult result : resultRepository.findByCouncilIds(ids)) {
			results.put(result.getCouncil().getId(), result);
		}
		return results;
	}

	public Council createFromInput(Long topicId, String name, List<String> lecturerCodes, List<String> roleNames) {
		List<String> codes = new ArrayList<>();
		List<CouncilRole> roles = new ArrayList<>();
		if (lecturerCodes != null) {
			for (int i = 0; i < lecturerCodes.size(); i++) {
				String code = lecturerCodes.get(i);
				if (code == null || code.isBlank()) {
					continue;
				}
				codes.add(code.trim());
				roles.add(CouncilRole.parseOrMember(roleNames == null || roleNames.size() <= i
						? null
						: roleNames.get(i)));
			}
		}
		return create(topicId, name, codes, roles);
	}

	public Council create(Long topicId, String name, List<String> lecturerCodes, List<CouncilRole> roles) {
		Topic topic = topicService.findById(topicId);
		if (!topic.isRegistered()) {
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
			if (topic.isAdvisorOf(lecturer.getId())) {
				throw new InvalidCouncilException(
						"Giảng viên " + lecturer.getFullName() + " (" + lecturer.getLecturerCode()
								+ ") là cán bộ hướng dẫn của đề tài, không được tham gia hội đồng");
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
		CouncilMember member = council.memberOf(actor.getId());
		if (member == null) {
			throw new InvalidCouncilException("Bạn không phải thành viên của hội đồng này");
		}
		Topic topic = council.getTopic();
		if (topic.requiresReviewerScore() && !topic.hasReviewerScore()) {
			throw new InvalidCouncilException("Giảng viên phản biện chưa chấm điểm, hội đồng chưa thể chấm điểm");
		}
		ScoreRules.validate(score).ifPresent(message -> {
			throw new InvalidCouncilException(message);
		});
		TopicResult result = resultOf(council);
		if (result != null && result.isPublished()) {
			throw new InvalidCouncilException("Kết quả đã công bố, không thể sửa điểm");
		}
		member.setScore(score);
		member.setComment(comment);
		member.setScoredAt(LocalDateTime.now());
		refreshAverage(council);
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
		if (council.getTopic().requiresReviewerScore() && !council.getTopic().hasReviewerScore()) {
			throw new InvalidCouncilException("Giảng viên phản biện chưa chấm điểm, chưa thể tổng hợp điểm");
		}
		TopicResult result = resultOf(council);
		if (result != null && result.isPublished()) {
			throw new InvalidCouncilException("Kết quả đã công bố, không thể tổng hợp lại");
		}
		if (!council.isFullyScored()) {
			throw new InvalidCouncilException("Còn thành viên chưa nhập điểm");
		}
		if (result == null) {
			result = new TopicResult();
			result.setCouncil(council);
		}
		result.setAverageScore(council.averageScore());
		return resultRepository.save(result);
	}

	public TopicResult publish(Long resultId) {
		TopicResult result = resultRepository.findById(resultId)
				.orElseThrow(() -> new CouncilNotFoundException("Không tìm thấy kết quả có id: " + resultId));
		if (result.isPublished()) {
			throw new InvalidCouncilException("Kết quả đã được công bố");
		}
		result.publish();
		Council council = result.getCouncil();
		Team team = teamService.findByTopicId(council.getTopic().getId());
		if (team != null) {
			for (Student member : team.getMembers()) {
				notificationService.send(member, NotificationType.RESULT, "Đã công bố điểm bảo vệ",
						"Đề tài \"" + council.getTopic().getName() + "\" đạt " + result.getFinalScore() + " điểm",
						"/results");
				if (member.getEmail() != null && !member.getEmail().isBlank()) {
					emailService.sendResultAnnouncement(member.getEmail(), member.getFullName(),
							council.getTopic().getName(), result.getFinalScore());
				}
			}
		}
		for (Lecturer advisor : council.getTopic().getAdvisors()) {
			notificationService.send(advisor, NotificationType.RESULT, "Đã công bố điểm bảo vệ",
					"Đề tài \"" + council.getTopic().getName() + "\" đạt " + result.getFinalScore() + " điểm",
					"/results");
		}
		return resultRepository.save(result);
	}

	@Transactional(readOnly = true)
	public TopicResult findResultByCouncilId(Long councilId) {
		return resultRepository.findByCouncilId(councilId).orElse(null);
	}

	@Transactional(readOnly = true)
	public TopicResult findResultById(Long id) {
		return resultRepository.findById(id)
				.orElseThrow(() -> new CouncilNotFoundException("Không tìm thấy kết quả có id: " + id));
	}

	@Transactional(readOnly = true)
	public List<TopicResult> findPublishedResultsFor(Student student) {
		return resultRepository.findPublishedForStudent(student.getId());
	}

	@Transactional(readOnly = true)
	public List<TopicResult> findPublishedResultsOfAdvisor(Lecturer lecturer) {
		return resultRepository.findPublishedOfAdvisor(lecturer.getId());
	}

	private void refreshAverage(Council council) {
		TopicResult result = resultOf(council);
		if (result == null || result.isPublished() || !council.isFullyScored()) {
			return;
		}
		result.setAverageScore(council.averageScore());
		resultRepository.save(result);
	}

	private TopicResult resultOf(Council council) {
		return findResultByCouncilId(council.getId());
	}
}