package project.qldt.controller.api.v1;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import project.qldt.dto.api.request.ApprovalRequest;
import project.qldt.dto.api.request.InvitationResponseRequest;
import project.qldt.dto.api.request.LecturerCodeRequest;
import project.qldt.dto.api.response.TopicResponse;
import project.qldt.dto.topic.TopicRequest;
import project.qldt.model.topic.Topic;
import project.qldt.model.topic.TopicAvailability;
import project.qldt.model.topic.TopicStatus;
import project.qldt.service.account.AccountService;
import project.qldt.service.topic.TopicService;

@RestController
@RequestMapping("/api/v1/topics")
public class TopicRestController extends ApiControllerSupport {

	private final TopicService topicService;

	public TopicRestController(TopicService topicService, AccountService accountService) {
		super(accountService);
		this.topicService = topicService;
	}

	@GetMapping
	public List<TopicResponse> list(@RequestParam(required = false) Long periodId,
			@RequestParam(required = false) TopicStatus status,
			@RequestParam(required = false) TopicAvailability availability,
			@RequestParam(required = false) Long advisorId,
			@RequestParam(required = false) Long reviewerId) {
		return topicService.search(periodId, status, availability, advisorId, reviewerId).stream()
				.map(TopicResponse::of).toList();
	}

	@GetMapping("/{id}")
	public TopicResponse getOne(@PathVariable Long id) {
		return TopicResponse.of(topicService.findById(id));
	}

	@PreAuthorize("hasRole('STUDENT')")
	@GetMapping("/available")
	public List<TopicResponse> available(@RequestParam Long periodId) {
		return topicService.findAvailablePublishedFor(periodId).stream().map(TopicResponse::of).toList();
	}

	@PreAuthorize("hasRole('DEAN')")
	@GetMapping("/pending")
	public List<TopicResponse> pending() {
		return topicService.findByStatus(TopicStatus.PENDING).stream().map(TopicResponse::of).toList();
	}

	@PreAuthorize("hasRole('LECTURER')")
	@GetMapping("/mine")
	public List<TopicResponse> mine(Authentication authentication) {
		return topicService.findByLecturer(currentLecturer(authentication)).stream().map(TopicResponse::of)
				.toList();
	}

	@PreAuthorize("hasRole('LECTURER')")
	@PostMapping
	public ResponseEntity<TopicResponse> create(@Valid @RequestBody TopicRequest request,
			Authentication authentication) {
		Topic topic = topicService.create(request, currentLecturer(authentication));
		return ResponseEntity.created(URI.create("/api/v1/topics/" + topic.getId()))
				.body(TopicResponse.of(topic));
	}

	@PreAuthorize("hasRole('LECTURER')")
	@PutMapping("/{id}")
	public TopicResponse update(@PathVariable Long id, @Valid @RequestBody TopicRequest request,
			Authentication authentication) {
		return TopicResponse.of(topicService.update(id, request, currentLecturer(authentication)));
	}

	@PreAuthorize("hasRole('LECTURER')")
	@PostMapping("/{id}/advisors/invitations")
	public TopicResponse inviteAdvisor(@PathVariable Long id, @Valid @RequestBody LecturerCodeRequest request,
			Authentication authentication) {
		return TopicResponse.of(topicService.inviteAdvisor(id, request.lecturerCode(),
				currentLecturer(authentication)));
	}

	@PreAuthorize("hasRole('LECTURER')")
	@PostMapping("/{id}/advisors/invitations/response")
	public TopicResponse respondInvitation(@PathVariable Long id,
			@Valid @RequestBody InvitationResponseRequest request, Authentication authentication) {
		return TopicResponse.of(topicService.respondInvitation(id, currentLecturer(authentication),
				request.accept(), request.reason()));
	}

	@PreAuthorize("hasRole('LECTURER')")
	@DeleteMapping("/{id}/advisors/invitations")
	public TopicResponse cancelAdvisorInvitation(@PathVariable Long id, Authentication authentication) {
		return TopicResponse.of(topicService.cancelAdvisorInvitation(id, currentLecturer(authentication)));
	}

	@PreAuthorize("hasRole('LECTURER')")
	@DeleteMapping("/{id}/advisors/{lecturerCode}")
	public TopicResponse removeAdvisor(@PathVariable Long id, @PathVariable String lecturerCode,
			Authentication authentication) {
		return TopicResponse.of(topicService.removeCoAdvisor(id, lecturerCode, currentLecturer(authentication)));
	}

	@PreAuthorize("hasRole('DEAN')")
	@PatchMapping("/{id}/approval")
	public TopicResponse approval(@PathVariable Long id, @Valid @RequestBody ApprovalRequest request) {
		return TopicResponse.of(topicService.handleApproval(id, request.approved(), request.reason()));
	}
}
