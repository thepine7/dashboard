# 데이터베이스 쿼리 최적화 가이드

## 📋 개요

이 문서는 HnT Sensor API 프로젝트의 데이터베이스 쿼리 최적화 규칙과 가이드를 제공합니다.

## 🗄️ 데이터베이스 정보

- **데이터베이스명**: `hnt`
- **엔진**: MySQL 5.7.9
- **서버**: hntsolution.co.kr:3306
- **사용자**: root
- **비밀번호**: HntRoot123!

## 📊 기존 인덱스 현황 (2025-10-01 확인)

### **hnt_sensor_data 테이블 인덱스**

| 인덱스명 | 컬럼 | 타입 | 용도 | 카디널리티 |
|---------|------|------|------|-----------|
| **idx_hnt_sensor_data_uuid_inst_dtm** | uuid, inst_dtm | 복합 | 장치별 시간 범위 조회용 복합 인덱스 | 33, 34,290,536 |
| **idx_hnt_sensor_data_user_id_uuid** | user_id, uuid | 복합 | 사용자별 장치 조회용 복합 인덱스 | 19, 36 |
| **idx_sensor_data_performance** | user_id, sensor_id, uuid, inst_dtm | 복합 | 성능 최적화용 복합 인덱스 | 19, 19, 36, 34,290,536 |
| **hnt_sensor_data_uuid_IDX** | uuid | 단일 | 장치별 조회용 | 33 |
| **hnt_sensor_data_inst_dtm_IDX** | inst_dtm | 단일 | 시간 범위 조회용 | 17,145,268 |

### **hnt_sensor_info 테이블 인덱스**

| 인덱스명 | 컬럼 | 타입 | 용도 |
|---------|------|------|------|
| **idx_sensor_info_user_sensor** | user_id, sensor_id | 복합 | 사용자별 센서 조회용 |
| **hnt_sensor_info_sensor_id_IDX** | sensor_id | 단일 | 센서별 조회용 |

## 🔧 쿼리 최적화 규칙

### **1. 기존 인덱스 활용 우선**

- ✅ **새로운 인덱스 생성 금지**: 기존 인덱스를 최대한 활용
- ✅ **복합 인덱스 우선**: `idx_hnt_sensor_data_uuid_inst_dtm` (uuid, inst_dtm) 활용
- ✅ **인덱스 힌트 사용 금지**: MySQL 쿼리 플래너가 자동으로 최적 인덱스 선택

### **2. MyBatis XML 쿼리 작성 규칙**

#### **✅ 올바른 예시: 기존 인덱스 활용**
```xml
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
```

#### **❌ 잘못된 예시: 인덱스 힌트 사용**
```xml
<select id="selectSensorData" parameterType="java.util.HashMap" resultType="java.util.HashMap">
    select ...
    from hnt_sensor_data USE INDEX (idx_sensor_data_uuid_time)  <!-- 금지 -->
    where ...
</select>
```

### **3. 쿼리 성능 확인 방법**

#### **쿼리 실행 계획 확인**
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
```

#### **기존 인덱스 확인**
```sql
-- hnt_sensor_data 테이블 인덱스 확인
SHOW INDEX FROM hnt_sensor_data;

-- hnt_sensor_info 테이블 인덱스 확인
SHOW INDEX FROM hnt_sensor_info;
```

### **4. 쿼리 최적화 체크리스트**

- [ ] **기존 인덱스 활용**: 새 인덱스 생성 대신 기존 인덱스 활용
- [ ] **복합 인덱스 우선**: 단일 인덱스보다 복합 인덱스 활용
- [ ] **WHERE 절 순서**: 인덱스 컬럼 순서에 맞춰 WHERE 절 작성
- [ ] **인덱스 힌트 금지**: USE INDEX, FORCE INDEX 등 사용 금지
- [ ] **EXPLAIN 확인**: 쿼리 실행 계획으로 인덱스 사용 여부 확인

## 🚀 성능 최적화 팁

### **1. 인덱스 활용 최적화**

#### **복합 인덱스 활용**
- **idx_hnt_sensor_data_uuid_inst_dtm**: uuid + inst_dtm 조합으로 장치별 시간 범위 조회 최적화
- **idx_hnt_sensor_data_user_id_uuid**: user_id + uuid 조합으로 사용자별 장치 조회 최적화

#### **WHERE 절 작성 순서**
```sql
-- ✅ 올바른 순서: 인덱스 컬럼 순서에 맞춤
WHERE uuid = ? AND inst_dtm BETWEEN ? AND ?

-- ❌ 잘못된 순서: 인덱스 활용도 저하
WHERE inst_dtm BETWEEN ? AND ? AND uuid = ?
```

### **2. 쿼리 작성 모범 사례**

#### **조건문 단순화**
```xml
<!-- ✅ 단순화된 조건문 -->
<choose>
    <when test='gu == "d"'>
        and inst_dtm between date_format(now(), '%Y-%m-%d 00:00:00') and now()
    </when>
    <when test='gu == "w"'>
        and inst_dtm between date_add(now(), interval -1 month) and now()
    </when>
    <when test='gu == "y"'>
        and inst_dtm between date_add(now(), interval -1 year) and now()
    </when>
</choose>

<!-- ❌ 복잡한 중첩 조건문 -->
<choose>
    <when test='gu != "" and gu != null'>
        <choose>
            <when test='gu == "d"'>
                <choose>
                    <when test='startDateTime != null and startDateTime != ""'>
                        <!-- 복잡한 중첩 -->
                    </when>
                </choose>
            </when>
        </choose>
    </when>
</choose>
```

### **3. 성능 모니터링**

#### **쿼리 실행 시간 측정**
```sql
-- 쿼리 실행 시간 측정
SET profiling = 1;
SELECT ... FROM hnt_sensor_data WHERE ...;
SHOW PROFILES;
```

#### **인덱스 사용률 확인**
```sql
-- 인덱스 사용 통계 확인
SELECT 
    TABLE_SCHEMA,
    TABLE_NAME,
    INDEX_NAME,
    CARDINALITY,
    SUB_PART,
    PACKED,
    NULLABLE,
    INDEX_TYPE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'hnt' 
ORDER BY TABLE_NAME, INDEX_NAME;
```

## 📚 참고 자료

### **MySQL 쿼리 최적화 관련 문서**
- [MySQL 5.7 Query Optimization](https://dev.mysql.com/doc/refman/5.7/en/optimization.html)
- [MySQL Index Optimization](https://dev.mysql.com/doc/refman/5.7/en/mysql-indexes.html)
- [EXPLAIN Output Format](https://dev.mysql.com/doc/refman/5.7/en/explain-output.html)

### **MyBatis XML 관련 문서**
- [MyBatis Dynamic SQL](https://mybatis.org/mybatis-3/dynamic-sql.html)
- [MyBatis XML Mapper](https://mybatis.org/mybatis-3/sqlmap-xml.html)

## 🔄 업데이트 이력

- **2025-10-01**: 초기 가이드 작성, 기존 인덱스 현황 확인
- **2025-10-01**: selectSensorData 쿼리 최적화 완료
- **2025-10-01**: 쿼리 최적화 규칙 및 체크리스트 추가
