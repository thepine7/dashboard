package com.andrew.hnt.api.config;

import com.andrew.hnt.api.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import javax.annotation.PostConstruct;

/**
 * 권한 검증 설정
 * 권한 검증 전략들을 자동으로 등록
 */
@Configuration
public class PermissionValidationConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(PermissionValidationConfig.class);
    
    @Autowired
    private PermissionValidationManager permissionValidationManager;
    
    @Autowired
    private BasicPermissionValidationStrategy basicPermissionValidationStrategy;
    
    @Autowired
    private AdminPermissionValidationStrategy adminPermissionValidationStrategy;
    
    @Autowired
    private DeviceManagementPermissionValidationStrategy deviceManagementPermissionValidationStrategy;
    
    /**
     * 애플리케이션 시작 시 권한 검증 전략 등록
     */
    @PostConstruct
    public void registerValidationStrategies() {
        logger.info("권한 검증 전략 등록 시작");
        
        try {
            // 기본 권한 검증 전략 등록
            permissionValidationManager.registerValidationStrategy(basicPermissionValidationStrategy);
            logger.info("기본 권한 검증 전략 등록 완료");
            
            // 관리자 권한 검증 전략 등록
            permissionValidationManager.registerValidationStrategy(adminPermissionValidationStrategy);
            logger.info("관리자 권한 검증 전략 등록 완료");
            
            // 장치 관리 권한 검증 전략 등록
            permissionValidationManager.registerValidationStrategy(deviceManagementPermissionValidationStrategy);
            logger.info("장치 관리 권한 검증 전략 등록 완료");
            
            logger.info("모든 권한 검증 전략 등록 완료");
            
        } catch (Exception e) {
            logger.error("권한 검증 전략 등록 중 오류 발생", e);
        }
    }
    
    /**
     * 컨텍스트 새로고침 시 권한 검증 전략 재등록
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        logger.info("컨텍스트 새로고침 감지 - 권한 검증 전략 재등록");
        registerValidationStrategies();
    }
}
