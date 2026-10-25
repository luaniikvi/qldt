package project.qldt.repository.department;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import project.qldt.model.department.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

	Optional<Department> findByCode(String code);

	Optional<Department> findByNameIgnoreCase(String name);

	List<Department> findAllByOrderByNameAsc();
}
