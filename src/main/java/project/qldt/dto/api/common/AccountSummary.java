package project.qldt.dto.api.common;

import java.util.Set;

import project.qldt.model.account.Account;
import project.qldt.model.account.Lecturer;
import project.qldt.model.account.Role;
import project.qldt.model.account.Student;
import project.qldt.model.department.Department;

public record AccountSummary(Long id, String username, String fullName, String code, Set<Role> roles,
		String departmentCode, String departmentName) {

	public static AccountSummary of(Account account) {
		if (account == null) {
			return null;
		}
		String code = account instanceof Lecturer lecturer
				? lecturer.getLecturerCode()
				: account instanceof Student student ? student.getStudentCode() : account.getUsername();
		Department department = account instanceof Lecturer lecturer ? lecturer.getDepartment() : null;
		return new AccountSummary(account.getId(), account.getUsername(), account.getFullName(), code,
				account.getRoles(), department == null ? null : department.getCode(),
				department == null ? null : department.getName());
	}
}
