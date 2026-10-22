package project.qldt.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import project.qldt.model.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {

	Optional<Report> findByTeamId(Long teamId);
}