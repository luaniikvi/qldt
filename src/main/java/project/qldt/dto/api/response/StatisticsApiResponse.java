package project.qldt.dto.api.response;

import java.util.List;

public record StatisticsApiResponse(PeriodStatisticsResponse summary, List<StatisticsRowResponse> rows) {
}
