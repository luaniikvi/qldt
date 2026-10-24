package project.qldt.controller.api.v1;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.dto.period.RegistrationPeriodResponse;
import project.qldt.exception.InvalidRegistrationPeriodException;
import project.qldt.exception.RegistrationPeriodNotFoundException;
import project.qldt.service.period.RegistrationPeriodService;

@RestController
@RequestMapping("/api/v1/periods")
public class RegistrationPeriodRestController {

	private final RegistrationPeriodService periodService;

	public RegistrationPeriodRestController(RegistrationPeriodService periodService) {
		this.periodService = periodService;
	}

	@GetMapping
	public ResponseEntity<List<RegistrationPeriodResponse>> list() {
		return ResponseEntity.ok(periodService.findAll().stream().map(RegistrationPeriodResponse::of).toList());
	}

	@GetMapping("/{id}")
	public ResponseEntity<RegistrationPeriodResponse> getOne(@PathVariable Long id) {
		return ResponseEntity.ok(RegistrationPeriodResponse.of(periodService.findById(id)));
	}

	@PreAuthorize("hasRole('DEAN')")
	@PostMapping
	public ResponseEntity<RegistrationPeriodResponse> create(@Valid @RequestBody RegistrationPeriodRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(RegistrationPeriodResponse.of(periodService.create(request)));
	}

	@PreAuthorize("hasRole('DEAN')")
	@PutMapping("/{id}")
	public ResponseEntity<RegistrationPeriodResponse> update(@PathVariable Long id,
			@Valid @RequestBody RegistrationPeriodRequest request) {
		return ResponseEntity.ok(RegistrationPeriodResponse.of(periodService.update(id, request)));
	}

	@PreAuthorize("hasRole('DEAN')")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		periodService.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(RegistrationPeriodNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNotFound(RegistrationPeriodNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(Map.of("error", "NOT_FOUND", "message", ex.getMessage()));
	}

	@ExceptionHandler(InvalidRegistrationPeriodException.class)
	public ResponseEntity<Map<String, Object>> handleInvalid(InvalidRegistrationPeriodException ex) {
		return ResponseEntity.badRequest()
				.body(Map.of("error", "INVALID_PERIOD", "message", ex.getMessage()));
	}
}
