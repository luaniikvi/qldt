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
@Table(name = "student")
@PrimaryKeyJoinColumn(name = "account_id")
@DiscriminatorValue("STUDENT")
public class Student extends Account {

	@Column(name = "student_code", nullable = false, unique = true, length = 30)
	private String studentCode;

	@Override
	public Role getBaseRole() {
		return Role.STUDENT;
	}
}