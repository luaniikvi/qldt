package project.qldt.service;
import project.qldt.support.Beans;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import project.qldt.dto.AccountRequest;
import project.qldt.exception.InvalidAccountException;
import project.qldt.model.Account;
import project.qldt.model.AccountType;
import project.qldt.model.Lecturer;
import project.qldt.model.Role;
import project.qldt.model.Student;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountServiceTest {

	private final AccountService accountService = Beans.get(AccountService.class);

	@Test
	void studentCoRoleStudent() {
		Student student = accountService.createStudent("123456", "Sinh viên", "sv01@qldt.local", "SV901");

		assertEquals(Role.STUDENT, student.getBaseRole());
		assertTrue(student.getRoles().contains(Role.STUDENT));
		assertFalse(student.getRoles().contains(Role.DEAN));
	}

	@Test
	void giangVienCoBoMon() {
		Lecturer lecturer = accountService.createLecturer("123456", "Giảng viên", "gv01@qldt.local", "GV901",
				"Công nghệ phần mềm");

		assertEquals("GV901", lecturer.getLecturerCode());
		assertEquals("Công nghệ phần mềm", lecturer.getDepartment());
	}

	@Test
	void dangNhapBangMaSinhVien() {
		accountService.createStudent("123456", "Sinh viên", "sv01@qldt.local", "SV901");

		Account account = accountService.findByUsername("SV901");

		assertEquals("SV901", account.getUsername());
		assertTrue(account instanceof Student);
	}

	@Test
	void dangNhapBangMaGiangVien() {
		accountService.createLecturer("123456", "Giảng viên", "gv01@qldt.local", "GV901", "Công nghệ phần mềm");

		Account account = accountService.findByUsername("GV901");

		assertEquals("GV901", account.getUsername());
		assertTrue(account instanceof Lecturer);
	}

	@Test
	void maTrungGiuaGiangVienVaSinhVienBiChan() {
		accountService.createLecturer("123456", "Giảng viên", "gv01@qldt.local", "GV901", "Công nghệ phần mềm");

		assertThrows(InvalidAccountException.class,
				() -> accountService.createStudent("123456", "Sinh viên", "sv01@qldt.local", "GV901"));
	}

	@Test
	void trungMaSinhVienBiChan() {
		accountService.createStudent("123456", "Sinh viên", "sv01@qldt.local", "SV901");

		assertThrows(InvalidAccountException.class,
				() -> accountService.createStudent("123456", "Sinh viên 2", "sv02@qldt.local", "SV901"));
	}

	@Test
	void boMonRongBiChan() {
		assertThrows(InvalidAccountException.class,
				() -> accountService.createLecturer("123456", "Giảng viên", "gv01@qldt.local", "GV901", " "));
	}

	@Test
	void giangVienCoTheDuocGanDean() {
		accountService.createLecturer("123456", "Giảng viên", "gv01@qldt.local", "GV901", "Công nghệ phần mềm");

		Lecturer lecturer = accountService.assignDean("GV901", true);

		assertTrue(lecturer.isDean());
		assertTrue(lecturer.getRoles().contains(Role.LECTURER));
		assertTrue(lecturer.getRoles().contains(Role.DEAN));
	}

	@Test
	void boNhiemDeanChiAnhHuongGiangVienDuocChon() {
		accountService.createLecturer("123456", "GV 1", "gv1@qldt.local", "GV901", "Công nghệ phần mềm");
		accountService.createLecturer("123456", "GV 2", "gv2@qldt.local", "GV902", "Hệ thống thông tin");

		accountService.assignDean("GV901", true);

		assertTrue(accountService.findLecturerByCode("GV901").isDean());
		assertFalse(accountService.findLecturerByCode("GV902").isDean());
	}

	@Test
	void trungTenDangNhapAdminBiChan() {
		accountService.createAdmin("admin_t", "123456", "Quản trị", "admin@qldt.local");

		assertThrows(InvalidAccountException.class,
				() -> accountService.createAdmin("admin_t", "123456", "Quản trị 2", "admin2@qldt.local"));
	}

	@Test
	void matKhauDuocBamBangBcrypt() {
		Student student = accountService.createStudent("123456", "Sinh viên", "sv01@qldt.local", "SV901");

		assertNotEquals("123456", student.getPassword());
		assertTrue(new BCryptPasswordEncoder().matches("123456", student.getPassword()));
	}

	@Test
	void khoaVaMoKhoaTaiKhoan() {
		Student student = accountService.createStudent("123456", "Sinh viên", "sv01@qldt.local", "SV901");

		assertFalse(accountService.setEnabled(student.getId(), false).isEnabled());
		assertTrue(accountService.setEnabled(student.getId(), true).isEnabled());
	}

	@Test
	void datLaiMatKhau() {
		Student student = accountService.createStudent("123456", "Sinh viên", "sv01@qldt.local", "SV901");

		accountService.resetPassword(student.getId(), "654321");

		assertTrue(new BCryptPasswordEncoder().matches("654321", student.getPassword()));
	}

	@Test
	void chiGiangVienMoiDuocGanDean() {
		Student student = accountService.createStudent("123456", "Sinh viên", "sv01@qldt.local", "SV901");

		assertThrows(InvalidAccountException.class, () -> accountService.setDean(student.getId(), true));
	}

	@Test
	void taoTaiKhoanTuRequest() {
		AccountRequest request = new AccountRequest();
		request.setType(AccountType.LECTURER);
		request.setCode("GV909");
		request.setFullName("Giảng viên 9");
		request.setEmail("gv9@qldt.local");
		request.setPassword("123456");
		request.setConfirmPassword("123456");
		request.setDepartment("Công nghệ phần mềm");

		Account account = accountService.createAccount(request);

		assertEquals("GV909", account.getUsername());
		assertTrue(account instanceof Lecturer);
	}
}
