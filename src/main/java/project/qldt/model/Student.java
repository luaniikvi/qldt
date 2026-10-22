package project.qldt.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Student extends Account {

	private String studentCode;

	@Override
	public Role getBaseRole() {
		return Role.STUDENT;
	}
}
