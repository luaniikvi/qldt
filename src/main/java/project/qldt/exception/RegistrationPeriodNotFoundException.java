package project.qldt.exception;

public class RegistrationPeriodNotFoundException extends RuntimeException {

	public RegistrationPeriodNotFoundException(Long id) {
		super("Không tìm thấy đợt đăng ký có id: " + id);
	}
}
