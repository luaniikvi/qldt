package project.qldt.repository.account;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.qldt.model.account.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

	Optional<Student> findByStudentCode(String studentCode);

	List<Student> findAllByOrderByStudentCodeAsc();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from Student s where s.id = :id")
	Optional<Student> findByIdForUpdate(@Param("id") Long id);
}