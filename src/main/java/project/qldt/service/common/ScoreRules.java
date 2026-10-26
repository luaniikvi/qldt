package project.qldt.service.common;

import java.util.Optional;

public final class ScoreRules {

	public static final double MIN_SCORE = 0;
	public static final double MAX_SCORE = 10;
	public static final double REVIEWER_WEIGHT = 0.4;
	public static final double COUNCIL_WEIGHT = 0.6;

	private ScoreRules() {
	}

	public static Optional<String> validate(double score) {
		if (score < MIN_SCORE || score > MAX_SCORE) {
			return Optional.of("Điểm phải nằm trong khoảng 0 đến 10");
		}
		if (Math.abs(score * 10 - Math.rint(score * 10)) > 0.0000001) {
			return Optional.of("Điểm chỉ được có 1 chữ số thập phân");
		}
		return Optional.empty();
	}

	public static double finalScore(Double reviewerScore, double councilAverageScore) {
		double value = reviewerScore == null
				? councilAverageScore
				: reviewerScore * REVIEWER_WEIGHT + councilAverageScore * COUNCIL_WEIGHT;
		return Math.round(value * 100.0) / 100.0;
	}
}
