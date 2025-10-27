package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import dto.ParkingDashboardDTO;
import dto.ParkingSpaceDTO;
import dto.ParkingSummaryDTO;
import util.DBUtil;

public class AdminParkingDAOImpl implements AdminParkingDao {

	@Override
	public ParkingDashboardDTO getSystem() {
		ParkingDashboardDTO dto = new ParkingDashboardDTO();

		String sql1 = """
				    SELECT COUNT(*) AS total_users,
				           COUNT(CASE WHEN vehicle_no IS NOT NULL AND vehicle_no != '' THEN 1 END) AS registered
				   FROM users
				""";
		String sql2 = "SELECT COUNT(*) AS total_spaces, SUM(is_occupied=1) AS used_spaces FROM parking_space";

		Connection con = null;
		PreparedStatement ptmt1 = null;
		PreparedStatement ptmt2 = null;
		ResultSet rs1 = null;
		ResultSet rs2 = null;

		try {
			con = DBUtil.getConnect();

			ptmt1 = con.prepareStatement(sql1);
			rs1 = ptmt1.executeQuery();
			if (rs1.next()) {
				dto.setTotalUsers(rs1.getInt("total_users"));
				dto.setRegisteredVehicles(rs1.getInt("registered"));
			}

			ptmt2 = con.prepareStatement(sql2);
			rs2 = ptmt2.executeQuery();
			if (rs2.next()) {
				dto.setTotalSpaces(rs2.getInt("total_spaces"));
				dto.setUsedSpaces(rs2.getInt("used_spaces"));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DBUtil.close(rs1, ptmt1, null);
			DBUtil.close(rs2, ptmt2, con);
		}

		return dto;
	}

	@Override
	public List<ParkingSpaceDTO> getAllSpace() {
		String sql = """
				    SELECT s.space_id, s.location, s.is_occupied, s.last_update, u.name AS user_name
				    FROM parking_space s
				    LEFT JOIN parking_log p ON s.space_id = p.space_id AND p.out_time IS NULL
				    LEFT JOIN users u ON p.user_id = u.user_id
				    ORDER BY s.space_id
				""";

		List<ParkingSpaceDTO> list = new ArrayList<>();
		try (Connection con = DBUtil.getConnect();
				PreparedStatement ptmt = con.prepareStatement(sql);
				ResultSet rs = ptmt.executeQuery()) {

			while (rs.next()) {
				ParkingSpaceDTO dto = new ParkingSpaceDTO();
				dto.setSpaceId(rs.getInt("space_id"));
				dto.setLocation(rs.getString("location"));
				dto.setOccupied(rs.getBoolean("is_occupied"));
				dto.setLastUpdate(rs.getTimestamp("last_update"));
				dto.setCurrentUserName(rs.getString("user_name"));
				list.add(dto);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return list;
	}

	@Override
	public List<ParkingSummaryDTO> getUserParkingSummary() {
		String sql = """
				    SELECT
				        u.name,
				        u.vehicle_no,
				        MAX(p.in_time) AS last_in,
				        MAX(p.out_time) AS last_out,
				        COUNT(p.parking_id) AS total_logs,
				        IFNULL(SUM(p.duration_min), 0) AS total_minutes
				    FROM users u
				    LEFT JOIN parking_log p ON u.user_id = p.user_id
				    GROUP BY u.user_id
				    ORDER BY total_minutes DESC
				""";

		List<ParkingSummaryDTO> list = new ArrayList<>();
		try (Connection con = DBUtil.getConnect();
				PreparedStatement ptmt = con.prepareStatement(sql);
				ResultSet rs = ptmt.executeQuery()) {

			while (rs.next()) {
				ParkingSummaryDTO dto = new ParkingSummaryDTO();
				dto.setName(rs.getString("name"));
				dto.setVehicleNo(rs.getString("vehicle_no"));
				dto.setLastIn(String.valueOf(rs.getTimestamp("last_in")));
				dto.setLastOut(String.valueOf(rs.getTimestamp("last_out")));
				dto.setTotalLogs(rs.getInt("total_logs"));
				dto.setTotalMinutes(rs.getInt("total_minutes"));
				list.add(dto);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}	

		return list;
	}
}