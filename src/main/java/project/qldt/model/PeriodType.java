package project.qldt.model;

public enum PeriodType {

	COURSE("Môn học"),
	RESEARCH("NCKH"),
	SPECIALIZED_THESIS("TLCN"),
	GRADUATION_THESIS("KLTN");

	private final String label;

	PeriodType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public boolean requiresReviewerScoreDeadline() {
		return this == SPECIALIZED_THESIS || this == GRADUATION_THESIS;
	}

	public boolean requiresCouncilReportDate() {
		return this == GRADUATION_THESIS;
	}
}
