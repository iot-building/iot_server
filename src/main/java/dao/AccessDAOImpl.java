package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import dto.AccessLogDTO;
import util.DBUtil;

public class AccessDAOImpl implements AccessDAO {

    @Override
    public int getOfficeFloor(int OfficeId){
        String sql = "SELECT floor_no FROM offices WHERE office_id = ?";
        try (Connection conn = DBUtil.getConnect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, OfficeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("floor_no");
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
	// 자신의 floor_no에 해당하는 사무실 출입 가능
    @Override
    public boolean checkPermission(int userId, int targetOfficeId, int accessLevel) {
    	String sql = """
    	        SELECT u.office_id AS user_office, o.floor_no AS user_floor
    	        FROM users u
    	        LEFT JOIN offices o ON u.office_id = o.office_id
    	        WHERE u.user_id = ?
    	    """;

    	    try (Connection conn = DBUtil.getConnect();
    	         PreparedStatement ps = conn.prepareStatement(sql)) {

    	        ps.setInt(1, userId);
    	        ResultSet rs = ps.executeQuery();

    	        if (rs.next()) {
    	            int userOffice = rs.getInt("user_office");
    	            int userFloor = rs.getInt("user_floor");

    	            // 1️⃣ 전체 관리자: 모든 곳 출입 가능
    	            if (accessLevel == 3) return true;

    	            // 2️⃣ 층 관리자: 같은 층의 사무실이면 출입 가능
    	            if (accessLevel == 2) {
                        int targetFloor = getOfficeFloor(targetOfficeId);
                        return userFloor == targetFloor;
    	            }

    	            // 3️⃣ 일반 사용자: 자신의 사무실만 출입 가능
    	            if (accessLevel == 1) {
    	                return userOffice == targetOfficeId;
    	            }
    	        }

    	    } catch (SQLException e) {
    	        e.printStackTrace();
    	    }
    	    return false; // 기본적으로 출입 불가
    	}
    
    // 출입 시도 기록
    @Override
    public boolean recordAccessEvent(AccessLogDTO log) {
        String sql = """
            INSERT INTO event_log (device_id, user_id, office_id, event_type, event_action, value, timestamp, note)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection con = DBUtil.getConnect();
             PreparedStatement ptmt = con.prepareStatement(sql)) {
            ptmt.setInt(1, log.getDeviceId());
        	ptmt.setInt(2, log.getUserId());
        	ptmt.setInt(3, log.getOfficeId());
            ptmt.setString(4, log.getEventType());
            ptmt.setString(5, log.getEventAction());
            ptmt.setString(6, log.getValue());
            ptmt.setTimestamp(7, log.getTimestamp());
            ptmt.setString(8, log.getNote());
            ptmt.executeUpdate();
            return true;
        } catch (SQLException e) {
        	e.printStackTrace();
            return false;
        }
    }

    @Override
    public void showLogsByUser(int userId) {
        String sql = "SELECT event_id, event_action, timestamp, note FROM event_log WHERE user_id = ?";
        try (Connection con = DBUtil.getConnect();
             PreparedStatement ptmt = con.prepareStatement(sql)) {
            ptmt.setInt(1, userId);
            ResultSet rs = ptmt.executeQuery();
            System.out.println("\n[내 출입 로그]");
            while (rs.next()) {
                System.out.printf("%d | %s | %s | %s%n",
                        rs.getInt("event_id"),
                        rs.getString("event_action"),
                        rs.getTimestamp("timestamp"),
                        rs.getString("note"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void showLogsByOffice(int officeId) {
        String sql = "SELECT event_id, user_id, event_action, timestamp FROM event_log WHERE office_id = ?";
        try (Connection con = DBUtil.getConnect();
             PreparedStatement ptmt = con.prepareStatement(sql)) {
            ptmt.setInt(1, officeId);
            ResultSet rs = ptmt.executeQuery();
            System.out.println("\n[층 출입 로그]");
            while (rs.next()) {
                System.out.printf("%d | User:%d | %s | %s%n",
                        rs.getInt("event_id"),
                        rs.getInt("user_id"),
                        rs.getString("event_action"),
                        rs.getTimestamp("timestamp"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void showAllLogs() {
        String sql = "SELECT * FROM event_log ORDER BY timestamp DESC";
        try (Connection con = DBUtil.getConnect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\n[전체 출입 로그]");
            while (rs.next()) {
                System.out.printf("%d | user:%d | office:%d | %s | %s%n",
                        rs.getInt("event_id"),
                        rs.getInt("user_id"),
                        rs.getInt("office_id"),
                        rs.getString("event_action"),
                        rs.getTimestamp("timestamp"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void listDevicesByOffice(int officeId) {
        String sql = "SELECT name, type, status FROM devices WHERE office_id = ?";
        try (Connection con = DBUtil.getConnect();
             PreparedStatement ptmt = con.prepareStatement(sql)) {
            ptmt.setInt(1, officeId);
            ResultSet rs = ptmt.executeQuery();
            System.out.println("\n[층 장치 목록]");
            while (rs.next()) {
                System.out.printf("%s | %s | %s%n",
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("status"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void listAllDevices() {
        String sql = "SELECT name, type, status FROM devices";
        try (Connection con = DBUtil.getConnect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\n[전체 장치 목록]");
            while (rs.next()) {
                System.out.printf("%s | %s | %s%n",
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("status"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}