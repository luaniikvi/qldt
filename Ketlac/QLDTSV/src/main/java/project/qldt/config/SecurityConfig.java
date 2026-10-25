package project.qldt.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import project.qldt.repository.department.DepartmentRepository;
import project.qldt.service.account.AccountService;
import project.qldt.service.department.DepartmentService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private final DepartmentRepository departmentRepository;

    SecurityConfig(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() {
		return web -> web.ignoring().requestMatchers("/images/**", "/css/**", "/js/**", "/favicon.ico");
	}

	@Bean
	@Order(1)
	public CommandLineRunner initAccounts(AccountService accountService, DepartmentService departmentService) {
		return args -> {
			seedDepartments(departmentService);
			if (!accountService.existsByUsername("admin")) {
				accountService.createAdmin("admin", "123456", "Quản trị hệ thống", "admin@qldt.local");
			}
			seedLecturer(accountService, "Lê Văn Vinh", "GV001", "Công nghệ phần mềm");
			accountService.setDean(accountService.findLecturerByCode("GV001").getId(),true);
			seedLecturer(accountService, "Nguyễn Minh Đạo", "GV002","Công nghệ phần mềm");
			seedLecturer(accountService, "Nguyễn Trần Thi Văn", "GV003","Công nghệ phần mềm");
			seedLecturer(accountService, "Hoàng Thái Xuân Khoa", "GV004","Công nghệ phần mềm");
			seedLecturer(accountService, "Vũ Đình Bảo", "GV005","Công nghệ phần mềm");
			seedLecturer(accountService, "Nguyễn Thanh Tuấn", "GV006","Hệ thống thông tin");
			seedLecturer(accountService, "Phan Thị Thể", "GV007","Hệ thống thông tin");
			seedLecturer(accountService, "Trần Quang Khải", "GV008","Hệ thống thông tin");
			seedLecturer(accountService, "Nguyễn Thị Thanh Vân", "GV009","Mạng và an ninh mạng");
			seedLecturer(accountService, "Đinh Công Đoan", "GV010","Mạng và an ninh mạng");
			seedLecturer(accountService, "Nguyễn Đăng Quang", "GV011","Mạng và an ninh mạng");
			seedLecturer(accountService, "Phan Thị Huyền Trang", "GV012","Trí tuệ nhân tạo");
			seedLecturer(accountService, "Hoàng Văn Dũng", "GV013","Trí tuệ nhân tạo");

			seedStudent(accountService, "Nguyễn Văn An", "SV001");
			seedStudent(accountService, "Trần Nguyên Bảo", "SV002");
			seedStudent(accountService, "Huỳnh Thị Châu", "SV003");
			seedStudent(accountService, "Cao Ngọc Minh Anh", "SV004");
			seedStudent(accountService, "Vũ Cát Tiên", "SV005");
			seedStudent(accountService, "Lăng Vi", "SV006");
			seedStudent(accountService, "Trần Công Thái", "SV007");
			seedStudent(accountService, "Vũ Đình Khôi", "SV008");
			seedStudent(accountService, "Đỗ Hoàng Long", "SV009");
			seedStudent(accountService, "Phạm Ngọc Ánh", "SV010");
			seedStudent(accountService, "Lê Tuấn Kiệt", "SV011");
			seedStudent(accountService, "Trần Minh Quân", "SV012");
			seedStudent(accountService, "Hoàng Gia Hưng", "SV013");
			seedStudent(accountService, "Đinh Thu Hà", "SV014");
			seedStudent(accountService, "Võ Thành Nam", "SV015");
		};
	}

	private void seedDepartments(DepartmentService departmentService) {
		departmentService.createIfMissing("CNPM", "Công nghệ phần mềm");
		departmentService.createIfMissing("HTTT", "Hệ thống thông tin");
		departmentService.createIfMissing("ATTT", "Mạng và an ninh mạng");
		departmentService.createIfMissing("AI", "Trí tuệ nhân tạo");
	}

	private void seedLecturer(AccountService accountService, String fullName, String code, String department) {
		if (!accountService.existsByUsername(code)) {
			accountService.createLecturer("123456", fullName, "lecturer@qldt.local", code, department);
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
