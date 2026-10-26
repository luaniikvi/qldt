package project.qldt.model.council;

public enum CouncilRole {
	CHAIR("Chủ tịch"), SECRETARY("Thư ký"), MEMBER("Thành viên");

	private final String label;

	CouncilRole(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}