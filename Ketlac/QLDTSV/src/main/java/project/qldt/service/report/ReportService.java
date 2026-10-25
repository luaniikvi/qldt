package project.qldt.service.report;

import project.qldt.service.notification.NotificationService;
import project.qldt.service.team.TeamService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import project.qldt.config.AppProperties;
import project.qldt.exception.InvalidReportException;
import project.qldt.exception.ReportNotFoundException;
import project.qldt.model.account.Lecturer;
import project.qldt.model.notification.NotificationType;
import project.qldt.model.report.Report;
import project.qldt.model.account.Student;
import project.qldt.model.council.Council;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;
import project.qldt.model.topic.TopicAvailability;
import project.qldt.repository.report.ReportRepository;
import project.qldt.service.council.CouncilService;

@Service
@Transactional
public class ReportService {

	public static final long MAX_SIZE_MB = 10;
	private static final long MAX_SIZE_B = MAX_SIZE_MB * 1024 * 1024;

	private final ReportRepository reportRepository;
	private final TeamService teamService;
	private final NotificationService notificationService;
	private final CouncilService councilService;
	private final Path uploadDir;

	public ReportService(ReportRepository reportRepository, TeamService teamService,
			NotificationService notificationService, CouncilService councilService, AppProperties appProperties) {
		this.reportRepository = reportRepository;
		this.teamService = teamService;
		this.notificationService = notificationService;
		this.councilService = councilService;
		this.uploadDir = appProperties.uploadPath();
	}

	public Report submit(Long teamId, Student actor, MultipartFile file) {
		Team team = teamService.findById(teamId);
		requireLeader(team, actor);
		if (team.getTopic() == null || team.getTopic().getAvailability() != TopicAvailability.REGISTERED) {
			throw new InvalidReportException("Đề tài của nhóm chưa được duyệt, chưa thể nộp báo cáo");
		}
		validateDeadline(team);
		validateFile(file);

		Report existing = findByTeam(team);
		int version = existing == null ? 1 : existing.getVersion() + 1;
		String originalFileName = originalFileName(file);
		String storedFileName = storedName(team.getId(), version, originalFileName);
		Path target = uploadDir.resolve(storedFileName);
		try {
			Files.createDirectories(uploadDir);
			file.transferTo(target);
		} catch (IOException ex) {
			throw new UncheckedIOException("Không lưu được tệp báo cáo", ex);
		}

		Report report = existing == null ? new Report() : existing;
		if (existing == null) {
			report.setTeam(team);
		}
		report.setVersion(version);
		report.setOriginalFileName(originalFileName);
		report.setStoredFileName(storedFileName);
		report.setSize(file.getSize());
		report.setSubmittedAt(LocalDateTime.now());
		try {
			Report saved = reportRepository.saveAndFlush(report);
			if (existing != null) {
				deleteQuietly(existing.getStoredFileName(), storedFileName);
			}
			notifyAdvisors(team, originalFileName);
			return saved;
		} catch (RuntimeException ex) {
			deleteQuietly(storedFileName, null);
			throw ex;
		}
	}

	private void notifyAdvisors(Team team, String fileName) {
		String content = "Nhóm của " + team.getLeader().getFullName() + " đã nộp báo cáo \"" + fileName + "\"";
		for (Lecturer advisor : team.getTopic().getAdvisors()) {
			notificationService.send(advisor, NotificationType.APPROVAL, "Báo cáo mới được nộp", content, "/reports");
		}
	}

	private String originalFileName(MultipartFile file) {
		String name = file.getOriginalFilename();
		if (name == null || name.isBlank()) {
			return "bao-cao";
		}
		return Paths.get(name).getFileName().toString();
	}

	private String storedName(Long teamId, int version, String originalFileName) {
		String extension = extensionOf(originalFileName);
		return "team-" + teamId + "-v" + version + (extension.isEmpty() ? "" : "." + extension);
	}

	private String extensionOf(String fileName) {
		int dot = fileName.lastIndexOf('.');
		if (dot < 0 || dot == fileName.length() - 1) {
			return "";
		}
		return fileName.substring(dot + 1).toLowerCase();
	}

	private void deleteQuietly(String fileName, String keepFileName) {
		if (fileName == null || fileName.isBlank() || fileName.equals(keepFileName)) {
			return;
		}
		try {
			Files.deleteIfExists(uploadDir.resolve(fileName));
		} catch (IOException ex) {
			throw new UncheckedIOException("Không dọn được tệp báo cáo cũ", ex);
		}
	}

	@Transactional(readOnly = true)
	public Report findByTeam(Team team) {
		return reportRepository.findByTeamId(team.getId()).orElse(null);
	}

	@Transactional(readOnly = true)
	public Report findById(Long id) {
		return reportRepository.findById(id)
				.orElseThrow(() -> new ReportNotFoundException("Không tìm thấy báo cáo có id: " + id));
	}

	@Transactional(readOnly = true)
	public Report findByTopicId(Long topicId) {
		return reportRepository.findByTeamTopicId(topicId).orElse(null);
	}

	@Transactional(readOnly = true)
	public Map<Long, Report> findReportsByTopicIds(Collection<Long> topicIds) {
		if (topicIds == null || topicIds.isEmpty()) {
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

	@Transactional(readOnly = true)
	public List<Report> findByAdvisor(Lecturer lecturer) {
		return reportRepository.findByAdvisorId(lecturer.getId());
	}

	@Transactional(readOnly = true)
	public List<Report> findByPeriod(Long periodId) {
		return reportRepository.findByPeriodId(periodId);
	}

	@Transactional(readOnly = true)
	public long countByPeriod(Long periodId) {
		return reportRepository.countByTeamPeriodId(periodId);
	}

	@Transactional(readOnly = true)
	public Report findForLecturer(Long reportId, Lecturer lecturer) {
		Report report = findById(reportId);
		Topic topic = report.getTeam().getTopic();
		if (topic == null) {
			throw new InvalidReportException("Báo cáo chưa gắn với đề tài nào");
		}
		boolean reviewer = topic.getReviewer() != null && topic.getReviewer().getId().equals(lecturer.getId());
		if (!lecturer.isDean() && !topic.isAdvisorOf(lecturer.getId()) && !reviewer && !isCouncilMember(topic.getId(), lecturer.getId())) {
			throw new InvalidReportException("Bạn không có quyền tải báo cáo của đề tài này");
		}
		return report;
	}

	private boolean isCouncilMember(Long topicId, Long lecturerId) {
		Council council = councilService.findByTopicId(topicId);
		return council != null && council.memberOf(lecturerId) != null;
	}

	@Transactional(readOnly = true)
	public Report findForTeamMember(Long teamId, Student student) {
		Team team = teamService.findById(teamId);
		if (!team.isMember(student.getId())) {
			throw new InvalidReportException("Bạn không phải thành viên của nhóm này");
		}
		Report report = findByTeam(team);
		if (report == null) {
			throw new ReportNotFoundException("Nhóm chưa nộp báo cáo");
		}
		return report;
	}

	public ReportFile prepareDownload(Report report) {
		Path path = uploadDir.resolve(report.getStoredFileName());
		if (!Files.exists(path)) {
			throw new ReportNotFoundException("Tệp báo cáo không còn trên máy chủ");
		}
		return new ReportFile(downloadFileName(report), new FileSystemResource(path));
	}

	private String downloadFileName(Report report) {
		String originalFileName = report.getOriginalFileName();
		if (originalFileName == null || originalFileName.isBlank()) {
			String extension = extensionOf(report.getStoredFileName());
			return "bao-cao-" + report.getId() + (extension.isEmpty() ? "" : "." + extension);
		}
		return originalFileName;
	}

	public record ReportFile(String fileName, Resource resource) {
	}

	private void requireLeader(Team team, Student actor) {
		if (!team.isLeader(actor.getId())) {
			throw new InvalidReportException("Chỉ nhóm trưởng mới được nộp báo cáo");
		}
	}

	private void validateDeadline(Team team) {
		LocalDate deadline = team.getPeriod().getReportDeadline();
		if (deadline != null && LocalDate.now().isAfter(deadline)) {
			throw new InvalidReportException("Đã quá hạn nộp báo cáo (hạn: " + deadline + ")");
		}
	}

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new InvalidReportException("Vui lòng chọn tệp báo cáo");
		}
		if (file.getSize() > MAX_SIZE_B) {
			throw new InvalidReportException("Tệp báo cáo tối đa 10MB");
		}
	}
}