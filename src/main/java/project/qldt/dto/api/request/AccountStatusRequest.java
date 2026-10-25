package project.qldt.dto.api.request;

import jakarta.validation.constraints.NotNull;

public record AccountStatusRequest(@NotNull(message = "Trạng thái không được để trống") Boolean enabled) {
}
