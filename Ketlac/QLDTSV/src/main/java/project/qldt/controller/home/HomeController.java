package project.qldt.controller.home;

import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import project.qldt.model.account.Account;
import project.qldt.model.announcement.Announcement;
import project.qldt.model.council.Council;
import project.qldt.model.account.Lecturer;
import project.qldt.model.account.Role;
import project.qldt.model.account.Student;
import project.qldt.model.team.Team;
import project.qldt.model.topic.Topic;
import project.qldt.model.topic.TopicStatus;
import project.qldt.service.account.AccountService;
import project.qldt.service.announcement.AnnouncementService;
import project.qldt.service.council.CouncilService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.team.TeamService;
import project.qldt.service.topic.TopicRegistrationService;
import project.qldt.service.topic.TopicService;

@Controller
public class HomeController {

	private final AccountService accountService;
	private final AnnouncementService announcementService;
	private final RegistrationPeriodService periodService;
	private final TopicService topicService;
	private final TeamService teamService;
	private final TopicRegistrationService registrationService;
	private final CouncilService councilService;

	public HomeController(AccountService accountService,
			AnnouncementService announcementService,
			RegistrationPeriodService periodService,
			TopicService topicService,
			TeamService teamService,
			TopicRegistrationService registrationService,
			CouncilService councilService) {
		this.accountService = accountService;
		this.announcementService = announcementService;
		this.periodService = periodService;
		this.topicService = topicService;
		this.teamService = teamService;
		this.registrationService = registrationService;
		this.councilService = councilService;
	}

	@GetMapping("/")
	public String root(Authentication authentication, Model model) {
		if (isAuthenticated(authentication)) {
			return "redirect:/announcements";
		}
		return "home";
	}

	@GetMapping("/home")
	public String home() {
		return "home";
	}

	@GetMapping("/dashboard")
	public String dashboard(Authentication authentication, Model model) {
		if (isAuthenticated(authentication)) {
			return "redirect:/announcements";
		}
		return "redirect:/login";
	}

	@GetMapping("/component-preview")
	public String componentPreview() {
		return "component-preview";
	}

	private boolean isAuthenticated(Authentication authentication) {
		return authentication != null
				&& authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);
	}
}
