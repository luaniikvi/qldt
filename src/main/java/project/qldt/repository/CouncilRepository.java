package project.qldt.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import project.qldt.model.Council;

public interface CouncilRepository extends JpaRepository<Council, Long> {

	Optional<Council> findByTopicId(Long topicId);
}