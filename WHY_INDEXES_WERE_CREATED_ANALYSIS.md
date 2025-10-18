# 개선 필요 인덱스가 왜 만들어졌는지 분석 보고서

**작성일**: 2025-10-17  
**분석 대상**: hnt_sensor_info, hnt_user 테이블의 미사용/중복 인덱스

---

## 📋 목차

1. [개요](#개요)
2. [sensor_uuid 관련 인덱스 분석](#sensor_uuid-관련-인덱스-분석)
3. [del_yn 관련 인덱스 분석](#del_yn-관련-인덱스-분석)
4. [기타 미사용 인덱스 분석](#기타-미사용-인덱스-분석)
5. [결론 및 권장사항](#결론-및-권장사항)

---

## 개요

### 분석 배경

차트설정 및 사용자관리 페이지 DB 쿼리 검증 중 다음과 같은 문제를 발견했습니다:

1. **sensor_uuid로만 조회 시 인덱스 미사용** (Full table scan)
2. **del_yn 필터링 시 인덱스 미사용** (Full table scan)
3. **다수의 중복/미사용 인덱스 존재**

이 보고서는 **"왜 이런 인덱스들이 처음에 만들어졌는지"**를 분석하여, 개발 초기 의도와 실제 사용 패턴 간의 차이를 이해하고자 합니다.

---

## sensor_uuid 관련 인덱스 분석

### 현재 상태

**테이블**: `hnt_sensor_info`

| 인덱스명 | 컬럼 | 타입 | 문제 |
|---------|------|------|------|
| hnt_sensor_info_UN | **(user_id, sensor_id, sensor_uuid)** | UNIQUE | sensor_uuid만으로 조회 시 사용 불가 |

### 왜 이렇게 설계되었을까?

#### 1️⃣ **원래 설계 의도: 복합 비즈니스 키**

```sql
UNIQUE KEY `hnt_sensor_info_UN` (`user_id`,`sensor_id`,`sensor_uuid`)
```

**설계 의도**:
- **user_id**: 센서를 사용하는 사용자 (현재 사용자 또는 부계정)
- **sensor_id**: 센서의 실제 소유자 (주계정)
- **sensor_uuid**: 센서의 물리적 고유 식별자 (하드웨어 ID)

**비즈니스 로직**:
```
주계정(thepine) → sensor_id = "thepine", user_id = "thepine", sensor_uuid = "0008DC755397"
부계정(thepine7) → sensor_id = "thepine", user_id = "thepine7", sensor_uuid = "0008DC755397"
```

이는 **한 센서를 여러 사용자가 공유**할 수 있도록 설계된 것입니다.

#### 2️⃣ **초기 쿼리 패턴: 복합 조건 사용**

**예상 초기 쿼리**:
```sql
-- 예상 1: user_id + sensor_uuid 조합 조회
SELECT * FROM hnt_sensor_info
WHERE user_id = 'thepine' AND sensor_uuid = '0008DC755397';

-- 예상 2: user_id + sensor_id + sensor_uuid 조합 조회
SELECT * FROM hnt_sensor_info
WHERE user_id = 'thepine' AND sensor_id = 'thepine' AND sensor_uuid = '0008DC755397';
```

이러한 쿼리에서는 **복합 UNIQUE 인덱스가 효율적으로 사용**됩니다.

#### 3️⃣ **실제 사용 패턴: sensor_uuid만으로 조회**

**실제 쿼리 패턴** (AdminMapper.xml, DataMapper.xml, MqttMapper.xml 분석 결과):

```sql
-- 패턴 1: sensor_uuid만으로 조회 (가장 빈번)
SELECT * FROM hnt_sensor_info WHERE sensor_uuid = '0008DC755397';

-- 패턴 2: sensor_uuid로 삭제
DELETE FROM hnt_sensor_info WHERE sensor_uuid = '0008DC755397';

-- 패턴 3: sensor_uuid로 업데이트
UPDATE hnt_sensor_info SET sensor_name = '...' WHERE sensor_uuid = '0008DC755397';
```

**실제 사용처 (32개 발견)**:
1. `getSensorInfoByUuid` (AdminMapper.xml 라인 35) - **차트 페이지 로딩 시마다 호출**
2. `selectSensorId` (AdminMapper.xml 라인 130)
3. `deleteSensorInfo` (DataMapper.xml 라인 24) - 장치 삭제
4. `updateSensorInfo` (DataMapper.xml 라인 19) - 장치 정보 수정
5. `getSensorInfoByUuid` (MqttMapper.xml 라인 154) - MQTT 메시지 처리
6. `deleteSensorInfoByUuid` (MqttMapper.xml 라인 160) - 장치 이전
7. `deleteConfigByUuid` (MqttMapper.xml 라인 165) - 설정 삭제
8. `deleteAlarmByUuid` (MqttMapper.xml 라인 175) - 알림 삭제
9. ... 그 외 24개 더

**호출 빈도**:
- **차트 페이지**: 로딩 시마다 (1일 수백~수천 회)
- **장치 관리**: 수정/삭제 시 (1일 수십 회)
- **MQTT 메시지**: 실시간 처리 (1초당 수십 회)

#### 4️⃣ **왜 패턴이 바뀌었을까?**

**가설 1: 비즈니스 로직 단순화**
```
초기: 사용자별로 센서를 구분해서 관리
     → WHERE user_id = ? AND sensor_uuid = ?

현재: 센서 UUID가 전역 고유하므로 UUID만으로 식별
     → WHERE sensor_uuid = ?
```

**가설 2: UNIQUE 제약 조건의 부작용**
```sql
-- 초기 설계 의도
UNIQUE (user_id, sensor_id, sensor_uuid) 
→ "동일한 센서를 여러 사용자가 공유 가능"

-- 실제 데이터 상황
hnt_sensor_info 테이블:
user_id   sensor_id   sensor_uuid
thepine   thepine     0008DC755397  (주계정 소유)
thepine7  thepine     0008DC755397  (부계정 공유)
```

하지만 **실제로는 sensor_uuid가 이미 전역 고유**하므로, user_id와 sensor_id 구분이 불필요했습니다.

**가설 3: 코드 리팩토링 과정**
```
초기 버전: 복합 조건 사용 → 인덱스 활용
    ↓
중간 버전: 비즈니스 로직 변경 → sensor_uuid만으로 충분
    ↓
현재 버전: 쿼리는 단순화, 인덱스는 그대로 → 인덱스 미활용
```

### 누락된 것: sensor_uuid 단독 인덱스

**원인**: 
- 초기에는 `(user_id, sensor_id, sensor_uuid)` 복합 인덱스로 충분했음
- 비즈니스 로직 변경 후 sensor_uuid 단독 쿼리가 증가
- **인덱스는 업데이트되지 않음**

**해결책**:
```sql
CREATE INDEX idx_sensor_info_sensor_uuid ON hnt_sensor_info(sensor_uuid);
```

---

## del_yn 관련 인덱스 분석

### 현재 상태

**테이블**: `hnt_user`

| 인덱스명 | 컬럼 | 타입 | 문제 |
|---------|------|------|------|
| idx_user_parent_del | **(parent_user_id, del_yn)** | INDEX | del_yn만으로 조회 시 사용 불가 |

### 왜 이렇게 설계되었을까?

#### 1️⃣ **원래 설계 의도: 부계정 조회 최적화**

```sql
KEY `idx_user_parent_del` (`parent_user_id`, `del_yn`)
```

**설계 의도**:
```sql
-- 특정 주계정의 활성 부계정 조회
SELECT * FROM hnt_user
WHERE parent_user_id = 'thepine'
  AND del_yn = 'N';
```

이 쿼리에서는 **복합 인덱스가 효율적으로 사용**됩니다.

#### 2️⃣ **실제 사용 패턴: del_yn만으로 필터링**

**실제 쿼리 패턴** (LoginMapper.xml 분석 결과):

```sql
-- 패턴 1: 전체 활성 사용자 목록 조회 (가장 빈번)
SELECT * FROM hnt_user
WHERE del_yn = 'N'
ORDER BY no DESC
LIMIT 100;

-- 패턴 2: 활성 사용자 수 조회
SELECT COUNT(*) FROM hnt_user WHERE del_yn = 'N';

-- 패턴 3: 특정 사용자 조회
SELECT * FROM hnt_user
WHERE user_id = 'thepine'
  AND del_yn = 'N';
```

**실제 사용처 (18개 발견)**:
1. `getUserListWithActivityStatus` (LoginMapper.xml 라인 37) - **사용자 목록 페이지**
2. `getSubUserList` (LoginMapper.xml 라인 58) - 부계정 목록
3. `getUserAndSubUserList` (LoginMapper.xml 라인 81) - 사용자+부계정 목록
4. `getUserAndSubUserListWithActivityStatus` (LoginMapper.xml 라인 109) - 활동 상태 포함
5. `selectUser` (LoginMapper.xml 라인 130) - 로그인 검증
6. `getUserInfoByUserId` (LoginMapper.xml 라인 147) - 사용자 정보 조회
7. `getUserCount` (AdminMapper.xml 라인 465) - 헬스체크
8. ... 그 외 11개 더

**호출 빈도**:
- **사용자 목록 페이지**: 페이지 로딩 시마다 (1일 수십~수백 회)
- **로그인 검증**: 로그인 시마다 (1일 수십 회)
- **세션 검증**: API 호출 시마다 (1일 수천~수만 회)

#### 3️⃣ **왜 패턴이 바뀌었을까?**

**가설 1: 사용자 관리 방식 변경**
```
초기: 주계정-부계정 관계 기반 쿼리
     → WHERE parent_user_id = ? AND del_yn = 'N'

현재: 전체 활성 사용자 조회 중심
     → WHERE del_yn = 'N'
```

**가설 2: 사용자 수 증가**
```
초기: 사용자 수 < 100명
     → Full scan 성능 문제 없음
     → del_yn 인덱스 불필요

현재: 사용자 수 40명 (향후 수백~수천 명 예상)
     → Full scan 성능 저하 우려
     → del_yn 인덱스 필요
```

**가설 3: Soft Delete 패턴 도입**
```sql
-- 기존: 실제 삭제
DELETE FROM hnt_user WHERE user_id = 'thepine';

-- 변경: 논리적 삭제 (Soft Delete)
UPDATE hnt_user SET del_yn = 'Y' WHERE user_id = 'thepine';
```

Soft Delete 패턴 도입 후, **del_yn='N' 조건이 모든 쿼리에 추가**되었지만, 이에 맞는 인덱스는 추가되지 않았습니다.

### 누락된 것: del_yn 단독 또는 복합 인덱스

**원인**:
- 초기에는 `parent_user_id + del_yn` 조합만 사용
- Soft Delete 패턴 도입 후 del_yn 단독 쿼리 증가
- **인덱스는 업데이트되지 않음**

**해결책**:
```sql
-- 옵션 A: del_yn + no 복합 인덱스 (권장)
CREATE INDEX idx_user_del_no ON hnt_user(del_yn, no DESC);
-- 이유: ORDER BY no DESC와 함께 사용되므로 filesort 제거 가능

-- 옵션 B: del_yn 단독 인덱스
CREATE INDEX idx_user_del_yn ON hnt_user(del_yn);
-- 이유: 단순하지만 ORDER BY 시 filesort 발생
```

---

## 기타 미사용 인덱스 분석

### 1. hnt_sensor_info_no_IDX

**인덱스**: `KEY hnt_sensor_info_no_IDX (no)`

**왜 만들어졌을까?**:
- **AUTO_INCREMENT 컬럼**에 대한 자동 인덱스 생성 습관
- 많은 개발자가 "PRIMARY KEY가 아닌 추가 인덱스"를 습관적으로 생성

**실제 사용**:
- ❌ **전혀 사용되지 않음**
- PRIMARY KEY가 이미 `no` 컬럼에 존재
- **100% 중복 인덱스**

**왜 삭제되지 않았을까?**:
- 개발 초기에 자동 생성되었거나 스크립트로 일괄 생성
- "혹시 나중에 필요할까봐" 삭제하지 않음
- **영향이 없어서 방치됨**

---

### 2. idx_sensor_info_user_sensor

**인덱스**: `KEY idx_sensor_info_user_sensor (user_id, sensor_id)`

**왜 만들어졌을까?**:
- **주계정의 센서 목록 조회** 최적화 의도
```sql
SELECT * FROM hnt_sensor_info
WHERE user_id = 'thepine' AND sensor_id = 'thepine';
```

**실제 사용**:
- ⚠️ **부분적으로 사용됨** (user_id 단독 조회 시)
- 하지만 `hnt_sensor_info_UN (user_id, sensor_id, sensor_uuid)`이 이미 존재
- **복합 UNIQUE 인덱스의 앞 2개 컬럼과 동일** → 중복

**왜 중복 생성되었을까?**:
```
시나리오:
1. 초기: idx_sensor_info_user_sensor 생성
2. 중기: UNIQUE 제약 추가 필요 → hnt_sensor_info_UN 생성
3. 현재: 두 인덱스가 공존 → 기존 인덱스 삭제 누락
```

---

### 3. hnt_sensor_info_inst_dtm_IDX, hnt_sensor_info_mdf_dtm_IDX

**인덱스**: 
- `KEY hnt_sensor_info_inst_dtm_IDX (inst_dtm)`
- `KEY hnt_sensor_info_mdf_dtm_IDX (mdf_dtm)`

**왜 만들어졌을까?**:
- **"timestamp 컬럼은 인덱스를 만든다"는 관습**
- 감사(Audit) 목적으로 생성/수정 시간 조회 예상

**예상 사용 패턴**:
```sql
-- 최근 등록된 센서 조회
SELECT * FROM hnt_sensor_info
WHERE inst_dtm >= DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY inst_dtm DESC;

-- 최근 수정된 센서 조회
SELECT * FROM hnt_sensor_info
WHERE mdf_dtm >= DATE_SUB(NOW(), INTERVAL 1 DAY)
ORDER BY mdf_dtm DESC;
```

**실제 사용**:
- ❌ **전혀 사용되지 않음**
- 코드베이스에서 inst_dtm, mdf_dtm 조건 쿼리 없음
- **감사 기능이 구현되지 않음**

**왜 삭제되지 않았을까?**:
- "나중에 감사 기능 추가할 때 필요할 것"이라는 기대
- **실제로는 필요하지 않았음**

---

### 4. hnt_sensor_info_sensor_id_IDX

**인덱스**: `KEY hnt_sensor_info_sensor_id_IDX (sensor_id)`

**왜 만들어졌을까?**:
- **센서 소유자(주계정)별 센서 조회** 최적화 의도
```sql
SELECT * FROM hnt_sensor_info WHERE sensor_id = 'thepine';
```

**실제 사용**:
- ⚠️ **가끔 사용됨** (getUserSensorList, getSubSensorList)
- 하지만 `hnt_sensor_info_UN (user_id, sensor_id, sensor_uuid)`의 두 번째 컬럼
- **단독 sensor_id 조회 시에는 인덱스 미활용** (leftmost prefix 원칙)

**왜 효과가 없을까?**:
```sql
-- 예상 쿼리 (인덱스 사용 안 됨)
SELECT * FROM hnt_sensor_info WHERE sensor_id = 'thepine';

-- 실제 쿼리 (인덱스 사용됨)
SELECT * FROM hnt_sensor_info 
WHERE user_id = 'thepine' AND sensor_id = 'thepine';
```

복합 인덱스의 **두 번째 컬럼**이므로 단독 조회 시 인덱스 미활용.

---

### 5. hnt_user_inst_dtm_IDX, hnt_user_mdf_dtm_IDX

**인덱스**:
- `KEY hnt_user_inst_dtm_IDX (inst_dtm)`
- `KEY hnt_user_mdf_dtm_IDX (mdf_dtm)`

**이유**: hnt_sensor_info의 timestamp 인덱스와 동일한 이유

**실제 사용**: ❌ **전혀 사용되지 않음**

---

### 6. idx_hnt_user_parent_user_id

**인덱스**: `KEY idx_hnt_user_parent_user_id (parent_user_id)`

**왜 만들어졌을까?**:
- **부계정 조회** 최적화
```sql
SELECT * FROM hnt_user WHERE parent_user_id = 'thepine';
```

**실제 사용**:
- ⚠️ **가끔 사용됨**
- 하지만 `idx_user_parent_del (parent_user_id, del_yn)`이 이미 존재
- **복합 인덱스의 첫 번째 컬럼과 동일** → 중복

**왜 중복 생성되었을까?**:
```
시나리오:
1. 초기: idx_hnt_user_parent_user_id 생성
2. 중기: del_yn 필터링 추가 → idx_user_parent_del 생성
3. 현재: 두 인덱스가 공존 → 기존 인덱스 삭제 누락
```

---

## 결론 및 권장사항

### 인덱스 생성 패턴 분석

| 인덱스 | 생성 이유 | 실제 사용 | 문제 |
|-------|----------|----------|------|
| **복합 UNIQUE 인덱스** | 비즈니스 키 보장 | ✅ 부분 사용 | 단독 컬럼 조회 시 미활용 |
| **복합 일반 인덱스** | 특정 쿼리 최적화 | ⚠️ 부분 사용 | 중복 인덱스 존재 |
| **timestamp 인덱스** | 감사 기능 예상 | ❌ 미사용 | 구현되지 않은 기능 |
| **AUTO_INCREMENT 인덱스** | 습관적 생성 | ❌ 미사용 | PRIMARY KEY와 중복 |

### 왜 이런 일이 발생했을까?

#### 1. **"혹시 모르니까" 심리**
```
개발자: "나중에 필요할 수도 있으니 미리 만들어두자"
→ 결과: 실제로는 필요 없었음
→ 부작용: 인덱스 크기 증가, INSERT/UPDATE 성능 저하
```

#### 2. **비즈니스 로직 변경 후 인덱스 미정비**
```
초기: 복합 조건 쿼리 → 복합 인덱스 효과적
    ↓
현재: 단독 컬럼 쿼리 → 복합 인덱스 비효율
    ↓
문제: 인덱스는 그대로, 쿼리만 변경됨
```

#### 3. **Soft Delete 패턴 도입 후 인덱스 미추가**
```
기존: 실제 삭제 → del_yn 컬럼 없음
    ↓
변경: 논리적 삭제 → del_yn='N' 조건 추가
    ↓
문제: del_yn 인덱스 미추가 → Full scan
```

#### 4. **중복 인덱스 정리 누락**
```
초기: 인덱스 A 생성
    ↓
중기: 요구사항 변경 → 인덱스 B 생성 (A 포함)
    ↓
문제: 인덱스 A 삭제 누락 → 중복 유지
```

### 교훈 및 베스트 프랙티스

#### ✅ **해야 할 것**

1. **인덱스 생성 전 실제 쿼리 패턴 분석**
   - 예상이 아닌 실제 사용 패턴 기반 설계
   - EXPLAIN으로 검증

2. **비즈니스 로직 변경 시 인덱스 재검토**
   - 쿼리 패턴 변경 시 인덱스도 함께 조정
   - 정기적인 인덱스 사용 현황 점검

3. **Soft Delete 도입 시 del_yn 인덱스 필수**
   - del_yn='N' 조건이 모든 쿼리에 추가
   - 복합 인덱스 첫 번째 컬럼으로 배치

4. **중복 인덱스 주기적 정리**
   - 3개월마다 인덱스 사용 현황 점검
   - 미사용/중복 인덱스 정리

5. **인덱스 생성 문서화**
   - 왜 이 인덱스가 필요한지 주석 작성
   - 어떤 쿼리에서 사용되는지 명시

#### ❌ **하지 말아야 할 것**

1. **"혹시 모르니까" 인덱스 생성 금지**
   - 실제 필요성 없는 인덱스는 부담
   - 필요할 때 추가하는 것이 더 좋음

2. **timestamp 컬럼 자동 인덱스 생성 금지**
   - 실제 감사 기능이 필요할 때만 생성
   - 대부분의 경우 불필요

3. **PRIMARY KEY와 중복 인덱스 생성 금지**
   - AUTO_INCREMENT 컬럼은 이미 인덱스
   - 추가 인덱스는 100% 중복

4. **복합 인덱스 생성 후 단독 인덱스 방치 금지**
   - 복합 인덱스가 단독 인덱스를 포함하는지 확인
   - 중복 시 단독 인덱스 삭제

### 최종 권장사항

#### **Phase 1: 필수 인덱스 추가 (즉시)**
```sql
-- 실제 사용 패턴에 맞는 인덱스 추가
CREATE INDEX idx_sensor_info_sensor_uuid ON hnt_sensor_info(sensor_uuid);
CREATE INDEX idx_user_del_no ON hnt_user(del_yn, no DESC);
```

#### **Phase 2: 중복 인덱스 정리 (1주일 후)**
```sql
-- 미사용/중복 인덱스 삭제
-- hnt_sensor_info: 5개 삭제
-- hnt_user: 3개 삭제
```

#### **Phase 3: 인덱스 관리 프로세스 구축**
```
1. 분기별 인덱스 사용 현황 점검
2. 신규 기능 개발 시 인덱스 영향도 분석
3. 인덱스 생성/삭제 이력 관리
4. 성능 모니터링 자동화
```

---

**작성자**: AI Assistant  
**최종 업데이트**: 2025-10-17

