package project.qldt.dto.api.request;

import jakarta.validation.constraints.NotNull;

public record RegistrationRequest(@NotNull(message = "Đề tài không được để trống") Long topicId) {
}
