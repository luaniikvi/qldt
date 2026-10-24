package project.qldt.service;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.support.Beans;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.exception.InvalidRegistrationPeriodException;
import project.qldt.exception.RegistrationPeriodNotFoundException;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RegistrationPeriodServiceExtraTest {

	private RegistrationPeriodService periodService;

	@BeforeEach
	void setUp() {
		periodService = Beans.get(RegistrationPeriodService.class);
	}

	@Test
	void dotTlcnThieuHanGvpbBiChan() {
		RegistrationPeriodRequest req = baseRequest(PeriodType.SPECIALIZED_PROJECT);
		req.setReviewerScoreDeadline(null);

		assertThrows(InvalidRegistrationPeriodException.class, () -> periodService.create(req));
	}

	@Test
	void dotTlcnHanGvpbTruocNgaySvKetThucBiChan() {
		RegistrationPeriodRequest req = baseRequest(PeriodType.SPECIALIZED_PROJECT);
		req.setReviewerScoreDeadline(req.getStudentRegisterEnd().toLocalDate().minusDays(1));

		assertThrows(InvalidRegistrationPeriodException.class, () -> periodService.create(req));
	}

	@Test
	void dotTlcnHopLeLuuHanGvpbVaBoTrongNgayHoiDong() {
		RegistrationPeriodRequest req = baseRequest(PeriodType.SPECIALIZED_PROJECT);
		req.setReviewerScoreDeadline(LocalDate.of(2027, 2, 5));
		req.setCouncilReportDate(LocalDate.of(2027, 2, 10));

		RegistrationPeriod period = periodService.create(req);

		assertEquals(LocalDate.of(2027, 2, 5), period.getReviewerScoreDeadline());
		assertNull(period.getCouncilReportDate());
	}

	@Test
	void dotKltnThieuNgayHoiDongBiChan() {
		RegistrationPeriodRequest req = baseRequest(PeriodType.GRADUATION_THESIS);
		req.setReviewerScoreDeadline(LocalDate.of(2027, 2, 5));
		req.setCouncilReportDate(null);

		assertThrows(InvalidRegistrationPeriodException.class, () -> periodService.create(req));
	}

	@Test
	void dotKltnNgayHoiDongTruocHanGvpbBiChan() {
		RegistrationPeriodRequest req = baseRequest(PeriodType.GRADUATION_THESIS);
		req.setReviewerScoreDeadline(LocalDate.of(2027, 2, 10));
		req.setCouncilReportDate(LocalDate.of(2027, 2, 5));

		assertThrows(InvalidRegistrationPeriodException.class, () -> periodService.create(req));
	}

	@Test
	void dotKltnHopLeLuuCaHaiMoc() {
		RegistrationPeriodRequest req = baseRequest(PeriodType.GRADUATION_THESIS);
		req.setReviewerScoreDeadline(LocalDate.of(2027, 2, 5));
		req.setCouncilReportDate(LocalDate.of(2027, 2, 10));

		RegistrationPeriod period = periodService.create(req);

		assertEquals(LocalDate.of(2027, 2, 5), period.getReviewerScoreDeadline());
		assertEquals(LocalDate.of(2027, 2, 10), period.getCouncilReportDate());
	}

	@Test
	void dotNckhTuDongBoTrongMocGvpbVaHoiDong() {
		RegistrationPeriodRequest req = baseRequest(PeriodType.RESEARCH);
		req.setReviewerScoreDeadline(LocalDate.of(2027, 2, 5));
		req.setCouncilReportDate(LocalDate.of(2027, 2, 10));

		RegistrationPeriod period = periodService.create(req);

		assertNull(period.getReviewerScoreDeadline());
		assertNull(period.getCouncilReportDate());
	}

	@Test
	void dotNckhKhongNhapMocGvpbVaHoiDongThanhCong() {
		RegistrationPeriodRequest req = baseRequest(PeriodType.RESEARCH);
		req.setReviewerScoreDeadline(null);
		req.setCouncilReportDate(null);

		RegistrationPeriod period = periodService.create(req);

		assertNotNull(period);
		assertNull(period.getReviewerScoreDeadline());
		assertNull(period.getCouncilReportDate());
	}

	@Test
	void thoiGianGvKetThucTruocHoacBangBatDauBiChan() {
		RegistrationPeriodRequest req = baseRequest(PeriodType.COURSE_PROJECT);
		req.setTeacherRegisterEnd(req.getTeacherRegisterStart());

		assertThrows(InvalidRegistrationPeriodException.class, () -> periodService.create(req));

		req.setTeacherRegisterEnd(req.getTeacherRegisterStart().minusHours(1));
		assertThrows(InvalidRegistrationPeriodException.class, () -> periodService.create(req));
	}

	@Test
	void thoiGianSvKetThucTruocHoacBangBatDauBiChan() {
		RegistrationPeriodRequest req = baseRequest(PeriodType.COURSE_PROJECT);
		req.setStudentRegisterEnd(req.getStudentRegisterStart());

		assertThrows(InvalidRegistrationPeriodException.class, () -> periodService.create(req));

		req.setStudentRegisterEnd(req.getStudentRegisterStart().minusHours(1));
		assertThrows(InvalidRegistrationPeriodException.class, () -> periodService.create(req));
	}

	@Test
	void thieuHanNopBaoCaoBiChan() {
		RegistrationPeriodRequest req = baseRequest(PeriodType.COURSE_PROJECT);
		req.setReportDeadline(null);

		assertThrows(InvalidRegistrationPeriodException.class, () -> periodService.create(req));
	}

	@Test
	void timKiemTheoIdKhongTonTaiNemLoi() {
		assertThrows(RegistrationPeriodNotFoundException.class, () -> periodService.findById(9999L));
	}

	@Test
	void capNhatDotDangKy() {
		RegistrationPeriod period = periodService.create(baseRequest(PeriodType.COURSE_PROJECT));

		RegistrationPeriodRequest updateReq = baseRequest(PeriodType.COURSE_PROJECT);
		updateReq.setName("Đợt 1 Cập Nhật");
		updateReq.setReportDeadline(LocalDate.of(2027, 3, 1));

		RegistrationPeriod updated = periodService.update(period.getId(), updateReq);

		assertEquals("Đợt 1 Cập Nhật", updated.getName());
		assertEquals(LocalDate.of(2027, 3, 1), updated.getReportDeadline());
	}

	@Test
	void xoaDotDangKyTheoId() {
		RegistrationPeriod period = periodService.create(baseRequest(PeriodType.COURSE_PROJECT));
		Long id = period.getId();
		assertNotNull(periodService.findById(id));

		periodService.deleteById(id);

		assertThrows(RegistrationPeriodNotFoundException.class, () -> periodService.findById(id));
	}

	@Test
	void findAllTraVeBanSaoBaoVe() {
		periodService.create(baseRequest(PeriodType.COURSE_PROJECT));
		List<RegistrationPeriod> list = periodService.findAll();
		assertEquals(1, list.size());

		list.clear();
		assertEquals(1, periodService.findAll().size());
	}

	private RegistrationPeriodRequest baseRequest(PeriodType type) {
		RegistrationPeriodRequest req = new RegistrationPeriodRequest();
		req.setName("Đợt kiểm thử");
		req.setType(type);
		req.setTeacherRegisterStart(LocalDateTime.of(2027, 1, 1, 8, 0));
		req.setTeacherRegisterEnd(LocalDateTime.of(2027, 1, 10, 17, 0));
		req.setStudentRegisterStart(LocalDateTime.of(2027, 1, 11, 8, 0));
		req.setStudentRegisterEnd(LocalDateTime.of(2027, 1, 20, 17, 0));
		req.setReportDeadline(LocalDate.of(2027, 2, 28));
		return req;
	}
}
