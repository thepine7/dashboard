package com.andrew.hnt.api.util;

import com.andrew.hnt.api.model.ExcelTest;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class ExcelUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ExcelUtils.class);
    
    // === 성능 최적화: 스타일 생성 메서드들 ===
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
    
    private static CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THICK);
        style.setBorderBottom(BorderStyle.THICK);
        style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font titleFont = workbook.createFont();
        titleFont.setColor(Font.COLOR_NORMAL);
        titleFont.setBold(true);
        style.setFont(titleFont);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
    
    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    public static ByteArrayInputStream createListToExcel(List<String> excelHeader, List<ExcelTest> excelTestList, List<ExcelTest> excelTestList2) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("excelDownloadTest");
            Row row;
            Cell cell;
            int rowNo = 0;

            int headerSize = excelHeader.size();

            // 테이블 헤더 스타일 설정
            CellStyle headStyle = workbook.createCellStyle();
            // 경계선 설정
            headStyle.setBorderTop(BorderStyle.THIN);
            headStyle.setBorderBottom(BorderStyle.THIN);
            headStyle.setBorderLeft(BorderStyle.THIN);
            headStyle.setBorderRight(BorderStyle.THIN);
            // 색상 (노란색 대신 회색 사용)
            headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            // 헤더 가운데 정렬
            headStyle.setAlignment(HorizontalAlignment.CENTER);

            // 헤더 생성
            row = sheet.createRow(rowNo++);
            for (int i=0; i<headerSize; i++) {
                cell = row.createCell(i);
                cell.setCellStyle(headStyle);
                cell.setCellValue(excelHeader.get(i));
            }

            // 내용 생성
            for (int j=0; j<excelTestList.size(); j++) {
                Row dataRow = sheet.createRow(j + 1);
                dataRow.createCell(0).setCellValue(excelTestList.get(j).getName());
                dataRow.createCell(1).setCellValue(excelTestList.get(j).getBirth());
                dataRow.createCell(2).setCellValue(excelTestList.get(j).getPhoneNumber());
                dataRow.createCell(3).setCellValue(excelTestList.get(j).getAddress());
            }

            // 구분선 행 생성
            sheet.createRow(excelTestList.size() + 2);

            for (int j=0; j < excelTestList2.size(); j++) {
                Row dataRow2 = sheet.createRow(j + excelTestList.size() + 2 + 1);
                dataRow2.createCell(0).setCellValue(excelTestList2.get(j).getName());
                dataRow2.createCell(1).setCellValue(excelTestList2.get(j).getBirth());
                dataRow2.createCell(2).setCellValue(excelTestList2.get(j).getPhoneNumber());
                dataRow2.createCell(3).setCellValue(excelTestList2.get(j).getAddress());
            }

            // 컬럼 자동 크기 조정 제거 (성능 최적화)
            // sheet.autoSizeColumn(0);
            // sheet.autoSizeColumn(1);
            // sheet.autoSizeColumn(2);
            // sheet.autoSizeColumn(3);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());

        } catch (IOException e) {
            logger.error("엑셀 파일 생성 중 오류 발생", e);
            return null;
        }
    }

    public static ByteArrayInputStream createDataExcel(List<String> excelHeader, List<String> excelHeader2, List<String> excelHeader3, List<Map<String, Object>> dailyList, List<Map<String, Object>> monthlyList, List<Map<String, Object>> yearlyList, String sensorName) {
        long startTime = System.currentTimeMillis();
        logger.info("엑셀 파일 생성 시작 - 일간: {}, 월간: {}, 연간: {}", 
            dailyList != null ? dailyList.size() : 0,
            monthlyList != null ? monthlyList.size() : 0,
            yearlyList != null ? yearlyList.size() : 0);
        
        // === 템플릿 로드 시도 ===
        Workbook workbook = loadTemplateIfExists();
        boolean isFromTemplate = (workbook != null);
        
        if (isFromTemplate) {
            logger.info("엑셀 템플릿을 로드했습니다. 데이터를 채웁니다.");
        } else {
            logger.info("엑셀 템플릿이 없습니다. 새 워크북을 생성합니다.");
        }
            
        try {
            // 템플릿이 없으면 새 워크북 생성
            if (workbook == null) {
                workbook = new XSSFWorkbook();
            }
            
            // 시트 가져오기 또는 생성
            Sheet sheet;
            Sheet sheet2;
            if (isFromTemplate) {
                // 템플릿에서 시트 가져오기
                sheet = workbook.getSheet("일간 데이터");
                sheet2 = workbook.getSheet("월간 데이터");
                
                // 시트가 없으면 생성
                if (sheet == null) {
                    sheet = workbook.createSheet("일간 데이터");
                }
                if (sheet2 == null) {
                    sheet2 = workbook.createSheet("월간 데이터");
                }
            } else {
                // 새 시트 생성
                sheet = workbook.createSheet("일간 데이터");
                sheet2 = workbook.createSheet("월간 데이터");
            }
            
            // === 성능 최적화: 스타일 미리 생성 및 재사용 ===
            CellStyle headStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            int rowNo = 2;
            int rowNo2 = 2;
            
            // 변수 선언
            Row row, row2;
            Cell cell, cell2;

            // === 일간 데이터 처리 (성능 최적화) ===
            Row titleRow = sheet.createRow(rowNo++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellStyle(titleStyle);
            titleCell.setCellValue(sensorName + "장치 데이터");
            
            // 헤더 행 생성
            Row headerRow = sheet.createRow(rowNo++);
            
            // 첫 번째 셀: "날짜명" 헤더 (세로축)
            Cell dateHeaderCell = headerRow.createCell(0);
            dateHeaderCell.setCellStyle(headStyle);
            dateHeaderCell.setCellValue("날짜명");
            
            // 나머지 셀: 시간 헤더 (가로축: 0:00, 0:01, 0:02, ...)
            for (int i = 0; i < excelHeader.size(); i++) {
                Cell headerCell = headerRow.createCell(i + 1);
                headerCell.setCellStyle(headStyle);
                headerCell.setCellValue(excelHeader.get(i));
            }

            // === 성능 최적화: 데이터 매핑 최적화 (1분 단위) ===
            Map<String, Map<String, Object>> dailyDataMap = new HashMap<>();
            Set<String> daySet = new HashSet<>();
            
            // 한 번의 루프로 데이터 매핑과 날짜 수집을 동시에 처리
            for (Map<String, Object> data : dailyList) {
                String date = String.valueOf(data.get("getDate"));
                String time = String.valueOf(data.get("inst_dtm"));
                String timeOnly = time.contains(" ") ? time.split(" ")[1] : time;
                dailyDataMap.put(date + "_" + timeOnly, data);
                daySet.add(date);
            }
            
            logger.info("데이터 매핑 완료 - 날짜 수: {}, 전체 데이터 수: {}", daySet.size(), dailyList.size());
            
            List<String> dayList = new ArrayList<>(daySet);
            Collections.sort(dayList);

            // === 성능 최적화: 일간 데이터 행 생성 (빈 셀 생략) ===
            for (int j = 0; j < dayList.size(); j++) {
                Row dataRow = sheet.createRow(j + rowNo);
                Cell dateCell = dataRow.createCell(0);
                dateCell.setCellStyle(dataStyle);
                dateCell.setCellValue(dayList.get(j));
                
                // 각 시간대별 데이터 매핑 (최적화: 빈 값은 셀을 생성하지 않음)
                for (int k = 0; k < excelHeader.size(); k++) {
                    String timeSlot = excelHeader.get(k);
                    String key = dayList.get(j) + "_" + timeSlot;
                    Map<String, Object> data = dailyDataMap.get(key);
                    
                    // 데이터가 있을 때만 셀 생성 (성능 최적화)
                    if (data != null) {
                    Cell dataCell = dataRow.createCell(k + 1);
                    dataCell.setCellStyle(dataStyle);
                    
                        Object sensorValue = data.get("sensor_value");
                        if (sensorValue != null) {
                            try {
                                // 숫자 타입으로 변환 시도 (성능 최적화)
                                double numValue = Double.parseDouble(String.valueOf(sensorValue));
                                dataCell.setCellValue(numValue);
                            } catch (NumberFormatException e) {
                                // 문자열로 저장
                                dataCell.setCellValue(String.valueOf(sensorValue));
                            }
                        }
                    }
                }
            }
            
            logger.info("일간 데이터 처리 완료 - 소요시간: {}ms", System.currentTimeMillis() - startTime);

            // === 월간 데이터 처리 (세로=년도+월, 가로=일자) ===
            Row titleRow2 = sheet2.createRow(rowNo2++);
            Cell titleCell2 = titleRow2.createCell(0);
            titleCell2.setCellStyle(titleStyle);
            titleCell2.setCellValue(sensorName + "장치 데이터");
            
            // 월간 데이터 매핑 (년월과 일자 추출)
            Map<String, Map<String, Object>> monthlyDataMap = new HashMap<>();
            Set<String> yearMonthSet = new HashSet<>();
            
            if (monthlyList != null && !monthlyList.isEmpty()) {
                for (Map<String, Object> data : monthlyList) {
                    String instDtm = String.valueOf(data.get("inst_dtm"));
                    // inst_dtm 형식: YYYY-MM-DD
                    if (instDtm != null && instDtm.length() >= 10) {
                        String yearMonth = instDtm.substring(0, 7);  // YYYY-MM
                        String day = instDtm.substring(8, 10);  // DD
                        monthlyDataMap.put(yearMonth + "_" + day, data);
                        yearMonthSet.add(yearMonth);
                    }
                }
            }
            
            List<String> yearMonthList = new ArrayList<>(yearMonthSet);
            Collections.sort(yearMonthList);
            
            // 헤더 행 생성
            Row headerRow2 = sheet2.createRow(rowNo2++);
            
            // 첫 번째 셀: "날짜명" 헤더
            Cell yearMonthHeaderCell = headerRow2.createCell(0);
            yearMonthHeaderCell.setCellStyle(headStyle);
            yearMonthHeaderCell.setCellValue("날짜명");
            
            // 나머지 셀: 일자 헤더 (1일~31일)
            for (int i = 1; i <= 31; i++) {
                Cell headerCell = headerRow2.createCell(i);
                headerCell.setCellStyle(headStyle);
                headerCell.setCellValue(i + "일");
            }
            
            // 월간 데이터 행 생성 (년월별로 행 생성, 성능 최적화)
            for (int j = 0; j < yearMonthList.size(); j++) {
                Row dataRow2 = sheet2.createRow(j + rowNo2);
                Cell yearMonthCell = dataRow2.createCell(0);
                yearMonthCell.setCellStyle(dataStyle);
                yearMonthCell.setCellValue(yearMonthList.get(j));
                
                // 각 일자별 데이터 매핑 (1일~31일, 빈 셀 생략)
                for (int day = 1; day <= 31; day++) {
                    String yearMonth = yearMonthList.get(j);
                    String dayStr = String.format("%02d", day);  // 01, 02, ..., 31
                    String key = yearMonth + "_" + dayStr;
                    Map<String, Object> data = monthlyDataMap.get(key);
                    
                    // 데이터가 있을 때만 셀 생성 (성능 최적화)
                    if (data != null) {
                        Cell dataCell = dataRow2.createCell(day);
                        dataCell.setCellStyle(dataStyle);
                        
                        Object sensorValue = data.get("sensor_value");
                        if (sensorValue != null) {
                            try {
                                // 숫자 타입으로 변환 시도 (성능 최적화)
                                double numValue = Double.parseDouble(String.valueOf(sensorValue));
                                dataCell.setCellValue(numValue);
                            } catch (NumberFormatException e) {
                                // 문자열로 저장
                                dataCell.setCellValue(String.valueOf(sensorValue));
                            }
                        }
                    }
                }
            }

            // === 차트 생성 비활성화 (안정성 우선) ===
            // Apache POI로 생성한 차트는 엑셀에서 "내용에 문제가 있다"는 경고를 발생시킬 수 있습니다.
            // 데이터만 깔끔하게 제공하여 사용자가 엑셀에서 직접 차트를 생성하는 것이 더 안정적입니다.
            logger.info("엑셀 파일 생성 완료 - 총 소요시간: {}ms", System.currentTimeMillis() - startTime);

            // 컬럼 자동 크기 조정 제거 (성능 최적화)
            // for (int i = 0; i < headerSize; i++) {
            //     sheet.autoSizeColumn(i);
            // }
            // 
            // for (int j = 0; j < headerSize2; j++) {
            //     sheet2.autoSizeColumn(j);
            // }
            // 
            // for (int k = 0; k < headerSize3; k++) {
            //     sheet3.autoSizeColumn(k);
            // }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());

        } catch (IOException e) {
            logger.error("엑셀 파일 생성 중 오류 발생", e);
            return null;
        } finally {
            // Workbook 리소스 정리
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    logger.warn("Workbook 닫기 실패", e);
                }
            }
        }
    }
    
    /**
     * 숫자를 엑셀 컬럼명으로 변환 (1 -> A, 2 -> B, ..., 27 -> AA, ...)
     */
    private static String getExcelColumnName(int columnNumber) {
        StringBuilder columnName = new StringBuilder();
        while (columnNumber > 0) {
            int rem = columnNumber % 26;
            if (rem == 0) {
                columnName.insert(0, "Z");
                columnNumber = (columnNumber / 26) - 1;
            } else {
                columnName.insert(0, (char) ((rem - 1) + 'A'));
                columnNumber = columnNumber / 26;
            }
        }
        return columnName.toString();
    }
    
    /**
     * 엑셀 템플릿 파일을 로드합니다.
     * 템플릿이 없으면 null을 반환합니다.
     */
    private static Workbook loadTemplateIfExists() {
        try {
            // 클래스패스에서 템플릿 파일 찾기
            java.io.InputStream templateStream = ExcelUtils.class.getResourceAsStream("/excel-templates/chart-template.xlsx");
            
            if (templateStream != null) {
                logger.info("엑셀 템플릿 파일을 찾았습니다: /excel-templates/chart-template.xlsx");
                XSSFWorkbook workbook = new XSSFWorkbook(templateStream);
                templateStream.close();
                return workbook;
            } else {
                logger.debug("엑셀 템플릿 파일이 없습니다: /excel-templates/chart-template.xlsx");
                return null;
            }
        } catch (Exception e) {
            logger.warn("엑셀 템플릿 로드 실패: {}. 새 워크북을 생성합니다.", e.getMessage());
            return null;
        }
    }
}