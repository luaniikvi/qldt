package project.qldt.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import project.qldt.exception.AccountNotFoundException;
import project.qldt.exception.InvalidAccountException;
import project.qldt.model.Account;
import project.qldt.model.Admin;
import project.qldt.model.Lecturer;
import project.qldt.model.Role;
import project.qldt.model.Student;

@Service
public class AccountService {

	private final List<Account> store = new ArrayList<>();
	private final AtomicLong sequence = new AtomicLong(1);
	private final PasswordEncoder passwordEncoder;

	public AccountService(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	public List<Account> findAll() {
		return new ArrayList<>(store);
	}

	public List<Student> findAllStudents() {
		List<Student> students = new ArrayList<>();
		for (Account account : store) {
			if (account instanceof Student student) {
				students.add(student);
			}
		}
		return students;
	}

	public List<Lecturer> findAllLecturers() {
		List<Lecturer> lecturers = new ArrayList<>();
		for (Account account : store) {
			if (account instanceof Lecturer lecturer) {
				lecturers.add(lecturer);
			}
		}
		return lecturers;
	}

	public Account findByUsername(String username) {
		return store.stream()
				.filter(account -> account.getUsername().equals(username))
				.findFirst()
				.orElseThrow(() -> new AccountNotFoundException("Không tìm thấy tài khoản: " + username));
	}

	public Student findStudentByCode(String studentCode) {
		return findAllStudents().stream()
				.filter(student -> student.getStudentCode().equals(studentCode))
				.findFirst()
				.orElseThrow(() -> new AccountNotFoundException("Không tìm thấy sinh viên có mã: " + studentCode));
	}

	public Lecturer findLecturerByCode(String lecturerCode) {
		return findAllLecturers().stream()
				.filter(lecturer -> lecturer.getLecturerCode().equals(lecturerCode))
				.findFirst()
				.orElseThrow(() -> new AccountNotFoundException("Không tìm thấy giảng viên có mã: " + lecturerCode));
	}

	public Admin createAdmin(String username, String rawPassword, String fullName, String email) {
		validateUsername(username);
		Admin admin = new Admin();
		admin.setId(sequence.getAndIncrement());
		applyCommon(admin, username, rawPassword, fullName, email);
		store.add(admin);
		return admin;
	}

	public Lecturer createLecturer(String username, String rawPassword, String fullName, String email, String lecturerCode) {
		validateUsername(username);
		validateLecturerCode(lecturerCode);
		Lecturer lecturer = new Lecturer();
		lecturer.setId(sequence.getAndIncrement());
		lecturer.setLecturerCode(lecturerCode);
		applyCommon(lecturer, username, rawPassword, fullName, email);
		store.add(lecturer);
		return lecturer;
	}

	public Student createStudent(String username, String rawPassword, String fullName, String email, String studentCode) {
		validateUsername(username);
		validateStudentCode(studentCode);
		Student student = new Student();
		student.setId(sequence.getAndIncrement());
		student.setStudentCode(studentCode);
		applyCommon(student, username, rawPassword, fullName, email);
		store.add(student);
		return student;
	}

	public Lecturer assignDean(String lecturerCode, boolean dean) {
		Lecturer lecturer = findLecturerByCode(lecturerCode);
		if (dean) {
			lecturer.getExtraRoles().add(Role.DEAN);
		} else {
			lecturer.getExtraRoles().remove(Role.DEAN);
		}
		return lecturer;
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
		boolean exists = store.stream().anyMatch(account -> account.getUsername().equals(username));
		if (exists) {
			throw new InvalidAccountException("Tên đăng nhập đã tồn tại: " + username);
		}
	}

	private void validateLecturerCode(String lecturerCode) {
		if (lecturerCode == null || lecturerCode.isBlank()) {
			throw new InvalidAccountException("Mã giảng viên không được để trống");
		}
		boolean exists = findAllLecturers().stream()
				.anyMatch(lecturer -> lecturer.getLecturerCode().equals(lecturerCode));
		if (exists) {
			throw new InvalidAccountException("Mã giảng viên đã tồn tại: " + lecturerCode);
		}
	}

	private void validateStudentCode(String studentCode) {
		if (studentCode == null || studentCode.isBlank()) {
			throw new InvalidAccountException("Mã sinh viên không được để trống");
		}
		boolean exists = findAllStudents().stream()
				.anyMatch(student -> student.getStudentCode().equals(studentCode));
		if (exists) {
			throw new InvalidAccountException("Mã sinh viên đã tồn tại: " + studentCode);
		}
	}
}
