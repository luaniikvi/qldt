package project.qldt.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import project.qldt.model.TopicResult;

public interface TopicResultRepository extends JpaRepository<TopicResult, Long> {

	Optional<TopicResult> findByCouncilId(Long councilId);
}