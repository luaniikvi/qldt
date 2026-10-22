package project.qldt.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import project.qldt.dto.RegistrationPeriodRequest;
import project.qldt.exception.InvalidRegistrationPeriodException;
import project.qldt.exception.RegistrationPeriodNotFoundException;
import project.qldt.model.RegistrationPeriod;

@Service
public class RegistrationPeriodService {

	private final List<RegistrationPeriod> store = new ArrayList<>();
	private final AtomicLong sequence = new AtomicLong(1);

	public List<RegistrationPeriod> findAll() {
		return new ArrayList<>(store);
	}

	public RegistrationPeriod findById(Long id) {
		return store.stream()
				.filter(period -> period.getId().equals(id))
				.findFirst()
				.orElseThrow(() -> new RegistrationPeriodNotFoundException(id));
	}

	public RegistrationPeriod create(RegistrationPeriodRequest request) {
		validate(request);
		RegistrationPeriod period = new RegistrationPeriod(sequence.getAndIncrement());
		apply(period, request);
		store.add(period);
		return period;
	}

	public RegistrationPeriod update(Long id, RegistrationPeriodRequest request) {
		validate(request);
		RegistrationPeriod period = findById(id);
		apply(period, request);
		return period;
	}

	public void deleteById(Long id) {
		store.remove(findById(id));
	}

	private void validate(RegistrationPeriodRequest request) {
		if (!request.getTeacherRegisterEnd().isAfter(request.getTeacherRegisterStart())) {
			throw new InvalidRegistrationPeriodException("Thời điểm kết thúc GV đăng ký phải sau thời điểm bắt đầu");
		}
		if (request.getStudentRegisterStart().isBefore(request.getTeacherRegisterEnd())) {
			throw new InvalidRegistrationPeriodException("Thời điểm SV đăng ký phải bắt đầu sau khi GV đăng ký kết thúc");
		}
		if (!request.getStudentRegisterEnd().isAfter(request.getStudentRegisterStart())) {
			throw new InvalidRegistrationPeriodException("Thời điểm kết thúc SV đăng ký phải sau thời điểm bắt đầu");
		}
		if (request.getType().requiresReviewerScoreDeadline()) {
			validateReviewerScoreDeadline(request);
		}
		if (request.getType().requiresCouncilReportDate()) {
			validateCouncilReportDate(request);
		}
	}

	private void validateReviewerScoreDeadline(RegistrationPeriodRequest request) {
		if (request.getReviewerScoreDeadline() == null) {
			throw new InvalidRegistrationPeriodException(
					"Đợt " + request.getType().getLabel() + " phải có hạn nộp điểm GVPB");
		}
		if (request.getReviewerScoreDeadline().isBefore(request.getStudentRegisterEnd().toLocalDate())) {
			throw new InvalidRegistrationPeriodException(
					"Hạn nộp điểm GVPB phải sau thời điểm SV đăng ký kết thúc");
		}
	}

	private void validateCouncilReportDate(RegistrationPeriodRequest request) {
		if (request.getCouncilReportDate() == null) {
			throw new InvalidRegistrationPeriodException(
					"Đợt " + request.getType().getLabel() + " phải có ngày báo cáo hội đồng");
		}
		if (request.getCouncilReportDate().isBefore(request.getReviewerScoreDeadline())) {
			throw new InvalidRegistrationPeriodException(
					"Ngày báo cáo hội đồng phải sau hạn nộp điểm GVPB");
		}
	}

	private void apply(RegistrationPeriod period, RegistrationPeriodRequest request) {
		period.setName(request.getName());
		period.setType(request.getType());
		period.setTeacherRegisterStart(request.getTeacherRegisterStart());
		period.setTeacherRegisterEnd(request.getTeacherRegisterEnd());
		period.setStudentRegisterStart(request.getStudentRegisterStart());
		period.setStudentRegisterEnd(request.getStudentRegisterEnd());
		period.setReviewerScoreDeadline(
				request.getType().requiresReviewerScoreDeadline() ? request.getReviewerScoreDeadline() : null);
		period.setCouncilReportDate(
				request.getType().requiresCouncilReportDate() ? request.getCouncilReportDate() : null);
	}
}
