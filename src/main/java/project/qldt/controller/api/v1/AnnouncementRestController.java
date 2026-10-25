package project.qldt.controller.api.v1;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import project.qldt.dto.api.request.AnnouncementRequest;
import project.qldt.dto.api.response.AnnouncementResponse;
import project.qldt.model.announcement.Announcement;
import project.qldt.service.announcement.AnnouncementService;

@RestController
@RequestMapping("/api/v1/announcements")
public class AnnouncementRestController {

	private final AnnouncementService announcementService;

	public AnnouncementRestController(AnnouncementService announcementService) {
		this.announcementService = announcementService;
	}

	@GetMapping
	public List<AnnouncementResponse> list(@RequestParam(required = false) Integer limit) {
		if (limit == null) {
			return announcementService.findAll().stream().map(AnnouncementResponse::of).toList();
		}
		return announcementService.findRecent().stream().limit(Math.max(0, limit))
				.map(AnnouncementResponse::of).toList();
	}

	@GetMapping("/{id}")
	public AnnouncementResponse getOne(@PathVariable Long id) {
		return AnnouncementResponse.of(announcementService.findById(id));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping
	public ResponseEntity<AnnouncementResponse> create(@Valid @RequestBody AnnouncementRequest request) {
		Announcement announcement = announcementService.create(request.title(), request.content());
		return ResponseEntity.created(URI.create("/api/v1/announcements/" + announcement.getId()))
				.body(AnnouncementResponse.of(announcement));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/{id}")
	public AnnouncementResponse update(@PathVariable Long id, @Valid @RequestBody AnnouncementRequest request) {
		return AnnouncementResponse.of(announcementService.update(id, request.title(), request.content()));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		announcementService.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
