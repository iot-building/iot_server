package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import util.DBUtil;

public class OfficeDAO {

    public int getFloorByOfficeId(int officeId) {
        String sql = "SELECT floor_no FROM offices WHERE office_id = ?";
        try (Connection conn = DBUtil.getConnect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, officeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("floor_no");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // 찾지 못했을 때
    }
}