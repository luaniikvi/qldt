package project.qldt.dto.api.request;

import jakarta.validation.constraints.NotBlank;

public record ReasonRequest(@NotBlank(message = "Lý do không được để trống") String reason) {
}
