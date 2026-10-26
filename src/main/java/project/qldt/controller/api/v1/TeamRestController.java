package project.qldt.controller.api.v1;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import project.qldt.dto.api.request.InvitationResponseRequest;
import project.qldt.dto.api.request.StudentCodeRequest;
import project.qldt.dto.api.response.TeamResponse;
import project.qldt.dto.team.TeamRequest;
import project.qldt.dto.team.TeamView;
import project.qldt.model.team.Team;
import project.qldt.service.account.AccountService;
import project.qldt.service.report.ReportService;
import project.qldt.service.team.TeamService;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamRestController extends ApiControllerSupport {

	private final TeamService teamService;
	private final ReportService reportService;

	public TeamRestController(TeamService teamService, ReportService reportService, AccountService accountService) {
		super(accountService);
		this.teamService = teamService;
		this.reportService = reportService;
	}

	@GetMapping
	public List<TeamResponse> list(@RequestParam(required = false) Long periodId, Authentication authentication) {
		return teamService.findViewsFor(currentAccount(authentication), periodId).stream().map(this::toResponse)
				.toList();
	}

	@PreAuthorize("hasRole('STUDENT')")
	@GetMapping("/{id}")
	public TeamResponse getOne(@PathVariable Long id, Authentication authentication) {
		Team team = teamService.findForMember(id, currentStudent(authentication));
		return TeamResponse.of(team, reportService.findByTeam(team));
	}

	@PreAuthorize("hasRole('STUDENT')")
	@PostMapping
	public ResponseEntity<TeamResponse> create(@Valid @RequestBody TeamRequest request,
			Authentication authentication) {
		Team team = teamService.create(request.getPeriodId(), currentStudent(authentication));
		return ResponseEntity.created(URI.create("/api/v1/teams/" + team.getId()))
				.body(TeamResponse.of(team, null));
	}

	@PreAuthorize("hasRole('STUDENT')")
	@PostMapping("/{id}/invitations")
	public TeamResponse invite(@PathVariable Long id, @Valid @RequestBody StudentCodeRequest request,
			Authentication authentication) {
		return toResponse(teamService.invite(id, request.studentCode(), currentStudent(authentication)));
	}

	@PreAuthorize("hasRole('STUDENT')")
	@PostMapping("/{id}/invitations/response")
	public TeamResponse respondInvitation(@PathVariable Long id,
			@Valid @RequestBody InvitationResponseRequest request, Authentication authentication) {
		return toResponse(teamService.respondInvitation(id, currentStudent(authentication), request.accept(),
				request.reason()));
	}

	@PreAuthorize("hasRole('STUDENT')")
	@DeleteMapping("/{id}/invitations/{studentCode}")
	public TeamResponse cancelInvitation(@PathVariable Long id, @PathVariable String studentCode,
			Authentication authentication) {
		return toResponse(teamService.cancelInvitation(id, studentCode, currentStudent(authentication)));
	}

	@PreAuthorize("hasRole('STUDENT')")
	@PutMapping("/{id}/leader")
	public TeamResponse transferLeader(@PathVariable Long id, @Valid @RequestBody StudentCodeRequest request,
			Authentication authentication) {
		return toResponse(teamService.transferLeader(id, request.studentCode(), currentStudent(authentication)));
	}

	@PreAuthorize("hasRole('STUDENT')")
	@DeleteMapping("/{id}/members/{studentCode}")
	public TeamResponse removeMember(@PathVariable Long id, @PathVariable String studentCode,
			Authentication authentication) {
		return toResponse(teamService.removeMember(id, studentCode, currentStudent(authentication)));
	}

	@PreAuthorize("hasRole('STUDENT')")
	@DeleteMapping("/{id}/members/me")
	public TeamResponse leave(@PathVariable Long id, Authentication authentication) {
		return toResponse(teamService.leave(id, currentStudent(authentication)));
	}

	private TeamResponse toResponse(Team team) {
		return TeamResponse.of(team, reportService.findByTeam(team));
	}

	private TeamResponse toResponse(TeamView view) {
		return TeamResponse.of(view.team(), view.report());
	}
}
