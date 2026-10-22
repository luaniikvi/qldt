package project.qldt.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "lecturer")
@PrimaryKeyJoinColumn(name = "account_id")
@DiscriminatorValue("LECTURER")
public class Lecturer extends Account {

	@Column(name = "lecturer_code", nullable = false, unique = true, length = 30)
	private String lecturerCode;

	@Column(length = 120)
	private String department;

	@Override
	public Role getBaseRole() {
		return Role.LECTURER;
	}

	public boolean isDean() {
		return getExtraRoles().contains(Role.DEAN);
	}
}