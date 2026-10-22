package project.qldt.model;

public enum DepartmentType {
	CNPM("Công Nghệ Phần Mềm"), HTTT("Hệ Thống Thông Tin"), ATTT("Mạng và An Ninh Mạng"), AI("Trí Tuệ Nhân Tạo");

	private final String label;

	DepartmentType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
