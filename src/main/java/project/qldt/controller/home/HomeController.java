package project.qldt.controller.home;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import project.qldt.security.CurrentAuthentication;

@Controller
public class HomeController {

	@GetMapping("/")
	public String root(Authentication authentication) {
		return CurrentAuthentication.resolve(authentication) != null ? "redirect:/announcements" : "home";
	}

	@GetMapping("/home")
	public String home() {
		return "home";
	}

	@GetMapping("/dashboard")
	public String dashboard(Authentication authentication) {
		return CurrentAuthentication.resolve(authentication) != null
				? "redirect:/announcements"
				: "redirect:/login";
	}

	@GetMapping("/component-preview")
	public String componentPreview() {
		return "component-preview";
	}
}
