package project.qldt.controller.api.v1;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import project.qldt.dto.api.request.LockRequest;
import project.qldt.dto.api.request.ReasonRequest;
import project.qldt.dto.api.response.BulkActionResponse;
import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.dto.period.RegistrationPeriodResponse;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.service.notification.ReminderService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.topic.TopicService;

@RestController
@RequestMapping("/api/v1/periods")
public class RegistrationPeriodRestController {

	private final RegistrationPeriodService periodService;
	private final TopicService topicService;
	private final ReminderService reminderService;

	public RegistrationPeriodRestController(RegistrationPeriodService periodService, TopicService topicService,
			ReminderService reminderService) {
		this.periodService = periodService;
		this.topicService = topicService;
		this.reminderService = reminderService;
	}

	@GetMapping
	public ResponseEntity<List<RegistrationPeriodResponse>> list() {
		return ResponseEntity.ok(periodService.findAll().stream().map(RegistrationPeriodResponse::of).toList());
	}

	@GetMapping("/active")
	public ResponseEntity<RegistrationPeriodResponse> active() {
		RegistrationPeriod period = periodService.findActiveOrFirst();
		return period == null
				? ResponseEntity.noContent().build()
				: ResponseEntity.ok(RegistrationPeriodResponse.of(period));
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
	@PatchMapping("/{id}/lock")
	public ResponseEntity<RegistrationPeriodResponse> lock(@PathVariable Long id,
			@Valid @RequestBody LockRequest request) {
		return ResponseEntity.ok(RegistrationPeriodResponse.of(
				periodService.setRegistrationLocked(id, request.locked())));
	}

	@PreAuthorize("hasRole('DEAN')")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		periodService.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	@PreAuthorize("hasRole('DEAN')")
	@PostMapping("/{periodId}/topics/publish")
	public BulkActionResponse publish(@PathVariable Long periodId) {
		return new BulkActionResponse(topicService.publish(periodId));
	}

	@PreAuthorize("hasRole('DEAN')")
	@PostMapping("/{periodId}/topics/reject")
	public BulkActionResponse rejectPeriod(@PathVariable Long periodId, @Valid @RequestBody ReasonRequest request) {
		return new BulkActionResponse(topicService.rejectPeriod(periodId, request.reason()));
	}

	@PreAuthorize("hasRole('DEAN')")
	@PostMapping("/{periodId}/reminders/report-deadline")
	public BulkActionResponse remindReportDeadline(@PathVariable Long periodId) {
		return new BulkActionResponse(reminderService.remindReportDeadline(periodId));
	}

	@PreAuthorize("hasRole('DEAN')")
	@PostMapping("/{periodId}/reminders/council-date")
	public BulkActionResponse remindCouncilDate(@PathVariable Long periodId) {
		return new BulkActionResponse(reminderService.remindCouncilDate(periodId));
	}
}
