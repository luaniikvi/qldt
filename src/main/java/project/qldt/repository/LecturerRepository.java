package project.qldt.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import project.qldt.model.Lecturer;

public interface LecturerRepository extends JpaRepository<Lecturer, Long> {

	Optional<Lecturer> findByLecturerCode(String lecturerCode);

	List<Lecturer> findAllByOrderByLecturerCodeAsc();
}