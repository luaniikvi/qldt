package project.qldt.controller.api.v1;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import project.qldt.dto.api.request.CouncilCreateRequest;
import project.qldt.dto.api.request.ScoreRequest;
import project.qldt.dto.api.response.CouncilResponse;
import project.qldt.dto.api.response.TopicResponse;
import project.qldt.dto.api.response.TopicResultResponse;
import project.qldt.dto.council.CouncilView;
import project.qldt.model.council.Council;
import project.qldt.service.account.AccountService;
import project.qldt.service.council.CouncilService;

@RestController
@RequestMapping("/api/v1/councils")
public class CouncilRestController extends ApiControllerSupport {

	private final CouncilService councilService;

	public CouncilRestController(CouncilService councilService, AccountService accountService) {
		super(accountService);
		this.councilService = councilService;
	}

	@PreAuthorize("hasRole('DEAN')")
	@GetMapping
	public List<CouncilResponse> list(@RequestParam(required = false) Long periodId) {
		List<CouncilView> views = periodId == null
				? councilService.findAllViews()
				: councilService.findViewsByPeriod(periodId);
		return views.stream().map(CouncilResponse::of).toList();
	}

	@PreAuthorize("hasRole('LECTURER')")
	@GetMapping("/mine")
	public List<CouncilResponse> mine(Authentication authentication) {
		return councilService.findViewsFor(currentLecturer(authentication)).stream().map(CouncilResponse::of)
				.toList();
	}

	@PreAuthorize("hasAnyRole('DEAN','LECTURER')")
	@GetMapping("/{id}")
	public CouncilResponse getOne(@PathVariable Long id) {
		return CouncilResponse.of(councilService.findView(id));
	}

	@PreAuthorize("hasAnyRole('DEAN','LECTURER')")
	@GetMapping("/by-topic/{topicId}")
	public CouncilResponse byTopic(@PathVariable Long topicId) {
		return CouncilResponse.of(councilService.findViewByTopicId(topicId));
	}

	@PreAuthorize("hasRole('DEAN')")
	@GetMapping("/ready-topics")
	public List<TopicResponse> readyTopics() {
		return councilService.findTopicsReadyForCouncil().stream().map(TopicResponse::of).toList();
	}

	@PreAuthorize("hasRole('DEAN')")
	@PostMapping
	public ResponseEntity<CouncilResponse> create(@Valid @RequestBody CouncilCreateRequest request) {
		List<String> codes = request.members().stream().map(member -> member.lecturerCode().trim()).toList();
		List<String> roles = request.members().stream().map(member -> member.role()).toList();
		Council council = councilService.createFromInput(request.topicId(), request.name(), codes, roles);
		return ResponseEntity.created(URI.create("/api/v1/councils/" + council.getId()))
				.body(CouncilResponse.of(councilService.findView(council.getId())));
	}

	@PreAuthorize("hasRole('LECTURER')")
	@PostMapping("/{id}/scores")
	public CouncilResponse score(@PathVariable Long id, @Valid @RequestBody ScoreRequest request,
			Authentication authentication) {
		councilService.score(id, currentLecturer(authentication), request.score(), request.comment());
		return CouncilResponse.of(councilService.findView(id));
	}

	@PreAuthorize("hasRole('LECTURER')")
	@PostMapping("/{id}/confirmation")
	public TopicResultResponse confirm(@PathVariable Long id, Authentication authentication) {
		return TopicResultResponse.of(councilService.confirm(id, currentLecturer(authentication)));
	}
}
