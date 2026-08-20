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
     * 生成一个简单的 Excel 工作簿（.xlsx 格式）并输出为字节数组。
     * <p>
     * 该方法使用 Apache POI 的 {@link XSSFWorkbook} 创建 Excel 文件，适用于中小规模数据导出。
     * 表头为第一行，后续每行数据按顺序填充。所有单元格内容均转为字符串形式。
     * 生成的 Excel 不包含任何样式（如边框、颜色、列宽自动调整），如需复杂格式请另行扩展。
     *
     * @param sheetName 工作表名称，不能为 {@code null} 或空字符串
     * @param headers   表头数组，按顺序对应各列标题，不能为 {@code null}
     * @param rows      数据行列表，每行为一个字符串列表，代表该行的各列值；
     *                  若某一列值为 {@code null}，则写入空字符串
     * @return Excel 文件的字节数组，可直接用于输出到响应流或保存为文件
     * @throws RuntimeException 当 IO 异常或生成过程中发生错误时抛出（包装为运行时异常）
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
