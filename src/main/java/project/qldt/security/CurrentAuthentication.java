package project.qldt.security;

import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentAuthentication {

	private CurrentAuthentication() {
	}

	public static Authentication resolve(Authentication authentication) {
		if (isAuthenticated(authentication)) {
			return authentication;
		}
		Authentication contextAuthentication = SecurityContextHolder.getContext().getAuthentication();
		return isAuthenticated(contextAuthentication) ? contextAuthentication : null;
	}

	public static List<String> authoritiesOf(Authentication authentication) {
		if (authentication == null || authentication.getAuthorities() == null) {
			return List.of();
		}
		return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
	}

	public static String usernameOf(Authentication authentication) {
		return authentication == null ? null : authentication.getName();
	}

	private static boolean isAuthenticated(Authentication authentication) {
		return authentication != null && authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);
	}
}
