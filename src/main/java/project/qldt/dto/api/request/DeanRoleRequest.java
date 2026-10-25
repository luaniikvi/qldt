package project.qldt.dto.api.request;

import jakarta.validation.constraints.NotNull;

public record DeanRoleRequest(@NotNull(message = "Vai trò không được để trống") Boolean dean) {
}
