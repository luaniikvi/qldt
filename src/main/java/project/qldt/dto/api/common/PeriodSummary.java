package project.qldt.dto.api.common;

import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;

public record PeriodSummary(Long id, String name, PeriodType type, boolean registrationLocked) {

	public static PeriodSummary of(RegistrationPeriod period) {
		return period == null
				? null
				: new PeriodSummary(period.getId(), period.getName(), period.getType(),
						period.isRegistrationLocked());
	}
}
