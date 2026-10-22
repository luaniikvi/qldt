package project.qldt.model;

public enum TopicStatus {
	PENDING("Chờ duyệt"), APPROVED("Đã duyệt"), REJECTED("Bị từ chối"), PUBLISHED("Đã công bố");

	private final String label;

	TopicStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
