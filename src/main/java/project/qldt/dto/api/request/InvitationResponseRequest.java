package project.qldt.dto.api.request;

import jakarta.validation.constraints.NotNull;

public record InvitationResponseRequest(@NotNull(message = "Lựa chọn phản hồi không được để trống") Boolean accept,
		String reason) {
}
