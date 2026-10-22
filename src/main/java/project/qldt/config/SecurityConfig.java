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
			accountService.createAdmin("admin", "123456", "Quản trị hệ thống", "admin@qldt.local");
			accountService.createLecturer("dean", "123456", "Trưởng khoa CNTT", "dean@qldt.local", "GV001");
			accountService.assignDean("GV001", true);
			accountService.createLecturer("lecturer", "123456", "Giảng viên CNTT", "lecturer@qldt.local", "GV002");
			accountService.createStudent("student", "123456", "Sinh viên CNTT", "student@qldt.local", "SV001");
		};
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/", "/home", "/login", "/error", "/css/**", "/js/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/periods/new", "/periods/*/edit").hasRole("DEAN")
						.requestMatchers(HttpMethod.GET, "/periods/**").authenticated()
						.requestMatchers("/periods/**").hasRole("DEAN")
						.requestMatchers("/api/**").authenticated()
						.anyRequest().authenticated())
				.formLogin(form -> form.defaultSuccessUrl("/", true))
				.httpBasic(Customizer.withDefaults())
				.logout(logout -> logout.logoutSuccessHandler(
						(request, response, authentication) -> response
								.sendRedirect(request.getContextPath() + "/home?logout")))
				.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));
		return http.build();
	}
}
