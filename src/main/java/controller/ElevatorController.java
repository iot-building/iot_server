package controller;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.AccessDAOImpl;
import dto.MemberDTO;
import mqtt.MqttManager;
import mqtt.devices.ELVHandler;
import service.ElevatorService;
import service.ElevatorServiceImpl;
import view.ElevatorUI;

public class ElevatorController {
    private MemberDTO loginUser;
    private ElevatorUI view;
    private ElevatorService evService;
    private MqttManager mqttManager;
    // ✅ 1. 응답 데이터를 담을 Thread-safe Queue 생성
    private final BlockingQueue<String> responseQueue;
    private int last_floor = 1;

    public ElevatorController(MemberDTO loginUser, MqttManager mqttManager) {
        this.loginUser = loginUser;
        this.view = new ElevatorUI();
        this.mqttManager = mqttManager;
        this.evService = new ElevatorServiceImpl(loginUser,mqttManager);
        this.responseQueue = new LinkedBlockingQueue<>();
        // ✅ 2. 자신의 핸들러를 생성하고, 큐를 전달
        ELVHandler handler = new ELVHandler(this.responseQueue);

        // ✅ 3. 자신의 토픽을 MqttManager에 리스너로 직접 등록!
        // officeId, deviceId 등은 설정 파일이나 loginUser 정보로부터 가져올 수 있습니다.
        String officeId = "1"; // 예시
        String deviceId = "1"; // 예시
        String stateTopic = officeId + "/elevator/+/state";
        this.mqttManager.addListener(stateTopic, handler);
    }
    // 관리자 권한으로 로그인 했을 때 사용되는 메서드
    public void adminAccess(){
        while (true){
            int input = view.adminUI();
            // 권한 관리 메뉴
            if(input == 1){
                authController();
            }
            // 조작 메뉴
            else if (input == 2) {
                stateController();
            }
            else{
                System.out.println("페이지를 종료합니다.");
                break;
            }
        }
    }
    // 사용자로 로그인했을 때 실행되는 메서드
    public void userAccess(){
        while(true){
            int input = view.userUI();
            // 엘리베이터 호출
            if(input == 1){
                stateSelect();
                int floor = view.floorControl();
                boolean hasAccessLevel = checkUserAuth(floor);
                if(hasAccessLevel){
                    evService.callEVFloor(last_floor,floor);
                    last_floor = floor;
                }
            }
            else{
                System.out.println("페이지를 종료합니다.");
                break;
            }
        }
    }

    public void authController() {
        // 엘리베이터의 권한이란 어떤 권한인가?
        // => 결론: access_level: 1-2 인 경우, 해당 office_id 에 대해서 office_id 의 floor 에 대한 엘리베이터 권한을 갖는 것임.
        // 3인 경우, 모든 floor 에 대한 엘리베이터 권한을 가짐.
        int input = view.authUI();
        switch (input){
            case 1: // 권한 목록 조회 (어떻게 보여줄건지?)

                break;
        }
    }

    public void stateController() {
        int input = view.stateUI();
        switch (input){
            case 1: //엘리베이터 상태 조회
                stateSelect();
                break;
            case 2: //엘리베이터 이용 상태 변경
                String result = view.stateSTOP().toLowerCase();
                if(result.equals("y")){
                    System.out.println("엘리베이터 상태를 이용 가능으로 변경합니다.");
                    evService.setEVEnable(true);
                } else if (result.equals("n")) {
                    System.out.println("엘리베이터 상태를 이용 불가로 변경합니다.");
                    System.out.println("이제 엘리베이터를 이용할 수 없습니다.");
                    evService.setEVEnable(false);
                }
                break;
            case 3: //엘리베이터 위치 제어(원격 제어)  //1-3 로직은 모두 MQTT 통신과 관련된 기능
                stateSelect();
                int floor = view.floorControl();
                boolean hasAccessLevel = checkUserAuth(floor);
                if(hasAccessLevel){
                    evService.callEVFloor(last_floor,floor);
                    last_floor = floor;
                }
                break;
            case 4: //엘리베이터 통계 리포트 (DB로그 확인)
                evService.showELVLog();
                break;
        }
    }
    private void stateSelect(){
        System.out.println("엘리베이터 상태 정보를 요청합니다...");
        evService.requestEVState();
        // 메인 스레드가 바로 실행되는 것을 방지하기 위해 잠시 대기
        try {
            // 스레드가 연결될 시간을 잠시 줍니다.
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // ✅ 큐에 데이터가 있는지 먼저 확인!
        checkMqttResponse();
    }
    public boolean checkUserAuth(int floor){
        boolean isAbleToCall = true;
        System.out.println(loginUser.getName()+"님의 권한 확인 결과:");
        switch (loginUser.getAccess_level()){
            case 1:
            case 2:
                AccessDAOImpl accessDAO = new AccessDAOImpl();
                int officeFloor = accessDAO.getOfficeFloor(loginUser.getOfficeId());
                if(officeFloor != floor){
                    System.out.println(floor+"층은 접근할 권한이 없습니다.");
                    System.out.println("이전 화면으로 되돌아갑니다.");
                    return false;
                }
        }
        System.out.println("권한 확인되었습니다. 엘리베이터를 호출합니다.");
        return true;
    }
    private void checkMqttResponse() {
        String response = responseQueue.poll();
        if (response != null) {
            // 여기서 Gson 등을 사용해 JSON 문자열을 객체로 변환할 수 있습니다.
             JsonObject obj = new Gson().fromJson(response, JsonObject.class);
             int floor = obj.get("from_floor").getAsInt();
             String actionStr = obj.get("action").getAsString();

             switch (actionStr){
                 case "enable":
                 case "disable":
                     boolean action = actionStr.equals("enable") ? true:false;
                     view.stateSELECT(1,obj.get("from_floor").getAsInt(),action);
                     break;
                 case "arrive":
                     view.stateARRIVE(obj.get("to_floor").getAsInt());
                     break;

             }
        }
    }
}
