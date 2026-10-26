package project.qldt.repository.account;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import project.qldt.model.account.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {

	Optional<Account> findByUsername(String username);

	boolean existsByUsername(String username);

	List<Account> findAllByOrderByIdDesc();

	@Query("""
			select a from Account a
			where (:type is null or type(a) = :type)
			  and (:enabled is null or a.enabled = :enabled)
			  and (:q is null or (lower(a.fullName) like lower(concat('%', :q, '%'))
			       or lower(a.username) like lower(concat('%', :q, '%'))))
			order by a.id desc
			""")
	List<Account> search(@Param("type") Class<? extends Account> type, @Param("enabled") Boolean enabled,
			@Param("q") String q);
}
