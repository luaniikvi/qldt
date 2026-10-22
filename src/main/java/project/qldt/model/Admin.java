package project.qldt.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "admin")
@PrimaryKeyJoinColumn(name = "account_id")
@DiscriminatorValue("ADMIN")
public class Admin extends Account {

	@Override
	public Role getBaseRole() {
		return Role.ADMIN;
	}
}