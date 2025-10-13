package com.andrew.hnt.api.config;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * 느린 쿼리 감지 인터셉터
 * MyBatis 쿼리 실행 시간을 모니터링하여 느린 쿼리를 식별
 */
@Component
@Intercepts({
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class SlowQueryInterceptor implements Interceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(SlowQueryInterceptor.class);
    
    // 느린 쿼리 임계값 (밀리초) - application.yml에서 주입
    @Value("${slow-query.threshold:1000}")
    private long slowQueryThreshold;
    
    // 느린 쿼리 감지 활성화 여부
    @Value("${slow-query.enabled:true}")
    private boolean slowQueryEnabled;
    
    // SQL 로깅 활성화 여부
    @Value("${slow-query.log-sql:true}")
    private boolean sqlLogEnabled;
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 느린 쿼리 감지가 비활성화된 경우 바로 실행
        if (!slowQueryEnabled) {
            return invocation.proceed();
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 쿼리 실행
            Object result = invocation.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
            String statementId = mappedStatement.getId();
            
            // 느린 쿼리 감지
            if (executionTime > slowQueryThreshold) {
                logger.warn("=== 느린 쿼리 감지 ===");
                logger.warn("Statement ID: {}", statementId);
                logger.warn("실행 시간: {}ms (임계값: {}ms)", executionTime, slowQueryThreshold);
                
                if (sqlLogEnabled) {
                    try {
                        String sql = mappedStatement.getSqlSource().getBoundSql(invocation.getArgs()[1]).getSql();
                        logger.warn("SQL: {}", sql);
                    } catch (Exception e) {
                        logger.warn("SQL 로깅 중 오류 발생: {}", e.getMessage());
                    }
                }
                logger.warn("========================");
            } else if (executionTime > 500) { // 500ms 이상이면 정보 로그
                logger.info("쿼리 실행 시간: {}ms - Statement ID: {}", executionTime, statementId);
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
            String statementId = mappedStatement.getId();
            
            logger.error("쿼리 실행 실패 - Statement ID: {}, 실행 시간: {}ms, 오류: {}", 
                        statementId, executionTime, e.getMessage(), e);
            
            throw e;
        }
    }
    
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    
    @Override
    public void setProperties(Properties properties) {
        // 설정 파일에서 임계값을 읽어올 수 있도록 구현
        String threshold = properties.getProperty("slowQueryThreshold");
        if (threshold != null) {
            try {
                this.slowQueryThreshold = Long.parseLong(threshold);
                logger.info("느린 쿼리 임계값 설정: {}ms", this.slowQueryThreshold);
            } catch (NumberFormatException e) {
                logger.warn("잘못된 slowQueryThreshold 값: {}, 기본값 사용: {}ms", threshold, this.slowQueryThreshold);
            }
        }
    }
    
    /**
     * 느린 쿼리 임계값 설정
     * @param threshold 임계값 (밀리초)
     */
    public void setSlowQueryThreshold(long threshold) {
        this.slowQueryThreshold = threshold;
        logger.info("느린 쿼리 임계값 변경: {}ms", threshold);
    }
    
    /**
     * 현재 설정된 임계값 조회
     * @return 임계값 (밀리초)
     */
    public long getSlowQueryThreshold() {
        return slowQueryThreshold;
    }
}
