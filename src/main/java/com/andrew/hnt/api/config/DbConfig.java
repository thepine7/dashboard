package com.andrew.hnt.api.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableTransactionManagement
public class DbConfig {
    
    @Autowired
    private SlowQueryInterceptor slowQueryInterceptor;

	@Bean
	@ConfigurationProperties(prefix = "spring.datasource.hikari")
	public HikariConfig hikariConfig() {
		HikariConfig config = new HikariConfig();
		
		// 데이터베이스 연결 정보 설정
		config.setJdbcUrl("jdbc:mysql://hntsolution.co.kr:3306/hnt?useUnicode=yes&characterEncoding=UTF-8&allowMultiQueries=true&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&useSSL=false");
		config.setUsername("root");
		config.setPassword("HntRoot123!");
		config.setDriverClassName("com.mysql.cj.jdbc.Driver");
		
		// 연결 풀 설정
		config.setMaximumPoolSize(8);
		config.setMinimumIdle(2);
		config.setConnectionTimeout(10000);
		config.setIdleTimeout(300000);
		config.setMaxLifetime(600000);
		config.setLeakDetectionThreshold(60000);
		config.setConnectionTestQuery("SELECT 1");
		config.setPoolName("HnTSensorAPI-Pool");
		
		// MySQL 성능 최적화 설정
		config.addDataSourceProperty("socketTimeout", "30000");
		config.addDataSourceProperty("connectTimeout", "10000");
		config.addDataSourceProperty("autoReconnect", "true");
		config.addDataSourceProperty("maxReconnects", "3");
		config.addDataSourceProperty("queryTimeout", "30000");
		config.addDataSourceProperty("useServerPrepStmts", "false");
		config.addDataSourceProperty("cachePrepStmts", "false");
		config.addDataSourceProperty("useLocalSessionState", "true");
		config.addDataSourceProperty("rewriteBatchedStatements", "true");
		config.addDataSourceProperty("useCompression", "false");
		config.addDataSourceProperty("cacheResultSetMetadata", "true");
		config.addDataSourceProperty("cacheServerConfiguration", "true");
		config.addDataSourceProperty("elideSetAutoCommits", "true");
		config.addDataSourceProperty("maintainTimeStats", "false");
		
		return config;
	}

	@Bean
	public DataSource dataSource(HikariConfig hikariConfig) {
		return new HikariDataSource(hikariConfig);
	}

	@Bean
	public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
		SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		bean.setDataSource(dataSource);
		bean.setMapperLocations(resolver.getResources("classpath*:mapper/*.xml"));
		
		// 느린 쿼리 감지 인터셉터 추가
		bean.setPlugins(slowQueryInterceptor);
		
		return bean.getObject();
	}
	
	@Bean
	public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) throws Exception {
		return new SqlSessionTemplate(sqlSessionFactory);
	}
}
