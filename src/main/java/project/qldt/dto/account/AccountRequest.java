package project.qldt.dto.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import project.qldt.model.account.AccountType;

@Getter
@Setter
public class AccountRequest {

	@NotNull(message = "Loại tài khoản không được để trống")
	private AccountType type;

	@NotBlank(message = "Mã đăng nhập không được để trống")
	@Size(max = 30, message = "Mã đăng nhập tối đa 30 ký tự")
	private String code;

	@NotBlank(message = "Họ tên không được để trống")
	@Size(max = 100, message = "Họ tên tối đa 100 ký tự")
	private String fullName;

	@NotBlank(message = "Email không được để trống")
	@Email(message = "Email không đúng định dạng")
	private String email;

	@NotBlank(message = "Mật khẩu không được để trống")
	@Size(min = 6, max = 50, message = "Mật khẩu phải từ 6 đến 50 ký tự")
	private String password;

	@NotBlank(message = "Vui lòng xác nhận mật khẩu")
	@Size(max = 50, message = "Mật khẩu xác nhận tối đa 50 ký tự")
	private String confirmPassword;

	@Size(max = 100, message = "Bộ môn tối đa 100 ký tự")
	private String department;
}
