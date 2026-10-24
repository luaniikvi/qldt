package project.qldt.model.period;

public enum PeriodType {

	COURSE_PROJECT("Môn học"), RESEARCH("NCKH"), SPECIALIZED_PROJECT("TLCN"), GRADUATION_THESIS("KLTN");

	private final String label;

	PeriodType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public boolean requiresReviewerScoreDeadline() {
		return this == SPECIALIZED_PROJECT || this == GRADUATION_THESIS;
	}

	public boolean requiresCouncilReportDate() {
		return this == GRADUATION_THESIS;
	}
}
