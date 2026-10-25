package project.qldt.dto.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnouncementRequest(
		@NotBlank(message = "Tiêu đề thông báo không được để trống") @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự") String title,
		@NotBlank(message = "Nội dung thông báo không được để trống") @Size(max = 4000, message = "Nội dung tối đa 4000 ký tự") String content) {
}
