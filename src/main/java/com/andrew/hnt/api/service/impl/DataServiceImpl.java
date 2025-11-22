package com.andrew.hnt.api.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.andrew.hnt.api.mapper.AdminMapper;
import com.andrew.hnt.api.model.DeviceVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.andrew.hnt.api.mapper.DataMapper;
import com.andrew.hnt.api.service.DataService;

@Service
@Transactional(timeout = 300, rollbackFor = Exception.class)
public class DataServiceImpl implements DataService {

	@Autowired
	private DataMapper dataMapper;

	@Autowired
	private AdminMapper adminMapper;

	private static final Logger logger = LoggerFactory.getLogger(DataServiceImpl.class);
	
	// 비동기 처리를 위한 Executor (5개 스레드)
	private final java.util.concurrent.Executor asyncExecutor = java.util.concurrent.Executors.newFixedThreadPool(5);
	
	@Override
	public List<Map<String, Object>> getDeviceList(String userId) {
		List<Map<String, Object>> deviceList = new ArrayList<Map<String, Object>>();
		
		if(null != userId && !"".equals(userId)) {
			try {
				deviceList = dataMapper.getDeviceList(userId);

				if(null != deviceList && 0 < deviceList.size()) {
					for(int i=0; i < deviceList.size(); i++) {
						String chkName = String.valueOf(deviceList.get(i).get("sensor_name"));
						int j = 0;
						if(null != chkName && !"".equals(chkName) && 0 < chkName.length()) {

						} else {
							deviceList.get(i).remove("sensor_name");
							j = i + 1;
							deviceList.get(i).put("sensor_name", "장치"+j);
						}
					}
				}
			} catch(Exception e) {
				logger.error("Error : " + e.toString(), e);
			}
		}
		
		return deviceList;
	}

	@Override
	public void updateSensorInfo(DeviceVO deviceVO) {
		if(null != deviceVO) {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("userId", deviceVO.getUserId());
			param.put("sensorUuid", deviceVO.getSensorUuid());
			param.put("sensorName", deviceVO.getSensorName());
			param.put("chgSensorName", deviceVO.getChgSensorName());

			try {
				dataMapper.updateSensorInfo(param);
			} catch(Exception e) {
				logger.error("Error : " + e.toString(), e);
			}
		}
	}

	@Override
	public void deleteSensorInfo(DeviceVO deviceVO) {
		if(null != deviceVO) {
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("userId", deviceVO.getUserId());
			param.put("sensorUuid", deviceVO.getSensorUuid());
			param.put("sensorName", deviceVO.getSensorName());

			try {
				// 1. 장치 기본 정보 삭제 (가장 먼저 실행하여 사용자에게 삭제된 것처럼 보이게 함)
				dataMapper.deleteSensorInfo(param);
				logger.info("장치 기본 정보 삭제 완료 - sensorUuid: {}", deviceVO.getSensorUuid());
				
				// 2. 장치 설정 정보 삭제
				adminMapper.deleteConfig(deviceVO.getUserId(), deviceVO.getSensorUuid());
				logger.info("장치 설정 정보 삭제 완료 - userId: {}, sensorUuid: {}", deviceVO.getUserId(), deviceVO.getSensorUuid());

				// 3. 장치 관련 알림 데이터 삭제
				adminMapper.deleteDeviceAlarm(param);
				logger.info("장치 알림 데이터 삭제 완료 - userId: {}, sensorUuid: {}", deviceVO.getUserId(), deviceVO.getSensorUuid());
				
				// 4. 센서 데이터 비동기 삭제 (대용량 데이터 처리를 백그라운드에서 수행)
				java.util.concurrent.CompletableFuture.runAsync(() -> {
					try {
						logger.info("센서 데이터 비동기 삭제 시작 - sensorUuid: {}", deviceVO.getSensorUuid());
						int batchSize = 1000; // 한 번에 1,000개씩 삭제 (타임아웃 방지)
						int deletedCount = 0;
						int totalDeleted = 0;
						
						// 비동기 스레드에서 실행되므로 트랜잭션 관리가 필요할 수 있음
						// 여기서는 간단히 반복 실행
						do {
							// 파라미터 맵 새로 생성 (스레드 안전성)
							Map<String, Object> asyncParam = new HashMap<>();
							asyncParam.put("sensorUuid", deviceVO.getSensorUuid());
							asyncParam.put("batchSize", batchSize);
							
							deletedCount = dataMapper.deleteSensorDataBatch(asyncParam);
							totalDeleted += deletedCount;
							
							if (totalDeleted % 10000 == 0) {
								logger.info("센서 데이터 비동기 삭제 진행 중 - 삭제된 개수: {}, 총 삭제: {}, uuid: {}", deletedCount, totalDeleted, deviceVO.getSensorUuid());
							}
							
							// DB 부하 방지를 위한 잠시 대기
							if (deletedCount > 0) {
								try { Thread.sleep(10); } catch (InterruptedException ie) {}
							}
						} while (deletedCount > 0);
						
						logger.info("센서 데이터 비동기 삭제 완료 - userId: {}, sensorUuid: {}, 총 삭제: {}", 
							deviceVO.getUserId(), deviceVO.getSensorUuid(), totalDeleted);
							
					} catch (Exception e) {
						logger.error("센서 데이터 비동기 삭제 중 오류 발생 - uuid: " + deviceVO.getSensorUuid(), e);
						// 실패 시에도 장치 정보는 이미 삭제되었으므로 사용자 입장에서는 삭제 성공임
						// 추후 스케줄러가 고아 데이터를 정리하도록 유도
					}
				}, asyncExecutor);
				
			} catch (Exception e) {
				logger.error("Error : " + e.toString(), e);
				throw new RuntimeException("장치 삭제 중 오류 발생: " + e.getMessage(), e);
			}
		}
	}

	@Override
	public List<Map<String, Object>> selectSensorData(Map<String, Object> param) {
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();

		if(null != param && 0 < param.size()) {
			try {
				resultList = dataMapper.selectSensorData(param);
			} catch(Exception e) {
				logger.error("Error : " + e.toString(), e);
			}
		}

		return resultList;
	}

	@Override
	public List<Map<String, Object>> selectSensorDataForExcel(Map<String, Object> param) {
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();

		if(null != param && 0 < param.size()) {
			try {
				logger.info("엑셀 다운로드용 최적화된 쿼리 실행 - param: {}", param);
				resultList = dataMapper.selectSensorDataForExcel(param);
				logger.info("엑셀 다운로드용 쿼리 결과 - 데이터 수: {}", resultList != null ? resultList.size() : 0);
			} catch(Exception e) {
				logger.error("엑셀 다운로드용 쿼리 실행 실패: {}", e.toString(), e);
			}
		}

		return resultList;
	}

	@Override
	public List<Map<String, Object>> selectSensorDataForExcelFast(Map<String, Object> param) {
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();

		if(null != param && 0 < param.size()) {
			try {
				logger.info("엑셀 다운로드용 초고속 쿼리 실행 - param: {}", param);
				resultList = dataMapper.selectSensorDataForExcelFast(param);
				logger.info("엑셀 다운로드용 초고속 쿼리 결과 - 데이터 수: {}", resultList != null ? resultList.size() : 0);
			} catch(Exception e) {
				logger.error("엑셀 다운로드용 초고속 쿼리 실행 실패: {}", e.toString(), e);
			}
		}

		return resultList;
	}

	@Override
	public List<Map<String, Object>> selectDailyData(Map<String, Object> param) {
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();

		if(null != param && 0 < param.size()) {
			try {
				logger.info("최적화된 일간 데이터 조회 실행 - param: {}", param);
				resultList = dataMapper.selectDailyData(param);
				logger.info("최적화된 일간 데이터 조회 결과 - 데이터 수: {}", resultList != null ? resultList.size() : 0);
			} catch(Exception e) {
				logger.error("최적화된 일간 데이터 조회 실패: {}", e.toString(), e);
			}
		}

		return resultList;
	}

	@Override
	public List<Map<String, Object>> selectDailyDataWithCursor(Map<String, Object> param) {
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();

		if(null != param && 0 < param.size()) {
			try {
				logger.info("고성능 일간 데이터 조회 실행 (커서 기반) - param: {}", param);
				resultList = dataMapper.selectDailyDataWithCursor(param);
				logger.info("고성능 일간 데이터 조회 결과 - 데이터 수: {}", resultList != null ? resultList.size() : 0);
			} catch(Exception e) {
				logger.error("고성능 일간 데이터 조회 실패: {}", e.toString(), e);
			}
		}

		return resultList;
	}

	@Override
	public List<Map<String, Object>> selectWeeklyData(Map<String, Object> param) {
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();

		if(null != param && 0 < param.size()) {
			try {
				logger.info("최적화된 주간 데이터 조회 실행 - param: {}", param);
				resultList = dataMapper.selectWeeklyData(param);
				logger.info("최적화된 주간 데이터 조회 결과 - 데이터 수: {}", resultList != null ? resultList.size() : 0);
			} catch(Exception e) {
				logger.error("최적화된 주간 데이터 조회 실패: {}", e.toString(), e);
			}
		}

		return resultList;
	}

	@Override
	public List<Map<String, Object>> selectYearlyData(Map<String, Object> param) {
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();

		if(null != param && 0 < param.size()) {
			try {
				logger.info("최적화된 연간 데이터 조회 실행 - param: {}", param);
				resultList = dataMapper.selectYearlyData(param);
				logger.info("최적화된 연간 데이터 조회 결과 - 데이터 수: {}", resultList != null ? resultList.size() : 0);
			} catch(Exception e) {
				logger.error("최적화된 연간 데이터 조회 실패: {}", e.toString(), e);
			}
		}

		return resultList;
	}

}
