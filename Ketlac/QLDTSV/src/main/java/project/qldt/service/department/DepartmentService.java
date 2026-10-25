package project.qldt.service.department;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.exception.InvalidAccountException;
import project.qldt.model.department.Department;
import project.qldt.repository.department.DepartmentRepository;

@Service
@Transactional(readOnly = true)
public class DepartmentService {

	private final DepartmentRepository departmentRepository;

	public DepartmentService(DepartmentRepository departmentRepository) {
		this.departmentRepository = departmentRepository;
	}

	public List<Department> findAll() {
		return departmentRepository.findAllByOrderByNameAsc();
	}

	public Optional<Department> find(String codeOrName) {
		if (codeOrName == null || codeOrName.isBlank()) {
			return Optional.empty();
		}
		String value = codeOrName.trim();
		Optional<Department> byCode = departmentRepository.findByCode(value);
		return byCode.isPresent() ? byCode : departmentRepository.findByNameIgnoreCase(value);
	}

	public Department resolve(String codeOrName) {
		return find(codeOrName)
				.orElseThrow(() -> new InvalidAccountException("Bộ môn không tồn tại: " + codeOrName));
	}

	@Transactional
	public Department createIfMissing(String code, String name) {
		return departmentRepository.findByCode(code)
				.orElseGet(() -> departmentRepository.save(new Department(code, name)));
	}
}
