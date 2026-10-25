package project.qldt.controller.api.v1;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import project.qldt.dto.api.request.ApprovalRequest;
import project.qldt.dto.api.request.ReasonRequest;
import project.qldt.dto.api.request.RegistrationRequest;
import project.qldt.dto.api.response.RegistrationResponse;
import project.qldt.dto.api.response.TeamResponse;
import project.qldt.dto.api.response.TopicResponse;
import project.qldt.dto.topic.RegistrationView;
import project.qldt.model.team.Team;
import project.qldt.service.account.AccountService;
import project.qldt.service.report.ReportService;
import project.qldt.service.team.TeamService;
import project.qldt.service.topic.TopicRegistrationService;

@RestController
@RequestMapping("/api/v1")
public class RegistrationRestController extends ApiControllerSupport {

	private final TopicRegistrationService registrationService;
	private final TeamService teamService;
	private final ReportService reportService;

	public RegistrationRestController(TopicRegistrationService registrationService, TeamService teamService,
			ReportService reportService, AccountService accountService) {
		super(accountService);
		this.registrationService = registrationService;
		this.teamService = teamService;
		this.reportService = reportService;
	}

	@PreAuthorize("hasRole('STUDENT')")
	@GetMapping("/teams/{teamId}/available-topics")
	public List<TopicResponse> availableTopics(@PathVariable Long teamId, Authentication authentication) {
		Team team = teamService.findForMember(teamId, currentStudent(authentication));
		return registrationService.findAvailableTopicsFor(team).stream().map(TopicResponse::of).toList();
	}

	@PreAuthorize("hasRole('STUDENT')")
	@PostMapping("/teams/{teamId}/registration")
	public TeamResponse register(@PathVariable Long teamId, @Valid @RequestBody RegistrationRequest request,
			Authentication authentication) {
		return toResponse(registrationService.register(teamId, request.topicId(),
				currentStudent(authentication)));
	}

	@PreAuthorize("hasRole('STUDENT')")
	@DeleteMapping("/teams/{teamId}/registration")
	public TeamResponse cancel(@PathVariable Long teamId, Authentication authentication) {
		return toResponse(registrationService.cancel(teamId, currentStudent(authentication)));
	}

	@PreAuthorize("hasRole('LECTURER')")
	@GetMapping("/registrations/pending")
	public List<RegistrationResponse> pending(Authentication authentication) {
		return registrationService.findPendingForAdvisor(currentLecturer(authentication)).stream()
				.map(this::toResponse).toList();
	}

	@PreAuthorize("hasRole('DEAN')")
	@GetMapping("/registrations")
	public List<RegistrationResponse> list(@RequestParam(required = false) Long periodId) {
		return registrationService.findRegistrationsForDean(periodId).stream().map(this::toResponse).toList();
	}

	@PreAuthorize("hasRole('LECTURER')")
	@PatchMapping("/topics/{topicId}/registration/approval")
	public TeamResponse approval(@PathVariable Long topicId, @Valid @RequestBody ApprovalRequest request,
			Authentication authentication) {
		Team team = request.approved()
				? registrationService.approve(topicId, currentLecturer(authentication))
				: registrationService.reject(topicId, currentLecturer(authentication), request.reason());
		return toResponse(team);
	}

	@PreAuthorize("hasRole('DEAN')")
	@PostMapping("/topics/{topicId}/registration/cancellation")
	public TeamResponse cancelByDean(@PathVariable Long topicId, @Valid @RequestBody ReasonRequest request) {
		return toResponse(registrationService.cancelByDean(topicId, request.reason()));
	}

	private TeamResponse toResponse(Team team) {
		return team == null ? null : TeamResponse.of(team, reportService.findByTeam(team));
	}

	private RegistrationResponse toResponse(RegistrationView view) {
		return RegistrationResponse.of(view, reportService.findByTeam(view.team()));
	}
}
