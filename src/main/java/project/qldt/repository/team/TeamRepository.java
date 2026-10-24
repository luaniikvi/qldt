package project.qldt.repository.team;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.qldt.model.report.Report;
import project.qldt.model.team.Team;
import project.qldt.model.topic.InvitationStatus;

public interface TeamRepository extends JpaRepository<Team, Long> {

	Optional<Team> findByTopicId(Long topicId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from Team t where t.id = :id")
	Optional<Team> findByIdForUpdate(@Param("id") Long id);

	long countByPeriodId(Long periodId);

	@Query("select t from Team t where t.period.id = :periodId and t.topic is not null "
			+ "and t.topic.availability = project.qldt.model.topic.TopicAvailability.REGISTERED "
			+ "and not exists (select r.id from Report r where r.team = t) order by t.id")
	List<Team> findUnreportedByPeriod(@Param("periodId") Long periodId);

	List<Team> findByMembersId(Long studentId);

	List<Team> findByPeriodIdOrderByIdAsc(Long periodId);

	List<Team> findByTopicIdIn(Collection<Long> topicIds);

	boolean existsByPeriodId(Long periodId);

	boolean existsByPeriodIdAndMembersId(Long periodId, Long studentId);

	@Query("select distinct t from Team t left join t.members m left join t.invitations i "
			+ "where m.id = :studentId or (i.student.id = :studentId and i.status = :status) order by t.id")
	List<Team> findInvolvedOf(@Param("studentId") Long studentId, @Param("status") InvitationStatus status);

	@Query("select t from Team t where t.topic is not null order by t.id")
	List<Team> findAllWithTopic();
}
