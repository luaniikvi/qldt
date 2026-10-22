package project.qldt.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamRequest {

	@NotNull(message = "Đợt đăng ký không được để trống")
	private Long periodId;
}
