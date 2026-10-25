package project.qldt.model.council;

import project.qldt.exception.InvalidCouncilException;

public enum CouncilRole {
	CHAIR("Chủ tịch"), SECRETARY("Thư ký"), MEMBER("Thành viên");

	private final String label;

	CouncilRole(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public static CouncilRole parseOrMember(String name) {
		if (name == null || name.isBlank()) {
			return MEMBER;
		}
		try {
			return valueOf(name.trim());
		} catch (IllegalArgumentException ex) {
			throw new InvalidCouncilException("Vai trò trong hội đồng không hợp lệ");
		}
	}
}