package project.qldt.service.account;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.account.AccountRequest;
import project.qldt.exception.AccountNotFoundException;
import project.qldt.exception.InvalidAccountException;
import project.qldt.model.account.Account;
import project.qldt.model.account.AccountType;
import project.qldt.model.account.Admin;
import project.qldt.model.account.Lecturer;
import project.qldt.model.account.Role;
import project.qldt.model.account.Student;
import project.qldt.repository.account.AccountRepository;
import project.qldt.repository.account.LecturerRepository;
import project.qldt.repository.account.StudentRepository;
import project.qldt.repository.notification.NotificationRepository;
import project.qldt.service.department.DepartmentService;

@Service
@Transactional
public class AccountService {

	private final AccountRepository accountRepository;
	private final LecturerRepository lecturerRepository;
	private final StudentRepository studentRepository;
	private final PasswordEncoder passwordEncoder;
	private final DepartmentService departmentService;
	private final NotificationRepository notificationRepository;

	public AccountService(AccountRepository accountRepository, LecturerRepository lecturerRepository,
			StudentRepository studentRepository, PasswordEncoder passwordEncoder,
			DepartmentService departmentService, NotificationRepository notificationRepository) {
		this.accountRepository = accountRepository;
		this.lecturerRepository = lecturerRepository;
		this.studentRepository = studentRepository;
		this.passwordEncoder = passwordEncoder;
		this.departmentService = departmentService;
		this.notificationRepository = notificationRepository;
	}

	@Transactional(readOnly = true)
	public List<Account> findAll() {
		return accountRepository.findAll();
	}

	@Transactional(readOnly = true)
	public List<Account> search(AccountType type, Boolean enabled, String q) {
		Class<? extends Account> typeClass = null;
		if (type == AccountType.ADMIN) {
			typeClass = Admin.class;
		} else if (type == AccountType.LECTURER) {
			typeClass = Lecturer.class;
		} else if (type == AccountType.STUDENT) {
			typeClass = Student.class;
		}
		String keyword = q == null || q.isBlank() ? null : q.trim();
		return accountRepository.search(typeClass, enabled, keyword);
	}

	@Transactional(readOnly = true)
	public long count() {
		return accountRepository.count();
	}

	@Transactional(readOnly = true)
	public long countStudents() {
		return studentRepository.count();
	}

	@Transactional(readOnly = true)
	public long countLecturers() {
		return lecturerRepository.count();
	}

	@Transactional(readOnly = true)
	public List<Lecturer> findAllLecturers() {
		return lecturerRepository.findAllByOrderByLecturerCodeAsc();
	}

	@Transactional(readOnly = true)
	public Account findByUsername(String username) {
		return accountRepository.findByUsername(username)
				.orElseThrow(() -> new AccountNotFoundException("Không tìm thấy tài khoản: " + username));
	}

	@Transactional(readOnly = true)
	public boolean existsByUsername(String username) {
		return accountRepository.existsByUsername(username);
	}

	@Transactional(readOnly = true)
	public Account findById(Long id) {
		return accountRepository.findById(id)
				.orElseThrow(() -> new AccountNotFoundException("Không tìm thấy tài khoản có id: " + id));
	}

	public Account setEnabled(Long id, boolean enabled) {
		Account account = findById(id);
		account.setEnabled(enabled);
		return accountRepository.save(account);
	}

	public Account resetPassword(Long id, String rawPassword) {
		if (rawPassword == null || rawPassword.length() < 6) {
			throw new InvalidAccountException("Mật khẩu phải có ít nhất 6 ký tự");
		}
		Account account = findById(id);
		account.setPassword(passwordEncoder.encode(rawPassword));
		return accountRepository.save(account);
	}

	public void changePassword(String username, project.qldt.dto.account.ChangePasswordRequest request) {
		if (request == null) {
			throw new InvalidAccountException("Yêu cầu không hợp lệ");
		}
		if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
			throw new InvalidAccountException("Mật khẩu cũ không được để trống");
		}
		if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
			throw new InvalidAccountException("Mật khẩu mới không được để trống");
		}
		if (request.getNewPassword().length() < 6) {
			throw new InvalidAccountException("Mật khẩu mới phải có ít nhất 6 ký tự");
		}
		if (request.getConfirmPassword() == null || !request.getNewPassword().equals(request.getConfirmPassword())) {
			throw new InvalidAccountException("Xác nhận mật khẩu mới không khớp");
		}
		Account account = findByUsername(username);
		if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPassword())) {
			throw new InvalidAccountException("Mật khẩu cũ không chính xác");
		}
		if (passwordEncoder.matches(request.getNewPassword(), account.getPassword())) {
			throw new InvalidAccountException("Mật khẩu mới không được trùng với mật khẩu cũ");
		}
		account.setPassword(passwordEncoder.encode(request.getNewPassword()));
		accountRepository.save(account);
	}

	public Account createAccount(AccountRequest request) {
		validatePasswords(request);
		return switch (request.getType()) {
			case ADMIN ->
				createAdmin(request.getCode(), request.getPassword(), request.getFullName(), request.getEmail());
			case LECTURER -> createLecturer(request.getPassword(), request.getFullName(), request.getEmail(),
					request.getCode(), request.getDepartment());
			case STUDENT -> createStudent(request.getPassword(), request.getFullName(), request.getEmail(),
					request.getCode());
		};
	}

	@Transactional(readOnly = true)
	public Student findStudentByCode(String studentCode) {
		return studentRepository.findByStudentCode(studentCode)
				.orElseThrow(() -> new AccountNotFoundException("Không tìm thấy sinh viên có mã: " + studentCode));
	}

	@Transactional(readOnly = true)
	public Lecturer findLecturerByCode(String lecturerCode) {
		return lecturerRepository.findByLecturerCode(lecturerCode)
				.orElseThrow(() -> new AccountNotFoundException("Không tìm thấy giảng viên có mã: " + lecturerCode));
	}

	public Admin createAdmin(String username, String rawPassword, String fullName, String email) {
		validateUsername(username);
		Admin admin = new Admin();
		applyCommon(admin, username, rawPassword, fullName, email);
		return accountRepository.save(admin);
	}

	public Lecturer createLecturer(String rawPassword, String fullName, String email, String lecturerCode,
			String department) {
		validateLoginCode(lecturerCode, "Mã giảng viên");
		Lecturer lecturer = new Lecturer();
		lecturer.setLecturerCode(lecturerCode);
		lecturer.setDepartment(departmentService.resolve(department));
		applyCommon(lecturer, lecturerCode, rawPassword, fullName, email);
		return lecturerRepository.save(lecturer);
	}

	public Student createStudent(String rawPassword, String fullName, String email, String studentCode) {
		validateLoginCode(studentCode, "Mã sinh viên");
		Student student = new Student();
		student.setStudentCode(studentCode);
		applyCommon(student, studentCode, rawPassword, fullName, email);
		return studentRepository.save(student);
	}

	public Lecturer setDean(Long id, boolean dean) {
		Account account = findById(id);
		if (!(account instanceof Lecturer lecturer)) {
			throw new InvalidAccountException("Hệ thống chỉ cho phép gán vai trò Trưởng khoa cho tài khoản Giảng viên");
		}
		if (dean) {
			List<Lecturer> currentDeans = lecturerRepository
					.findByExtraRolesContainingAndIdNot(Role.DEAN, lecturer.getId());
			for (Lecturer oldDean : currentDeans) {
				oldDean.getExtraRoles().remove(Role.DEAN);
				lecturerRepository.save(oldDean);
			}
			lecturer.getExtraRoles().add(Role.DEAN);
		} else {
			lecturer.getExtraRoles().remove(Role.DEAN);
		}
		return lecturerRepository.save(lecturer);
	}

	public Lecturer assignDean(String lecturerCode, boolean dean) {
		return setDean(findLecturerByCode(lecturerCode).getId(), dean);
	}

	public void deleteAccount(Long id, String currentUsername) {
		Account account = findById(id);
		if (account.getUsername().equalsIgnoreCase(currentUsername)) {
			throw new InvalidAccountException("Không thể tự xóa tài khoản quản trị đang đăng nhập");
		}
		try {
			var notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(id);
			if (notifications != null && !notifications.isEmpty()) {
				notificationRepository.deleteAll(notifications);
			}
			accountRepository.delete(account);
			accountRepository.flush();
		} catch (Exception ex) {
			throw new InvalidAccountException(
					"Không thể xóa tài khoản này do đã có dữ liệu liên quan (đề tài, nhóm sinh viên hoặc hội đồng). Bạn có thể sử dụng chức năng Khóa tài khoản.");
		}
	}

	private void applyCommon(Account account, String username, String rawPassword, String fullName, String email) {
		account.setUsername(username);
		account.setPassword(passwordEncoder.encode(rawPassword));
		account.setFullName(fullName);
		account.setEmail(email);
	}

	private void validateUsername(String username) {
		if (username == null || username.isBlank()) {
			throw new InvalidAccountException("Tên đăng nhập không được để trống");
		}
		if (accountRepository.existsByUsername(username)) {
			throw new InvalidAccountException("Tên đăng nhập đã tồn tại: " + username);
		}
	}

	private void validateLoginCode(String code, String label) {
		if (code == null || code.isBlank()) {
			throw new InvalidAccountException(label + " không được để trống");
		}
		if (accountRepository.existsByUsername(code)) {
			throw new InvalidAccountException(label + " đã tồn tại: " + code);
		}
	}

	private void validatePasswords(AccountRequest request) {
		String password = request.getPassword();
		String confirmPassword = request.getConfirmPassword();
		if (password == null || password.isBlank()) {
			throw new InvalidAccountException("Mật khẩu không được để trống");
		}
		if (password.length() < 6) {
			throw new InvalidAccountException("Mật khẩu phải có ít nhất 6 ký tự");
		}
		if (confirmPassword == null || confirmPassword.isBlank()) {
			throw new InvalidAccountException("Vui lòng xác nhận mật khẩu");
		}
		if (!password.equals(confirmPassword)) {
			throw new InvalidAccountException("Mật khẩu xác nhận không khớp");
		}
	}
}