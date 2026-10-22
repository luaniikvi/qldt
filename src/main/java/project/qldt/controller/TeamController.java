package project.qldt.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import project.qldt.dto.TeamRequest;
import project.qldt.dto.TeamView;
import project.qldt.exception.AccountNotFoundException;
import project.qldt.exception.InvalidRegistrationException;
import project.qldt.exception.InvalidReportException;
import project.qldt.exception.InvalidTeamException;
import project.qldt.exception.TeamNotFoundException;
import project.qldt.exception.TopicNotFoundException;
import project.qldt.model.Account;
import project.qldt.model.Report;
import project.qldt.model.Student;
import project.qldt.model.Team;
import project.qldt.service.AccountService;
import project.qldt.service.RegistrationPeriodService;
import project.qldt.service.ReportService;
import project.qldt.service.TeamService;
import project.qldt.service.TopicRegistrationService;

@Controller
@RequestMapping("/teams")
@PreAuthorize("hasRole('STUDENT')")
public class TeamController {

	private final TeamService teamService;
	private final RegistrationPeriodService periodService;
	private final AccountService accountService;
	private final TopicRegistrationService registrationService;
	private final ReportService reportService;

	public TeamController(TeamService teamService, RegistrationPeriodService periodService,
			AccountService accountService, TopicRegistrationService registrationService,
			ReportService reportService) {
		this.teamService = teamService;
		this.periodService = periodService;
		this.accountService = accountService;
		this.registrationService = registrationService;
		this.reportService = reportService;
	}

	@GetMapping
	public String list(Authentication authentication, Model model) {
		Student student = currentStudent(authentication);
		List<TeamView> teams = teamService.findByStudent(student).stream()
				.map(team -> TeamView.of(team, student, reportService.findByTeam(team)))
				.toList();
		model.addAttribute("teams", teams);
		return "teams/list";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		return form(model, new TeamRequest());
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("team") TeamRequest request,
			BindingResult result,
			Authentication authentication,
			Model model) {
		if (result.hasErrors()) {
			return form(model, request);
		}
		try {
			teamService.create(request.getPeriodId(), currentStudent(authentication));
		} catch (InvalidTeamException ex) {
			result.reject("invalid", ex.getMessage());
			return form(model, request);
		}
		return "redirect:/teams";
	}

	@PostMapping("/{id}/invite")
	public String invite(@PathVariable Long id,
			@RequestParam String studentCode,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			teamService.invite(id, studentCode, currentStudent(authentication));
			redirectAttributes.addFlashAttribute("message", "Đã gửi lời mời vào nhóm");
		} catch (InvalidTeamException | AccountNotFoundException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/teams";
	}

	@PostMapping("/{id}/invitation")
	public String respondInvitation(@PathVariable Long id,
			@RequestParam boolean accept,
			@RequestParam(required = false) String reason,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			teamService.respondInvitation(id, currentStudent(authentication), accept, reason);
			redirectAttributes.addFlashAttribute("message",
					accept ? "Đã tham gia nhóm" : "Đã từ chối lời mời");
		} catch (InvalidTeamException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/teams";
	}

	@PostMapping("/{id}/leader")
	public String transferLeader(@PathVariable Long id,
			@RequestParam String studentCode,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			teamService.transferLeader(id, studentCode, currentStudent(authentication));
			redirectAttributes.addFlashAttribute("message", "Đã chuyển quyền nhóm trưởng");
		} catch (InvalidTeamException | AccountNotFoundException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/teams";
	}

	@PostMapping("/{id}/leave")
	public String leave(@PathVariable Long id,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			teamService.leave(id, currentStudent(authentication));
			redirectAttributes.addFlashAttribute("message", "Đã rời nhóm");
		} catch (InvalidTeamException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/teams";
	}

	@PostMapping("/{id}/invitation/cancel")
	public String cancelInvitation(@PathVariable Long id,
			@RequestParam String studentCode,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			teamService.cancelInvitation(id, studentCode, currentStudent(authentication));
			redirectAttributes.addFlashAttribute("message", "Đã hủy lời mời vào nhóm");
		} catch (InvalidTeamException | AccountNotFoundException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/teams";
	}

	@PostMapping("/{id}/members/remove")
	public String removeMember(@PathVariable Long id,
			@RequestParam String studentCode,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			teamService.removeMember(id, studentCode, currentStudent(authentication));
			redirectAttributes.addFlashAttribute("message", "Đã xóa thành viên khỏi nhóm");
		} catch (InvalidTeamException | AccountNotFoundException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/teams";
	}

	@GetMapping("/{id}/register")
	public String registerForm(@PathVariable Long id, Authentication authentication,
			RedirectAttributes redirectAttributes, Model model) {
		Team team = teamService.findById(id);
		Student student = currentStudent(authentication);
		if (!team.getLeader().getId().equals(student.getId())) {
			redirectAttributes.addFlashAttribute("error", "Chỉ nhóm trưởng mới được đăng ký đề tài");
			return "redirect:/teams";
		}
		model.addAttribute("team", team);
		model.addAttribute("topics", registrationService.findAvailableTopicsFor(team));
		return "teams/register";
	}

	@PostMapping("/{id}/register")
	public String register(@PathVariable Long id,
			@RequestParam Long topicId,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			registrationService.register(id, topicId, currentStudent(authentication));
			redirectAttributes.addFlashAttribute("message",
					"Đã đăng ký đề tài, chờ giảng viên hướng dẫn duyệt");
		} catch (InvalidRegistrationException | TopicNotFoundException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/teams";
	}

	@PostMapping("/{id}/registration/cancel")
	public String cancelRegistration(@PathVariable Long id,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			registrationService.cancel(id, currentStudent(authentication));
			redirectAttributes.addFlashAttribute("message", "Đã hủy đăng ký đề tài");
		} catch (InvalidRegistrationException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/teams";
	}

	@ExceptionHandler(TeamNotFoundException.class)
	public String handleNotFound(TeamNotFoundException ex, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("error", ex.getMessage());
		return "redirect:/teams";
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public String handleMaxSize(MaxUploadSizeExceededException ex, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("error",String.format("Tệp báo cáo vượt quá dung lượng cho phép (tối đa %dMB)", ReportService.MAX_SIZE_MB));
		return "redirect:/teams";
	}

	private String form(Model model, TeamRequest request) {
		model.addAttribute("team", request);
		model.addAttribute("periods", periodService.findAll());
		return "teams/form";
	}

	private Student currentStudent(Authentication authentication) {
		Account account = accountService.findByUsername(authentication.getName());
		if (account instanceof Student student) {
			return student;
		}
		throw new AccessDeniedException("Chỉ sinh viên mới sử dụng được chức năng này");
	}

	@PostMapping("/{id}/report")
	public String submitReport(@PathVariable Long id,
			@RequestParam("file") MultipartFile file,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			Report report = reportService.submit(id, currentStudent(authentication), file);
			redirectAttributes.addFlashAttribute("message",
					"Đã nộp báo cáo: " + report.getOriginalFileName() + " (phiên bản " + report.getVersion() + ")");
		} catch (InvalidReportException | TeamNotFoundException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/teams";
	}
}
