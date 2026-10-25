package project.qldt.service;
import project.qldt.service.account.AccountService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.team.TeamService;
import project.qldt.support.Beans;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.exception.InvalidTeamException;
import project.qldt.model.topic.InvitationStatus;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.account.Student;
import project.qldt.model.team.Team;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TeamServiceTest {

	private final AccountService accountService = Beans.get(AccountService.class);
	private final RegistrationPeriodService periodService = Beans.get(RegistrationPeriodService.class);
	private final TeamService teamService = Beans.get(TeamService.class);

	private Student sv1;
	private Student sv2;
	private Student sv3;
	private Student sv4;
	private Student sv5;
	private Student sv6;
	private RegistrationPeriod period;

	@BeforeEach
	void setUp() {
		sv1 = createStudent("SV901");
		sv2 = createStudent("SV902");
		sv3 = createStudent("SV903");
		sv4 = createStudent("SV904");
		sv5 = createStudent("SV905");
		sv6 = createStudent("SV906");
		period = createPeriod(LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
	}

	@Test
	void taoNhomTrongThoiGianSinhVienDangKy() {
		Team team = teamService.create(period.getId(), sv1);

		assertEquals(sv1.getId(), team.getLeader().getId());
		assertEquals(1, team.getMembers().size());
	}

	@Test
	void taoNhomNgoaiThoiGianSinhVienDangKyVanDuoc() {
		RegistrationPeriod past = createPeriod(LocalDateTime.now().minusDays(7), LocalDateTime.now().minusDays(6));

		Team team = teamService.create(past.getId(), sv1);

		assertEquals(1, team.getMembers().size());
	}

	@Test
	void taoVaMoiThanhVienKhiDotBiKhoaVanDuoc() {
		periodService.setRegistrationLocked(period.getId(), true);

		Team team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV902", sv1);
		teamService.respondInvitation(team.getId(), sv2, true, null);

		assertEquals(2, team.getMembers().size());
	}

	@Test
	void moiVaChapNhanLoiMoi() {
		Team team = teamService.create(period.getId(), sv1);

		teamService.invite(team.getId(), "SV902", sv1);
		assertEquals(1, team.getMembers().size());
		assertEquals(InvitationStatus.PENDING, team.getInvitations().get(0).getStatus());

		teamService.respondInvitation(team.getId(), sv2, true, null);

		assertEquals(2, team.getMembers().size());
		assertEquals(InvitationStatus.ACCEPTED, team.getInvitations().get(0).getStatus());
	}

	@Test
	void toiDaNamThanhVien() {
		Team team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV902", sv1);
		teamService.invite(team.getId(), "SV903", sv1);
		teamService.invite(team.getId(), "SV904", sv1);
		teamService.invite(team.getId(), "SV905", sv1);

		assertThrows(InvalidTeamException.class, () -> teamService.invite(team.getId(), "SV906", sv1));
	}

	@Test
	void motSinhVienChiThuocMotNhomTrongDot() {
		teamService.create(period.getId(), sv1);

		assertThrows(InvalidTeamException.class, () -> teamService.create(period.getId(), sv1));
	}

	@Test
	void sinhVienDaCoNhomKhongDuocMoi() {
		teamService.create(period.getId(), sv2);
		Team team = teamService.create(period.getId(), sv1);

		assertThrows(InvalidTeamException.class, () -> teamService.invite(team.getId(), "SV902", sv1));
	}

	@Test
	void tuChoiLoiMoiCanNhapLyDo() {
		Team team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV902", sv1);

		assertThrows(InvalidTeamException.class,
				() -> teamService.respondInvitation(team.getId(), sv2, false, " "));

		teamService.respondInvitation(team.getId(), sv2, false, "Bận việc");

		assertEquals(InvitationStatus.REJECTED, team.getInvitations().get(0).getStatus());
		assertEquals("Bận việc", team.getInvitations().get(0).getResponseReason());
		assertEquals(1, team.getMembers().size());
	}

	@Test
	void chuyenQuyenNhomTruong() {
		Team team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV902", sv1);
		teamService.respondInvitation(team.getId(), sv2, true, null);

		teamService.transferLeader(team.getId(), "SV902", sv1);

		assertEquals(sv2.getId(), team.getLeader().getId());
	}

	@Test
	void nhomTruongKhongDuocRoiNhom() {
		Team team = teamService.create(period.getId(), sv1);

		assertThrows(InvalidTeamException.class, () -> teamService.leave(team.getId(), sv1));
	}

	@Test
	void thanhVienRoiNhom() {
		Team team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV902", sv1);
		teamService.respondInvitation(team.getId(), sv2, true, null);

		teamService.leave(team.getId(), sv2);

		assertEquals(1, team.getMembers().size());
	}

	@Test
	void huyLoiMoiVaoNhomVaMoiLai() {
		Team team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV902", sv1);

		teamService.cancelInvitation(team.getId(), "SV902", sv1);

		assertEquals(InvitationStatus.CANCELLED, team.getInvitations().get(0).getStatus());

		teamService.invite(team.getId(), "SV902", sv1);
		assertEquals(2, team.getInvitations().size());
	}

	@Test
	void chiNhomTruongMoiDuocHuyLoiMoi() {
		Team team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV902", sv1);

		assertThrows(InvalidTeamException.class,
				() -> teamService.cancelInvitation(team.getId(), "SV902", sv2));
	}

	@Test
	void xoaThanhVienKhoiNhom() {
		Team team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV902", sv1);
		teamService.respondInvitation(team.getId(), sv2, true, null);

		teamService.removeMember(team.getId(), "SV902", sv1);

		assertEquals(1, team.getMembers().size());
		assertEquals(InvitationStatus.CANCELLED, team.getInvitations().get(0).getStatus());
	}

	@Test
	void khongTheXoaNhomTruong() {
		Team team = teamService.create(period.getId(), sv1);

		assertThrows(InvalidTeamException.class, () -> teamService.removeMember(team.getId(), "SV901", sv1));
	}

	private Student createStudent(String code) {
		return accountService.createStudent("123456", "SV " + code, code.toLowerCase() + "@qldt.local", code);
	}

	private RegistrationPeriod createPeriod(LocalDateTime studentStart, LocalDateTime studentEnd) {
		RegistrationPeriodRequest request = new RegistrationPeriodRequest();
		request.setName("Đợt sinh viên");
		request.setType(PeriodType.COURSE_PROJECT);
		request.setTeacherRegisterStart(studentStart.minusDays(5));
		request.setTeacherRegisterEnd(studentStart.minusDays(2));
		request.setStudentRegisterStart(studentStart);
		request.setStudentRegisterEnd(studentEnd);
		request.setReportDeadline(studentEnd.plusDays(3).toLocalDate());
		return periodService.create(request);
	}
}
