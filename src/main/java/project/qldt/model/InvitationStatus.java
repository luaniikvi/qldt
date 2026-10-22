package project.qldt.model;

public enum InvitationStatus {
	PENDING("Chờ phản hồi"), ACCEPTED("Đã đồng ý"), REJECTED("Đã từ chối"), CANCELLED("Đã hủy");

	private final String label;

	InvitationStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
