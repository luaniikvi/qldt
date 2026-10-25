package project.qldt.model.notification;

public enum NotificationType {
	INVITATION("Lời mời"), APPROVAL("Duyệt"), REJECTION("Từ chối"), RESULT("Kết quả"), REMINDER("Nhắc hạn");

	private final String label;

	NotificationType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}