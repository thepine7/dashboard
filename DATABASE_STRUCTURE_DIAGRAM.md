# HnT Sensor API 데이터베이스 구조도

## 📊 데이터베이스 개요

- **데이터베이스명**: `hnt`
- **엔진**: MySQL 5.7.9
- **서버**: hntsolution.co.kr:3306
- **총 테이블 수**: 25개 (2025-10-01 기준)
- **업데이트일**: 2025-10-01

## 🏗️ 핵심 테이블 구조

### 1. **hnt_sensor_data** (메인 센서 데이터 테이블)

```sql
CREATE TABLE hnt_sensor_data (
    no INT(8) NOT NULL AUTO_INCREMENT COMMENT '번호',
    user_id VARCHAR(100) NOT NULL COMMENT '사용자아이디',
    sensor_id VARCHAR(100) NOT NULL COMMENT '센서아이디',
    uuid VARCHAR(100) DEFAULT NULL COMMENT '고유아이디',
    sensor_type VARCHAR(20) DEFAULT NULL COMMENT '센서유형',
    sensor_value VARCHAR(50) NOT NULL COMMENT '센서측정값',
    topic VARCHAR(300) DEFAULT NULL COMMENT '토픽정보',
    raw_data VARCHAR(500) DEFAULT NULL COMMENT '원본데이터',
    inst_id VARCHAR(50) NOT NULL COMMENT '입력자아이디',
    inst_dtm DATETIME NOT NULL COMMENT '입력일시',
    mdf_id VARCHAR(50) NOT NULL COMMENT '수정자아이디',
    mdf_dtm DATETIME NOT NULL COMMENT '수정일시',
    PRIMARY KEY (no)
);
```

**인덱스:**
- `idx_hnt_sensor_data_uuid_inst_dtm` (uuid, inst_dtm) - 장치별 시간 범위 조회용
- `idx_hnt_sensor_data_user_id_uuid` (user_id, uuid) - 사용자별 장치 조회용
- `idx_sensor_data_performance` (user_id, sensor_id, uuid, inst_dtm) - 성능 최적화용

### 2. **hnt_sensor_info** (센서 정보 테이블)

```sql
CREATE TABLE hnt_sensor_info (
    no INT(8) NOT NULL AUTO_INCREMENT COMMENT '번호',
    user_id VARCHAR(100) NOT NULL COMMENT '사용자아이디',
    sensor_id VARCHAR(100) NOT NULL COMMENT '센서아이디',
    sensor_uuid VARCHAR(100) NOT NULL COMMENT '센서고유아이디',
    sensor_loc VARCHAR(300) DEFAULT NULL COMMENT '센서위치',
    sensor_type VARCHAR(100) DEFAULT NULL COMMENT '센서유형',
    sensor_gu VARCHAR(100) DEFAULT NULL COMMENT '센서구분',
    sensor_name VARCHAR(100) DEFAULT NULL COMMENT '센서명',
    chart_type VARCHAR(100) DEFAULT NULL COMMENT '차트유형',
    inst_id VARCHAR(50) NOT NULL COMMENT '입력자아이디',
    inst_dtm DATETIME NOT NULL COMMENT '입력일시',
    mdf_id VARCHAR(100) NOT NULL COMMENT '수정자아이디',
    mdf_dtm DATETIME NOT NULL COMMENT '수정일시',
    temp_high VARCHAR(100) DEFAULT NULL COMMENT '고온알림설정값',
    temp_low VARCHAR(100) DEFAULT NULL COMMENT '저온알림설정값',
    temp_high_alarm_yn CHAR(1) DEFAULT NULL COMMENT '고온알림여부',
    temp_low_alarm_yn CHAR(1) DEFAULT NULL COMMENT '저온알림여부',
    PRIMARY KEY (no)
);
```

**인덱스:**
- `idx_sensor_info_user_sensor` (user_id, sensor_id) - 사용자별 센서 조회용

### 3. **hnt_user** (사용자 정보 테이블)

```sql
CREATE TABLE hnt_user (
    no INT(8) NOT NULL AUTO_INCREMENT COMMENT '번호',
    user_nm VARCHAR(100) NOT NULL COMMENT '사용자명',
    user_tel VARCHAR(13) DEFAULT NULL COMMENT '전화번호',
    user_email VARCHAR(300) DEFAULT NULL COMMENT '이메일',
    user_id VARCHAR(100) NOT NULL COMMENT '사용자아이디',
    main_user_id VARCHAR(50) DEFAULT NULL COMMENT '메인사용자아이디',
    user_pass VARCHAR(300) NOT NULL COMMENT '비밀번호',
    topic_id VARCHAR(100) DEFAULT NULL COMMENT '토픽아이디',
    user_grade VARCHAR(100) NOT NULL COMMENT '사용자등급',
    use_yn CHAR(1) NOT NULL COMMENT '사용여부',
    del_yn CHAR(1) NOT NULL COMMENT '삭제여부',
    inst_id VARCHAR(50) NOT NULL COMMENT '입력자아이디',
    inst_dtm DATETIME NOT NULL COMMENT '입력일시',
    mdf_id VARCHAR(50) DEFAULT NULL COMMENT '수정자아이디',
    mdf_dtm DATETIME NOT NULL COMMENT '수정일시',
    token VARCHAR(500) DEFAULT NULL COMMENT '토큰',
    last_login_dtm CHAR(1) NOT NULL DEFAULT 'N' COMMENT '마지막로그인일시',
    logout_dtm CHAR(1) NOT NULL DEFAULT 'N' COMMENT '로그아웃일시',
    user_token VARCHAR(500) DEFAULT NULL COMMENT '사용자토큰',
    parent_user_id VARCHAR(50) DEFAULT NULL COMMENT '부모사용자아이디',
    PRIMARY KEY (no),
    UNIQUE KEY (user_id)
);
```

### 4. **hnt_config** (센서 설정 테이블)

```sql
CREATE TABLE hnt_config (
    no INT(11) NOT NULL AUTO_INCREMENT COMMENT '번호',
    user_id VARCHAR(100) NOT NULL COMMENT '사용자아이디',
    sensor_id VARCHAR(100) NOT NULL COMMENT '센서아이디',
    sensor_uuid VARCHAR(100) NOT NULL COMMENT '센서고유아이디',
    alarm_yn1 CHAR(1) DEFAULT NULL COMMENT '알림여부1',
    set_val1 VARCHAR(10) DEFAULT NULL COMMENT '설정값1',
    delay_time1 INT(11) DEFAULT NULL COMMENT '지연시간1',
    alarm_yn2 CHAR(1) DEFAULT NULL COMMENT '알림여부2',
    set_val2 VARCHAR(10) DEFAULT NULL COMMENT '설정값2',
    delay_time2 INT(11) DEFAULT NULL COMMENT '지연시간2',
    alarm_yn3 CHAR(1) DEFAULT NULL COMMENT '알림여부3',
    set_val3 VARCHAR(10) DEFAULT NULL COMMENT '설정값3',
    delay_time3 INT(11) DEFAULT NULL COMMENT '지연시간3',
    alarm_yn4 CHAR(1) DEFAULT NULL COMMENT '알림여부4',
    set_val4 VARCHAR(10) DEFAULT NULL COMMENT '설정값4',
    delay_time4 INT(11) DEFAULT NULL COMMENT '지연시간4',
    alarm_yn5 CHAR(1) DEFAULT NULL COMMENT '알림여부5',
    delay_time5 INT(11) DEFAULT NULL COMMENT '지연시간5',
    inst_id VARCHAR(100) DEFAULT NULL COMMENT '입력자아이디',
    inst_dtm DATETIME DEFAULT NULL COMMENT '입력일시',
    mdf_id VARCHAR(100) DEFAULT NULL COMMENT '수정자아이디',
    mdf_dtm DATETIME DEFAULT NULL COMMENT '수정일시',
    re_delay_time1 INT(11) DEFAULT NULL COMMENT '재지연시간1',
    re_delay_time2 INT(11) DEFAULT NULL COMMENT '재지연시간2',
    re_delay_time3 INT(11) DEFAULT NULL COMMENT '재지연시간3',
    re_delay_time4 INT(11) DEFAULT NULL COMMENT '재지연시간4',
    re_delay_time5 INT(11) DEFAULT NULL COMMENT '재지연시간5',
    topic VARCHAR(300) DEFAULT NULL COMMENT '토픽정보',
    PRIMARY KEY (no)
);
```

### 5. **hnt_alarm** (알림 데이터 테이블)

```sql
CREATE TABLE hnt_alarm (
    no INT(11) NOT NULL AUTO_INCREMENT COMMENT '번호',
    user_id VARCHAR(100) NOT NULL COMMENT '사용자아이디',
    user_token VARCHAR(500) DEFAULT NULL COMMENT '사용자토큰',
    sensor_uuid VARCHAR(100) DEFAULT NULL COMMENT '센서고유아이디',
    alarm_time DATETIME DEFAULT NULL COMMENT '알림시간',
    inst_id VARCHAR(100) DEFAULT NULL COMMENT '입력자아이디',
    inst_dtm DATETIME DEFAULT NULL COMMENT '입력일시',
    mdf_id VARCHAR(100) DEFAULT NULL COMMENT '수정자아이디',
    mdf_dtm DATETIME DEFAULT NULL COMMENT '수정일시',
    siren_yn VARCHAR(1) DEFAULT NULL COMMENT '사이렌여부',
    alarm_type VARCHAR(100) DEFAULT NULL COMMENT '알림유형',
    cur_temp VARCHAR(100) DEFAULT NULL COMMENT '현재온도',
    in_temp VARCHAR(100) DEFAULT NULL COMMENT '내부온도',
    urgent_yn CHAR(1) DEFAULT NULL COMMENT '긴급여부',
    release_yn CHAR(1) DEFAULT 'N' COMMENT '해제여부',
    PRIMARY KEY (no)
);
```

### 6. **hnt_user_relation** (사용자 관계 테이블)

```sql
CREATE TABLE hnt_user_relation (
    id INT(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    main_user_id VARCHAR(50) NOT NULL COMMENT '메인사용자아이디',
    sub_user_id VARCHAR(50) NOT NULL COMMENT '하위사용자아이디',
    inst_id VARCHAR(20) DEFAULT 'hnt' COMMENT '입력자아이디',
    inst_dtm DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    mdf_id VARCHAR(20) DEFAULT 'hnt' COMMENT '수정자아이디',
    mdf_dtm DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (id)
);
```

## 🔗 테이블 관계도

```
hnt_user (사용자)
    ├── hnt_user_relation (사용자 관계)
    │   ├── main_user_id → hnt_user.user_id
    │   └── sub_user_id → hnt_user.user_id
    ├── hnt_sensor_info (센서 정보)
    │   ├── user_id → hnt_user.user_id
    │   └── sensor_uuid → hnt_sensor_data.uuid
    ├── hnt_config (센서 설정)
    │   ├── user_id → hnt_user.user_id
    │   └── sensor_uuid → hnt_sensor_data.uuid
    ├── hnt_alarm (알림 데이터)
    │   ├── user_id → hnt_user.user_id
    │   └── sensor_uuid → hnt_sensor_data.uuid
    └── hnt_sensor_data (센서 데이터)
        ├── user_id → hnt_user.user_id
        └── uuid → hnt_sensor_info.sensor_uuid
```

## 📊 사용자별 센서 데이터 테이블

### 동적 생성 테이블들
- `hnt_sensor_data_[사용자ID]_[센서UUID]` - 사용자별 센서 데이터
- 예시:
  - `hnt_sensor_data_thepine_0008DC755575`
  - `hnt_sensor_data_poscom_0008DC755D10`
  - `hnt_sensor_data_pieter_0008DC751D9A`

## 🚀 인덱스 최적화 현황

### **hnt_sensor_data 테이블 인덱스**
| 인덱스명 | 컬럼 | 타입 | 용도 | 카디널리티 |
|---------|------|------|------|-----------|
| **idx_hnt_sensor_data_uuid_inst_dtm** | uuid, inst_dtm | 복합 | 장치별 시간 범위 조회용 | 33, 34,290,536 |
| **idx_hnt_sensor_data_user_id_uuid** | user_id, uuid | 복합 | 사용자별 장치 조회용 | 19, 36 |
| **idx_sensor_data_performance** | user_id, sensor_id, uuid, inst_dtm | 복합 | 성능 최적화용 | 19, 19, 36, 34,290,536 |
| **hnt_sensor_data_uuid_IDX** | uuid | 단일 | 장치별 조회용 | 33 |
| **hnt_sensor_data_inst_dtm_IDX** | inst_dtm | 단일 | 시간 범위 조회용 | 17,145,268 |

### **hnt_sensor_info 테이블 인덱스**
| 인덱스명 | 컬럼 | 타입 | 용도 |
|---------|------|------|------|
| **idx_sensor_info_user_sensor** | user_id, sensor_id | 복합 | 사용자별 센서 조회용 |

## 📈 데이터 현황 (2025-10-01 기준)

| 테이블명 | 레코드 수 | 주요 용도 |
|---------|----------|----------|
| **hnt_sensor_data** | 37,088,429 | 메인 센서 데이터 |
| **hnt_user** | 41 | 사용자 정보 |
| **hnt_sensor_info** | 39 | 센서 정보 |
| **hnt_config** | 31 | 센서 설정 |
| **hnt_alarm** | 0 | 알림 데이터 |
| **hnt_user_relation** | - | 사용자 관계 |

## 🔧 쿼리 최적화 가이드

### **기존 인덱스 활용 우선**
- ✅ **새로운 인덱스 생성 금지**: 기존 인덱스를 최대한 활용
- ✅ **복합 인덱스 우선**: `idx_hnt_sensor_data_uuid_inst_dtm` (uuid, inst_dtm) 활용
- ✅ **인덱스 힌트 사용 금지**: MySQL 쿼리 플래너가 자동으로 최적 인덱스 선택

### **쿼리 작성 예시**
```sql
-- ✅ 올바른 예시: 기존 인덱스 활용
SELECT 
    date_format(inst_dtm, '%Y-%m-%d') as getDate,
    date_format(inst_dtm, '%Y-%m-%d %H') as inst_dtm,
    round(avg(sensor_value), 1) as sensor_value
FROM hnt_sensor_data
WHERE uuid = 'test_uuid'
AND inst_dtm BETWEEN '2025-10-01 00:00:00' AND '2025-10-01 23:59:59'
GROUP BY date_format(inst_dtm, '%Y-%m-%d'), date_format(inst_dtm, '%Y-%m-%d %H')
ORDER BY getDate ASC, inst_dtm ASC
LIMIT 200;
```

## 🔄 업데이트 이력

- **2025-10-01**: 데이터베이스 구조도 초기 작성
- **2025-10-01**: 인덱스 최적화 현황 추가
- **2025-10-01**: 쿼리 최적화 가이드 추가
- **2025-10-01**: 사용자별 센서 데이터 테이블 현황 추가
