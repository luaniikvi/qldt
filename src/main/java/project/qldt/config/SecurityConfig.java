package project.qldt.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import project.qldt.service.account.AccountService;
import project.qldt.service.department.DepartmentService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() {
		return web -> web.ignoring().requestMatchers("/images/**", "/css/**", "/js/**", "/favicon.ico");
	}

	@Bean
	public CommandLineRunner initAccounts(AccountService accountService, DepartmentService departmentService) {
		return args -> {
			seedDepartments(departmentService);
			if (!accountService.existsByUsername("admin")) {
				accountService.createAdmin("admin", "123456", "Quản trị hệ thống", "admin@qldt.local");
			}
			seedLecturer(accountService, "Giảng viên 1", "GV001");
			// accountService.assignDean("GV001", true);
			seedLecturer(accountService, "Giảng viên 2", "GV002");
			seedLecturer(accountService, "Giảng viên 3", "GV003");
			seedLecturer(accountService, "Giảng viên 4", "GV004");
			seedLecturer(accountService, "Giảng viên 5", "GV005");
			seedStudent(accountService, "Sinh viên 1", "SV001");
			seedStudent(accountService, "Sinh viên 2", "SV002");
			seedStudent(accountService, "Sinh viên 3", "SV003");
		};
	}

	private void seedDepartments(DepartmentService departmentService) {
		departmentService.createIfMissing("CNPM", "Công nghệ phần mềm");
		departmentService.createIfMissing("HTTT", "Hệ thống thông tin");
		departmentService.createIfMissing("ATTT", "Mạng và an ninh mạng");
		departmentService.createIfMissing("AI", "Trí tuệ nhân tạo");
	}

	private void seedLecturer(AccountService accountService, String fullName, String code) {
		if (!accountService.existsByUsername(code)) {
			accountService.createLecturer("123456", fullName, "lecturer@qldt.local", code, "Hệ thống thông tin");
		}
	}

	private void seedStudent(AccountService accountService, String fullName, String code) {
		if (!accountService.existsByUsername(code)) {
			accountService.createStudent("123456", fullName, "student@qldt.local", code);
		}
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/", "/home", "/login", "/error", "/css/**", "/js/**", "/images/**",
								"/component-preview")
						.permitAll()
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.requestMatchers("/dean/**").hasRole("DEAN")
						.requestMatchers("/topics/**").hasRole("LECTURER")
						.requestMatchers("/teams/**").hasRole("STUDENT")
						.requestMatchers("/registrations/**").hasRole("LECTURER")
						.requestMatchers("/reports/**").hasRole("LECTURER")
						.requestMatchers("/councils/**").hasRole("LECTURER")
						.requestMatchers("/published-topics").authenticated()
						.requestMatchers(HttpMethod.GET, "/periods/new", "/periods/*/edit").hasRole("DEAN")
						.requestMatchers(HttpMethod.GET, "/periods/**").authenticated()
						.requestMatchers("/periods/**").hasRole("DEAN")
						.requestMatchers("/api/**").authenticated()
						.anyRequest().authenticated())
				.formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true))
				.httpBasic(Customizer.withDefaults())
				.logout(logout -> logout.logoutSuccessHandler(
						(request, response, authentication) -> response
								.sendRedirect(request.getContextPath() + "/home?logout")))
				.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));
		return http.build();
	}
}
