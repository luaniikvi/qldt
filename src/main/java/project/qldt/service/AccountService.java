package project.qldt.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.AccountRequest;
import project.qldt.exception.AccountNotFoundException;
import project.qldt.exception.InvalidAccountException;
import project.qldt.model.Account;
import project.qldt.model.Admin;
import project.qldt.model.Lecturer;
import project.qldt.model.Role;
import project.qldt.model.Student;
import project.qldt.repository.AccountRepository;
import project.qldt.repository.LecturerRepository;
import project.qldt.repository.StudentRepository;

@Service
@Transactional
public class AccountService {

	private final AccountRepository accountRepository;
	private final LecturerRepository lecturerRepository;
	private final StudentRepository studentRepository;
	private final PasswordEncoder passwordEncoder;

	public AccountService(AccountRepository accountRepository, LecturerRepository lecturerRepository,
			StudentRepository studentRepository, PasswordEncoder passwordEncoder) {
		this.accountRepository = accountRepository;
		this.lecturerRepository = lecturerRepository;
		this.studentRepository = studentRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public List<Account> findAll() {
		return accountRepository.findAll();
	}

	@Transactional(readOnly = true)
	public List<Student> findAllStudents() {
		return studentRepository.findAllByOrderByStudentCodeAsc();
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

	public Account createAccount(AccountRequest request) {
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
		validateDepartment(department);
		Lecturer lecturer = new Lecturer();
		lecturer.setLecturerCode(lecturerCode);
		lecturer.setDepartment(department);
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
			throw new InvalidAccountException("Chỉ giảng viên mới được gán vai trò trưởng khoa");
		}
		if (dean) {
			lecturer.getExtraRoles().add(Role.DEAN);
		} else {
			lecturer.getExtraRoles().remove(Role.DEAN);
		}
		return lecturerRepository.save(lecturer);
	}

	public Lecturer assignDean(String lecturerCode, boolean dean) {
		return setDean(findLecturerByCode(lecturerCode).getId(), dean);
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

	private void validateDepartment(String department) {
		if (department == null || department.isBlank()) {
			throw new InvalidAccountException("Bộ môn của giảng viên không được để trống");
		}
	}
}