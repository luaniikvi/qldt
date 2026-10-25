package project.qldt.repository.announcement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import project.qldt.model.announcement.Announcement;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

	List<Announcement> findAllByOrderByCreatedAtAsc();

	List<Announcement> findTop5ByOrderByCreatedAtDesc();
}
