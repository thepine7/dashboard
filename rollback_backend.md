# 백엔드 롤백 방법

## 1. WebConfig.java 롤백
```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
            .allowedOrigins("http://iot.hntsolution.co.kr:8080", "http://iot.hntsolution.co.kr:8888", "https://iot.hntsolution.co.kr")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
}
```

## 2. web.xml 롤백
```xml
<session-config>
  <session-timeout>30</session-timeout>
  <cookie-config>
    <name>JSESSIONID</name>
    <domain>hntsolution.co.kr</domain>
    <path>/</path>
    <http-only>true</http-only>
    <secure>false</secure>
    <max-age>1800</max-age>
  </cookie-config>
</session-config>
```

## 3. application.yml 롤백
```yaml
server:
  servlet:
    session:
      cookie:
        max-age: 1800
        http-only: true
        secure: false
        same-site: lax
```

## 4. 롤백 명령어
```bash
# Git을 사용하는 경우
git checkout HEAD~1 -- src/main/java/com/andrew/hnt/api/config/WebConfig.java
git checkout HEAD~1 -- src/main/webapp/WEB-INF/web.xml
git checkout HEAD~1 -- src/main/resources/application.yml

# 또는 백업 파일에서 복원
copy backup\WebConfig_backup.java src\main\java\com\andrew\hnt\api\config\WebConfig.java
copy backup\web_backup.xml src\main\webapp\WEB-INF\web.xml
copy backup\application_backup.yml src\main\resources\application.yml
```
