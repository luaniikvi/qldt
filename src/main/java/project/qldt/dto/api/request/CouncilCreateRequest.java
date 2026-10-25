package project.qldt.dto.api.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CouncilCreateRequest(
		@NotNull(message = "Đề tài không được để trống") Long topicId,
		String name,
		@NotEmpty(message = "Danh sách giảng viên không được để trống") @Size(min = 3, max = 5, message = "Hội đồng phải có từ 3 đến 5 giảng viên") @Valid List<CouncilMemberRequest> members) {
}
