package project.qldt.controller.api.v1;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import project.qldt.dto.api.request.LecturerCodeRequest;
import project.qldt.dto.api.request.ScoreRequest;
import project.qldt.dto.api.response.TopicResponse;
import project.qldt.service.account.AccountService;
import project.qldt.service.reviewer.ReviewerService;

@RestController
@RequestMapping("/api/v1/reviewer")
public class ReviewerRestController extends ApiControllerSupport {

	private final ReviewerService reviewerService;

	public ReviewerRestController(ReviewerService reviewerService, AccountService accountService) {
		super(accountService);
		this.reviewerService = reviewerService;
	}

	@PreAuthorize("hasRole('LECTURER')")
	@GetMapping("/topics")
	public List<TopicResponse> assigned(Authentication authentication) {
		return reviewerService.findAssigned(currentLecturer(authentication)).stream().map(TopicResponse::of)
				.toList();
	}

	@PreAuthorize("hasRole('DEAN')")
	@GetMapping("/topics/assignable")
	public List<TopicResponse> assignable() {
		return reviewerService.findAssignable().stream().map(TopicResponse::of).toList();
	}

	@PreAuthorize("hasAnyRole('DEAN','LECTURER')")
	@GetMapping("/topics/{topicId}")
	public TopicResponse getOne(@PathVariable Long topicId) {
		return TopicResponse.of(reviewerService.findTopic(topicId));
	}

	@PreAuthorize("hasRole('DEAN')")
	@PutMapping("/topics/{topicId}/reviewer")
	public TopicResponse assign(@PathVariable Long topicId, @Valid @RequestBody LecturerCodeRequest request) {
		return TopicResponse.of(reviewerService.assign(topicId, request.lecturerCode()));
	}

	@PreAuthorize("hasRole('LECTURER')")
	@PostMapping("/topics/{topicId}/score")
	public TopicResponse score(@PathVariable Long topicId, @Valid @RequestBody ScoreRequest request,
			Authentication authentication) {
		return TopicResponse.of(reviewerService.score(topicId, currentLecturer(authentication), request.score(),
				request.comment()));
	}

	@GetMapping("/scores")
	public List<TopicResponse> scores(Authentication authentication) {
		return reviewerService.findVisibleScores(currentAccount(authentication)).stream().map(TopicResponse::of)
				.toList();
	}
}
