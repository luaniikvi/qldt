package project.qldt.service;
import project.qldt.support.Beans;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import project.qldt.dto.AccountRequest;
import project.qldt.exception.AccountNotFoundException;
import project.qldt.exception.InvalidAccountException;
import project.qldt.model.Account;
import project.qldt.model.AccountType;
import project.qldt.model.Admin;
import project.qldt.model.Lecturer;
import project.qldt.model.Role;
import project.qldt.model.Student;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountServiceExtraTest {

	private AccountService accountService;

	@BeforeEach
	void setUp() {
		accountService = Beans.get(AccountService.class);
	}

	@Test
	void tenDangNhapAdminRongBiChan() {
		assertThrows(InvalidAccountException.class,
				() -> accountService.createAdmin(null, "123456", "Tên", "a@qldt.local"));
		assertThrows(InvalidAccountException.class,
				() -> accountService.createAdmin("   ", "123456", "Tên", "a@qldt.local"));
	}

	@Test
	void maGiangVienRongBiChan() {
		assertThrows(InvalidAccountException.class,
				() -> accountService.createLecturer("123456", "Tên", "a@qldt.local", null, "Bộ môn"));
		assertThrows(InvalidAccountException.class,
				() -> accountService.createLecturer("123456", "Tên", "a@qldt.local", "  ", "Bộ môn"));
	}

	@Test
	void maSinhVienRongBiChan() {
		assertThrows(InvalidAccountException.class,
				() -> accountService.createStudent("123456", "Tên", "a@qldt.local", null));
		assertThrows(InvalidAccountException.class,
				() -> accountService.createStudent("123456", "Tên", "a@qldt.local", "  "));
	}

	@Test
	void boMonGiangVienNullBiChan() {
		assertThrows(InvalidAccountException.class,
				() -> accountService.createLecturer("123456", "Tên", "a@qldt.local", "GV01", null));
	}

	@Test
	void timKiemTheoIdKhongTonTaiNemLoi() {
		assertThrows(AccountNotFoundException.class, () -> accountService.findById(9999L));
	}

	@Test
	void timKiemTheoUsernameKhongTonTaiNemLoi() {
		assertThrows(AccountNotFoundException.class, () -> accountService.findByUsername("KHONG_TON_TAI"));
	}

	@Test
	void timSinhVienTheoMaKhongTonTaiNemLoi() {
		assertThrows(AccountNotFoundException.class, () -> accountService.findStudentByCode("SV999"));
	}

	@Test
	void timGiangVienTheoMaKhongTonTaiNemLoi() {
		assertThrows(AccountNotFoundException.class, () -> accountService.findLecturerByCode("GV999"));
	}

	@Test
	void datLaiMatKhauNganHonSauKyTuBiChan() {
		Student student = accountService.createStudent("123456", "Sinh viên", "sv@qldt.local", "SV901");

		assertThrows(InvalidAccountException.class, () -> accountService.resetPassword(student.getId(), null));
		assertThrows(InvalidAccountException.class, () -> accountService.resetPassword(student.getId(), "12345"));
	}

	@Test
	void ganDeanChoAdminBiChan() {
		Admin admin = accountService.createAdmin("admin_t", "123456", "Admin", "admin@qldt.local");

		assertThrows(InvalidAccountException.class, () -> accountService.setDean(admin.getId(), true));
	}

	@Test
	void baiNhiemDeanChoGiangVien() {
		accountService.createLecturer("123456", "GV 1", "gv1@qldt.local", "GV901", "Công nghệ phần mềm");
		accountService.assignDean("GV901", true);
		assertTrue(accountService.findLecturerByCode("GV901").isDean());

		accountService.assignDean("GV901", false);
		assertFalse(accountService.findLecturerByCode("GV901").isDean());
		assertFalse(accountService.findLecturerByCode("GV901").getRoles().contains(Role.DEAN));
	}

	@Test
	void taoTaiKhoanTuRequestAdminVaStudent() {
		AccountRequest adminReq = new AccountRequest();
		adminReq.setType(AccountType.ADMIN);
		adminReq.setCode("admin2");
		adminReq.setFullName("Admin 2");
		adminReq.setEmail("admin2@qldt.local");
		adminReq.setPassword("123456");

		Account admin = accountService.createAccount(adminReq);
		assertTrue(admin instanceof Admin);
		assertEquals("admin2", admin.getUsername());

		AccountRequest svReq = new AccountRequest();
		svReq.setType(AccountType.STUDENT);
		svReq.setCode("SV902");
		svReq.setFullName("Sinh viên 2");
		svReq.setEmail("sv2@qldt.local");
		svReq.setPassword("123456");

		Account sv = accountService.createAccount(svReq);
		assertTrue(sv instanceof Student);
		assertEquals("SV902", sv.getUsername());
	}

	@Test
	void danhSachLocTheoLoaiTaiKhoan() {
		accountService.createAdmin("admin_t", "123456", "Admin", "admin@qldt.local");
		accountService.createLecturer("123456", "GV", "gv@qldt.local", "GV01", "CNPM");
		accountService.createStudent("123456", "SV", "sv@qldt.local", "SV01");

		List<Account> all = accountService.findAll();
		List<Student> students = accountService.findAllStudents();
		List<Lecturer> lecturers = accountService.findAllLecturers();

		assertTrue(all.size() >= 3);
		assertTrue(students.stream().anyMatch(student -> "SV01".equals(student.getStudentCode())));
		assertTrue(lecturers.stream().anyMatch(lecturer -> "GV01".equals(lecturer.getLecturerCode())));
	}
}
