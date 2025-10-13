package com.andrew.hnt.api.controller;

import com.andrew.hnt.api.client.RetroClient;
import com.andrew.hnt.api.client.RetroInterface;
import com.andrew.hnt.api.service.LoginService;
import com.andrew.hnt.api.model.UserInfo;
import com.andrew.hnt.api.util.AES256Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {

	private static final Logger logger = LoggerFactory.getLogger(TestController.class);
	
	@Autowired
	private LoginService loginService;

	@GetMapping(value = "/test", produces = MediaType.APPLICATION_JSON_VALUE)
	public String test() {
		return "test";
	}
	
	@GetMapping(value = "/test/db", produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Object> testDatabase() {
		Map<String, Object> result = new HashMap<>();
		try {
			// 데이터베이스 연결 테스트
			UserInfo userInfo = loginService.getUserInfoByUserId("thepine");
			if (userInfo != null) {
				result.put("status", "success");
				result.put("message", "데이터베이스 연결 성공");
				result.put("userInfo", userInfo);
			} else {
				result.put("status", "error");
				result.put("message", "사용자 정보를 찾을 수 없음");
			}
		} catch (Exception e) {
			result.put("status", "error");
			result.put("message", "데이터베이스 연결 실패: " + e.getMessage());
			logger.error("데이터베이스 연결 테스트 실패", e);
		}
		return result;
	}
	
	@GetMapping(value = "/test/encrypt", produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Object> testEncrypt() {
		Map<String, Object> result = new HashMap<>();
		try {
			AES256Util aes256 = new AES256Util();
			String password = "hnt2023";
			String encrypted = aes256.encrypt(password);
			
			result.put("original", password);
			result.put("encrypted", encrypted);
			result.put("db_value", "KOTTmmlJqtJ6MAcOXKG47g==");
			result.put("match", encrypted.equals("KOTTmmlJqtJ6MAcOXKG47g=="));
			
			logger.info("암호화 테스트 - 원본: {}, 암호화: {}, DB값: {}, 일치: {}", 
				password, encrypted, "KOTTmmlJqtJ6MAcOXKG47g==", encrypted.equals("KOTTmmlJqtJ6MAcOXKG47g=="));
		} catch (Exception e) {
			result.put("status", "error");
			result.put("message", "암호화 실패: " + e.getMessage());
			logger.error("암호화 테스트 실패", e);
		}
		return result;
	}

	public static void main(String[] args) {
		try {
			String token = "dimotaOSTsuaGRM5LGoBfb:APA91bEWbuPNACaOKKF9320Z7CjNxZKZY4emUROs18zscNalOXQgVFNIB-ujaKg0pNE9wAqOdQLErA3JI0hrAnX89p8lk3kLrHK4tpjgscmakenOPc0d6fcIm3euOhPncXFIqTiblQOe";

			RetroInterface retroInterface = RetroClient.getApiService();
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("userId", "andrew2768");
			param.put("sensorId", "andrew2768");
			param.put("sensorUuid", "2C3AE80A61AC");
			param.put("sensorValue", "27");
			param.put("token", token);
			param.put("name", "ain");
			param.put("type", "1");

			Call<Map<String, Object>> result = retroInterface.sendAlarm(param);
			result.enqueue(new Callback<Map<String, Object>>() {
				@Override
				public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
					if(response.isSuccessful()) {
						Map<String, Object> resultMap = new HashMap<String, Object>();
						resultMap = response.body();

						if(null != resultMap && 0 < resultMap.size()) {
							logger.info("결과 코드: {}", resultMap.get("resultCode"));
						}
					}
				}

				@Override
				public void onFailure(Call<Map<String, Object>> call, Throwable t) {
					logger.error("API 호출 실패", t);
				}
			});

		} catch(Exception e) {
			logger.error("테스트 실행 중 오류 발생", e);
		}
	}

}
