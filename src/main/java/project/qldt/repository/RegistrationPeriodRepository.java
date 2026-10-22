package project.qldt.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import project.qldt.model.RegistrationPeriod;

public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, Long> {
}