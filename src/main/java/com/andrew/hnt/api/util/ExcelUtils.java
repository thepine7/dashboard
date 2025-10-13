package com.andrew.hnt.api.util;

import com.andrew.hnt.api.model.ExcelTest;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class ExcelUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ExcelUtils.class);

    public static ByteArrayInputStream createListToExcel(List<String> excelHeader, List<ExcelTest> excelTestList, List<ExcelTest> excelTestList2) {
        try (Workbook workbook = new HSSFWorkbook()) {
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
        try (Workbook workbook = new HSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("일간 데이터");
            Sheet sheet2 = workbook.createSheet("월간 데이터");
            Sheet sheet3 = workbook.createSheet("연간 데이터");
            Row row;
            Row row2;
            Row row3;
            Cell cell;
            Cell cell2;
            Cell cell3;
            int rowNo = 2;
            int rowNo2 = 2;
            int rowNo3 = 2;

            int headerSize2 = excelHeader2.size();
            int headerSize3 = excelHeader3.size();

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

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setBorderTop(BorderStyle.THICK);
            titleStyle.setBorderBottom(BorderStyle.THICK);
            titleStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font titleFont = workbook.createFont();
            titleFont.setColor(Font.COLOR_NORMAL);
            titleFont.setBold(true);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            // === 일간 데이터 처리 (성능 개선) ===
            row = sheet.createRow(rowNo++);
            Cell titleCell = row.createCell(0);
            titleCell.setCellStyle(titleStyle);
            titleCell.setCellValue(sensorName + "장치 데이터");
            for (int i = 0; i < excelHeader.size(); i++) {
                cell = row.createCell(i+1);
                cell.setCellStyle(headStyle);
                cell.setCellValue(excelHeader.get(i));
            }

            // 데이터 매핑을 위한 Map 생성 (성능 개선)
            Map<String, Map<String, Object>> dailyDataMap = new HashMap<>();
            for (Map<String, Object> data : dailyList) {
                String date = String.valueOf(data.get("getDate"));
                String time = String.valueOf(data.get("inst_dtm"));
                // 시간 형식 정규화 (YYYY-MM-DD HH:mm → HH:mm)
                String timeOnly = time.contains(" ") ? time.split(" ")[1] : time;
                dailyDataMap.put(date + "_" + timeOnly, data);
            }

            // 날짜별로 데이터 정리
            Set<String> daySet = new HashSet<>();
            for (Map<String, Object> data : dailyList) {
                daySet.add(String.valueOf(data.get("getDate")));
            }
            List<String> dayList = new ArrayList<>(daySet);
            Collections.sort(dayList);

            // 일간 데이터 행 생성 (성능 개선된 로직)
            for (int j = 0; j < dayList.size(); j++) {
                Row dataRow = sheet.createRow(j + 3);
                dataRow.createCell(0).setCellValue(dayList.get(j));
                
                // 각 시간대별 데이터 매핑
                for (int k = 0; k < excelHeader.size(); k++) {
                    String timeSlot = excelHeader.get(k); // "00:00", "00:30" 등
                    String key = dayList.get(j) + "_" + timeSlot;
                    Map<String, Object> data = dailyDataMap.get(key);
                    
                    if (data != null) {
                        String sensorValue = String.valueOf(data.get("sensor_value"));
                        dataRow.createCell(k + 1).setCellValue(sensorValue);
                    } else {
                        dataRow.createCell(k + 1).setCellValue(""); // 데이터 없음
                    }
                }
            }

            // === 월간 데이터 처리 ===
            row2 = sheet2.createRow(rowNo2++);
            Cell titleCell2 = row2.createCell(0);
            titleCell2.setCellStyle(titleStyle);
            titleCell2.setCellValue(sensorName + "장치 데이터");
            for (int i = 0; i < headerSize2; i++) {
                cell2 = row2.createCell(i + 1);
                cell2.setCellStyle(headStyle);
                cell2.setCellValue(excelHeader2.get(i));
            }

            // 월간 데이터 행 생성
            if (monthlyList != null && !monthlyList.isEmpty()) {
                Row dataRow2 = sheet2.createRow(3);
                for (int j = 0; j < monthlyList.size(); j++) {
                    Map<String, Object> data = monthlyList.get(j);
                    String sensorValue = String.valueOf(data.get("sensor_value"));
                    dataRow2.createCell(j + 1).setCellValue(sensorValue);
                }
            }

            // === 연간 데이터 처리 ===
            row3 = sheet3.createRow(rowNo3++);
            Cell titleCell3 = row3.createCell(0);
            titleCell3.setCellStyle(titleStyle);
            titleCell3.setCellValue(sensorName + "장치 데이터");
            for (int i = 0; i < headerSize3; i++) {
                cell3 = row3.createCell(i + 1);
                cell3.setCellStyle(headStyle);
                cell3.setCellValue(excelHeader3.get(i));
            }

            // 연간 데이터 행 생성
            if (yearlyList != null && !yearlyList.isEmpty()) {
                Row dataRow3 = sheet3.createRow(3);
                for (int j = 0; j < yearlyList.size(); j++) {
                    Map<String, Object> data = yearlyList.get(j);
                    String sensorValue = String.valueOf(data.get("sensor_value"));
                    dataRow3.createCell(j + 1).setCellValue(sensorValue);
                }
            }

            // === 차트 생성 (선택적 - 성능 개선) ===
            // 차트 생성이 너무 느리면 주석 처리
            /*
            DefaultCategoryDataset chartData1 = new DefaultCategoryDataset();
            for (Map<String, Object> data : dailyList) {
                try {
                    double value = Double.parseDouble(String.valueOf(data.get("sensor_value")));
                    String time = String.valueOf(data.get("inst_dtm"));
                    chartData1.addValue(value, "Series1", time);
                } catch (NumberFormatException e) {
                    // 숫자 변환 실패 시 무시
                }
            }

            JFreeChart LineChartObject = ChartFactory.createLineChart("Daily Chart", "Date", "Temp", chartData1, PlotOrientation.VERTICAL, false, true, false);
            int width = 640;
            int height = 480;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ChartUtilities.writeChartAsPNG(baos, LineChartObject, width, height);

            int my_picture_id = workbook.addPicture(baos.toByteArray(), workbook.PICTURE_TYPE_PNG);
            HSSFClientAnchor anchor = new HSSFClientAnchor();
            int col1 = 1, row1 = sheet.getLastRowNum();
            anchor.setAnchor((short) col1, row1 + 2, 0, 0, (short) 28, row1 + 30, 0, 0);
            anchor.setAnchorType(ClientAnchor.AnchorType.byId(3));
            HSSFPatriarch patriarch = (HSSFPatriarch) sheet.createDrawingPatriarch();
            patriarch.createPicture(anchor, my_picture_id);
            */

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
        }
    }
}