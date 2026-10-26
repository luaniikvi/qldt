package project.qldt.service;

import project.qldt.service.account.AccountService;
import project.qldt.service.council.CouncilService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.report.ReportService;
import project.qldt.service.statistics.StatisticsService;
import project.qldt.service.team.TeamService;
import project.qldt.service.topic.TopicRegistrationService;
import project.qldt.service.topic.TopicService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.period.PeriodStatistics;
import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.dto.statistics.StatisticsChart;
import project.qldt.dto.statistics.StatisticsCharts;
import project.qldt.dto.topic.TopicRequest;
import project.qldt.dto.statistics.TopicStatisticsRow;
import project.qldt.model.council.CouncilRole;
import project.qldt.model.account.Lecturer;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.account.Student;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;
import project.qldt.model.council.TopicResult;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StatisticsServiceTest {

	@Autowired
	private AccountService accountService;

	@Autowired
	private RegistrationPeriodService periodService;

	@Autowired
	private TopicService topicService;

	@Autowired
	private TeamService teamService;

	@Autowired
	private TopicRegistrationService registrationService;

	@Autowired
	private ReportService reportService;

	@Autowired
	private CouncilService councilService;

	@Autowired
	private StatisticsService statisticsService;

	private RegistrationPeriod period;
	private Topic topicOne;
	private Topic topicTwo;
	private Lecturer advisor;

	@BeforeEach
	void setUp() {
		advisor = accountService.createLecturer("123456", "GV 801", "gv801@qldt.local", "GV801", "CNPM");
		Lecturer chair = accountService.createLecturer("123456", "GV 802", "gv802@qldt.local", "GV802", "CNPM");
		Lecturer secretary = accountService.createLecturer("123456", "GV 803", "gv803@qldt.local", "GV803", "CNPM");
		Lecturer member = accountService.createLecturer("123456", "GV 804", "gv804@qldt.local", "GV804", "CNPM");
		Student sv801 = accountService.createStudent("123456", "SV 801", "sv801@qldt.local", "SV801");
		Student sv802 = accountService.createStudent("123456", "SV 802", "sv802@qldt.local", "SV802");
		Student sv803 = accountService.createStudent("123456", "SV 803", "sv803@qldt.local", "SV803");

		period = periodService.create(periodRequest());
		topicOne = topicService.create(topicRequest(period.getId(), "De tai mot"), advisor);
		topicService.approve(topicOne.getId());
		topicTwo = topicService.create(topicRequest(period.getId(), "De tai hai"), advisor);
		topicService.publish(period.getId());
		moveToStudentPhase(period);

		Team team = teamService.create(period.getId(), sv801);
		teamService.invite(team.getId(), "SV802", sv801);
		teamService.respondInvitation(team.getId(), sv802, true, null);
		teamService.invite(team.getId(), "SV803", sv801);
		teamService.respondInvitation(team.getId(), sv803, true, null);
		registrationService.register(team.getId(), topicOne.getId(), sv801);
		registrationService.approve(topicOne.getId(), advisor);
		reportService.submit(team.getId(), sv801, new MockMultipartFile("file", "bao-cao.pdf", "application/pdf",
				"noi dung".getBytes()));

		var council = councilService.create(topicOne.getId(), "Hoi dong thong ke", List.of("GV802", "GV803", "GV804"),
				List.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER));
		councilService.score(council.getId(), chair, 8.0, "Tot");
		councilService.score(council.getId(), secretary, 7.5, "Kha");
		councilService.score(council.getId(), member, 8.5, "Tot");
		TopicResult result = councilService.confirm(council.getId(), chair);
		councilService.publish(result.getId());
	}

	@Test
	void thongKeDemDungSoLuong() {
		PeriodStatistics statistics = statisticsService.summarize(period.getId());

		assertEquals(2, statistics.topicCount());
		assertEquals(1, statistics.publishedTopicCount());
		assertEquals(1, statistics.registeredTopicCount());
		assertEquals(1, statistics.teamCount());
		assertEquals(1, statistics.reportCount());
		assertEquals(1, statistics.councilCount());
		assertEquals(1, statistics.publishedResultCount());
	}

	@Test
	void diemTrungBinhSauKhiCongBo() {
		PeriodStatistics statistics = statisticsService.summarize(period.getId());

		assertEquals(8.0, statistics.averageScore());
	}

	@Test
	void dongThongKeCoDuLieuCuaNhomVaHoiDong() {
		List<TopicStatisticsRow> rows = statisticsService.rows(period.getId());

		assertEquals(2, rows.size());
		TopicStatisticsRow registered = rows.stream()
				.filter(row -> row.topic().getId().equals(topicOne.getId())).findFirst().orElseThrow();
		assertEquals("SV 801", registered.teamLeader());
		assertEquals(3, registered.memberCount());
		assertEquals("bao-cao.pdf", registered.reportFileName());
		assertEquals("Hoi dong thong ke", registered.councilName());
		assertEquals(8.0, registered.score());
		assertTrue(registered.published());

		TopicStatisticsRow empty = rows.stream().filter(row -> row.topic().getId().equals(topicTwo.getId())).findFirst()
				.orElseThrow();
		assertEquals("", empty.teamLeader());
		assertEquals(0, empty.memberCount());
		assertFalse(empty.published());
	}

	@Test
	void xuatCsvCoTieuDeVaDuLieu() {
		String csv = statisticsService.exportCsv(period.getId());

		assertTrue(csv.startsWith("Đề tài,Giảng viên hướng dẫn,Nhóm trưởng"));
		assertTrue(csv.contains("De tai mot"));
		assertTrue(csv.contains("SV 801"));
		assertTrue(csv.contains("8.0"));
	}

	@Test
	void dotChuaCoDuLieuThiTraVeKhong() {
		RegistrationPeriod emptyPeriod = periodService.create(periodRequest());

		PeriodStatistics statistics = statisticsService.summarize(emptyPeriod.getId());

		assertEquals(0, statistics.topicCount());
		assertEquals(0, statistics.teamCount());
		assertEquals(0.0, statistics.averageScore());
	}

	@Test
	void xuatXlsxCoTieuDeVaDuLieu() throws IOException {
		byte[] content = statisticsService.exportXlsx(period.getId());

		try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
			Sheet sheet = workbook.getSheetAt(0);
			assertEquals("Đề tài", sheet.getRow(0).getCell(0).getStringCellValue());
			assertEquals("De tai hai", sheet.getRow(1).getCell(0).getStringCellValue());
			assertEquals("De tai mot", sheet.getRow(2).getCell(0).getStringCellValue());
			assertEquals(8.0, sheet.getRow(2).getCell(6).getNumericCellValue());
		}
	}

	@Test
	void bieuDoTrangThaiTienDoVaDiem() {
		StatisticsCharts charts = statisticsService.charts(period.getId());

		StatisticsChart published = charts.statusBars().stream()
				.filter(bar -> bar.label().equals("Đã công bố")).findFirst().orElseThrow();
		assertEquals(1.0, published.value());
		assertEquals(50.0, published.percent());

		StatisticsChart reports = charts.progressBars().stream()
				.filter(bar -> bar.label().equals("Báo cáo đã nộp")).findFirst().orElseThrow();
		assertEquals(1.0, reports.value());

		assertEquals(1, charts.scoreBars().size());
		StatisticsChart score = charts.scoreBars().get(0);
		assertEquals("Hoi dong thong ke", score.label());
		assertEquals(8.0, score.value());
		assertEquals(80.0, score.percent());
	}

	private void moveToStudentPhase(RegistrationPeriod target) {
		LocalDateTime now = LocalDateTime.now();
		target.setTeacherRegisterStart(now.minusDays(10));
		target.setTeacherRegisterEnd(now.minusDays(9));
		target.setStudentRegisterStart(now.minusDays(1));
		target.setStudentRegisterEnd(now.plusDays(1));
	}

	private RegistrationPeriodRequest periodRequest() {
		RegistrationPeriodRequest request = new RegistrationPeriodRequest();
		request.setName("Đợt thống kê");
		request.setType(PeriodType.COURSE_PROJECT);
		request.setTeacherRegisterStart(LocalDateTime.now().minusDays(1));
		request.setTeacherRegisterEnd(LocalDateTime.now().plusDays(1));
		request.setStudentRegisterStart(LocalDateTime.now().plusDays(2));
		request.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));
		request.setReportDeadline(LocalDateTime.now().plusDays(10).toLocalDate());
		return request;
	}

	private TopicRequest topicRequest(Long periodId, String name) {
		TopicRequest request = new TopicRequest();
		request.setName(name);
		request.setDescription("Noi dung " + name);
		request.setPeriodId(periodId);
		return request;
	}
}