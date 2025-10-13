package com.andrew.hnt.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
    // 불필요한 자동 설정 비활성화로 시작 시간 단축
    org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration.class,
    org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration.class,
    org.springframework.boot.autoconfigure.websocket.reactive.WebSocketReactiveAutoConfiguration.class,
    org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration.class
})
@EnableScheduling
public class HntSensorApiApplication {
	
	private static volatile boolean isRunning = false;

	public static void main(String[] args) {
		// 중복 실행 방지 (WAR 배포 시 main 메서드가 호출되지 않아야 함)
		// ServletInitializer를 통한 배포만 허용
		if (isRunning) {
			System.out.println("Application is already running. Skipping duplicate initialization.");
			return;
		}
		
		synchronized (HntSensorApiApplication.class) {
			if (isRunning) {
				return;
			}
			isRunning = true;
		}
		
		// Spring Boot 시작 최적화
		SpringApplication app = new SpringApplication(HntSensorApiApplication.class);
		
		// 불필요한 배너 비활성화
		app.setBannerMode(org.springframework.boot.Banner.Mode.OFF);
		
		// 로그 레벨 최적화
		app.setLogStartupInfo(false);
		
		// JVM 최적화 설정
		System.setProperty("spring.jmx.enabled", "false");
		System.setProperty("spring.main.lazy-initialization", "false");
		System.setProperty("spring.jpa.open-in-view", "false");
		
		app.run(args);
	}
}
