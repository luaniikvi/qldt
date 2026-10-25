package project.qldt.dto.api.request;

import jakarta.validation.constraints.NotNull;

public record ApprovalRequest(@NotNull(message = "Trạng thái duyệt không được để trống") Boolean approved,
		String reason) {
}
