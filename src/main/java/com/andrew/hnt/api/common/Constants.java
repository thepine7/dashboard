package com.andrew.hnt.api.common;

/**
 * 공통 상수 클래스
 * HnT Sensor API 프로젝트 전용
 */
public class Constants {
    
    // ==================== MQTT 관련 상수 ====================
    public static final class Mqtt {
        public static final String SERVER = "tcp://hntsolution.co.kr:1883";
        public static final String USERNAME = "hnt1";
        public static final String PASSWORD = "abcde";
        public static final String CLIENT_ID_PREFIX = "hnt-sensor-api";
        public static final String TOPIC_WILDCARD = "#";
        
        // 메시지 타입
        public static final String MESSAGE_TYPE_SET = "SET&";
        public static final String MESSAGE_TYPE_GET = "GET&";
        public static final String MESSAGE_TYPE_ACT = "ACT&";
        
        // 액션 타입
        public static final String ACTION_FORCEDEF = "forcedef";
        public static final String ACTION_OUTPUT = "output";
        public static final String ACTION_USER_ID = "userId";
        
        // 연결 설정
        public static final int CONNECTION_TIMEOUT = 30;
        public static final int KEEP_ALIVE_INTERVAL = 60;
        public static final int MAX_RECONNECT_ATTEMPTS = 5;
        public static final int BASE_RECONNECT_DELAY = 2000;
        public static final int MAX_RECONNECT_DELAY = 30000;
    }
    
    // ==================== 포트 번호 상수 ====================
    public static final class Ports {
        public static final int MQTT_PORT = 1883;
        public static final int TOMCAT1_PORT = 8080;
        public static final int TOMCAT2_PORT = 8888;
    }
    
    // ==================== 사용자 권한 상수 ====================
    public static final class UserGrade {
        public static final String ADMIN = "A";
        public static final String USER = "U";
        public static final String SUB_USER = "B";
    }
    
    // ==================== 사용자 등급 상수 (기존 호환성) ====================
    public static final String USER_GRADE_ADMIN = "A";
    public static final String USER_GRADE_USER = "U";
    public static final String USER_GRADE_SUB = "B";
    
    // ==================== 세션 속성 키 ====================
    public static final class SessionKeys {
        public static final String USER_ID = "userId";
        public static final String USER_NM = "userNm";
        public static final String USER_GRADE = "userGrade";
        public static final String USER_EMAIL = "userEmail";
        public static final String USER_TEL = "userTel";
        public static final String LOGIN_USER_ID = "loginUserId";
        public static final String SENSOR_ID = "sensorId";
    }
    
    // ==================== 세션 속성 키 (Constants 전용) ====================
    public static final String SESSION_USER_ID = "SESSION_USER_ID";
    public static final String SESSION_USER_NM = "SESSION_USER_NM";
    public static final String SESSION_USER_GRADE = "SESSION_USER_GRADE";
    public static final String SESSION_USER_EMAIL = "SESSION_USER_EMAIL";
    public static final String SESSION_USER_TEL = "SESSION_USER_TEL";
    public static final String SESSION_LOGIN_USER_ID = "SESSION_LOGIN_USER_ID";
    public static final String SESSION_SENSOR_ID = "SESSION_SENSOR_ID";
    public static final String SESSION_TOKEN = "SESSION_TOKEN";
    
    // ==================== HTTP 상태 코드 ====================
    public static final class HttpStatus {
        public static final String SUCCESS = "200";
        public static final String BAD_REQUEST = "400";
        public static final String UNAUTHORIZED = "401";
        public static final String FORBIDDEN = "403";
        public static final String NOT_FOUND = "404";
        public static final String INTERNAL_SERVER_ERROR = "500";
    }
    
    // ==================== 센서 관련 상수 ====================
    public static final class Sensor {
        public static final String ACT_CODE_LIVE = "live";
        public static final String ACT_CODE_SETRES = "setres";
        public static final String ACT_CODE_ACTRES = "actres";
        public static final String ERROR_VALUE = "Error";
        
        // 센서 타입
        public static final String TYPE_TEMP = "temp";
        public static final String TYPE_HUMIDITY = "humidity";
        public static final String TYPE_PRESSURE = "pressure";
        public static final String TYPE_CO2 = "co2";
        public static final String TYPE_PT100 = "pt100";
        public static final String TYPE_4_20MA = "4-20ma";
        public static final String TYPE_ALARM = "alarm";
        public static final String TYPE_H_L = "h/l";
        public static final String TYPE_COUNTER = "counter";
        public static final String TYPE_FREQ = "freq";
        
        // 센서 값 범위
        public static final double MIN_TEMP_VALUE = -50.0;
        public static final double MAX_TEMP_VALUE = 125.0;
        
        // 센서 상태
        public static final String STATUS_NORMAL = "normal";
        public static final String STATUS_ERROR = "error";
        public static final String STATUS_OFFLINE = "offline";
        public static final String STATUS_MAINTENANCE = "maintenance";
        
    }
    
    // ==================== 센서 타입 (기존 호환성) ====================
    public static final String SENSOR_TYPE_TEMPERATURE = "temp";
    public static final String SENSOR_TYPE_HUMIDITY = "humidity";
    public static final String SENSOR_TYPE_PRESSURE = "pressure";
    public static final String SENSOR_TYPE_CO2 = "co2";
    public static final String SENSOR_TYPE_PT100 = "pt100";
    public static final String SENSOR_TYPE_4_20MA = "4-20ma";
    
    // ==================== 센서 상태 (기존 호환성) ====================
    public static final String SENSOR_STATUS_NORMAL = "normal";
    public static final String SENSOR_STATUS_ERROR = "error";
    public static final String SENSOR_STATUS_OFFLINE = "offline";
    public static final String SENSOR_STATUS_MAINTENANCE = "maintenance";
    
    // ==================== 에러 메시지 ====================
    public static final class ErrorMessages {
        public static final String SESSION_EXPIRED = "세션이 만료되었습니다.";
        public static final String PERMISSION_DENIED = "권한이 없습니다.";
        public static final String DATA_NOT_FOUND = "데이터를 찾을 수 없습니다.";
        public static final String SENSOR_NOT_FOUND = "센서를 찾을 수 없습니다.";
        public static final String SENSOR_SETTING_EMPTY = "센서 설정 데이터가 없습니다.";
        public static final String MQTT_CONNECTION_FAILED = "MQTT 연결에 실패했습니다.";
        public static final String INTERNAL_ERROR = "내부 서버 오류가 발생했습니다.";
    }
    
    // ==================== 성공 메시지 ====================
    public static final class SuccessMessages {
        public static final String OPERATION_SUCCESS = "작업이 성공적으로 완료되었습니다.";
        public static final String DATA_SAVED = "데이터가 저장되었습니다.";
        public static final String SENSOR_SETTING_SAVED = "센서 설정이 저장되었습니다.";
    }
    
    // ==================== 정규식 패턴 ====================
    public static final class Patterns {
        public static final String USER_ID = "^[a-zA-Z0-9_-]{3,20}$";
        public static final String UUID = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        public static final String EMAIL = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        public static final String PHONE = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$";
        public static final String MQTT_TOPIC = "^HBEE/[^/]+/[^/]+/[^/]+/(DEV|SER)$";
        
        // XSS 방지 패턴
        public static final String SCRIPT_TAG = "<script[^>]*>.*?</script>";
        public static final String JAVASCRIPT_PROTOCOL = "javascript:";
        public static final String ONLOAD_ATTRIBUTE = "onload\\s*=";
        public static final String ONERROR_ATTRIBUTE = "onerror\\s*=";
        public static final String ONCLICK_ATTRIBUTE = "onclick\\s*=";
        
        // SQL Injection 방지 패턴
        public static final String SQL_INJECTION = "(union|select|insert|delete|update|drop|alter|exec|execute)";
        public static final String SQL_COMMENT = "--";
        public static final String SQL_QUOTE = "['\"]";
    }
    
    // ==================== 정규식 패턴 (기존 호환성) ====================
    public static final String EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    public static final String PHONE_PATTERN = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$";
    public static final String USERNAME_PATTERN = "^[a-zA-Z0-9_-]{3,20}$";
    public static final String PASSWORD_PATTERN = "^.{6,20}$";
    
    // ==================== 시간 관련 상수 ====================
    public static final class Time {
        public static final long SECOND = 1000;
        public static final long MINUTE = 60 * SECOND;
        public static final long HOUR = 60 * MINUTE;
        public static final long SESSION_TIMEOUT = 30 * MINUTE;
    }
    
    // ==================== 파일 경로 ====================
    public static final class Paths {
        public static final String STATIC_CSS = "/css/";
        public static final String STATIC_JS = "/js/";
        public static final String STATIC_IMAGES = "/images/";
        public static final String REDIRECT_LOGIN = "redirect:/login/login";
        public static final String REDIRECT_MAIN = "redirect:/main/main";
    }
    
    // ==================== 응답 코드 (기존 호환성) ====================
    public static final String SUCCESS_CODE = "200";
    public static final String VALIDATION_ERROR_CODE = "400";
    public static final String AUTHENTICATION_ERROR_CODE = "401";
    public static final String AUTHORIZATION_ERROR_CODE = "403";
    public static final String NOT_FOUND_ERROR_CODE = "404";
    public static final String DATABASE_ERROR_CODE = "500";
    public static final String MQTT_ERROR_CODE = "502";
    
    // ==================== 페이지네이션 (기존 호환성) ====================
    public static final int DEFAULT_PAGE_NUMBER = 1;
    public static final int MAX_PAGE_NUMBER = 1000;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;
    
    // ==================== 정렬 (기존 호환성) ====================
    public static final String SORT_ASC = "asc";
    public static final String SORT_DESC = "desc";
    
    // ==================== 알림 타입 (기존 호환성) ====================
    public static final String ALARM_TYPE_TEMPERATURE = "temperature";
    public static final String ALARM_TYPE_HUMIDITY = "humidity";
    public static final String ALARM_TYPE_CONNECTION = "connection";
    public static final String ALARM_TYPE_MAINTENANCE = "maintenance";
    
    // ==================== 알림 레벨 (기존 호환성) ====================
    public static final String ALARM_LEVEL_INFO = "info";
    public static final String ALARM_LEVEL_WARNING = "warning";
    public static final String ALARM_LEVEL_ERROR = "error";
    public static final String ALARM_LEVEL_CRITICAL = "critical";
}