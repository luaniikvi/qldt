package project.qldt.model.council;

import project.qldt.model.topic.Topic;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "councils")
public class Council {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String name;

	@OneToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "topic_id", nullable = false, unique = true)
	private Topic topic;

	@OneToMany(mappedBy = "council", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@OrderBy("id")
	private List<CouncilMember> members = new ArrayList<>();

	public boolean isFullyScored() {
		return members.stream().allMatch(member -> member.getScore() != null);
	}

	public double averageScore() {
		long scored = members.stream().filter(member -> member.getScore() != null).count();
		if (scored == 0) {
			return 0;
		}
		double sum = members.stream().filter(member -> member.getScore() != null)
				.mapToDouble(CouncilMember::getScore)
				.sum();
		return Math.round((sum / scored) * 100.0) / 100.0;
	}

	public CouncilMember memberOf(Long lecturerId) {
		return members.stream()
				.filter(member -> member.getLecturer().getId().equals(lecturerId))
				.findFirst()
				.orElse(null);
	}

	public boolean isChair(Long lecturerId) {
		CouncilMember member = memberOf(lecturerId);
		return member != null && member.getRole() == CouncilRole.CHAIR;
	}
}