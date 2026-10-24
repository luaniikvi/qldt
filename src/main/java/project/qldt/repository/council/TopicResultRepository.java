package project.qldt.repository.council;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.qldt.model.council.TopicResult;

public interface TopicResultRepository extends JpaRepository<TopicResult, Long> {

	Optional<TopicResult> findByCouncilId(Long councilId);

	@Query("select r from TopicResult r where r.council.id in :councilIds")
	List<TopicResult> findByCouncilIds(@Param("councilIds") Collection<Long> councilIds);

	@Query("select count(r) from TopicResult r where r.council.topic.period.id = :periodId and r.published = true")
	long countPublishedByPeriod(@Param("periodId") Long periodId);

	@Query("select coalesce(avg(r.averageScore), 0) from TopicResult r where r.council.topic.period.id = :periodId and r.published = true")
	double averagePublishedScoreByPeriod(@Param("periodId") Long periodId);

	@Query("select r.council.name, r.averageScore from TopicResult r where r.council.topic.period.id = :periodId order by r.council.name")
	List<Object[]> scoreRowsByPeriod(@Param("periodId") Long periodId);

	@Query("select r from TopicResult r where r.council.topic.period.id = :periodId order by r.id")
	List<TopicResult> findByPeriodId(@Param("periodId") Long periodId);

	@Query("select r from TopicResult r where r.published = true and exists "
			+ "(select t.id from Team t join t.members m where t.topic = r.council.topic and m.id = :studentId) order by r.id")
	List<TopicResult> findPublishedForStudent(@Param("studentId") Long studentId);

	@Query("select distinct r from TopicResult r join r.council.topic.advisors a "
			+ "where r.published = true and a.id = :lecturerId order by r.id")
	List<TopicResult> findPublishedOfAdvisor(@Param("lecturerId") Long lecturerId);
}
