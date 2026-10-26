package project.qldt.service.statistics;

import project.qldt.service.council.CouncilService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.report.ReportService;
import project.qldt.service.team.TeamService;
import project.qldt.service.topic.TopicService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.period.PeriodStatistics;
import project.qldt.dto.statistics.StatisticsChart;
import project.qldt.dto.statistics.StatisticsCharts;
import project.qldt.dto.statistics.TopicStatisticsRow;
import project.qldt.model.council.Council;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.report.Report;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;
import project.qldt.model.topic.TopicAvailability;
import project.qldt.model.council.TopicResult;
import project.qldt.model.topic.TopicStatus;

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

	public Long resolvePeriodId(Long requestedPeriodId) {
		if (requestedPeriodId != null) {
			return requestedPeriodId;
		}
		List<RegistrationPeriod> periods = periods();
		return periods.isEmpty() ? null : periods.get(0).getId();
	}

	public PeriodStatistics summarize(Long periodId) {
		RegistrationPeriod period = periodService.findById(periodId);
		long totalTopics = topicService.countByPeriod(periodId);
		long publishedTopics = topicService.countByPeriodAndStatus(periodId, TopicStatus.PUBLISHED);
		long registeredTopics = topicService.countByPeriodAndAvailability(periodId, TopicAvailability.REGISTERED);
		long teams = teamService.countByPeriod(periodId);
		long reports = reportService.countByPeriod(periodId);
		long councils = councilService.countByPeriod(periodId);
		long publishedResults = councilService.countPublishedResults(periodId);
		double average = councilService.averagePublishedScore(periodId);
		return new PeriodStatistics(period, (int) totalTopics, (int) publishedTopics, (int) registeredTopics,
				(int) teams, (int) reports, (int) councils, (int) publishedResults,
				Math.round(average * 100.0) / 100.0);
	}

	public List<TopicStatisticsRow> rows(Long periodId) {
		Map<Long, Team> teams = teamsByTopic(periodId);
		Map<Long, Report> reports = reportsByTeam(periodId);
		Map<Long, Council> councils = councilsByTopic(periodId);
		Map<Long, TopicResult> results = resultsByCouncil(periodId);
		List<TopicStatisticsRow> rows = new ArrayList<>();
		for (Topic topic : topicService.findByPeriod(periodId)) {
			Team team = teams.get(topic.getId());
			Report report = team == null ? null : reports.get(team.getId());
			Council council = councils.get(topic.getId());
			TopicResult result = council == null ? null : results.get(council.getId());
			rows.add(new TopicStatisticsRow(topic, topicService.advisorNames(topic),
					team == null ? "" : team.getLeader().getFullName(),
					team == null ? 0 : team.getMembers().size(), report == null ? "" : report.getOriginalFileName(),
					council == null ? "" : council.getName(), result == null ? null : result.getAverageScore(),
					result != null && result.isPublished(), topicService.requiresReviewerScore(topic)));
		}
		return rows;
	}

	private Map<Long, Team> teamsByTopic(Long periodId) {
		Map<Long, Team> map = new HashMap<>();
		for (Team team : teamService.findByPeriod(periodId)) {
			if (team.getTopic() != null) {
				map.put(team.getTopic().getId(), team);
			}
		}
		return map;
	}

	private Map<Long, Report> reportsByTeam(Long periodId) {
		Map<Long, Report> map = new HashMap<>();
		for (Report report : reportService.findByPeriod(periodId)) {
			map.put(report.getTeam().getId(), report);
		}
		return map;
	}

	private Map<Long, Council> councilsByTopic(Long periodId) {
		Map<Long, Council> map = new HashMap<>();
		for (Council council : councilService.findByPeriod(periodId)) {
			map.put(council.getTopic().getId(), council);
		}
		return map;
	}

	private Map<Long, TopicResult> resultsByCouncil(Long periodId) {
		Map<Long, TopicResult> map = new HashMap<>();
		for (TopicResult result : councilService.findResultsByPeriod(periodId)) {
			map.put(result.getCouncil().getId(), result);
		}
		return map;
	}

	public byte[] exportCsvWithBom(Long periodId) {
		byte[] content = exportCsv(periodId).getBytes(StandardCharsets.UTF_8);
		byte[] body = new byte[content.length + 3];
		body[0] = (byte) 0xEF;
		body[1] = (byte) 0xBB;
		body[2] = (byte) 0xBF;
		System.arraycopy(content, 0, body, 3, content.length);
		return body;
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
		Map<TopicStatus, Long> statusCounts = topicService.statusCounts(periodId);
		long totalTopics = topicService.countByPeriod(periodId);

		List<StatisticsChart> statusBars = new ArrayList<>();
		for (TopicStatus status : TopicStatus.values()) {
			long count = statusCounts.getOrDefault(status, 0L);
			statusBars.add(new StatisticsChart(status.getLabel(), count, percent(count, totalTopics)));
		}

		long publishedTopics = topicService.countByPeriodAndStatus(periodId, TopicStatus.PUBLISHED);
		long registeredTopics = topicService.countByPeriodAndAvailability(periodId, TopicAvailability.REGISTERED);
		long teams = teamService.countByPeriod(periodId);
		long reports = reportService.countByPeriod(periodId);
		long councils = councilService.countByPeriod(periodId);
		long publishedResults = councilService.countPublishedResults(periodId);
		long max = Math.max(1, Math.max(totalTopics, Math.max(teams, councils)));
		List<StatisticsChart> progressBars = List.of(
				new StatisticsChart("Đề tài đã công bố", publishedTopics, percent(publishedTopics, max)),
				new StatisticsChart("Đề tài đã có nhóm", registeredTopics, percent(registeredTopics, max)),
				new StatisticsChart("Nhóm sinh viên", teams, percent(teams, max)),
				new StatisticsChart("Báo cáo đã nộp", reports, percent(reports, max)),
				new StatisticsChart("Hội đồng", councils, percent(councils, max)),
				new StatisticsChart("Kết quả đã công bố", publishedResults, percent(publishedResults, max)));

		List<StatisticsChart> scoreBars = new ArrayList<>();
		for (Map.Entry<String, Double> entry : councilService.scoreChart(periodId).entrySet()) {
			double score = Math.round(entry.getValue() * 100) / 100.0;
			scoreBars.add(new StatisticsChart(entry.getKey(), score, Math.min(100, score * 10)));
		}
		return new StatisticsCharts(statusBars, progressBars, scoreBars);
	}

	private double percent(long value, long total) {
		if (total <= 0) {
			return 0;
		}
		return Math.round(value * 1000.0 / total) / 10.0;
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

}