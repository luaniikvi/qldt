package project.qldt.dto.api.response;

import java.util.Set;

import project.qldt.dto.api.common.DepartmentResponse;
import project.qldt.model.account.Account;
import project.qldt.model.account.Admin;
import project.qldt.model.account.Lecturer;
import project.qldt.model.account.Role;
import project.qldt.model.account.Student;

public record AccountResponse(Long id, String username, String fullName, String email, boolean enabled,
		String type, Set<Role> roles, String lecturerCode, String studentCode, DepartmentResponse department) {

	public static AccountResponse of(Account account) {
		String type = account instanceof Admin
				? "ADMIN"
				: account instanceof Lecturer ? "LECTURER" : account instanceof Student ? "STUDENT" : "UNKNOWN";
		Lecturer lecturer = account instanceof Lecturer value ? value : null;
		Student student = account instanceof Student value ? value : null;
		return new AccountResponse(account.getId(), account.getUsername(), account.getFullName(), account.getEmail(),
				account.isEnabled(), type, account.getRoles(),
				lecturer == null ? null : lecturer.getLecturerCode(),
				student == null ? null : student.getStudentCode(),
				lecturer == null ? null : DepartmentResponse.of(lecturer.getDepartment()));
	}
}
