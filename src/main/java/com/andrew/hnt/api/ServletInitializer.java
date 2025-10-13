package com.andrew.hnt.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Tomcat WAR 배포를 위한 ServletInitializer
 * 중복 실행 방지 로직 포함
 */
public class ServletInitializer extends SpringBootServletInitializer {
	
	private static final Logger logger = LoggerFactory.getLogger(ServletInitializer.class);
	private static volatile boolean isInitialized = false;
	private static final Object lock = new Object();

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		// 중복 초기화 방지
		synchronized (lock) {
			if (isInitialized) {
				logger.warn("ServletInitializer already initialized. Skipping duplicate initialization.");
				return application;
			}
			isInitialized = true;
		}
		
		logger.info("ServletInitializer configuration starting...");
		return application.sources(HntSensorApiApplication.class);
	}
	
	/**
	 * 애플리케이션 종료 시 초기화 플래그 리셋
	 */
	@Override
	public void onStartup(javax.servlet.ServletContext servletContext) throws javax.servlet.ServletException {
		try {
			super.onStartup(servletContext);
		} catch (Exception e) {
			logger.error("ServletInitializer startup failed", e);
			// 실패 시 플래그 리셋
			isInitialized = false;
			throw e;
		}
	}

}
