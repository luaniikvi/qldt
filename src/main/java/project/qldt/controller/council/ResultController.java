package project.qldt.controller.council;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import project.qldt.model.account.Account;
import project.qldt.service.account.AccountService;
import project.qldt.service.council.CouncilService;

@Controller
public class ResultController {

	private final CouncilService councilService;
	private final AccountService accountService;

	public ResultController(CouncilService councilService, AccountService accountService) {
		this.councilService = councilService;
		this.accountService = accountService;
	}

	@GetMapping("/results")
	public String list(Authentication authentication, Model model) {
		Account account = accountService.findByUsername(authentication.getName());
		model.addAttribute("results", councilService.findPublishedResultsFor(account));
		return "results/list";
	}
}