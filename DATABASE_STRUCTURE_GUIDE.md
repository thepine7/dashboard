# HnT Sensor API 데이터베이스 구조 가이드

## 📊 데이터베이스 개요

### 🗄️ 데이터베이스 정보
- **데이터베이스명**: `hnt`
- **엔진**: MySQL 5.7.9
- **총 테이블 수**: 25개 (2025-10-01 기준)
- **총 데이터 크기**: 약 11.1GB (데이터 5.4GB + 인덱스 5.7GB)
- **총 레코드 수**: 37,088,429건

### 📈 테이블별 현황 (2025-10-01 업데이트)

| 테이블명 | 레코드 수 | 데이터 크기 | 인덱스 크기 | 총 크기 | 비고 |
|---------|----------|------------|------------|---------|------|
| **hnt_sensor_data** | 37,088,429 | 5.4GB | 5.7GB | 11.1GB | 메인 센서 데이터 |
| **hnt_user** | 41 | 14.6KB | 9.0KB | 23.6KB | 사용자 정보 |
| **hnt_sensor_info** | 39 | 3.5KB | 17.0KB | 20.5KB | 센서 정보 |
| **hnt_config** | 31 | 3.1KB | 19.0KB | 22.1KB | 센서 설정 |
| **hnt_alarm** | 0 | 652B | 13.0KB | 13.7KB | 알림 데이터 |
| **hnt_user_relation** | - | - | - | - | 사용자 관계 |

### 📊 사용자별 센서 데이터 테이블 (동적 생성)

| 테이블명 | 레코드 수 | 데이터 크기 | 인덱스 크기 | 총 크기 | 비고 |
|---------|----------|------------|------------|---------|------|
| hnt_sensor_data_thepine7_0008DC7553A4 | 279,597 | 41.6MB | 24.8MB | 66.4MB | 사용자별 센서 데이터 |
| hnt_sensor_data_thepine_0008DC755575 | 281,282 | 41.8MB | 24.4MB | 66.2MB | 사용자별 센서 데이터 |
| hnt_sensor_data_thepine_0008DC7550EE | 280,359 | 41.7MB | 24.3MB | 66.0MB | 사용자별 센서 데이터 |
| hnt_sensor_data_poscom_0008DC755D10 | 276,086 | 40.0MB | 23.4MB | 63.4MB | 사용자별 센서 데이터 |
| hnt_sensor_data_pieter_0008DC751D9A | 202,307 | 29.3MB | 17.1MB | 46.4MB | 사용자별 센서 데이터 |
| hnt_sensor_data_thepine_0008DC7514BC | 133,082 | 19.8MB | 11.5MB | 31.3MB | 사용자별 센서 데이터 |

---

## 🚀 데이터베이스 쿼리 최적화 가이드

### 📊 기존 인덱스 현황 (2025-10-01 확인)

#### **hnt_sensor_data 테이블 인덱스**
| 인덱스명 | 컬럼 | 타입 | 용도 | 카디널리티 |
|---------|------|------|------|-----------|
| **idx_hnt_sensor_data_uuid_inst_dtm** | uuid, inst_dtm | 복합 | 장치별 시간 범위 조회용 복합 인덱스 | 33, 34,290,536 |
| **idx_hnt_sensor_data_user_id_uuid** | user_id, uuid | 복합 | 사용자별 장치 조회용 복합 인덱스 | 19, 36 |
| **idx_sensor_data_performance** | user_id, sensor_id, uuid, inst_dtm | 복합 | 성능 최적화용 복합 인덱스 | 19, 19, 36, 34,290,536 |
| **hnt_sensor_data_uuid_IDX** | uuid | 단일 | 장치별 조회용 | 33 |
| **hnt_sensor_data_inst_dtm_IDX** | inst_dtm | 단일 | 시간 범위 조회용 | 17,145,268 |

#### **hnt_sensor_info 테이블 인덱스**
| 인덱스명 | 컬럼 | 타입 | 용도 |
|---------|------|------|------|
| **idx_sensor_info_user_sensor** | user_id, sensor_id | 복합 | 사용자별 센서 조회용 |
| **hnt_sensor_info_sensor_id_IDX** | sensor_id | 단일 | 센서별 조회용 |

### 🔧 쿼리 최적화 규칙

#### **1. 기존 인덱스 활용 우선**
- ✅ **새로운 인덱스 생성 금지**: 기존 인덱스를 최대한 활용
- ✅ **복합 인덱스 우선**: `idx_hnt_sensor_data_uuid_inst_dtm` (uuid, inst_dtm) 활용
- ✅ **인덱스 힌트 사용 금지**: MySQL 쿼리 플래너가 자동으로 최적 인덱스 선택

#### **2. MyBatis XML 쿼리 작성 규칙**
```xml
<!-- ✅ 올바른 예시: 기존 인덱스 활용 -->
<select id="selectSensorData" parameterType="java.util.HashMap" resultType="java.util.HashMap">
    select
        date_format(inst_dtm, '%Y-%m-%d') as getDate
        , date_format(inst_dtm, '%Y-%m-%d %H') as inst_dtm
        , round(avg(sensor_value), 1) as sensor_value
    from hnt_sensor_data
    where uuid = #{sensorUuid}
    and inst_dtm between #{startDateTime} and #{endDateTime}
    group by date_format(inst_dtm, '%Y-%m-%d'), date_format(inst_dtm, '%Y-%m-%d %H')
    order by getDate asc, inst_dtm asc
    limit 200
</select>

<!-- ❌ 잘못된 예시: 인덱스 힌트 사용 -->
<select id="selectSensorData" parameterType="java.util.HashMap" resultType="java.util.HashMap">
    select ...
    from hnt_sensor_data USE INDEX (idx_sensor_data_uuid_time)  <!-- 금지 -->
    where ...
</select>
```

#### **3. 쿼리 성능 확인 방법**
```sql
-- 쿼리 실행 계획 확인
EXPLAIN SELECT 
    date_format(inst_dtm, '%Y-%m-%d') as getDate,
    date_format(inst_dtm, '%Y-%m-%d %H') as inst_dtm,
    round(avg(sensor_value), 1) as sensor_value
FROM hnt_sensor_data
WHERE uuid = 'test_uuid'
AND inst_dtm BETWEEN '2025-10-01 00:00:00' AND '2025-10-01 23:59:59'
GROUP BY date_format(inst_dtm, '%Y-%m-%d'), date_format(inst_dtm, '%Y-%m-%d %H')
ORDER BY getDate ASC, inst_dtm ASC
LIMIT 200;

-- 기존 인덱스 확인
SHOW INDEX FROM hnt_sensor_data;
```

#### **4. 쿼리 최적화 체크리스트**
- [ ] **기존 인덱스 활용**: 새 인덱스 생성 대신 기존 인덱스 활용
- [ ] **복합 인덱스 우선**: 단일 인덱스보다 복합 인덱스 활용
- [ ] **WHERE 절 순서**: 인덱스 컬럼 순서에 맞춰 WHERE 절 작성
- [ ] **인덱스 힌트 금지**: USE INDEX, FORCE INDEX 등 사용 금지
- [ ] **EXPLAIN 확인**: 쿼리 실행 계획으로 인덱스 사용 여부 확인

---

## 🏗️ 핵심 테이블 구조

### 1. **hnt_user** (사용자 정보 테이블)

#### 📋 컬럼 구조
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

#### 🔑 인덱스
- **PRIMARY KEY**: `no`
- **UNIQUE KEY**: `user_id`
- **INDEX**: `parent_user_id` (부모 사용자 관계용)

### 2. **hnt_sensor_data** (메인 센서 데이터 테이블)

#### 📋 컬럼 구조
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

#### 🔍 주요 인덱스
- **PRIMARY**: `no` (AUTO_INCREMENT)
- **idx_hnt_sensor_data_uuid_inst_dtm**: `(uuid, inst_dtm)` - 장치별 시간 범위 조회용
- **idx_hnt_sensor_data_user_id_uuid**: `(user_id, uuid)` - 사용자별 장치 조회용
- **idx_hnt_sensor_data_inst_dtm**: `(inst_dtm)` - 시간 범위 조회용
- **idx_sensor_data_download_date_range**: `(inst_dtm, user_id, uuid)` - 다운로드용
- **idx_sensor_data_performance**: `(user_id, sensor_id, uuid, inst_dtm)` - 성능 최적화용

#### 📊 데이터 특성
- **데이터 유형**: 실시간 센서 측정값 (온도, 습도, 압력 등)
- **수신 주기**: 5초마다 수신
- **저장 주기**: 1분마다 또는 값 변경 시
- **데이터 보존**: 24개월 (2023-11-09 ~ 2025-09-29)

### 3. **hnt_sensor_info** (센서 정보 테이블)

#### 📋 컬럼 구조
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

#### 🔑 인덱스
- **PRIMARY KEY**: `no`
- **INDEX**: `idx_sensor_info_user_sensor` (user_id, sensor_id) - 사용자별 센서 조회용

### 4. **hnt_config** (센서 설정 테이블)

#### 📋 컬럼 구조
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

#### 📋 컬럼 구조
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

#### 📋 컬럼 구조
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

#### 🔑 인덱스
- **PRIMARY KEY**: `id`
- **INDEX**: `main_user_id` (메인 사용자 관계용)

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

### 3. **hnt_user** (사용자 테이블)

#### 📋 컬럼 구조
```sql
CREATE TABLE hnt_user (
    no INT(8) NOT NULL AUTO_INCREMENT COMMENT '번호-사용자고유아이디',
    user_nm VARCHAR(100) NOT NULL COMMENT '사용자명',
    user_tel VARCHAR(13) DEFAULT NULL COMMENT '사용자 전화번호',
    user_email VARCHAR(300) DEFAULT NULL COMMENT '사용자 메일주소',
    user_id VARCHAR(100) NOT NULL COMMENT '사용자 아이디',
    main_user_id VARCHAR(50) DEFAULT NULL COMMENT '주계정아이디',
    user_pass VARCHAR(300) NOT NULL COMMENT '사용자 비밀번호',
    topic_id VARCHAR(100) DEFAULT NULL COMMENT '토픽아이디',
    user_grade VARCHAR(100) NOT NULL COMMENT '사용자 등급 - A : Admin / U : User',
    use_yn CHAR(1) NOT NULL COMMENT '사용 여부',
    del_yn CHAR(1) NOT NULL COMMENT '삭제 여부',
    inst_id VARCHAR(50) NOT NULL COMMENT '입력자 아이디',
    inst_dtm DATETIME NOT NULL COMMENT '입력일시',
    mdf_id VARCHAR(50) DEFAULT NULL COMMENT '수정자 아이디',
    mdf_dtm DATETIME NOT NULL COMMENT '수정 일시',
    token VARCHAR(500) DEFAULT NULL COMMENT '앱 토큰',
    last_login_dtm CHAR(1) NOT NULL DEFAULT 'N' COMMENT '마지막 로그인 시간',
    logout_dtm CHAR(1) NOT NULL DEFAULT 'N' COMMENT '로그아웃 시간',
    user_token VARCHAR(500) DEFAULT NULL COMMENT 'FCM 사용자 토큰',
    parent_user_id VARCHAR(50) DEFAULT NULL COMMENT '부모 사용자 ID (부계정인 경우)',
    PRIMARY KEY (no)
);
```

#### 🔍 주요 인덱스
- **PRIMARY**: `no` (AUTO_INCREMENT)
- **hnt_user_UN**: `user_id` - UNIQUE
- **idx_user_parent_del**: `(parent_user_id, del_yn)` - 부계정 관계 조회용

#### 📊 데이터 특성
- **사용자 등급**: A(관리자), U(일반사용자), B(부계정)
- **부계정 관계**: `parent_user_id`로 주계정-부계정 관계 관리
- **총 사용자**: 41명

### 4. **hnt_config** (센서 설정 테이블)

#### 📋 컬럼 구조
```sql
CREATE TABLE hnt_config (
    no INT(11) NOT NULL AUTO_INCREMENT COMMENT '번호',
    user_id VARCHAR(100) NOT NULL COMMENT '사용자아이디',
    sensor_id VARCHAR(100) NOT NULL COMMENT '센서아이디',
    sensor_uuid VARCHAR(100) NOT NULL COMMENT '센서고유아이디',
    alarm_yn1 CHAR(1) DEFAULT NULL COMMENT '알람사용여부1',
    set_val1 VARCHAR(10) DEFAULT NULL COMMENT '알람설정값1',
    delay_time1 INT(11) DEFAULT NULL COMMENT '알람지연시간1',
    alarm_yn2 CHAR(1) DEFAULT NULL COMMENT '알람사용여부2',
    set_val2 VARCHAR(10) DEFAULT NULL COMMENT '알람설정값2',
    delay_time2 INT(11) DEFAULT NULL COMMENT '알람지연시간2',
    alarm_yn3 CHAR(1) DEFAULT NULL COMMENT '알람사용여부3',
    set_val3 VARCHAR(10) DEFAULT NULL COMMENT '알람설정값3',
    delay_time3 INT(11) DEFAULT NULL COMMENT '알람지연시간3',
    alarm_yn4 CHAR(1) DEFAULT NULL COMMENT '알람사용여부4',
    set_val4 VARCHAR(10) DEFAULT NULL COMMENT '알람설정값4',
    delay_time4 INT(11) DEFAULT NULL COMMENT '알람지연시간4',
    alarm_yn5 CHAR(1) DEFAULT NULL COMMENT '알람사용여부5',
    delay_time5 INT(11) DEFAULT NULL COMMENT '알람지연시간5',
    inst_id VARCHAR(100) DEFAULT NULL COMMENT '입력자아이디',
    inst_dtm DATETIME DEFAULT NULL COMMENT '입력일시',
    mdf_id VARCHAR(100) DEFAULT NULL COMMENT '수정자아이디',
    mdf_dtm DATETIME DEFAULT NULL COMMENT '수정일시',
    re_delay_time1 INT(11) DEFAULT NULL COMMENT '재전송지연시간1',
    re_delay_time2 INT(11) DEFAULT NULL COMMENT '재전송지연시간2',
    re_delay_time3 INT(11) DEFAULT NULL COMMENT '재전송지연시간3',
    re_delay_time4 INT(11) DEFAULT NULL COMMENT '재전송지연시간4',
    re_delay_time5 INT(11) DEFAULT NULL COMMENT '재전송지연시간4',
    topic VARCHAR(300) DEFAULT NULL COMMENT 'MQTT 토픽',
    PRIMARY KEY (no)
);
```

#### 🔍 주요 인덱스
- **PRIMARY**: `no` (AUTO_INCREMENT)
- **hnt_config_UN**: `(user_id, sensor_id, sensor_uuid)` - UNIQUE
- **idx_config_user_uuid**: `(user_id, sensor_uuid)` - 설정 조회용

#### 📊 데이터 특성
- **설정 항목**: 5개 알림 설정 (고온, 저온, 압력 등)
- **총 설정**: 31개 (센서별 설정)

### 5. **hnt_alarm** (알림 테이블)

#### 📋 컬럼 구조
```sql
CREATE TABLE hnt_alarm (
    no INT(11) NOT NULL AUTO_INCREMENT COMMENT '번호',
    user_id VARCHAR(100) NOT NULL COMMENT '사용자아이디',
    user_token VARCHAR(500) DEFAULT NULL COMMENT '사용자토큰',
    sensor_uuid VARCHAR(100) DEFAULT NULL COMMENT '토픽',
    alarm_time DATETIME DEFAULT NULL COMMENT '알람타임',
    inst_id VARCHAR(100) DEFAULT NULL COMMENT '입력자아이디',
    inst_dtm DATETIME DEFAULT NULL COMMENT '입력일시',
    mdf_id VARCHAR(100) DEFAULT NULL COMMENT '수정자아이디',
    mdf_dtm DATETIME DEFAULT NULL COMMENT '수정일시',
    siren_yn VARCHAR(1) DEFAULT NULL COMMENT '사이렌 여부',
    alarm_type VARCHAR(100) DEFAULT NULL COMMENT '알림 타입',
    cur_temp VARCHAR(100) DEFAULT NULL COMMENT '현재 온도',
    in_temp VARCHAR(100) DEFAULT NULL COMMENT '설정 온도',
    urgent_yn CHAR(1) DEFAULT NULL COMMENT '즉시발송여부',
    release_yn CHAR(1) NOT NULL DEFAULT 'N' COMMENT '정상 인입에 따른 알람 해제 여부',
    PRIMARY KEY (no)
);
```

#### 🔍 주요 인덱스
- **PRIMARY**: `no` (AUTO_INCREMENT)
- **hnt_alarm_UN**: `(no, user_id, sensor_uuid)` - UNIQUE

#### 📊 데이터 특성
- **현재 데이터**: 0건 (알림 이력 없음)
- **용도**: 센서 임계값 초과 시 알림 기록

---

## 🔗 테이블 관계도

```
hnt_user (사용자)
    ├── 1:N → hnt_sensor_info (센서 정보)
    │   └── 1:N → hnt_sensor_data (센서 데이터)
    │   └── 1:1 → hnt_config (센서 설정)
    └── 1:N → hnt_alarm (알림)

부계정 관계:
hnt_user.parent_user_id → hnt_user.user_id (주계정)
```

---

## 📋 데이터베이스 규칙 및 가이드

### 🔒 **데이터 무결성 규칙**

#### 1. **기본 키 규칙**
- 모든 테이블은 `no` (AUTO_INCREMENT)를 기본 키로 사용
- `no`는 절대 수정 불가, 삭제 시에만 사용

#### 2. **외래 키 규칙**
- `user_id`: hnt_user.user_id 참조
- `sensor_uuid`: hnt_sensor_info.sensor_uuid 참조
- `sensor_id`: hnt_sensor_info.sensor_id 참조

#### 3. **UNIQUE 제약조건**
- `hnt_user.user_id`: 사용자 ID 중복 불가
- `hnt_sensor_info`: (user_id, sensor_id, sensor_uuid) 조합 중복 불가
- `hnt_config`: (user_id, sensor_id, sensor_uuid) 조합 중복 불가

### 🎯 **인덱스 사용 가이드**

#### 1. **쿼리 패턴별 권장 인덱스**

**실시간 데이터 조회**
```sql
-- 권장: idx_hnt_sensor_data_uuid_inst_dtm
SELECT sensor_value FROM hnt_sensor_data 
WHERE uuid = ? AND inst_dtm >= DATE_ADD(NOW(), INTERVAL -1 HOUR) 
ORDER BY inst_dtm DESC LIMIT 1;
```

**사용자별 센서 목록 조회**
```sql
-- 권장: idx_hnt_sensor_data_user_id_uuid
SELECT * FROM hnt_sensor_data 
WHERE user_id = ? AND uuid = ?;
```

**시간 범위 조회 (차트 데이터)**
```sql
-- 권장: idx_hnt_sensor_data_inst_dtm
SELECT * FROM hnt_sensor_data 
WHERE inst_dtm BETWEEN ? AND ? 
ORDER BY inst_dtm;
```

**부계정 관계 조회**
```sql
-- 권장: idx_user_parent_del
SELECT * FROM hnt_user 
WHERE parent_user_id = ? AND del_yn = 'N';
```

#### 2. **인덱스 생성 금지 규칙**
- 단일 컬럼 인덱스는 최소화 (복합 인덱스 우선)
- `raw_data` 컬럼 인덱스 생성 금지 (카디널리티 낮음)
- 중복 기능 인덱스 생성 금지

### 🚀 **성능 최적화 가이드**

#### 1. **쿼리 작성 규칙**
- WHERE 절에 인덱스 컬럼 우선 사용
- 복합 인덱스의 컬럼 순서 고려
- `LIMIT` 절 적극 활용

#### 2. **데이터 삽입 최적화**
- 배치 삽입 사용 (`insertSensorDataBatch`)
- 트랜잭션 범위 최소화
- 인덱스 개수 최소화 (INSERT 성능)

#### 3. **데이터 조회 최적화**
- 필요한 컬럼만 SELECT
- `EXPLAIN` 분석 필수
- 인덱스 힌트 사용 금지 (MySQL 자동 최적화)

### 🗂️ **데이터 관리 규칙**

#### 1. **데이터 보존 정책**
- **센서 데이터**: 24개월 보존
- **사용자 데이터**: 삭제 시 soft delete (del_yn = 'Y')
- **설정 데이터**: 센서 삭제 시 함께 삭제

#### 2. **백업 정책**
- **전체 백업**: 주 1회 (일요일)
- **증분 백업**: 일 1회 (매일 새벽)
- **백업 보존**: 4주간

#### 3. **모니터링 지표**
- **테이블 크기**: hnt_sensor_data 12GB 초과 시 알림
- **인덱스 사용률**: 미사용 인덱스 정기 점검
- **쿼리 성능**: 1초 초과 쿼리 분석

### ⚠️ **주의사항**

#### 1. **스키마 변경 금지**
- 프로덕션 환경에서 DDL 실행 금지
- 테이블 구조 변경 시 반드시 백업 후 진행
- 컬럼 추가/삭제 시 애플리케이션 코드 동기화 필수

#### 2. **인덱스 관리**
- 인덱스 생성/삭제 시 서비스 중단 시간 고려
- 대용량 테이블 인덱스 작업은 새벽 시간대 진행
- 인덱스 생성 전 `EXPLAIN` 분석 필수

#### 3. **데이터 정합성**
- 부계정 삭제 시 주계정 데이터 보존
- 센서 삭제 시 관련 데이터 일괄 삭제
- 사용자 등급 변경 시 권한 재검토

---

## 📞 **문의 및 지원**

- **데이터베이스 관리**: 시스템 관리자
- **성능 이슈**: DBA 팀
- **데이터 복구**: 백업 관리자

---

**작성일**: 2025-09-29  
**버전**: 1.0  
**작성자**: AI Assistant
