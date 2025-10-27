package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import dto.MemberDTO;
import util.DBUtil;

public class UserDAOImpl implements UserDAO{
    @Override
    public MemberDTO login(String id, String pw) {
        String sql = """
                select * from users
                where id = ? and pass = ?
                """;
        Connection con = DBUtil.getConnect();
        PreparedStatement ptmt = null;
        ResultSet rs = null;
        MemberDTO user = null;
        try {
            ptmt = con.prepareStatement(sql);
            ptmt.setString(1,id);
            ptmt.setString(2,pw);

            rs = ptmt.executeQuery();
            if(rs.next()){
                user = new MemberDTO(rs.getInt("user_id"), rs.getString("id"),rs.getString("name"), rs.getString("card_id"),
                		rs.getString("pass"),rs.getInt("access_level"),rs.getInt("office_id"),rs.getString("vehicle_no"),rs.getBoolean("is_active"),
                		rs.getTimestamp("created_at"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }finally {
            DBUtil.close(rs,ptmt,con);
        }
        return user;
    }

    public MemberDTO getUserInfo(String userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection con = DBUtil.getConnect();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                MemberDTO dto = new MemberDTO();
                dto.setUserId(rs.getInt("user_id"));
                dto.setId(rs.getString("id"));
                dto.setName(rs.getString("name"));
                dto.setCardId(rs.getString("card_id"));
                dto.setVehicle_no(rs.getString("vehicle_no"));
                dto.setAccess_level(rs.getInt("access_level"));
                dto.setIsActive(rs.getBoolean("is_active"));
                dto.setCreated_at(rs.getTimestamp("created_at"));
                return dto;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public boolean updateVehicle(String id, String vehicleNo) {
        String checkSql = "SELECT vehicle_no FROM users WHERE id = ?";
        String updateSql = "UPDATE users SET vehicle_no = ? WHERE id = ?";
        
        try (Connection con = DBUtil.getConnect()) {
            // 현재 유저의 차량 유무 확인
            try (PreparedStatement check = con.prepareStatement(checkSql)) {
                check.setString(1, id);
    
                ResultSet rs = check.executeQuery();
                if (rs.next()) {
                    String currentVehicle = rs.getString("vehicle_no");
                    if (currentVehicle != null && !currentVehicle.trim().isEmpty()) {
                        System.out.println("이미 차량이 등록되어 있습니다. 기존 번호: " + currentVehicle);
                        return false;
                    }
                }
            }

            // 차량 등록 진행
            try (PreparedStatement update = con.prepareStatement(updateSql)) {
                update.setString(1, vehicleNo);
                update.setString(2, id);
                int rows = update.executeUpdate();
                if (rows > 0) {
                    System.out.println("🚗 차량 등록 성공!");
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

	
    @Override
    public boolean register(String id, String pw, String name) {
        String sql = "INSERT INTO users VALUES (null,?,?,'card', ?, 1, null,'123car3333',1,default)";
        try (Connection con = DBUtil.getConnect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, pw);
            
            int result = ps.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

}
