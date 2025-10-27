package dao;

import java.util.List;
import java.util.Map;

import dto.EnvironmentDTO;
import dto.FireEventDTO;

public interface FireDAO {
    List<EnvironmentDTO> getRecentEnvironmentData(int officeId);
    Map<Integer, List<EnvironmentDTO>> getAllOfficesEnvironmentData();
    void insertFireEvent(FireEventDTO event);
    List<FireEventDTO> getRecentFireLogs();
    List<FireEventDTO> getFireLogsByOffice(int officeId);
}