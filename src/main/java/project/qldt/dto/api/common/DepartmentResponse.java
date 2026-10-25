package project.qldt.dto.api.common;

import project.qldt.model.department.Department;

public record DepartmentResponse(Long id, String code, String name) {

	public static DepartmentResponse of(Department department) {
		return department == null
				? null
				: new DepartmentResponse(department.getId(), department.getCode(), department.getName());
	}
}
