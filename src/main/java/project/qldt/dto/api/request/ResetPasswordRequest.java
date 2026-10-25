package project.qldt.dto.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
		@NotBlank(message = "Mật khẩu mới không được để trống") @Size(min = 6, max = 50, message = "Mật khẩu phải từ 6 đến 50 ký tự") String newPassword) {
}
