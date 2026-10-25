package project.qldt.dto.api.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ScoreRequest(
		@NotNull(message = "Điểm không được để trống") 
		@DecimalMin(value = "0.0", message = "Điểm tối thiểu là 0") 
		@DecimalMax(value = "10.0", message = "Điểm tối đa là 10") 
		Double score,
		
		String comment) {
}
