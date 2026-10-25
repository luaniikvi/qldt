package project.qldt.config;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.model.account.Lecturer;
import project.qldt.model.account.Student;
import project.qldt.model.council.Council;
import project.qldt.model.council.CouncilMember;
import project.qldt.model.council.CouncilRole;
import project.qldt.model.council.TopicResult;
import project.qldt.model.department.Department;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.report.Report;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;
import project.qldt.model.topic.TopicAvailability;
import project.qldt.model.topic.TopicStatus;
import project.qldt.repository.council.CouncilRepository;
import project.qldt.repository.council.TopicResultRepository;
import project.qldt.repository.period.RegistrationPeriodRepository;
import project.qldt.repository.report.ReportRepository;
import project.qldt.repository.team.TeamRepository;
import project.qldt.repository.topic.TopicRepository;
import project.qldt.service.account.AccountService;
import project.qldt.service.department.DepartmentService;

@Component
@Profile("!test")
@Order(2)
@Transactional
public class SampleDataInitializer implements CommandLineRunner {

	private final RegistrationPeriodRepository periodRepository;
	private final TopicRepository topicRepository;
	private final TeamRepository teamRepository;
	private final CouncilRepository councilRepository;
	private final TopicResultRepository topicResultRepository;
	private final ReportRepository reportRepository;
	private final AccountService accountService;
	private final DepartmentService departmentService;

	public SampleDataInitializer(RegistrationPeriodRepository periodRepository,
			TopicRepository topicRepository,
			TeamRepository teamRepository,
			CouncilRepository councilRepository,
			TopicResultRepository topicResultRepository,
			ReportRepository reportRepository,
			AccountService accountService,
			DepartmentService departmentService) {
		this.periodRepository = periodRepository;
		this.topicRepository = topicRepository;
		this.teamRepository = teamRepository;
		this.councilRepository = councilRepository;
		this.topicResultRepository = topicResultRepository;
		this.reportRepository = reportRepository;
		this.accountService = accountService;
		this.departmentService = departmentService;
	}

	@Override
	public void run(String... args) {
		if (periodRepository.count() > 0) {
			return;
		}

		Department cnpm = departmentService.createIfMissing("CNPM", "Công nghệ phần mềm");
		Department httt = departmentService.createIfMissing("HTTT", "Hệ thống thông tin");
		Department attt = departmentService.createIfMissing("ATTT", "Mạng và an ninh mạng");
		Department ai = departmentService.createIfMissing("AI", "Trí tuệ nhân tạo");

		Lecturer gv1 = getOrCreateLecturer("GV001", "Lê Văn Vinh", "Công nghệ phần mềm");
		// accountService.setDean(gv1.getId(), true);
		Lecturer gv2 = getOrCreateLecturer("GV002", "Nguyễn Minh Đạo", "Công nghệ phần mềm");
		Lecturer gv3 = getOrCreateLecturer("GV003", "Nguyễn Trần Thi Văn", "Công nghệ phần mềm");
		Lecturer gv4 = getOrCreateLecturer("GV004", "Hoàng Thái Xuân Khoa", "Công nghệ phần mềm");
		Lecturer gv5 = getOrCreateLecturer("GV005", "Vũ Đình Bảo", "Công nghệ phần mềm");
		Lecturer gv6 = getOrCreateLecturer("GV006", "Nguyễn Thanh Tuấn", "Hệ thống thông tin");
		Lecturer gv7 = getOrCreateLecturer("GV007", "Phan Thị Thể", "Hệ thống thông tin");
		Lecturer gv8 = getOrCreateLecturer("GV008", "Trần Quang Khải", "Hệ thống thông tin");
		Lecturer gv9 = getOrCreateLecturer("GV009", "Nguyễn Thị Thanh Vân", "Mạng và an ninh mạng");
		Lecturer gv10 = getOrCreateLecturer("GV010", "Đinh Công Đoan", "Mạng và an ninh mạng");
		Lecturer gv11 = getOrCreateLecturer("GV011", "Nguyễn Đăng Quang", "Mạng và an ninh mạng");
		Lecturer gv12 = getOrCreateLecturer("GV012", "Phan Thị Huyền Trang", "Trí tuệ nhân tạo");
		Lecturer gv13 = getOrCreateLecturer("GV013", "Hoàng Văn Dũng", "Trí tuệ nhân tạo");

		Student sv1 = getOrCreateStudent("SV001", "Nguyễn Văn An");
		Student sv2 = getOrCreateStudent("SV002", "Trần Nguyên Bảo");
		Student sv3 = getOrCreateStudent("SV003", "Huỳnh Thị Châu");
		Student sv4 = getOrCreateStudent("SV004", "Cao Ngọc Minh Anh");
		Student sv5 = getOrCreateStudent("SV005", "Vũ Cát Tiên");
		Student sv6 = getOrCreateStudent("SV006", "Lăng Vi");
		Student sv7 = getOrCreateStudent("SV007", "Trần Công Thái");
		Student sv8 = getOrCreateStudent("SV008", "Vũ Đình Khôi");
		Student sv9 = getOrCreateStudent("SV009", "Đỗ Hoàng Long");
		Student sv10 = getOrCreateStudent("SV010", "Phạm Ngọc Ánh");
		Student sv11 = getOrCreateStudent("SV011", "Lê Tuấn Kiệt");
		Student sv12 = getOrCreateStudent("SV012", "Trần Minh Quân");
		Student sv13 = getOrCreateStudent("SV013", "Hoàng Gia Hưng");
		Student sv14 = getOrCreateStudent("SV014", "Đinh Thu Hà");
		Student sv15 = getOrCreateStudent("SV015", "Võ Thành Nam");

		RegistrationPeriod p1 = new RegistrationPeriod();
		p1.setName("Khóa luận tốt nghiệp - Học kỳ 1 (2025-2026)");
		p1.setType(PeriodType.GRADUATION_THESIS);
		p1.setTeacherRegisterStart(LocalDateTime.now().minusMonths(6));
		p1.setTeacherRegisterEnd(LocalDateTime.now().minusMonths(5).minusDays(15));
		p1.setStudentRegisterStart(LocalDateTime.now().minusMonths(5).minusDays(14));
		p1.setStudentRegisterEnd(LocalDateTime.now().minusMonths(4));
		p1.setReportDeadline(LocalDate.now().minusMonths(2));
		p1.setReviewerScoreDeadline(LocalDate.now().minusMonths(1).minusDays(15));
		p1.setCouncilReportDate(LocalDate.now().minusMonths(1));
		p1.setRegistrationLocked(true);
		periodRepository.save(p1);

		Topic t1 = new Topic();
		t1.setName("Xây dựng hệ thống quản lý đề tài sinh viên theo kiến trúc Microservices");
		t1.setDescription("Phát triển nền tảng quản lý quy trình đăng ký, xét duyệt, nghiệm thu đề tài NCKH và Khóa luận tốt nghiệp sử dụng kiến trúc Microservices kết hợp Spring Cloud và Docker.");
		t1.setPeriod(p1);
		t1.setPrimaryAdvisor(gv2);
		t1.setDepartment(cnpm);
		t1.getAdvisors().add(gv2);
		t1.getAdvisors().add(gv3);
		t1.setReviewer(gv4);
		t1.setReviewerScore(8.5);
		t1.setReviewerComment("Báo cáo cấu trúc rõ ràng, giải pháp phân tách service hợp lý, hiệu năng đáp ứng tốt.");
		t1.setReviewerScoredAt(LocalDateTime.now().minusMonths(1).minusDays(20));
		t1.setStatus(TopicStatus.PUBLISHED);
		t1.setAvailability(TopicAvailability.REGISTERED);
		topicRepository.save(t1);

		Team team1 = new Team();
		team1.setPeriod(p1);
		team1.setLeader(sv1);
		team1.getMembers().add(sv1);
		team1.getMembers().add(sv2);
		team1.getMembers().add(sv3);
		team1.setTopic(t1);
		teamRepository.save(team1);

		Report r1 = new Report();
		r1.setTeam(team1);
		r1.setOriginalFileName("Bao_cao_KLTN_Nhom1_Microservices.pdf");
		r1.setStoredFileName("seed_report_team1.pdf");
		r1.setSize(2457600L);
		r1.setVersion(1);
		r1.setSubmittedAt(LocalDateTime.now().minusMonths(2).minusDays(2));
		reportRepository.save(r1);

		Council c1 = new Council();
		c1.setName("Hội đồng KLTN Công nghệ phần mềm 01");
		c1.setTopic(t1);

		CouncilMember m11 = new CouncilMember();
		m11.setCouncil(c1);
		m11.setLecturer(gv1);
		m11.setRole(CouncilRole.CHAIR);
		m11.setScore(9.0);
		m11.setComment("Kiến trúc hệ thống rất tốt, trả lời phản biện lưu loát, tự tin.");
		m11.setScoredAt(LocalDateTime.now().minusMonths(1).minusDays(1));

		CouncilMember m12 = new CouncilMember();
		m12.setCouncil(c1);
		m12.setLecturer(gv3);
		m12.setRole(CouncilRole.SECRETARY);
		m12.setScore(8.5);
		m12.setComment("Đầy đủ chức năng, quy trình chuẩn hóa cao.");
		m12.setScoredAt(LocalDateTime.now().minusMonths(1).minusDays(1));

		CouncilMember m13 = new CouncilMember();
		m13.setCouncil(c1);
		m13.setLecturer(gv5);
		m13.setRole(CouncilRole.MEMBER);
		m13.setScore(9.0);
		m13.setComment("Thiết kế RESTful API và tài liệu OpenAPI hoàn chỉnh.");
		m13.setScoredAt(LocalDateTime.now().minusMonths(1).minusDays(1));

		c1.getMembers().add(m11);
		c1.getMembers().add(m12);
		c1.getMembers().add(m13);
		councilRepository.save(c1);

		TopicResult res1 = new TopicResult();
		res1.setCouncil(c1);
		res1.setAverageScore(8.83);
		res1.setFinalScore(8.70);
		res1.setPublished(true);
		res1.setPublishedAt(LocalDateTime.now().minusMonths(1));
		topicResultRepository.save(res1);

		RegistrationPeriod p2 = new RegistrationPeriod();
		p2.setName("Khóa luận tốt nghiệp - Học kỳ 2 (2025-2026)");
		p2.setType(PeriodType.GRADUATION_THESIS);
		p2.setTeacherRegisterStart(LocalDateTime.now().minusMonths(2));
		p2.setTeacherRegisterEnd(LocalDateTime.now().minusMonths(1).minusDays(15));
		p2.setStudentRegisterStart(LocalDateTime.now().minusMonths(1).minusDays(14));
		p2.setStudentRegisterEnd(LocalDateTime.now().minusDays(10));
		p2.setReportDeadline(LocalDate.now().plusDays(5));
		p2.setReviewerScoreDeadline(LocalDate.now().plusDays(15));
		p2.setCouncilReportDate(LocalDate.now().plusDays(25));
		p2.setRegistrationLocked(false);
		periodRepository.save(p2);

		Topic t2 = new Topic();
		t2.setName("Ứng dụng Trí tuệ nhân tạo nhận diện khuôn mặt trong hệ thống điểm danh tự động");
		t2.setDescription("Xây dựng mô hình Deep Learning nhận diện khuôn mặt đa góc nghiêng, tích hợp camera Edge AI để điểm danh sinh viên trong giảng đường.");
		t2.setPeriod(p2);
		t2.setPrimaryAdvisor(gv12);
		t2.setDepartment(ai);
		t2.getAdvisors().add(gv12);
		t2.setReviewer(gv13);
		t2.setReviewerScore(8.0);
		t2.setReviewerComment("Mô hình nhận diện đạt F1-score cao, tài liệu đầy đủ.");
		t2.setReviewerScoredAt(LocalDateTime.now().minusDays(2));
		t2.setStatus(TopicStatus.PUBLISHED);
		t2.setAvailability(TopicAvailability.REGISTERED);
		topicRepository.save(t2);

		Team team2 = new Team();
		team2.setPeriod(p2);
		team2.setLeader(sv4);
		team2.getMembers().add(sv4);
		team2.getMembers().add(sv5);
		team2.getMembers().add(sv6);
		team2.getMembers().add(sv7);
		team2.setTopic(t2);
		teamRepository.save(team2);

		Report r2 = new Report();
		r2.setTeam(team2);
		r2.setOriginalFileName("Bao_cao_KLTN_Nhom2_FaceRecognition.pdf");
		r2.setStoredFileName("seed_report_team2.pdf");
		r2.setSize(3145728L);
		r2.setVersion(1);
		r2.setSubmittedAt(LocalDateTime.now().minusDays(3));
		reportRepository.save(r2);

		Council c2 = new Council();
		c2.setName("Hội đồng KLTN Trí tuệ nhân tạo 01");
		c2.setTopic(t2);

		CouncilMember m21 = new CouncilMember();
		m21.setCouncil(c2);
		m21.setLecturer(gv1);
		m21.setRole(CouncilRole.CHAIR);
		m21.setScore(8.5);
		m21.setComment("Độ chính xác khả quan, xử lý trường hợp đặc biệt tốt.");
		m21.setScoredAt(LocalDateTime.now().minusDays(1));

		CouncilMember m22 = new CouncilMember();
		m22.setCouncil(c2);
		m22.setLecturer(gv6);
		m22.setRole(CouncilRole.SECRETARY);
		m22.setScore(8.0);
		m22.setComment("Giao diện trực quan, thao tác mượt.");
		m22.setScoredAt(LocalDateTime.now().minusDays(1));

		CouncilMember m23 = new CouncilMember();
		m23.setCouncil(c2);
		m23.setLecturer(gv11);
		m23.setRole(CouncilRole.MEMBER);
		m23.setScore(8.5);
		m23.setComment("Triển khai trên thiết bị nhúng ổn định.");
		m23.setScoredAt(LocalDateTime.now().minusDays(1));

		CouncilMember m24 = new CouncilMember();
		m24.setCouncil(c2);
		m24.setLecturer(gv8);
		m24.setRole(CouncilRole.MEMBER);

		c2.getMembers().add(m21);
		c2.getMembers().add(m22);
		c2.getMembers().add(m23);
		c2.getMembers().add(m24);
		councilRepository.save(c2);

		Topic t3 = new Topic();
		t3.setName("Nền tảng phát hiện mã độc và tấn công mạng dựa trên Deep Learning");
		t3.setDescription("Thu thập và phân tích lưu lượng mạng, trích xuất đặc trưng payload và sử dụng mô hình CNN-BiLSTM để phát hiện sớm các tấn công zero-day.");
		t3.setPeriod(p2);
		t3.setPrimaryAdvisor(gv9);
		t3.setDepartment(attt);
		t3.getAdvisors().add(gv9);
		t3.setReviewer(gv10);
		t3.setReviewerScore(9.0);
		t3.setReviewerComment("Phân tích tấn công mạng rất chuyên sâu, dữ liệu thực nghiệm phong phú.");
		t3.setReviewerScoredAt(LocalDateTime.now().minusDays(3));
		t3.setStatus(TopicStatus.PUBLISHED);
		t3.setAvailability(TopicAvailability.REGISTERED);
		topicRepository.save(t3);

		Team team3 = new Team();
		team3.setPeriod(p2);
		team3.setLeader(sv8);
		team3.getMembers().add(sv8);
		team3.getMembers().add(sv9);
		team3.getMembers().add(sv10);
		team3.getMembers().add(sv11);
		team3.getMembers().add(sv12);
		team3.setTopic(t3);
		teamRepository.save(team3);

		Report r3 = new Report();
		r3.setTeam(team3);
		r3.setOriginalFileName("Bao_cao_KLTN_Nhom3_MalwareDetection.pdf");
		r3.setStoredFileName("seed_report_team3.pdf");
		r3.setSize(4194304L);
		r3.setVersion(1);
		r3.setSubmittedAt(LocalDateTime.now().minusDays(2));
		reportRepository.save(r3);

		Council c3 = new Council();
		c3.setName("Hội đồng KLTN Mạng và An ninh mạng 01");
		c3.setTopic(t3);

		CouncilMember m31 = new CouncilMember();
		m31.setCouncil(c3);
		m31.setLecturer(gv1);
		m31.setRole(CouncilRole.CHAIR);
		m31.setScore(9.0);
		m31.setComment("Kết quả thực nghiệm ấn tượng.");
		m31.setScoredAt(LocalDateTime.now().minusDays(1));

		CouncilMember m32 = new CouncilMember();
		m32.setCouncil(c3);
		m32.setLecturer(gv10);
		m32.setRole(CouncilRole.SECRETARY);
		m32.setScore(8.5);
		m32.setComment("Bộ dữ liệu malware được dán nhãn chuẩn xác.");
		m32.setScoredAt(LocalDateTime.now().minusDays(1));

		CouncilMember m33 = new CouncilMember();
		m33.setCouncil(c3);
		m33.setLecturer(gv3);
		m33.setRole(CouncilRole.MEMBER);
		m33.setScore(8.5);
		m33.setComment("Trả lời phản biện rõ ràng.");
		m33.setScoredAt(LocalDateTime.now().minusDays(1));

		CouncilMember m34 = new CouncilMember();
		m34.setCouncil(c3);
		m34.setLecturer(gv5);
		m34.setRole(CouncilRole.MEMBER);
		m34.setScore(9.0);
		m34.setComment("Mô hình có tính ứng dụng thực tiễn cao.");
		m34.setScoredAt(LocalDateTime.now().minusDays(1));

		CouncilMember m35 = new CouncilMember();
		m35.setCouncil(c3);
		m35.setLecturer(gv4);
		m35.setRole(CouncilRole.MEMBER);
		m35.setScore(8.5);
		m35.setComment("Chất lượng báo cáo xuất sắc.");
		m35.setScoredAt(LocalDateTime.now().minusDays(1));

		c3.getMembers().add(m31);
		c3.getMembers().add(m32);
		c3.getMembers().add(m33);
		c3.getMembers().add(m34);
		c3.getMembers().add(m35);
		councilRepository.save(c3);

		TopicResult res3 = new TopicResult();
		res3.setCouncil(c3);
		res3.setAverageScore(8.70);
		res3.setFinalScore(8.82);
		res3.setPublished(false);
		topicResultRepository.save(res3);

		Topic t4 = new Topic();
		t4.setName("Hệ thống thương mại điện tử tích hợp trợ lý ảo tư vấn tự động");
		t4.setDescription("Xây dựng website bán lẻ trực tuyến tích hợp Chatbot AI hỗ trợ giải đáp thắc mắc và gợi ý sản phẩm dựa trên sở thích người dùng.");
		t4.setPeriod(p2);
		t4.setPrimaryAdvisor(gv7);
		t4.setDepartment(httt);
		t4.getAdvisors().add(gv7);
		t4.setReviewer(gv8);
		t4.setReviewerScore(8.0);
		t4.setReviewerComment("Ứng dụng thương mại điện tử chỉn chu, chatbot phản hồi nhanh.");
		t4.setReviewerScoredAt(LocalDateTime.now().minusDays(1));
		t4.setStatus(TopicStatus.PUBLISHED);
		t4.setAvailability(TopicAvailability.REGISTERED);
		topicRepository.save(t4);

		Team team4 = new Team();
		team4.setPeriod(p2);
		team4.setLeader(sv13);
		team4.getMembers().add(sv13);
		team4.getMembers().add(sv14);
		team4.getMembers().add(sv15);
		team4.setTopic(t4);
		teamRepository.save(team4);

		Report r4 = new Report();
		r4.setTeam(team4);
		r4.setOriginalFileName("Bao_cao_KLTN_Nhom4_Ecommerce.pdf");
		r4.setStoredFileName("seed_report_team4.pdf");
		r4.setSize(2097152L);
		r4.setVersion(1);
		r4.setSubmittedAt(LocalDateTime.now().minusDays(1));
		reportRepository.save(r4);

		Council c4 = new Council();
		c4.setName("Hội đồng KLTN Hệ thống thông tin 01");
		c4.setTopic(t4);

		CouncilMember m41 = new CouncilMember();
		m41.setCouncil(c4);
		m41.setLecturer(gv1);
		m41.setRole(CouncilRole.CHAIR);

		CouncilMember m42 = new CouncilMember();
		m42.setCouncil(c4);
		m42.setLecturer(gv2);
		m42.setRole(CouncilRole.SECRETARY);

		CouncilMember m43 = new CouncilMember();
		m43.setCouncil(c4);
		m43.setLecturer(gv6);
		m43.setRole(CouncilRole.MEMBER);

		c4.getMembers().add(m41);
		c4.getMembers().add(m42);
		c4.getMembers().add(m43);
		councilRepository.save(c4);

		RegistrationPeriod p3 = new RegistrationPeriod();
		p3.setName("Nghiên cứu khoa học sinh viên năm 2026");
		p3.setType(PeriodType.RESEARCH);
		p3.setTeacherRegisterStart(LocalDateTime.now().minusDays(10));
		p3.setTeacherRegisterEnd(LocalDateTime.now().plusDays(20));
		p3.setStudentRegisterStart(LocalDateTime.now().plusDays(21));
		p3.setStudentRegisterEnd(LocalDateTime.now().plusDays(45));
		p3.setReportDeadline(LocalDate.now().plusMonths(4));
		p3.setRegistrationLocked(false);
		periodRepository.save(p3);

		Topic t5 = new Topic();
		t5.setName("Nghiên cứu ứng dụng Blockchain trong xác thực văn bằng đại học");
		t5.setDescription("Khảo sát và xây dựng giải pháp lưu trữ hash chứng chỉ số trên mạng Ethereum nhằm ngăn ngừa tình trạng làm bằng giả.");
		t5.setPeriod(p3);
		t5.setPrimaryAdvisor(gv4);
		t5.setDepartment(cnpm);
		t5.getAdvisors().add(gv4);
		t5.setStatus(TopicStatus.PUBLISHED);
		t5.setAvailability(TopicAvailability.AVAILABLE);
		topicRepository.save(t5);

		Topic t6 = new Topic();
		t6.setName("Xây dựng hệ thống phát hiện tin giả trên mạng xã hội bằng mô hình ngôn ngữ lớn");
		t6.setDescription("Fine-tune mô hình PhoBERT và LLM tiếng Việt để phân loại tin tức thật - giả dựa trên nội dung bài viết và nguồn thông tin.");
		t6.setPeriod(p3);
		t6.setPrimaryAdvisor(gv12);
		t6.setDepartment(ai);
		t6.getAdvisors().add(gv12);
		t6.setStatus(TopicStatus.PUBLISHED);
		t6.setAvailability(TopicAvailability.AVAILABLE);
		topicRepository.save(t6);
	}

	private Lecturer getOrCreateLecturer(String code, String fullName, String department) {
		if (!accountService.existsByUsername(code)) {
			accountService.createLecturer("123456", fullName, "lecturer@qldt.local", code, department);
		}
		return accountService.findLecturerByCode(code);
	}

	private Student getOrCreateStudent(String code, String fullName) {
		if (!accountService.existsByUsername(code)) {
			accountService.createStudent("123456", fullName, "student@qldt.local", code);
		}
		return accountService.findStudentByCode(code);
	}
}
