package project.qldt.repository.topic;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.topic.InvitationStatus;
import project.qldt.model.topic.Topic;
import project.qldt.model.topic.TopicAvailability;
import project.qldt.model.topic.TopicStatus;

public interface TopicRepository extends JpaRepository<Topic, Long> {

	long countByStatusAndAvailability(TopicStatus status, TopicAvailability availability);

	long countByPeriodId(Long periodId);

	long countByPeriodIdAndStatus(Long periodId, TopicStatus status);

	long countByPeriodIdAndAvailability(Long periodId, TopicAvailability availability);

	@Query("select t.status, count(t) from Topic t where t.period.id = :periodId group by t.status")
	List<Object[]> countByStatusGrouped(@Param("periodId") Long periodId);

	@Modifying(flushAutomatically = true)
	@Query("update Topic t set t.availability = :newStatus where t.id = :topicId and t.availability = :expected")
	int updateAvailabilityIfCurrent(@Param("topicId") Long topicId, @Param("expected") TopicAvailability expected,
			@Param("newStatus") TopicAvailability newStatus);

	List<Topic> findByStatus(TopicStatus status);

	List<Topic> findByStatusAndAvailability(TopicStatus status, TopicAvailability availability);

	List<Topic> findByStatusAndPeriodIdOrderByIdAsc(TopicStatus status, Long periodId);

	List<Topic> findByStatusAndAvailabilityAndPeriodIdOrderByIdAsc(TopicStatus status, TopicAvailability availability,
			Long periodId);

	List<Topic> findByPeriodIdOrderByIdAsc(Long periodId);

	List<Topic> findByReviewerIdOrderByIdAsc(Long reviewerId);

	List<Topic> findByReviewerScoreIsNotNullOrderByIdAsc();

	boolean existsByPeriodId(Long periodId);

	// MySQL: với SELECT DISTINCT, ORDER BY phải dùng chính alias được select (p.id), không được t.period.id
	@Query("select distinct p from Topic t join t.period p where t.status = :status order by p.id")
	List<RegistrationPeriod> findPeriodsByStatus(@Param("status") TopicStatus status);

	@Query("select distinct t from Topic t join t.advisors a where a.id = :lecturerId order by t.id")
	List<Topic> findByAdvisorId(@Param("lecturerId") Long lecturerId);

	@Query("select distinct t from Topic t join t.advisors a where a.id = :lecturerId and t.availability = :availability order by t.id")
	List<Topic> findByAdvisorIdAndAvailability(@Param("lecturerId") Long lecturerId,
			@Param("availability") TopicAvailability availability);

	@Query("select distinct t from Topic t join t.invitation i where i.lecturer.id = :lecturerId and i.status = :status order by t.id")
	List<Topic> findByPendingInvitationOf(@Param("lecturerId") Long lecturerId,
			@Param("status") InvitationStatus status);

	@Query("select t from Topic t where t.availability = :availability and not exists (select c.id from Council c where c.topic = t) order by t.id")
	List<Topic> findWithoutCouncil(@Param("availability") TopicAvailability availability);

	@Query("select t from Topic t where t.availability = :availability and t.period.type in :types order by t.id")
	List<Topic> findForReviewer(@Param("availability") TopicAvailability availability,
			@Param("types") Collection<PeriodType> types);

	@Query("select distinct t from Topic t join t.advisors a where t.reviewerScore is not null and a.id = :lecturerId order by t.id")
	List<Topic> findReviewedOfAdvisor(@Param("lecturerId") Long lecturerId);

	@Query("select distinct t from Topic t join Council c on c.topic = t join c.members m "
			+ "where t.reviewerScore is not null and m.lecturer.id = :lecturerId and t.period.type = :type order by t.id")
	List<Topic> findReviewedForCouncilMember(@Param("lecturerId") Long lecturerId, @Param("type") PeriodType type);

	@Query("select distinct t from Topic t join Team tm on tm.topic = t join tm.members mem "
			+ "where t.reviewerScore is not null and mem.id = :studentId order by t.id")
	List<Topic> findReviewedOfStudent(@Param("studentId") Long studentId);

	@Query("select t from Topic t where t.reviewer.id = :reviewerId and t.reviewerScore is not null order by t.id asc")
	List<Topic> findReviewedByReviewerId(@Param("reviewerId") Long reviewerId);
}
