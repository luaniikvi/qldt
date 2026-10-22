package project.qldt.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.PeriodStatistics;
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