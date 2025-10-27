package service;

import java.util.List;
import java.util.Map;

import dto.EnvironmentDTO;
import dto.FireEventDTO;

public interface FireService {
    List<EnvironmentDTO> getRecentData(int officeId);
    Map<Integer, List<EnvironmentDTO>> getAllOfficeData(); // 🔹 전체 층용
    void recordFireEvent(FireEventDTO event);
    List<FireEventDTO> getFireLogs();
    List<FireEventDTO> getFireLogsByOffice(int officeId); // 🔹 층 관리자용
    EnvironmentDTO getLatestData();  // 🔹 최신 센서 데이터 1개 가져오기
    void logEvent(int userId, int deviceId, String type, String action, String note);  // 🔹 이벤트 기록
}
