package project.qldt.dto.api;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(LocalDateTime timestamp, int status, String error, String message, String path,
		List<ApiFieldError> fieldErrors) {

	public static ApiError of(int status, String error, String message, String path) {
		return new ApiError(LocalDateTime.now(), status, error, message, path, null);
	}

	public static ApiError of(int status, String error, String message, String path, List<ApiFieldError> fieldErrors) {
		return new ApiError(LocalDateTime.now(), status, error, message, path, fieldErrors);
	}
}
