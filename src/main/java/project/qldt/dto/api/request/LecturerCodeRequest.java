package project.qldt.dto.api.request;

import jakarta.validation.constraints.NotBlank;

public record LecturerCodeRequest(@NotBlank(message = "Mã giảng viên không được để trống") String lecturerCode) {
}
