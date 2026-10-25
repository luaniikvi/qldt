package project.qldt.service;
import project.qldt.service.account.AccountService;
import project.qldt.service.period.RegistrationPeriodService;
import project.qldt.service.team.TeamService;
import project.qldt.support.Beans;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import project.qldt.dto.period.RegistrationPeriodRequest;
import project.qldt.exception.AccountNotFoundException;
import project.qldt.exception.InvalidTeamException;
import project.qldt.exception.RegistrationPeriodNotFoundException;
import project.qldt.exception.TeamNotFoundException;
import project.qldt.model.topic.InvitationStatus;
import project.qldt.model.period.PeriodType;
import project.qldt.model.period.RegistrationPeriod;
import project.qldt.model.account.Student;
import project.qldt.model.team.Team;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TeamServiceExtraTest {

	private AccountService accountService;
	private RegistrationPeriodService periodService;
	private TeamService teamService;

	private Student sv1;
	private Student sv2;
	private Student sv3;
	private Student sv4;
	private RegistrationPeriod period;

	@BeforeEach
	void setUp() {
		accountService = Beans.get(AccountService.class);
		periodService = Beans.get(RegistrationPeriodService.class);
		teamService = Beans.get(TeamService.class);

		sv1 = accountService.createStudent("123456", "SV 1", "sv1@qldt.local", "SV901");
		sv2 = accountService.createStudent("123456", "SV 2", "sv2@qldt.local", "SV902");
		sv3 = accountService.createStudent("123456", "SV 3", "sv3@qldt.local", "SV903");
		sv4 = accountService.createStudent("123456", "SV 4", "sv4@qldt.local", "SV904");

		RegistrationPeriodRequest pReq = new RegistrationPeriodRequest();
		pReq.setName("Đợt test");
		pReq.setType(PeriodType.COURSE_PROJECT);
		pReq.setTeacherRegisterStart(LocalDateTime.now().minusDays(2));
		pReq.setTeacherRegisterEnd(LocalDateTime.now().minusDays(1));
		pReq.setStudentRegisterStart(LocalDateTime.now());
		pReq.setStudentRegisterEnd(LocalDateTime.now().plusDays(5));
		pReq.setReportDeadline(LocalDateTime.now().plusDays(10).toLocalDate());
		period = periodService.create(pReq);
	}

	@Test
	void taoNhomVoiDotKhongTonTaiNemLoi() {
		assertThrows(RegistrationPeriodNotFoundException.class, () -> teamService.create(9999L, sv1));
	}

	@Test
	void moiThanhVienKiemTraCacRangBuoc() {
		Team team = teamService.create(period.getId(), sv1);

		assertThrows(InvalidTeamException.class, () -> teamService.invite(team.getId(), "SV902", sv2));
		assertThrows(InvalidTeamException.class, () -> teamService.invite(team.getId(), "SV901", sv1));
		assertThrows(AccountNotFoundException.class, () -> teamService.invite(team.getId(), "SV999", sv1));

		teamService.invite(team.getId(), "SV902", sv1);
		assertThrows(InvalidTeamException.class, () -> teamService.invite(team.getId(), "SV902", sv1));

		teamService.respondInvitation(team.getId(), sv2, true, null);
		assertThrows(InvalidTeamException.class, () -> teamService.invite(team.getId(), "SV902", sv1));
	}

	@Test
	void moiSinhVienDaCoNhomTrongCungDotBiChan() {
		teamService.create(period.getId(), sv1);
		Team team2 = teamService.create(period.getId(), sv2);

		assertThrows(InvalidTeamException.class, () -> teamService.invite(team2.getId(), "SV901", sv2));
	}

	@Test
	void phanHoiLoiMoiKiemTraCacTruongHop() {
		Team team = teamService.create(period.getId(), sv1);

		assertThrows(InvalidTeamException.class, () -> teamService.respondInvitation(team.getId(), sv2, true, null));

		teamService.invite(team.getId(), "SV902", sv1);
		assertThrows(InvalidTeamException.class, () -> teamService.respondInvitation(team.getId(), sv2, false, "  "));

		teamService.respondInvitation(team.getId(), sv2, false, "Lý do bận");
		assertEquals(InvitationStatus.REJECTED, team.getInvitations().get(0).getStatus());
		assertEquals("Lý do bận", team.getInvitations().get(0).getResponseReason());
	}

	@Test
	void chapNhanLoiMoiKhiNhomDaDuThanhVienBiChan() {
		Team team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV902", sv1);
		teamService.invite(team.getId(), "SV903", sv1);

		teamService.respondInvitation(team.getId(), sv2, true, null);

		Student sv5 = accountService.createStudent("123456", "SV 5", "sv5@qldt.local", "SV905");
		Student sv6 = accountService.createStudent("123456", "SV 6", "sv6@qldt.local", "SV906");
		Student sv7 = accountService.createStudent("123456", "SV 7", "sv7@qldt.local", "SV907");
		team.getMembers().add(sv5);
		team.getMembers().add(sv6);
		team.getMembers().add(sv7);
		assertEquals(5, team.getMembers().size());

		assertThrows(InvalidTeamException.class, () -> teamService.respondInvitation(team.getId(), sv3, true, null));
	}

	@Test
	void chapNhanLoiMoiKhiDaGiaNhapNhomKhacBiChan() {
		Team team1 = teamService.create(period.getId(), sv1);
		teamService.invite(team1.getId(), "SV902", sv1);

		Team team2 = teamService.create(period.getId(), sv3);
		teamService.invite(team2.getId(), "SV902", sv3);
		teamService.respondInvitation(team2.getId(), sv2, true, null);

		assertThrows(InvalidTeamException.class, () -> teamService.respondInvitation(team1.getId(), sv2, true, null));
	}

	@Test
	void chuyenNhomTruongKiemTraRangBuoc() {
		Team team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV902", sv1);
		teamService.respondInvitation(team.getId(), sv2, true, null);

		assertThrows(InvalidTeamException.class, () -> teamService.transferLeader(team.getId(), "SV902", sv2));
		assertThrows(AccountNotFoundException.class, () -> teamService.transferLeader(team.getId(), "SV999", sv1));
		assertThrows(InvalidTeamException.class, () -> teamService.transferLeader(team.getId(), "SV903", sv1));

		teamService.transferLeader(team.getId(), "SV902", sv1);
		assertEquals(sv2.getId(), team.getLeader().getId());
	}

	@Test
	void roiNhomKiemTraRangBuoc() {
		Team team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV902", sv1);
		teamService.respondInvitation(team.getId(), sv2, true, null);

		assertThrows(InvalidTeamException.class, () -> teamService.leave(team.getId(), sv1));
		assertThrows(InvalidTeamException.class, () -> teamService.leave(team.getId(), sv3));

		teamService.leave(team.getId(), sv2);
		assertEquals(1, team.getMembers().size());
	}

	@Test
	void huyLoiMoiKiemTraRangBuoc() {
		Team team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV902", sv1);

		assertThrows(InvalidTeamException.class, () -> teamService.cancelInvitation(team.getId(), "SV902", sv2));
		assertThrows(InvalidTeamException.class, () -> teamService.cancelInvitation(team.getId(), "SV903", sv1));

		teamService.cancelInvitation(team.getId(), "SV902", sv1);
		assertEquals(InvitationStatus.CANCELLED, team.getInvitations().get(0).getStatus());
	}

	@Test
	void xoaThanhVienKiemTraRangBuoc() {
		Team team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV902", sv1);
		teamService.respondInvitation(team.getId(), sv2, true, null);

		assertThrows(InvalidTeamException.class, () -> teamService.removeMember(team.getId(), "SV902", sv2));
		assertThrows(InvalidTeamException.class, () -> teamService.removeMember(team.getId(), "SV901", sv1));
		assertThrows(InvalidTeamException.class, () -> teamService.removeMember(team.getId(), "SV903", sv1));

		teamService.removeMember(team.getId(), "SV902", sv1);
		assertEquals(1, team.getMembers().size());
	}

	@Test
	void timKiemNhomCuaSinhVienVaTheoTopicId() {
		Team team = teamService.create(period.getId(), sv1);
		teamService.invite(team.getId(), "SV902", sv1);

		List<Team> sv1Teams = teamService.findByStudent(sv1);
		List<Team> sv2Teams = teamService.findByStudent(sv2);
		List<Team> sv3Teams = teamService.findByStudent(sv3);

		assertEquals(1, sv1Teams.size());
		assertEquals(1, sv2Teams.size());
		assertEquals(0, sv3Teams.size());

		assertNull(teamService.findByTopicId(1234L));
	}

	@Test
	void timKiemTheoIdKhongTonTaiNemLoi() {
		assertThrows(TeamNotFoundException.class, () -> teamService.findById(9999L));
	}
}
