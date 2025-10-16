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
		logger.info("ServletInitializer configuration starting...");
		return application.sources(HntSensorApiApplication.class);
	}
	
	/**
	 * 중복 실행 방지 제거 - configure()에서만 초기화
	 */
	@Override
	public void onStartup(javax.servlet.ServletContext servletContext) throws javax.servlet.ServletException {
		try {
			logger.info("ServletInitializer onStartup starting...");
			super.onStartup(servletContext);
			logger.info("ServletInitializer onStartup completed successfully");
		} catch (Exception e) {
			logger.error("ServletInitializer startup failed", e);
			throw e;
		}
	}

}
