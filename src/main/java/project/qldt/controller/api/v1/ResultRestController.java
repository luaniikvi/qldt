package project.qldt.controller.api.v1;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import project.qldt.dto.api.response.TopicResultResponse;
import project.qldt.model.account.Account;
import project.qldt.model.account.Lecturer;
import project.qldt.model.account.Student;
import project.qldt.service.account.AccountService;
import project.qldt.service.council.CouncilService;

@RestController
@RequestMapping("/api/v1/results")
public class ResultRestController extends ApiControllerSupport {

	private final CouncilService councilService;

	public ResultRestController(CouncilService councilService, AccountService accountService) {
		super(accountService);
		this.councilService = councilService;
	}

	@PreAuthorize("hasRole('DEAN')")
	@GetMapping
	public List<TopicResultResponse> list(@RequestParam Long periodId) {
		return councilService.findResultsByPeriod(periodId).stream().map(TopicResultResponse::of).toList();
	}

	@GetMapping("/mine")
	public List<TopicResultResponse> mine(Authentication authentication) {
		Account account = currentAccount(authentication);
		if (account instanceof Student student) {
			return councilService.findPublishedResultsFor(student).stream().map(TopicResultResponse::of).toList();
		}
		if (account instanceof Lecturer lecturer) {
			return councilService.findPublishedResultsOfAdvisor(lecturer).stream().map(TopicResultResponse::of)
					.toList();
		}
		throw new AccessDeniedException("Bạn không có quyền xem kết quả này");
	}

	@PreAuthorize("hasRole('DEAN')")
	@GetMapping("/{id}")
	public TopicResultResponse getOne(@PathVariable Long id) {
		return TopicResultResponse.of(councilService.findResultById(id));
	}

	@PreAuthorize("hasRole('DEAN')")
	@PostMapping("/{id}/publish")
	public TopicResultResponse publish(@PathVariable Long id) {
		return TopicResultResponse.of(councilService.publish(id));
	}
}
