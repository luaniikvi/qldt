package project.qldt.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import project.qldt.model.Topic;
import project.qldt.model.TopicAvailability;
import project.qldt.model.TopicStatus;

public interface TopicRepository extends JpaRepository<Topic, Long> {

	List<Topic> findByStatus(TopicStatus status);

	List<Topic> findByStatusAndAvailability(TopicStatus status, TopicAvailability availability);
}