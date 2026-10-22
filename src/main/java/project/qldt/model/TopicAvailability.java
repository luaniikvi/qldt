package project.qldt.model;

public enum TopicAvailability {
	AVAILABLE("Chưa có nhóm đăng ký"), PENDING("Chờ duyệt"), REGISTERED("Đã duyệt");

	private final String label;

	TopicAvailability(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
