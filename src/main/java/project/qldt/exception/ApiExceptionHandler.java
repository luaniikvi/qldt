package project.qldt.exception;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import project.qldt.dto.api.ApiError;
import project.qldt.dto.api.ApiFieldError;

@RestControllerAdvice(basePackages = "project.qldt.controller.api")
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler({AccountNotFoundException.class, AnnouncementNotFoundException.class,
			CouncilNotFoundException.class, NotificationNotFoundException.class,
			RegistrationPeriodNotFoundException.class, ReportNotFoundException.class, TeamNotFoundException.class,
			TopicNotFoundException.class})
	public ResponseEntity<ApiError> handleNotFound(RuntimeException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidRegistrationPeriodException.class)
	public ResponseEntity<ApiError> handleInvalidPeriod(InvalidRegistrationPeriodException ex,
			HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidAccountException.class)
	public ResponseEntity<ApiError> handleInvalidAccount(InvalidAccountException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "INVALID_ACCOUNT", ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidAnnouncementException.class)
	public ResponseEntity<ApiError> handleInvalidAnnouncement(InvalidAnnouncementException ex,
			HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "INVALID_ANNOUNCEMENT", ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidCouncilException.class)
	public ResponseEntity<ApiError> handleInvalidCouncil(InvalidCouncilException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "INVALID_COUNCIL", ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidNotificationException.class)
	public ResponseEntity<ApiError> handleInvalidNotification(InvalidNotificationException ex,
			HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "INVALID_NOTIFICATION", ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidRegistrationException.class)
	public ResponseEntity<ApiError> handleInvalidRegistration(InvalidRegistrationException ex,
			HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "INVALID_REGISTRATION", ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidReportException.class)
	public ResponseEntity<ApiError> handleInvalidReport(InvalidReportException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "INVALID_REPORT", ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidTeamException.class)
	public ResponseEntity<ApiError> handleInvalidTeam(InvalidTeamException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "INVALID_TEAM", ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidTopicException.class)
	public ResponseEntity<ApiError> handleInvalidTopic(InvalidTopicException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "INVALID_TOPIC", ex.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<ApiFieldError> fieldErrors = new ArrayList<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> fieldErrors.add(new ApiFieldError(error.getField(), error.getDefaultMessage())));
		ex.getBindingResult().getGlobalErrors().forEach(
				error -> fieldErrors.add(new ApiFieldError(error.getObjectName(), error.getDefaultMessage())));
		return ResponseEntity.badRequest().body(ApiError.of(400, "VALIDATION_ERROR", "Dữ liệu không hợp lệ",
				request.getRequestURI(), fieldErrors));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
		List<ApiFieldError> fieldErrors = ex.getConstraintViolations().stream()
				.map(violation -> new ApiFieldError(violation.getPropertyPath().toString(), violation.getMessage()))
				.toList();
		return ResponseEntity.badRequest().body(ApiError.of(400, "VALIDATION_ERROR", "Dữ liệu không hợp lệ",
				request.getRequestURI(), fieldErrors));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "Nội dung JSON không hợp lệ", request);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
			HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "Tham số không hợp lệ: " + ex.getName(), request);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex,
			HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
				"Thiếu tham số bắt buộc: " + ex.getParameterName(), request);
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<ApiError> handleMissingPart(MissingServletRequestPartException ex,
			HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
				"Thiếu phần dữ liệu bắt buộc: " + ex.getRequestPartName(), request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage(), request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
			HttpServletRequest request) {
		return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", ex.getMessage(), request);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiError> handleMediaType(HttpMediaTypeNotSupportedException ex,
			HttpServletRequest request) {
		return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", ex.getMessage(), request);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiError> handleMaxUpload(MaxUploadSizeExceededException ex, HttpServletRequest request) {
		return build(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Tệp tải lên vượt quá dung lượng cho phép",
				request);
	} 

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Lỗi không xác định khi xử lý API {}", request.getRequestURI(), ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Đã xảy ra lỗi hệ thống", request);
	}

	private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
			HttpServletRequest request) {
		return ResponseEntity.status(status)
				.body(ApiError.of(status.value(), code, message, request.getRequestURI()));
	}
}
