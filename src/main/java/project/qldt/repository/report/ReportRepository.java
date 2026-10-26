package project.qldt.repository.report;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.qldt.model.report.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {

	Optional<Report> findByTeamId(Long teamId);

	List<Report> findByTeamIdIn(Collection<Long> teamIds);

	@Query("select r from Report r where r.team.period.id = :periodId order by r.submittedAt desc")
	List<Report> findByPeriodId(@Param("periodId") Long periodId);

	long countByTeamPeriodId(Long periodId);

	@Query("select distinct r from Report r join r.team t join t.topic tp join tp.advisors a "
			+ "where a.id = :lecturerId order by r.submittedAt desc")
	List<Report> findByAdvisorId(@Param("lecturerId") Long lecturerId);

	Optional<Report> findByTeamTopicId(Long topicId);

	@Query("select r from Report r where r.team.topic.id in :topicIds")
	List<Report> findByTopicIds(@Param("topicIds") Collection<Long> topicIds);
}
