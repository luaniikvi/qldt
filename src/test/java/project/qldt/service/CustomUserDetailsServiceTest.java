package project.qldt.service;
import project.qldt.support.Beans;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import project.qldt.model.Lecturer;
import project.qldt.model.Student;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomUserDetailsServiceTest {

	private AccountService accountService;
	private CustomUserDetailsService userDetailsService;

	@BeforeEach
	void setUp() {
		accountService = Beans.get(AccountService.class);
		userDetailsService = Beans.get(CustomUserDetailsService.class);
	}

	@Test
	void loadUserByUsernameAdmin() {
		accountService.createAdmin("admin_t", "123456", "Quản trị", "admin@qldt.local");

		UserDetails user = userDetailsService.loadUserByUsername("admin_t");

		assertEquals("admin_t", user.getUsername());
		assertTrue(user.isEnabled());
		assertTrue(user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
	}

	@Test
	void loadUserByUsernameLecturerAndDean() {
		Lecturer gv = accountService.createLecturer("123456", "GV 1", "gv1@qldt.local", "GV901", "Công nghệ phần mềm");
		accountService.assignDean("GV901", true);

		UserDetails user = userDetailsService.loadUserByUsername("GV901");

		assertEquals("GV901", user.getUsername());
		assertTrue(user.isEnabled());
		assertTrue(user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_LECTURER")));
		assertTrue(user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DEAN")));
	}

	@Test
	void loadUserByUsernameStudent() {
		Student sv = accountService.createStudent("123456", "SV 1", "sv1@qldt.local", "SV901");

		UserDetails user = userDetailsService.loadUserByUsername("SV901");

		assertEquals("SV901", user.getUsername());
		assertTrue(user.isEnabled());
		assertTrue(user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT")));
		assertFalse(user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DEAN")));
	}

	@Test
	void loadUserByUsernameDisabledAccountReturnsDisabledUserDetails() {
		Student sv = accountService.createStudent("123456", "SV 1", "sv1@qldt.local", "SV901");
		accountService.setEnabled(sv.getId(), false);

		UserDetails user = userDetailsService.loadUserByUsername("SV901");
		assertFalse(user.isEnabled());
	}

	@Test
	void loadUserByUsernameNotFoundThrows() {
		assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("KHONG_TON_TAI"));
	}
}
