package com.random.utils;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Excel 导出工具类。
 */
public class ExcelUtil {

    /**
     * 生成一个简单的 Excel 文件字节数组。
     *
     * @param sheetName 工作表名称
     * @param headers   表头
     * @param rows      数据行（每行为一列字符串集合）
     * @return Excel 文件字节数组
     */
    public static byte[] create(String sheetName, String[] headers, List<List<String>> rows) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (List<String> row : rows) {
                Row r = sheet.createRow(rowIdx++);
                for (int i = 0; i < row.size(); i++) {
                    String value = row.get(i);
                    r.createCell(i).setCellValue(value == null ? "" : value);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Excel生成失败", e);
        }
    }

}
