package project.qldt.controller.period;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.exception.InvalidRegistrationPeriodException;
import project.qldt.exception.RegistrationPeriodNotFoundException;
import project.qldt.model.period.PeriodType;
import project.qldt.service.period.RegistrationPeriodService;

@Controller
@RequestMapping("/periods")
public class RegistrationPeriodController {

	private final RegistrationPeriodService periodService;

	public RegistrationPeriodController(RegistrationPeriodService periodService) {
		this.periodService = periodService;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("periods", periodService.findAll());
		return "periods/list";
	}

	@PreAuthorize("hasRole('DEAN')")
	@GetMapping("/new")
	public String createForm(Model model) {
		return form(model, new RegistrationPeriodRequest(), null);
	}

	@PreAuthorize("hasRole('DEAN')")
	@PostMapping
	public String create(@Valid @ModelAttribute("period") RegistrationPeriodRequest request,
			BindingResult result,
			Model model) {
		if (result.hasErrors()) {
			return form(model, request, null);
		}
		try {
			periodService.create(request);
		} catch (InvalidRegistrationPeriodException ex) {
			result.reject("invalid", ex.getMessage());
			return form(model, request, null);
		}
		return "redirect:/periods";
	}

	@PreAuthorize("hasRole('DEAN')")
	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		return form(model, RegistrationPeriodRequest.from(periodService.findById(id)), id);
	}

	@PreAuthorize("hasRole('DEAN')")
	@PostMapping("/{id}")
	public String update(@PathVariable Long id,
			@Valid @ModelAttribute("period") RegistrationPeriodRequest request,
			BindingResult result,
			Model model) {
		if (result.hasErrors()) {
			return form(model, request, id);
		}
		try {
			periodService.update(id, request);
		} catch (InvalidRegistrationPeriodException ex) {
			result.reject("invalid", ex.getMessage());
			return form(model, request, id);
		}
		return "redirect:/periods";
	}

	@PreAuthorize("hasRole('DEAN')")
	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			periodService.deleteById(id);
			redirectAttributes.addFlashAttribute("message", "Đã xóa đợt đăng ký thành công");
		} catch (InvalidRegistrationPeriodException | RegistrationPeriodNotFoundException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		} catch (Exception ex) {
			redirectAttributes.addFlashAttribute("error",
					"Không thể xóa đợt đăng ký do đã có dữ liệu đề tài hoặc nhóm sinh viên liên kết");
		}
		return "redirect:/periods";
	}

	@PreAuthorize("hasRole('DEAN')")
	@PostMapping("/{id}/lock")
	public String lock(@PathVariable Long id) {
		periodService.setRegistrationLocked(id, true);
		return "redirect:/periods";
	}

	@PreAuthorize("hasRole('DEAN')")
	@PostMapping("/{id}/unlock")
	public String unlock(@PathVariable Long id) {
		periodService.setRegistrationLocked(id, false);
		return "redirect:/periods";
	}

	@ExceptionHandler(RegistrationPeriodNotFoundException.class)
	public String handleNotFound(RegistrationPeriodNotFoundException ex, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("error", ex.getMessage());
		return "redirect:/periods";
	}

	private String form(Model model, RegistrationPeriodRequest request, Long periodId) {
		model.addAttribute("period", request);
		model.addAttribute("periodId", periodId);
		model.addAttribute("periodTypes", PeriodType.values());
		return "periods/form";
	}
}
