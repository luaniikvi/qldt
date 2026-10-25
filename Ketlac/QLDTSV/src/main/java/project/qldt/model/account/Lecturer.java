package project.qldt.model.account;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import project.qldt.model.department.Department;

@Getter
@Setter
@Entity
@Table(name = "lecturers")
@PrimaryKeyJoinColumn(name = "account_id")
@DiscriminatorValue("LECTURER")
public class Lecturer extends Account {

	@Column(name = "lecturer_code", nullable = false, unique = true, length = 30)
	private String lecturerCode;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "department_id")
	private Department department;

	@Override
	public Role getBaseRole() {
		return Role.LECTURER;
	}
}