package service;

import java.util.List;
import java.util.Map;

import dao.FireDAO;
import dao.FireDAOImpl;
import dto.EnvironmentDTO;
import dto.FireEventDTO;

public class FireServiceImpl implements FireService {
    private final FireDAO dao = new FireDAOImpl();

    @Override
    public List<EnvironmentDTO> getRecentData(int officeId) {
        return dao.getRecentEnvironmentData(officeId);
    }

    @Override
    public Map<Integer, List<EnvironmentDTO>> getAllOfficeData() {
        return dao.getAllOfficesEnvironmentData();
    }

    @Override
    public void recordFireEvent(FireEventDTO event) {
        dao.insertFireEvent(event);
    }

    @Override
    public List<FireEventDTO> getFireLogs() {
        return dao.getRecentFireLogs();
    }

    @Override
    public List<FireEventDTO> getFireLogsByOffice(int officeId) {
        return dao.getFireLogsByOffice(officeId);
    }
    
    @Override
    public EnvironmentDTO getLatestData() {
        // 임시로 officeId=1 기준 데이터 1개 가져오기
        List<EnvironmentDTO> list = dao.getRecentEnvironmentData(1);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public void logEvent(int userId, int deviceId, String type, String action, String note) {
        FireEventDTO event = new FireEventDTO();
        event.setUserId(userId);
        event.setDeviceId(deviceId);
        event.setEventType(type);
        event.setEventAction(action);
        event.setNote(note);
        event.setTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
        dao.insertFireEvent(event);
    }
}