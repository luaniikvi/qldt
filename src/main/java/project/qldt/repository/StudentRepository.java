package project.qldt.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import project.qldt.model.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

	Optional<Student> findByStudentCode(String studentCode);

	List<Student> findAllByOrderByStudentCodeAsc();
}