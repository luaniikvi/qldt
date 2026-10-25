package project.qldt.service.period;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.exception.InvalidRegistrationPeriodException;
import project.qldt.exception.RegistrationPeriodNotFoundException;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.repository.period.RegistrationPeriodRepository;

import project.qldt.repository.team.TeamRepository;
import project.qldt.repository.topic.TopicRepository;

@Service
@Transactional
public class RegistrationPeriodService {

	private final RegistrationPeriodRepository periodRepository;
	private final TopicRepository topicRepository;
	private final TeamRepository teamRepository;

	public RegistrationPeriodService(RegistrationPeriodRepository periodRepository,
			TopicRepository topicRepository, TeamRepository teamRepository) {
		this.periodRepository = periodRepository;
		this.topicRepository = topicRepository;
		this.teamRepository = teamRepository;
	}

	@Transactional(readOnly = true)
	public List<RegistrationPeriod> findAll() {
		return periodRepository.findAll();
	}

	@Transactional(readOnly = true)
	public RegistrationPeriod findById(Long id) {
		return periodRepository.findById(id)
				.orElseThrow(() -> new RegistrationPeriodNotFoundException(id));
	}

	@Transactional(readOnly = true)
	public RegistrationPeriod findActiveOrFirst() {
		return periodRepository.findFirstByStudentRegisterEndAfterOrderByStudentRegisterEndAsc(LocalDateTime.now())
				.orElseGet(() -> periodRepository.findFirstByOrderByIdAsc().orElse(null));
	}

	public RegistrationPeriod create(RegistrationPeriodRequest request) {
		validate(request);
		RegistrationPeriod period = new RegistrationPeriod();
		apply(period, request);
		return periodRepository.save(period);
	}

	public RegistrationPeriod update(Long id, RegistrationPeriodRequest request) {
		validate(request);
		RegistrationPeriod period = findById(id);
		apply(period, request);
		return periodRepository.save(period);
	}

	public void deleteById(Long id) {
		RegistrationPeriod period = findById(id);
		if (topicRepository.existsByPeriodId(id) || teamRepository.existsByPeriodId(id)) {
			throw new InvalidRegistrationPeriodException(
					"Không thể xóa đợt đăng ký vì đã có đề tài hoặc nhóm sinh viên tham gia");
		}
		periodRepository.delete(period);
	}

	public RegistrationPeriod setRegistrationLocked(Long id, boolean locked) {
		RegistrationPeriod period = findById(id);
		period.setRegistrationLocked(locked);
		return periodRepository.save(period);
	}

	private void validate(RegistrationPeriodRequest request) {
		if (!request.getTeacherRegisterEnd().isAfter(request.getTeacherRegisterStart())) {
			throw new InvalidRegistrationPeriodException("Thời điểm kết thúc GV đăng ký phải sau thời điểm bắt đầu");
		}
		if (request.getStudentRegisterStart().isBefore(request.getTeacherRegisterEnd())) {
			throw new InvalidRegistrationPeriodException(
					"Thời điểm SV đăng ký phải bắt đầu sau khi GV đăng ký kết thúc");
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
		if (request.getReportDeadline() != null) {
			validateReportDeadline(request);
		} else {
			throw new InvalidRegistrationPeriodException("Hạn nộp báo cáo không được để trống");
		}
	}

	private void validateReportDeadline(RegistrationPeriodRequest request) {
		if (request.getReportDeadline().isBefore(request.getStudentRegisterEnd().toLocalDate())) {
			throw new InvalidRegistrationPeriodException(
					"Hạn nộp báo cáo phải sau thời điểm SV đăng ký kết thúc");
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
		if (request.getReviewerScoreDeadline() != null && request.getCouncilReportDate().isBefore(request.getReviewerScoreDeadline())) {
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
		period.setReportDeadline(request.getReportDeadline());
	}
}