package project.qldt.controller.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import project.qldt.dto.api.common.DepartmentResponse;
import project.qldt.service.department.DepartmentService;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentRestController {

	private final DepartmentService departmentService;

	public DepartmentRestController(DepartmentService departmentService) {
		this.departmentService = departmentService;
	}

	@GetMapping
	public List<DepartmentResponse> list() {
		return departmentService.findAll().stream().map(DepartmentResponse::of).toList();
	}
}
