package project.qldt.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Admin extends Account {

	@Override
	public Role getBaseRole() {
		return Role.ADMIN;
	}
}
