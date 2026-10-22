package project.qldt.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import project.qldt.config.AppProperties;
import project.qldt.exception.InvalidReportException;
import project.qldt.exception.ReportNotFoundException;
import project.qldt.model.Lecturer;
import project.qldt.model.Report;
import project.qldt.model.Student;
import project.qldt.model.Team;
import project.qldt.model.TopicAvailability;
import project.qldt.repository.ReportRepository;

@Service
@Transactional
public class ReportService {

	private static final long MAX_SIZE = 10L * 1024 * 1024;
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");

	private final ReportRepository reportRepository;
	private final TeamService teamService;
	private final Path uploadDir;

	public ReportService(ReportRepository reportRepository, TeamService teamService, AppProperties appProperties) {
		this.reportRepository = reportRepository;
		this.teamService = teamService;
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

		String extension = extensionOf(file.getOriginalFilename());
		String storedFileName = "team-" + team.getId() + "." + extension;
		try {
			Files.createDirectories(uploadDir);
			file.transferTo(uploadDir.resolve(storedFileName));
		} catch (IOException ex) {
			throw new UncheckedIOException("Không lưu được tệp báo cáo", ex);
		}

		Report report = findByTeam(team);
		if (report == null) {
			report = new Report();
			report.setTeam(team);
			report.setVersion(1);
		} else {
			report.setVersion(report.getVersion() + 1);
		}
		report.setOriginalFileName(file.getOriginalFilename());
		report.setStoredFileName(storedFileName);
		report.setSize(file.getSize());
		report.setSubmittedAt(LocalDateTime.now());
		return reportRepository.save(report);
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
	public List<Report> findByAdvisor(Lecturer lecturer) {
		List<Report> reports = new ArrayList<>();
		for (Report report : reportRepository.findAll()) {
			boolean isAdvisor = report.getTeam().getTopic() != null
					&& report.getTeam().getTopic().getAdvisors().stream()
							.anyMatch(advisor -> advisor.getId().equals(lecturer.getId()));
			if (isAdvisor) {
				reports.add(report);
			}
		}
		return reports;
	}

	public Path loadFile(Report report) {
		return uploadDir.resolve(report.getStoredFileName());
	}

	private void requireLeader(Team team, Student actor) {
		if (!team.getLeader().getId().equals(actor.getId())) {
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
		if (file.getSize() > MAX_SIZE) {
			throw new InvalidReportException("Tệp báo cáo tối đa 10MB");
		}
		if (!ALLOWED_EXTENSIONS.contains(extensionOf(file.getOriginalFilename()))) {
			throw new InvalidReportException("Chỉ chấp nhận tệp PDF, DOC hoặc DOCX");
		}
	}

	private String extensionOf(String fileName) {
		if (fileName == null || !fileName.contains(".")) {
			return "";
		}
		return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
	}
}