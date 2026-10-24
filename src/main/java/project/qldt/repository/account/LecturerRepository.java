package project.qldt.repository.account;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import project.qldt.model.account.Lecturer;
import project.qldt.model.account.Role;

public interface LecturerRepository extends JpaRepository<Lecturer, Long> {

	Optional<Lecturer> findByLecturerCode(String lecturerCode);

	List<Lecturer> findAllByOrderByLecturerCodeAsc();

	List<Lecturer> findByExtraRolesContainingAndIdNot(Role role, Long id);
}