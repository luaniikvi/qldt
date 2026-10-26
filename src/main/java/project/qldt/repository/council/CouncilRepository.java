package project.qldt.repository.council;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.qldt.model.council.Council;
import project.qldt.model.council.CouncilMember;

public interface CouncilRepository extends JpaRepository<Council, Long> {

	Optional<Council> findByTopicId(Long topicId);

	@Query("select distinct c from Council c join c.members m where m.lecturer.id = :lecturerId order by c.id desc")
	List<Council> findByMemberId(@Param("lecturerId") Long lecturerId);

	@Query("select distinct c from Council c left join fetch c.members order by c.id desc")
	List<Council> findAllWithMembers();

	@Query("select distinct c from Council c left join fetch c.members where c.id in "
			+ "(select m.council.id from CouncilMember m where m.lecturer.id = :lecturerId) order by c.id desc")
	List<Council> findWithMembersByMemberId(@Param("lecturerId") Long lecturerId);

	@Query("select count(c) from Council c where c.topic.period.id = :periodId")
	long countByPeriodId(@Param("periodId") Long periodId);

	@Query("select c from Council c where c.topic.period.id = :periodId order by c.id desc")
	List<Council> findByPeriodId(@Param("periodId") Long periodId);
}
