package project.qldt.dto.api.request;

import jakarta.validation.constraints.NotNull;

public record LockRequest(@NotNull(message = "Trạng thái khóa không được để trống") Boolean locked) {
}
