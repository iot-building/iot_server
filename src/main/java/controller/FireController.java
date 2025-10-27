package controller;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

import dao.OfficeDAO;
import dto.EnvironmentDTO;
import dto.FireEventDTO;
import dto.MemberDTO;
import service.FireService;
import service.FireServiceImpl;
import util.TimeUtil;

public class FireController {
    private final FireService service = new FireServiceImpl();
    private final Scanner sc = new Scanner(System.in);

    private double tempThreshold = 60.0;
    private String smokeSensitivity = "HIGH";
    private boolean alarmOn = false;
    
    // 🔹 로그인된 사용자 권한에 따라 모드 분기
    public void handleFireMode(MemberDTO user) {
        int level = user.getAccess_level();
        if (level == 3) {
            handleBuildingAdminMode(user);
        } else if (level == 2) {
            handleFloorAdminMode(user);
        } else {
            handleUserFireMode(user);
        }
    }

    // 🔸 건물 전체 관리자
    private void handleBuildingAdminMode(MemberDTO user) {
    	while (true) {
            System.out.println("====== [화재 감지 모드 - 건물 관리자] ======");
            System.out.println("현재시간 : " + TimeUtil.now());
            System.out.println("----------------------------------");
            System.out.println("[시스템 상태]");
            EnvironmentDTO latest = service.getLatestData();
            if (latest != null) {
                System.out.printf("온도: %.1f ℃%n습도: %.1f %%\n", latest.getTemperature(), latest.getHumidity());
                System.out.printf("연기 감지: %s%n", latest.getGasLevel() > 300 ? "감지됨" : "정상");
                System.out.printf("경보 상태: %s%n", alarmOn ? "ON" : "OFF");
            } else {
                System.out.println("센서 데이터가 없습니다.");
            }
            System.out.println("----------------------------------");
            System.out.println("[기능 선택]");
            System.out.println("1. 실시간 모니터링");
            System.out.println("2. 화재 임계값 설정");
            System.out.println("3. 수동 경보 발생");
            System.out.println("4. 경보 해제 / 시스템 리셋");
            System.out.println("5. 로그 확인");
            System.out.println("6. 모드 종료");
            System.out.println("----------------------------------");
            System.out.print("선택 >> ");
            int choice = sc.nextInt(); sc.nextLine();

            switch (choice) {
                case 1 -> startMonitoring(user);
                case 2 -> configureThreshold();
                case 3 -> manualAlarm(user);
                case 4 -> resetSystem(user);
                case 5 -> printLogs();
                case 6 -> { 
                    System.out.println("화재 감지 모드를 종료합니다.\n관리자 메뉴로 돌아갑니다.");
                    return;
                }
                default -> System.out.println("❌ 잘못된 입력입니다.");
            }
        }
    }

    // ✅ 1️⃣ 실시간 모니터링
    private void startMonitoring(MemberDTO user) {
        System.out.println("[모니터링 시작 - 3초 간격으로 센서 데이터 확인 중...]");

        for (int i = 0; i < 5; i++) {
            EnvironmentDTO data = service.getLatestData();
            String smoke = data.getGasLevel() > 300 ? "감지됨" : "정상";
            System.out.printf("[%s] 온도: %.1f°C | 연기: %s%n", TimeUtil.now(), data.getTemperature(), smoke);

            if (data.getTemperature() > tempThreshold || data.getGasLevel() > 300) {
                triggerAlarm(user, data);
                break;
            }
            sleep(3000);
        }

        if (!alarmOn) System.out.println("모니터링이 종료되었습니다. 이상 없음.");
    }

    // ✅ 2️⃣ 임계값 설정
    private void configureThreshold() {
        System.out.println("현재 설정값:");
        System.out.printf(" - 온도 임계값 : %.1f°C%n", tempThreshold);
        System.out.printf(" - 연기 감지 : %s 민감도%n", smokeSensitivity);
        System.out.println("----------------------------------");
        System.out.print("새로운 온도 임계값 입력 >> ");
        tempThreshold = sc.nextDouble(); sc.nextLine();
        System.out.print("새로운 연기 민감도 설정 (LOW / MEDIUM / HIGH) >> ");
        smokeSensitivity = sc.nextLine().toUpperCase();
        System.out.println("설정이 저장되었습니다.");
        System.out.println("----------------------------------");
        System.out.printf("현재 설정:\n - 온도 임계값 : %.1f°C\n - 연기 감지 : %s%n",
                tempThreshold, smokeSensitivity);
    }

    // ✅ 3️⃣ 수동 경보 발생
    private void manualAlarm(MemberDTO user) {
        alarmOn = true;
        System.out.println("수동으로 경보를 발생시켰습니다.");
        System.out.println("🚨 부저 ON / 경보등 ON");
        System.out.println("[안내] 모든 사용자에게 화재 알림 전송 중...");
        System.out.println("----------------------------------");
        service.logEvent(user.getUserId(), 1,  "FIRE", "MANUAL_TRIGGER", "관리자 수동 경보 발생");
    }

    // ✅ 4️⃣ 시스템 리셋
    private void resetSystem(MemberDTO user) {
        if (!alarmOn) {
            System.out.println("현재 경보가 꺼져 있습니다.");
            return;
        }
        alarmOn = false;
        System.out.println("경보를 해제합니다...");
        System.out.println("[부저 OFF] [LED OFF]");
        System.out.println("시스템이 정상 모드로 복귀했습니다.");
        System.out.println("----------------------------------");
        service.logEvent(user.getUserId(), 1, "FIRE", "RESET", "경보 해제 및 시스템 복귀");
    }

    // ✅ 5️⃣ 로그 보기
    private void printLogs() {
        System.out.println("====== 화재 감지 로그 ======");
        List<FireEventDTO> logs = service.getFireLogs(); // ✅ DTO 리스트로 받음

        if (logs.isEmpty()) {
            System.out.println("기록된 로그가 없습니다.");
        } else {
            for (FireEventDTO e : logs) {
                System.out.printf("%s | user:%d | %s | %s%n",
                        e.getTimestamp(), e.getUserId(), e.getEventAction(), e.getNote());
            }
            System.out.println("=============================");
            System.out.printf("총 %d개의 로그가 저장되어 있습니다.%n", logs.size());
        }
    }

    // 🔥 알람 발생
    private void triggerAlarm(MemberDTO user, EnvironmentDTO data) {
        alarmOn = true;
        System.out.printf("%n🚨 [화재 감지!] 온도: %.1f°C, 연기 감지됨%n", data.getTemperature());
        System.out.println("부저 ON / 경보등 ON");
        System.out.println("----------------------------------");

        service.logEvent(user.getUserId(), 1, "FIRE", "AUTO_TRIGGER", "센서 감지로 인한 자동 경보 발생");

        while (alarmOn) {
            System.out.println("조치 옵션:");
            System.out.println("1. 경보 해제");
            System.out.println("2. 로그 보기");
            System.out.println("3. 계속 모니터링");
            System.out.print("선택 >> ");
            int opt = sc.nextInt(); sc.nextLine();
            switch (opt) {
                case 1 -> resetSystem(user);
                case 2 -> printLogs();
                case 3 -> System.out.println("계속 모니터링 중...");
                default -> System.out.println("❌ 잘못된 입력");
            }
            if (!alarmOn) break;
        }
    }

    // 유틸
    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }


 // 🔸 층 관리자 모드
    private void handleFloorAdminMode(MemberDTO user) {
    	OfficeDAO officeDAO = new OfficeDAO();
        int officeId = user.getOfficeId();
        int floorNo = officeDAO.getFloorByOfficeId(officeId);

        while (true) {
        	System.out.printf("====== [화재 감지 모드 - 층 관리자 | %d층] ======%n", floorNo);
            System.out.println("현재시간 : " + TimeUtil.now());
            System.out.println("----------------------------------");

            EnvironmentDTO latest = getLatestByOffice(officeId);
            if (latest != null) {
                System.out.printf("온도: %.1f ℃%n습도: %.1f %%\n", latest.getTemperature(), latest.getHumidity());
                System.out.printf("연기 감지: %s%n", latest.getGasLevel() > 300 ? "감지됨" : "정상");
                System.out.printf("경보 상태: %s%n", alarmOn ? "ON" : "OFF");
            } else {
                System.out.println("⚠️ 센서 데이터가 없습니다.");
            }

            System.out.println("----------------------------------");
            System.out.println("[기능 선택]");
            System.out.println("1. 내 층 실시간 모니터링");
            System.out.println("2. 내 층 화재 로그 보기");
            System.out.println("3. 모드 종료");
            System.out.println("----------------------------------");
            System.out.print("선택 >> ");
            int choice = sc.nextInt(); sc.nextLine();

            switch (choice) {
                case 1 -> startFloorMonitoring(user, officeId);
                case 2 -> printLogsByOffice(officeId);
                case 3 -> {
                    System.out.println("층 관리자 모드를 종료합니다.");
                    return;
                }
                default -> System.out.println("❌ 잘못된 입력입니다.");
            }
        }
    }

    // 🔸 일반 사용자 모드
    private void handleUserFireMode(MemberDTO user) {
        int officeId = user.getOfficeId();
        System.out.printf("====== [화재 감지 모드 - 일반 사용자 | 사무실 ID:%d] ======%n", officeId);
        System.out.println("현재시간 : " + TimeUtil.now());
        System.out.println("----------------------------------");

        EnvironmentDTO latest = getLatestByOffice(officeId);
        if (latest != null) {
            System.out.printf("현재 온도: %.1f ℃%n현재 습도: %.1f %%\n", latest.getTemperature(), latest.getHumidity());
            System.out.printf("가스 농도: %.1f ppm%n", latest.getGasLevel());
            System.out.printf("상태: %s%n", (latest.getTemperature() > 55 || latest.getGasLevel() > 300) ? "🚨 화재 위험" : "정상");
        } else {
            System.out.println("⚠️ 센서 데이터가 없습니다.");
        }

        System.out.println("----------------------------------");
        System.out.println("※ 일반 사용자는 조회만 가능합니다.");
        System.out.println("----------------------------------");
    }
    
 // 🔹 특정 층의 최신 데이터 1건만 반환
    private EnvironmentDTO getLatestByOffice(int officeId) {
        List<EnvironmentDTO> list = service.getRecentData(officeId);
        if (list == null || list.isEmpty()) return null;
        return list.get(0);
    }

    // 🔹 층 관리자 전용 실시간 모니터링
    private void startFloorMonitoring(MemberDTO user, int officeId) {
        System.out.printf("[층 %d 실시간 모니터링 시작 - 3초 간격]%n", officeId);
        for (int i = 0; i < 5; i++) {
            EnvironmentDTO data = getLatestByOffice(officeId);
            if (data == null) {
                System.out.println("⚠️ 센서 데이터가 없습니다.");
                break;
            }

            String smoke = data.getGasLevel() > 300 ? "감지됨" : "정상";
            System.out.printf("[%s] 온도: %.1f°C | 연기: %s%n", TimeUtil.now(), data.getTemperature(), smoke);

            if (data.getTemperature() > tempThreshold || data.getGasLevel() > 300) {
                fireAlert(user, data);
                break;
            }
            sleep(3000);
        }

        if (!alarmOn) System.out.println("모니터링 종료. 이상 없음.");
    }

    // 🔹 특정 층(office_id) 데이터 출력
    private void printEnvironmentData(int officeId) {
        List<EnvironmentDTO> list = service.getRecentData(officeId);
        if (list.isEmpty()) {
            System.out.println("⚠️ 환경 데이터가 없습니다.");
            return;
        }
        EnvironmentDTO latest = list.get(0);
        System.out.printf("현재 온도: %.1f℃, 습도: %.1f%%, 가스농도: %.2f%n",
                latest.getTemperature(), latest.getHumidity(), latest.getGasLevel());
    }

    // 🔹 전체 층 센서 데이터 보기 (건물 관리자 전용)
    private void printAllFloorsData() {
        service.getAllOfficeData().forEach((officeId, dataList) -> {
            if (!dataList.isEmpty()) {
                EnvironmentDTO latest = dataList.get(0);
                System.out.printf("[Office %d] 온도: %.1f°C | 가스: %.1f | 습도: %.1f%%%n",
                        officeId, latest.getTemperature(), latest.getGasLevel(), latest.getHumidity());
            }
        });
    }

    // 🔹 특정 층 모니터링
    private void monitorSingleOffice(MemberDTO user, int officeId) {
        List<EnvironmentDTO> dataList = service.getRecentData(officeId);
        for (EnvironmentDTO data : dataList) {
            if (data.getTemperature() > 55 || data.getGasLevel() > 300) {
                fireAlert(user, data);
            } else {
                System.out.printf("[정상] %.1f°C | 가스 %.1f | 측정시각 %s%n",
                        data.getTemperature(), data.getGasLevel(), data.getMeasuredAt());
            }
        }
    }

    // 🔹 전체 층 모니터링 (건물 관리자)
    private void monitorAllFloors(MemberDTO user) {
        service.getAllOfficeData().forEach((officeId, dataList) -> {
            if (!dataList.isEmpty()) {
                EnvironmentDTO data = dataList.get(0);
                if (data.getTemperature() > 55 || data.getGasLevel() > 300) {
                    fireAlert(user, data);
                } else {
                    System.out.printf("[층 %d] 정상 - %.1f°C | %.1fppm%n",
                            officeId, data.getTemperature(), data.getGasLevel());
                }
            }
        });
    }

    // 🔹 화재 감지시 event_log 기록
    private void fireAlert(MemberDTO user, EnvironmentDTO data) {
        System.out.printf("🚨 화재 감지됨! [층 %d] 온도: %.1f°C, 가스농도: %.1f%n",
                data.getDeviceId(), data.getTemperature(), data.getGasLevel());

        FireEventDTO event = new FireEventDTO(
                data.getDeviceId(),
                user.getUserId(),
                user.getOfficeId(),
                "FIRE",
                "ALERT",
                String.valueOf(data.getTemperature()),
                "자동 화재 감지됨",
                Timestamp.valueOf(LocalDateTime.now())
        );
        service.recordFireEvent(event);
    }

//    // 🔹 전체 로그
//    private void printLogs() {
//        System.out.println("=== 최근 화재 로그 (전체) ===");
//        service.getFireLogs().forEach(log ->
//                System.out.printf("[%s] 층:%d | %s | %s%n",
//                        log.getTimestamp(), log.getOfficeId(),
//                        log.getEventAction(), log.getNote())
//        );
//    }

    // 🔹 특정 층 로그
    private void printLogsByOffice(int officeId) {
        System.out.printf("=== 최근 화재 로그 (층 %d) ===%n", officeId);
        service.getFireLogsByOffice(officeId).forEach(log ->
                System.out.printf("[%s] %s | %s%n",
                        log.getTimestamp(), log.getEventAction(), log.getNote())
        );
    }
}