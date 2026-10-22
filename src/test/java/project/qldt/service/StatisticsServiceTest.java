package project.qldt.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.PeriodStatistics;
import project.qldt.dto.RegistrationPeriodRequest;
import project.qldt.dto.TopicRequest;
import project.qldt.dto.TopicStatisticsRow;
import project.qldt.model.CouncilRole;
import project.qldt.model.Lecturer;
import project.qldt.model.PeriodType;
import project.qldt.model.RegistrationPeriod;
import project.qldt.model.Student;
import project.qldt.model.Team;
import project.qldt.model.Topic;
import project.qldt.model.TopicResult;

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

		period = periodService.create(periodRequest());
		topicOne = topicService.create(topicRequest(period.getId(), "De tai mot"), advisor);
		topicService.approve(topicOne.getId());
		topicTwo = topicService.create(topicRequest(period.getId(), "De tai hai"), advisor);
		topicService.publish(period.getId());
		moveToStudentPhase(period);

		Team team = teamService.create(period.getId(), sv801);
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
		assertEquals(1, registered.memberCount());
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
		request.setType(PeriodType.COURSE);
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