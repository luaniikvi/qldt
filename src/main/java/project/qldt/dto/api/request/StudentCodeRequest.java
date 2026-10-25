package project.qldt.dto.api.request;

import jakarta.validation.constraints.NotBlank;

public record StudentCodeRequest(@NotBlank(message = "Mã sinh viên không được để trống") String studentCode) {
}
