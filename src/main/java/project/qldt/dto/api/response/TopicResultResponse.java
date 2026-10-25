package project.qldt.dto.api.response;

import java.time.LocalDateTime;

import project.qldt.model.council.TopicResult;

public record TopicResultResponse(Long id, Long councilId, Long topicId, String topicName, double averageScore,
		Double finalScore, boolean published, LocalDateTime publishedAt) {

	public static TopicResultResponse of(TopicResult result) {
		return new TopicResultResponse(result.getId(), result.getCouncil().getId(),
				result.getCouncil().getTopic().getId(), result.getCouncil().getTopic().getName(),
				result.getAverageScore(), result.getFinalScore(), result.isPublished(), result.getPublishedAt());
	}
}
