package project.qldt.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import project.qldt.service.AccountService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CommandLineRunner initAccounts(AccountService accountService) {
		return args -> {
			if (!accountService.existsByUsername("admin")) {
				accountService.createAdmin("admin", "123456", "Quản trị hệ thống", "admin@qldt.local");
			}
			seedLecturer(accountService, "Trưởng khoa CNTT", "GV001");
			seedLecturer(accountService, "Giảng viên CNTT", "GV002");
			seedLecturer(accountService, "Giảng viên 3", "GV003");
			seedLecturer(accountService, "Giảng viên 4", "GV004");
			seedLecturer(accountService, "Giảng viên 5", "GV005");
			accountService.assignDean("GV001", true);
			seedStudent(accountService, "Sinh viên CNTT", "SV001");
			seedStudent(accountService, "Sinh viên CNTT", "SV002");
		};
	}

	private void seedLecturer(AccountService accountService, String fullName, String code) {
		if (!accountService.existsByUsername(code)) {
			accountService.createLecturer("123456", fullName, "lecturer@qldt.local", code, "Hệ Thống Thông Tin");
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
						.requestMatchers("/", "/home", "/login", "/error", "/css/**", "/js/**").permitAll()
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
