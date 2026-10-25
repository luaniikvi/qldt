package project.qldt.dto.api.common;

import project.qldt.model.topic.Topic;
import project.qldt.model.topic.TopicAvailability;
import project.qldt.model.topic.TopicStatus;

public record TopicSummary(Long id, String name, TopicStatus status, TopicAvailability availability, Long periodId) {

	public static TopicSummary of(Topic topic) {
		return topic == null
				? null
				: new TopicSummary(topic.getId(), topic.getName(), topic.getStatus(), topic.getAvailability(),
						topic.getPeriod() == null ? null : topic.getPeriod().getId());
	}
}
