package project.qldt.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import project.qldt.exception.InvalidAccountException;
import project.qldt.model.Lecturer;
import project.qldt.model.Role;
import project.qldt.model.Student;

class AccountServiceTest {

	private final AccountService accountService = new AccountService(new BCryptPasswordEncoder());

	@Test
	void studentCoRoleStudent() {
		Student student = accountService.createStudent("sv01", "123456", "Sinh viên", "sv01@qldt.local", "SV001");

		assertEquals(Role.STUDENT, student.getBaseRole());
		assertTrue(student.getRoles().contains(Role.STUDENT));
		assertFalse(student.getRoles().contains(Role.DEAN));
	}

	@Test
	void giangVienCoTheDuocGanDean() {
		accountService.createLecturer("gv01", "123456", "Giảng viên", "gv01@qldt.local", "GV001");

		Lecturer lecturer = accountService.assignDean("GV001", true);

		assertTrue(lecturer.isDean());
		assertTrue(lecturer.getRoles().contains(Role.LECTURER));
		assertTrue(lecturer.getRoles().contains(Role.DEAN));
	}

	@Test
	void boNhiemDeanChiAnhHuongGiangVienDuocChon() {
		accountService.createLecturer("gv01", "123456", "GV 1", "gv1@qldt.local", "GV001");
		accountService.createLecturer("gv02", "123456", "GV 2", "gv2@qldt.local", "GV002");

		accountService.assignDean("GV001", true);

		assertTrue(accountService.findLecturerByCode("GV001").isDean());
		assertFalse(accountService.findLecturerByCode("GV002").isDean());
	}

	@Test
	void trungTenDangNhapBiChan() {
		accountService.createStudent("sv01", "123456", "Sinh viên", "sv01@qldt.local", "SV001");

		assertThrows(InvalidAccountException.class,
				() -> accountService.createLecturer("sv01", "123456", "Giảng viên", "gv01@qldt.local", "GV001"));
	}

	@Test
	void trungMaSinhVienBiChan() {
		accountService.createStudent("sv01", "123456", "Sinh viên", "sv01@qldt.local", "SV001");

		assertThrows(InvalidAccountException.class,
				() -> accountService.createStudent("sv02", "123456", "Sinh viên 2", "sv02@qldt.local", "SV001"));
	}

	@Test
	void matKhauDuocBamBangBcrypt() {
		Student student = accountService.createStudent("sv01", "123456", "Sinh viên", "sv01@qldt.local", "SV001");

		assertNotEquals("123456", student.getPassword());
		assertTrue(new BCryptPasswordEncoder().matches("123456", student.getPassword()));
	}
}
