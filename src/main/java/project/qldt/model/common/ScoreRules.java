package project.qldt.model.common;

import java.util.Optional;

public final class ScoreRules {

	public static final double MIN_SCORE = 0;
	public static final double MAX_SCORE = 10;

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
}
