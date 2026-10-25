package project.qldt.dto.topic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import project.qldt.model.topic.Topic;

@Getter
@Setter
public class TopicRequest {

	@NotBlank(message = "Tên đề tài không được để trống")
	@Size(max = 200, message = "Tên đề tài tối đa 200 ký tự")
	private String name;

	@NotBlank(message = "Nội dung đề tài không được để trống")
	@Size(max = 2000, message = "Nội dung đề tài tối đa 2000 ký tự")
	private String description;

	@NotNull(message = "Đợt đăng ký không được để trống")
	private Long periodId;

	public static TopicRequest from(Topic topic) {
		TopicRequest request = new TopicRequest();
		request.setName(topic.getName());
		request.setDescription(topic.getDescription());
		request.setPeriodId(topic.getPeriod().getId());
		return request;
	}
}
