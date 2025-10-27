package mqtt;

import java.util.Arrays;
import java.util.List;

public class MqttSubClientParking {

    private final MqttManager mqttManager;

    public MqttSubClientParking() {
        this.mqttManager = new MqttManager();
    }

    public void start() {
        // MQTT 연결을 별도 스레드로 실행
        Thread mqttThread = new Thread(mqttManager);
        mqttThread.setDaemon(true);
        mqttThread.start();

        // 구독 리스너 등록
        mqttManager.addListener("1/parking/01/car", (topic, message) -> handleCarDetected(message));
        mqttManager.addListener("1/door/05/state", (topic, message) -> handleDoorState(message));
    }

    // 🚗 차량 감지 메시지 처리
    private void handleCarDetected(String payload) {
        String carNo = parseValue(payload, "car_no");
        System.out.println("🚗 차량 감지됨 → " + carNo);

        boolean authorized = checkCarRegistered(carNo);
        String status = authorized ? "authorized" : "unauthorized";
        String resultMsg = "{\"status\":\"" + status + "\"}";

        mqttManager.publish("1/parking/01/auth", resultMsg);
        System.out.println("📤 차량 인증 결과 전송 → " + status);
    }

    // 🚪 차단기 상태 메시지 처리
    private void handleDoorState(String payload) {
        String state = parseValue(payload, "state");
        System.out.println("🚪 차단기 상태 수신 → " + state.toUpperCase());
    }

    // ✅ JSON 문자열 파싱 (간단 버전)
    private String parseValue(String payload, String key) {
        try {
            int start = payload.indexOf(key);
            if (start == -1) return "unknown";
            int colon = payload.indexOf(':', start);
            int firstQuote = payload.indexOf('"', colon + 1);
            int secondQuote = payload.indexOf('"', firstQuote + 1);
            return payload.substring(firstQuote + 1, secondQuote);
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ✅ 차량 등록 여부 체크 (임시)
    private boolean checkCarRegistered(String carNo) {
        List<String> registeredCars = Arrays.asList("397로1075", "222나2222", "333다3333", "111가1111", "123가1234");
        return registeredCars.contains(carNo);
    }
}
