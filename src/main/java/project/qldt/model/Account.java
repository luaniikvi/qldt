package project.qldt.model;

import java.util.LinkedHashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Account {

	private Long id;
	private String username;
	private String password;
	private String fullName;
	private String email;
	private boolean enabled = true;
	private Set<Role> extraRoles = new LinkedHashSet<>();

	public abstract Role getBaseRole();

	public Set<Role> getRoles() {
		Set<Role> roles = new LinkedHashSet<>();
		roles.add(getBaseRole());
		roles.addAll(extraRoles);
		return roles;
	}
}
