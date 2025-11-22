package com.andrew.hnt.api.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface DataMapper {

	public List<Map<String, Object>> getDeviceList(String userId);

	public void updateSensorInfo(Map<String, Object> param);

	public void deleteSensorInfo(Map<String, Object> param);

	public void deleteSensorData(Map<String, Object> param);
	
	// 배치 삭제 (대용량 데이터 처리용)
	public int deleteSensorDataBatch(Map<String, Object> param);

	public List<Map<String, Object>> selectSensorData(Map<String, Object> param);

	// 엑셀 다운로드용 최적화된 쿼리
	public List<Map<String, Object>> selectSensorDataForExcel(Map<String, Object> param);

	// 엑셀 다운로드용 초고속 쿼리 (실제 데이터만 조회)
	public List<Map<String, Object>> selectSensorDataForExcelFast(Map<String, Object> param);
	
	// 최적화된 일간 데이터 조회 (30분 단위 그룹화)
	public List<Map<String, Object>> selectDailyData(Map<String, Object> param);
	
	// 고성능 일간 데이터 조회 (커서 기반 페이징)
	public List<Map<String, Object>> selectDailyDataWithCursor(Map<String, Object> param);
	
	// 최적화된 주간 데이터 조회
	public List<Map<String, Object>> selectWeeklyData(Map<String, Object> param);
	
	// 최적화된 연간 데이터 조회
	public List<Map<String, Object>> selectYearlyData(Map<String, Object> param);
}
