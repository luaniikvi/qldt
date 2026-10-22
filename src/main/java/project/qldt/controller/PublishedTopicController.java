package project.qldt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import project.qldt.service.TopicService;

@Controller
public class PublishedTopicController {

	private final TopicService topicService;

	public PublishedTopicController(TopicService topicService) {
		this.topicService = topicService;
	}

	@GetMapping("/published-topics")
	public String list(Model model) {
		model.addAttribute("topics", topicService.findPublished());
		return "topics/published";
	}
}
