package project.qldt.model;

public enum AccountType {
	ADMIN("Quản trị"), LECTURER("Giảng viên"), STUDENT("Sinh viên");

	private final String label;

	AccountType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
