package project.qldt.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import project.qldt.model.Account;
import project.qldt.model.Lecturer;
import project.qldt.model.Student;
import project.qldt.service.AccountService;
import project.qldt.service.CouncilService;

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
		if (account instanceof Student student) {
			model.addAttribute("results", councilService.findPublishedResultsFor(student));
		} else if (account instanceof Lecturer lecturer) {
			model.addAttribute("results", councilService.findPublishedResultsOfAdvisor(lecturer));
		} else {
			model.addAttribute("results", List.of());
		}
		return "results/list";
	}
}