package project.qldt.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import project.qldt.model.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

	Optional<Team> findByTopicId(Long topicId);

	List<Team> findByMembersId(Long studentId);
}