# Java 코드 수정 가이드 (parent_user_id 컬럼 사용)

## 1. AdminMapper.xml 수정

### 기존 쿼리 수정
```xml
<!-- 부계정 여부 확인 (parent_user_id 사용) -->
<select id="isSubAccount" parameterType="java.util.HashMap" resultType="boolean">
    SELECT COUNT(*) > 0
    FROM hnt_user 
    WHERE user_id = #{userId}
      AND parent_user_id IS NOT NULL
      AND del_yn = 'N'
</select>

<!-- 부계정의 주계정 ID 조회 (parent_user_id 사용) -->
<select id="getMainUserIdForSubUser" parameterType="java.util.HashMap" resultType="java.util.HashMap">
    SELECT parent_user_id as sensor_id
    FROM hnt_user 
    WHERE user_id = #{subUserId}
      AND del_yn = 'N'
</select>

<!-- 부계정이 접근 가능한 센서 리스트 조회 (parent_user_id 사용) -->
<select id="getSubSensorList" parameterType="java.util.HashMap" resultType="java.util.HashMap">
    SELECT 
        s.user_id,
        s.sensor_id,
        s.sensor_uuid,
        s.sensor_name,
        s.sensor_loc,
        s.sensor_type,
        s.sensor_gu,
        s.chart_type
    FROM hnt_sensor_info s
    INNER JOIN hnt_user u ON s.sensor_id = u.user_id
    WHERE u.user_id = #{subUserId}
      AND u.parent_user_id = s.sensor_id
      AND u.del_yn = 'N'
      AND s.user_id = s.sensor_id
</select>
```

## 2. AdminService.java 수정

### 메서드 시그니처는 동일하게 유지
```java
/**
 * 부계정 여부 확인 (parent_user_id 컬럼 사용)
 * @param userId 사용자 ID
 * @return true: 부계정, false: 주계정
 */
public boolean isSubAccount(String userId);

/**
 * 부계정의 주계정 ID 조회 (parent_user_id 컬럼 사용)
 * @param subUserId 부계정 사용자 ID
 * @return 주계정 사용자 ID
 */
public String getMainUserIdForSubUser(String subUserId);
```

## 3. AdminServiceImpl.java 수정

### 구현 로직은 동일하게 유지 (매퍼 쿼리만 변경됨)
```java
@Override
public boolean isSubAccount(String userId) {
    try {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("userId", userId);
        
        boolean isSub = adminMapper.isSubAccount(param);
        logger.info("부계정 여부 확인 - userId: {}, isSubAccount: {}", userId, isSub);
        return isSub;
    } catch (Exception e) {
        logger.error("부계정 여부 확인 실패 - userId: {}, error: {}", userId, e.toString());
        return false;
    }
}

@Override
public String getMainUserIdForSubUser(String subUserId) {
    try {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("subUserId", subUserId);
        
        Map<String, Object> result = adminMapper.getMainUserIdForSubUser(param);
        if (result != null && result.size() > 0) {
            return String.valueOf(result.get("sensor_id")); // parent_user_id가 sensor_id로 매핑됨
        }
        return null;
    } catch (Exception e) {
        logger.error("부계정의 주계정 ID 조회 실패 - subUserId: {}, error: {}", subUserId, e.toString());
        return null;
    }
}
```

## 4. 부계정 생성 로직 추가

### AdminService.java에 추가
```java
/**
 * 부계정 생성
 * @param subUser 부계정 정보
 * @param parentUserId 주계정 ID
 * @return 생성 결과
 */
public boolean createSubAccount(UserInfo subUser, String parentUserId);
```

### AdminServiceImpl.java에 구현
```java
@Override
public boolean createSubAccount(UserInfo subUser, String parentUserId) {
    try {
        // 1. 부계정 사용자 생성
        subUser.setUserGrade("B");
        subUser.setParentUserId(parentUserId); // 새로 추가된 필드
        loginService.insertUser(subUser);
        
        // 2. 부계정이 주계정의 센서에 접근할 수 있도록 hnt_sensor_info 설정
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("subUserId", subUser.getUserId());
        param.put("parentUserId", parentUserId);
        adminMapper.insertSubAccountSensorAccess(param);
        
        logger.info("부계정 생성 완료 - subUserId: {}, parentUserId: {}", 
                   subUser.getUserId(), parentUserId);
        return true;
    } catch (Exception e) {
        logger.error("부계정 생성 실패 - subUserId: {}, parentUserId: {}, error: {}", 
                    subUser.getUserId(), parentUserId, e.toString());
        return false;
    }
}
```

## 5. UserInfo.java 모델 수정

### parent_user_id 필드 추가
```java
public class UserInfo {
    // 기존 필드들...
    
    /**
     * 부모 사용자 ID (부계정인 경우)
     */
    private String parentUserId;
    
    // getter/setter 추가
    public String getParentUserId() {
        return parentUserId;
    }
    
    public void setParentUserId(String parentUserId) {
        this.parentUserId = parentUserId;
    }
}
```

## 6. AdminMapper.xml에 부계정 생성 관련 쿼리 추가

```xml
<!-- 부계정이 주계정의 센서에 접근할 수 있도록 설정 -->
<insert id="insertSubAccountSensorAccess" parameterType="java.util.HashMap">
    INSERT INTO hnt_sensor_info (
        user_id, sensor_id, sensor_uuid, sensor_name, sensor_loc, 
        sensor_type, sensor_gu, chart_type, inst_dtm
    )
    SELECT 
        #{subUserId} as user_id,
        sensor_id,
        sensor_uuid,
        sensor_name,
        sensor_loc,
        sensor_type,
        sensor_gu,
        chart_type,
        NOW() as inst_dtm
    FROM hnt_sensor_info
    WHERE sensor_id = #{parentUserId}
      AND user_id = sensor_id
</insert>
```

## 7. 장점

### 기존 방식 (hnt_sensor_info 기반)
- ❌ 복잡한 JOIN 쿼리 필요
- ❌ 센서 정보가 없으면 부계정 판단 불가
- ❌ 데이터 정합성 문제 가능성

### 새로운 방식 (parent_user_id 컬럼)
- ✅ 간단한 쿼리로 부계정 판단
- ✅ 명확한 부모-자식 관계
- ✅ 데이터 정합성 보장
- ✅ 성능 향상
- ✅ 유지보수 용이

## 8. 마이그레이션 순서

1. **DB 스키마 변경**: `add_parent_user_id_column.sql` 실행
2. **기존 데이터 마이그레이션**: 자동으로 hnt_sensor_info 기반으로 parent_user_id 설정
3. **Java 코드 수정**: 위의 가이드에 따라 수정
4. **테스트**: 부계정 기능 정상 동작 확인
5. **기존 로직 제거**: hnt_sensor_info 기반 로직 제거 (선택사항)
