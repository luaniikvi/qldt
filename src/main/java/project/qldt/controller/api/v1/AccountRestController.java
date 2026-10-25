package project.qldt.controller.api.v1;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import project.qldt.dto.account.AccountRequest;
import project.qldt.dto.api.request.AccountStatusRequest;
import project.qldt.dto.api.request.DeanRoleRequest;
import project.qldt.dto.api.request.ResetPasswordRequest;
import project.qldt.dto.api.response.AccountResponse;
import project.qldt.model.account.Account;
import project.qldt.model.account.AccountType;
import project.qldt.service.account.AccountService;

@RestController
@RequestMapping("/api/v1/accounts")
@PreAuthorize("hasRole('ADMIN')")
public class AccountRestController extends ApiControllerSupport {

	public AccountRestController(AccountService accountService) {
		super(accountService);
	}

	@GetMapping
	public List<AccountResponse> list(@RequestParam(required = false) AccountType type,
			@RequestParam(required = false) Boolean enabled, @RequestParam(required = false) String q) {
		return accountService.search(type, enabled, q).stream().map(AccountResponse::of).toList();
	}

	@GetMapping("/{id}")
	public AccountResponse getOne(@PathVariable Long id) {
		return AccountResponse.of(accountService.findById(id));
	}

	@PostMapping
	public ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountRequest request) {
		Account account = accountService.createAccount(request);
		return ResponseEntity.created(URI.create("/api/v1/accounts/" + account.getId()))
				.body(AccountResponse.of(account));
	}

	@PatchMapping("/{id}/status")
	public AccountResponse updateStatus(@PathVariable Long id, @Valid @RequestBody AccountStatusRequest request) {
		return AccountResponse.of(accountService.setEnabled(id, request.enabled()));
	}

	@PostMapping("/{id}/password-reset")
	public ResponseEntity<Void> resetPassword(@PathVariable Long id,
			@Valid @RequestBody ResetPasswordRequest request) {
		accountService.resetPassword(id, request.newPassword());
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{id}/dean")
	public AccountResponse setDean(@PathVariable Long id, @Valid @RequestBody DeanRoleRequest request) {
		return AccountResponse.of(accountService.setDean(id, request.dean()));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
		accountService.deleteAccount(id, authentication.getName());
		return ResponseEntity.noContent().build();
	}
}
