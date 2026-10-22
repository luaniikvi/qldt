package project.qldt.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import project.qldt.model.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {

	Optional<Account> findByUsername(String username);

	boolean existsByUsername(String username);
}