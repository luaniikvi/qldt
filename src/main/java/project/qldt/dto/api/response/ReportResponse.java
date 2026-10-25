package project.qldt.dto.api.response;

import java.time.LocalDateTime;

import project.qldt.model.report.Report;

public record ReportResponse(Long id, Long teamId, Long topicId, String originalFileName, long size, int version,
		LocalDateTime submittedAt, String downloadUrl) {

	public static ReportResponse of(Report report) {
		Long topicId = report.getTeam() != null && report.getTeam().getTopic() != null
				? report.getTeam().getTopic().getId()
				: null;
		return new ReportResponse(report.getId(), report.getTeam() == null ? null : report.getTeam().getId(), topicId,
				report.getOriginalFileName(), report.getSize(), report.getVersion(), report.getSubmittedAt(),
				"/api/v1/reports/" + report.getId() + "/file");
	}
}
