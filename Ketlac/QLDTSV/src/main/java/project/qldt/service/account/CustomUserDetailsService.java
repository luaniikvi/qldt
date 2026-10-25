package project.qldt.service.account;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import project.qldt.exception.AccountNotFoundException;
import project.qldt.model.account.Account;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final AccountService accountService;

	public CustomUserDetailsService(AccountService accountService) {
		this.accountService = accountService;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Account account;
		try {
			account = accountService.findByUsername(username);
		} catch (AccountNotFoundException ex) {
			throw new UsernameNotFoundException(ex.getMessage());
		}
		List<GrantedAuthority> authorities = new ArrayList<>();
		account.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name())));
		return new org.springframework.security.core.userdetails.User(
				account.getUsername(), account.getPassword(), account.isEnabled(), true, true, true, authorities);
	}
}
