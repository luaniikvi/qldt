package project.qldt.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.PeriodStatistics;
import project.qldt.dto.StatisticsChart;
import project.qldt.dto.StatisticsCharts;
import project.qldt.dto.TopicStatisticsRow;
import project.qldt.model.Council;
import project.qldt.model.Lecturer;
import project.qldt.model.RegistrationPeriod;
import project.qldt.model.Report;
import project.qldt.model.Team;
import project.qldt.model.Topic;
import project.qldt.model.TopicAvailability;
import project.qldt.model.TopicResult;
import project.qldt.model.TopicStatus;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

	private final RegistrationPeriodService periodService;
	private final TopicService topicService;
	private final TeamService teamService;
	private final ReportService reportService;
	private final CouncilService councilService;

	public StatisticsService(RegistrationPeriodService periodService, TopicService topicService,
			TeamService teamService, ReportService reportService, CouncilService councilService) {
		this.periodService = periodService;
		this.topicService = topicService;
		this.teamService = teamService;
		this.reportService = reportService;
		this.councilService = councilService;
	}

	public List<RegistrationPeriod> periods() {
		return periodService.findAll();
	}

	public PeriodStatistics summarize(Long periodId) {
		RegistrationPeriod period = periodService.findById(periodId);
		List<Topic> topics = topicsOf(periodId);
		List<Team> teams = teamsOf(periodId);
		List<Council> councils = councilsOf(periodId);
		List<TopicResult> results = resultsOf(councils);
		long publishedResults = results.stream().filter(TopicResult::isPublished).count();
		double average = results.stream().filter(TopicResult::isPublished).mapToDouble(TopicResult::getAverageScore)
				.average().orElse(0);
		int reportCount = (int) teams.stream().filter(team -> reportService.findByTeam(team) != null).count();
		int publishedTopics = (int) topics.stream().filter(topic -> topic.getStatus() == TopicStatus.PUBLISHED)
				.count();
		int registeredTopics = (int) topics.stream()
				.filter(topic -> topic.getAvailability() == TopicAvailability.REGISTERED).count();
		return new PeriodStatistics(period, topics.size(), publishedTopics, registeredTopics, teams.size(), reportCount,
				councils.size(), (int) publishedResults, Math.round(average * 100.0) / 100.0);
	}

	public List<TopicStatisticsRow> rows(Long periodId) {
		List<TopicStatisticsRow> rows = new ArrayList<>();
		for (Topic topic : topicsOf(periodId)) {
			Team team = teamService.findByTopicId(topic.getId());
			Report report = team == null ? null : reportService.findByTeam(team);
			Council council = councilService.findByTopicId(topic.getId());
			TopicResult result = council == null ? null : councilService.findResultByCouncilId(council.getId());
			rows.add(new TopicStatisticsRow(topic, advisorNames(topic),
					team == null ? "" : team.getLeader().getFullName(),
					team == null ? 0 : team.getMembers().size(), report == null ? "" : report.getOriginalFileName(),
					council == null ? "" : council.getName(), result == null ? null : result.getAverageScore(),
					result != null && result.isPublished()));
		}
		return rows;
	}

	public String exportCsv(Long periodId) {
		StringBuilder csv = new StringBuilder();
		csv.append("Đề tài,Giảng viên hướng dẫn,Nhóm trưởng,Số thành viên,Báo cáo,Hội đồng,Điểm trung bình,Công bố\n");
		for (TopicStatisticsRow row : rows(periodId)) {
			csv.append(cell(row.topic().getName())).append(',').append(cell(row.advisorNames())).append(',')
					.append(cell(row.teamLeader())).append(',').append(row.memberCount()).append(',')
					.append(cell(row.reportFileName())).append(',').append(cell(row.councilName())).append(',')
					.append(row.score() == null ? "" : row.score()).append(',')
					.append(row.published() ? "Đã công bố" : "Chưa công bố").append('\n');
		}
		return csv.toString();
	}

	public byte[] exportXlsx(Long periodId) {
		List<TopicStatisticsRow> data = rows(periodId);
		try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("Thong ke");
			CellStyle headerStyle = workbook.createCellStyle();
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerStyle.setFont(headerFont);
			String[] headers = {"Đề tài", "Giảng viên hướng dẫn", "Nhóm trưởng", "Số thành viên", "Báo cáo",
					"Hội đồng", "Điểm trung bình", "Công bố"};
			Row header = sheet.createRow(0);
			for (int i = 0; i < headers.length; i++) {
				Cell cell = header.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
			}
			int rowIndex = 1;
			for (TopicStatisticsRow item : data) {
				Row row = sheet.createRow(rowIndex++);
				row.createCell(0).setCellValue(item.topic().getName());
				row.createCell(1).setCellValue(item.advisorNames());
				row.createCell(2).setCellValue(item.teamLeader());
				row.createCell(3).setCellValue(item.memberCount());
				row.createCell(4).setCellValue(item.reportFileName());
				row.createCell(5).setCellValue(item.councilName());
				Cell scoreCell = row.createCell(6);
				if (item.score() != null) {
					scoreCell.setCellValue(item.score());
				}
				row.createCell(7).setCellValue(item.published() ? "Đã công bố" : "Chưa công bố");
			}
			for (int i = 0; i < headers.length; i++) {
				sheet.autoSizeColumn(i);
			}
			workbook.write(out);
			return out.toByteArray();
		} catch (IOException ex) {
			throw new UncheckedIOException("Không tạo được tệp Excel", ex);
		}
	}

	public StatisticsCharts charts(Long periodId) {
		List<Topic> topics = topicsOf(periodId);
		List<Team> teams = teamsOf(periodId);
		List<Council> councils = councilsOf(periodId);
		List<TopicResult> results = resultsOf(councils);

		List<StatisticsChart> statusBars = new ArrayList<>();
		for (TopicStatus status : TopicStatus.values()) {
			long count = topics.stream().filter(topic -> topic.getStatus() == status).count();
			statusBars.add(new StatisticsChart(status.getLabel(), count, percent(count, topics.size())));
		}

		long publishedTopics = topics.stream().filter(topic -> topic.getStatus() == TopicStatus.PUBLISHED)
				.count();
		long registeredTopics = topics.stream()
				.filter(topic -> topic.getAvailability() == TopicAvailability.REGISTERED).count();
		long reports = teams.stream().filter(team -> reportService.findByTeam(team) != null).count();
		long publishedResults = results.stream().filter(TopicResult::isPublished).count();
		long max = Math.max(1, Math.max(topics.size(), Math.max(teams.size(), councils.size())));
		List<StatisticsChart> progressBars = List.of(
				new StatisticsChart("Đề tài đã công bố", publishedTopics, percent(publishedTopics, max)),
				new StatisticsChart("Đề tài đã có nhóm", registeredTopics, percent(registeredTopics, max)),
				new StatisticsChart("Nhóm sinh viên", teams.size(), percent(teams.size(), max)),
				new StatisticsChart("Báo cáo đã nộp", reports, percent(reports, max)),
				new StatisticsChart("Hội đồng", councils.size(), percent(councils.size(), max)),
				new StatisticsChart("Kết quả đã công bố", publishedResults, percent(publishedResults, max)));

		List<StatisticsChart> scoreBars = new ArrayList<>();
		for (Council council : councils) {
			TopicResult result = councilService.findResultByCouncilId(council.getId());
			if (result != null) {
				double score = Math.round(result.getAverageScore() * 100) / 100.0;
				scoreBars.add(new StatisticsChart(council.getName(), score, Math.min(100, score * 10)));
			}
		}
		return new StatisticsCharts(statusBars, progressBars, scoreBars);
	}

	private double percent(long value, long total) {
		if (total <= 0) {
			return 0;
		}
		return Math.round(value * 1000.0 / total) / 10.0;
	}

	private String advisorNames(Topic topic) {
		return topic.getAdvisors().stream().map(Lecturer::getFullName).collect(Collectors.joining(", "));
	}

	private String cell(String value) {
		if (value == null) {
			return "";
		}
		if (value.contains(",") || value.contains("\"")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}

	private List<Topic> topicsOf(Long periodId) {
		return topicService.findAll().stream().filter(topic -> topic.getPeriod().getId().equals(periodId)).toList();
	}

	private List<Team> teamsOf(Long periodId) {
		return teamService.findAll().stream().filter(team -> team.getPeriod().getId().equals(periodId)).toList();
	}

	private List<Council> councilsOf(Long periodId) {
		return councilService.findAll().stream()
				.filter(council -> council.getTopic().getPeriod().getId().equals(periodId)).toList();
	}

	private List<TopicResult> resultsOf(List<Council> councils) {
		return councils.stream().map(council -> councilService.findResultByCouncilId(council.getId()))
				.filter(result -> result != null).toList();
	}
}