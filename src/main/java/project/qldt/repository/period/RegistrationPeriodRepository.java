package project.qldt.repository.period;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import project.qldt.model.period.RegistrationPeriod;

public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, Long> {

	List<RegistrationPeriod> findAllByOrderByIdDesc();

	Optional<RegistrationPeriod> findFirstByOrderByIdAsc();

	Optional<RegistrationPeriod> findFirstByStudentRegisterEndAfterOrderByStudentRegisterEndAsc(LocalDateTime now);
}