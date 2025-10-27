package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dto.EnvironmentDTO;
import dto.FireEventDTO;
import util.DBUtil;

public class FireDAOImpl implements FireDAO {

    @Override
    public List<EnvironmentDTO> getRecentEnvironmentData(int officeId) {
        List<EnvironmentDTO> list = new ArrayList<>();
        String sql = """
            SELECT e.* FROM environment_data e
            JOIN devices d ON e.device_id = d.device_id
            WHERE d.office_id = ?
            ORDER BY e.measured_at
        """;
        try (Connection con = DBUtil.getConnect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, officeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new EnvironmentDTO(
                        rs.getInt("record_id"),
                        rs.getInt("device_id"),
                        rs.getFloat("temperature"),
                        rs.getFloat("humidity"),
                        rs.getFloat("gas_level"),
                        rs.getTimestamp("measured_at")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Map<Integer, List<EnvironmentDTO>> getAllOfficesEnvironmentData() {
        Map<Integer, List<EnvironmentDTO>> map = new HashMap<>();
        String sql = """
            SELECT d.office_id, e.* FROM environment_data e
            JOIN devices d ON e.device_id = d.device_id
            ORDER BY e.measured_at DESC
        """;
        try (Connection con = DBUtil.getConnect();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int officeId = rs.getInt("office_id");
                EnvironmentDTO dto = new EnvironmentDTO(
                        rs.getInt("record_id"),
                        rs.getInt("device_id"),
                        rs.getFloat("temperature"),
                        rs.getFloat("humidity"),
                        rs.getFloat("gas_level"),
                        rs.getTimestamp("measured_at")
                );
                map.computeIfAbsent(officeId, k -> new ArrayList<>()).add(dto);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public void insertFireEvent(FireEventDTO event) {
        String sql = """
            INSERT INTO event_log (device_id, user_id, office_id, event_type, event_action, value, note, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection con = DBUtil.getConnect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, event.getDeviceId());
            ps.setInt(2, event.getUserId());
            ps.setInt(3, event.getOfficeId());
            ps.setString(4, event.getEventType());
            ps.setString(5, event.getEventAction());
            ps.setString(6, event.getValue());
            ps.setString(7, event.getNote());
            ps.setTimestamp(8, event.getTimestamp());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<FireEventDTO> getRecentFireLogs() {
        List<FireEventDTO> logs = new ArrayList<>();
        String sql = "SELECT * FROM event_log WHERE event_type='FIRE' ORDER BY timestamp DESC LIMIT 10";
        try (Connection con = DBUtil.getConnect();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                logs.add(extractEvent(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    @Override
    public List<FireEventDTO> getFireLogsByOffice(int officeId) {
        List<FireEventDTO> logs = new ArrayList<>();
        String sql = "SELECT * FROM event_log WHERE event_type='FIRE' AND office_id=? ORDER BY timestamp DESC LIMIT 10";
        try (Connection con = DBUtil.getConnect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, officeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                logs.add(extractEvent(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    private FireEventDTO extractEvent(ResultSet rs) throws SQLException {
        return new FireEventDTO(
                rs.getInt("device_id"),
                rs.getInt("user_id"),
                rs.getInt("office_id"),
                rs.getString("event_type"),
                rs.getString("event_action"),
                rs.getString("value"),
                rs.getString("note"),
                rs.getTimestamp("timestamp")
        );
    }
}