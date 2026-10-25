package project.qldt.controller.api.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import project.qldt.dto.account.ChangePasswordRequest;
import project.qldt.dto.api.response.AccountResponse;
import project.qldt.service.account.AccountService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthRestController extends ApiControllerSupport {

	public AuthRestController(AccountService accountService) {
		super(accountService);
	}

	@GetMapping("/me")
	public AccountResponse me(Authentication authentication) {
		return AccountResponse.of(currentAccount(authentication));
	}

	@PostMapping("/change-password")
	public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
			Authentication authentication) {
		accountService.changePassword(authentication.getName(), request);
		return ResponseEntity.noContent().build();
	}
}
