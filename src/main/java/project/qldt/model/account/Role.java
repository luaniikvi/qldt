package project.qldt.model.account;

public enum Role {
	ADMIN("Quản trị"), LECTURER("Giảng viên"), STUDENT("Sinh viên"), DEAN("Trưởng khoa");

	private final String label;

	Role(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
