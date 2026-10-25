package project.qldt.service;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.support.Beans;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.exception.InvalidRegistrationPeriodException;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RegistrationPeriodServiceTest {

	private final RegistrationPeriodService periodService = Beans.get(RegistrationPeriodService.class);

	@Test
	void taoDotHopLe() {
		RegistrationPeriod period = periodService.create(hopLe());

		assertEquals("Đợt 1", period.getName());
		assertFalse(period.isRegistrationLocked());
		assertEquals(LocalDate.of(2027, 1, 30), period.getReportDeadline());
	}

	@Test
	void hanNopBaoCaoTruocNgaySvKetThucBiChan() {
		RegistrationPeriodRequest request = hopLe();
		request.setReportDeadline(LocalDate.of(2027, 1, 15));

		assertThrows(InvalidRegistrationPeriodException.class, () -> periodService.create(request));
	}

	@Test
	void hanNopBaoCaoHopLeDuocLuu() {
		RegistrationPeriodRequest request = hopLe();
		request.setReportDeadline(LocalDate.of(2027, 1, 25));

		assertEquals(LocalDate.of(2027, 1, 25), periodService.create(request).getReportDeadline());
	}

	@Test
	void khoaVaMoKhoaDangKy() {
		RegistrationPeriod period = periodService.create(hopLe());

		assertTrue(periodService.setRegistrationLocked(period.getId(), true).isRegistrationLocked());
		assertFalse(periodService.setRegistrationLocked(period.getId(), false).isRegistrationLocked());
	}

	@Test
	void svBatDauTruocKhiGvKetThucBiChan() {
		RegistrationPeriodRequest request = hopLe();
		request.setStudentRegisterStart(LocalDateTime.of(2027, 1, 9, 8, 0));

		assertThrows(InvalidRegistrationPeriodException.class, () -> periodService.create(request));
	}

	private RegistrationPeriodRequest hopLe() {
		RegistrationPeriodRequest request = new RegistrationPeriodRequest();
		request.setName("Đợt 1");
		request.setType(PeriodType.COURSE_PROJECT);
		request.setTeacherRegisterStart(LocalDateTime.of(2027, 1, 5, 8, 0));
		request.setTeacherRegisterEnd(LocalDateTime.of(2027, 1, 10, 17, 0));
		request.setStudentRegisterStart(LocalDateTime.of(2027, 1, 11, 8, 0));
		request.setStudentRegisterEnd(LocalDateTime.of(2027, 1, 20, 17, 0));
		request.setReportDeadline(LocalDate.of(2027, 1, 30));
		return request;
	}
}
