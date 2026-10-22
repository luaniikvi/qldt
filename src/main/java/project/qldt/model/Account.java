package project.qldt.model;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "account")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "account_type", length = 20)
public abstract class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@Column(nullable = false, length = 100)
	private String password;

	@Column(name = "full_name", nullable = false, length = 120)
	private String fullName;

	@Column(length = 120)
	private String email;

	@Column(nullable = false)
	private boolean enabled = true;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "account_role", joinColumns = @JoinColumn(name = "account_id"))
	@Column(name = "role", length = 20)
	@Enumerated(EnumType.STRING)
	private Set<Role> extraRoles = new LinkedHashSet<>();

	public abstract Role getBaseRole();

	public Set<Role> getRoles() {
		Set<Role> roles = new LinkedHashSet<>();
		roles.add(getBaseRole());
		roles.addAll(extraRoles);
		return roles;
	}
}