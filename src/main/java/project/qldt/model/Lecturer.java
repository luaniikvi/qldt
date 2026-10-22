package project.qldt.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Lecturer extends Account {

	private String lecturerCode;

	@Override
	public Role getBaseRole() {
		return Role.LECTURER;
	}

	public boolean isDean() {
		return getExtraRoles().contains(Role.DEAN);
	}
}
