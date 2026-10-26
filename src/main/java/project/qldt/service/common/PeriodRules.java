package project.qldt.service.common;

import project.qldt.model.period.PeriodType;

public final class PeriodRules {

	private PeriodRules() {
	}

	public static boolean requiresReviewerScore(PeriodType type) {
		return type == PeriodType.SPECIALIZED_PROJECT || type == PeriodType.GRADUATION_THESIS;
	}

	public static boolean requiresCouncilReportDate(PeriodType type) {
		return type == PeriodType.GRADUATION_THESIS;
	}
}
