package com.mrOrchestrator.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Импортирует номера задач из XLSX-выгрузки.
 */
public class XlsxTaskImportService {

    private static final String TASK_TYPE_HEADER = "[jira] Тип задачи";
    private static final String TITLE_HEADER = "Название";
    private static final String SUB_TASK_MARKER = "(SUB-TASK)";
    private static final Pattern TASK_KEY_PATTERN = Pattern.compile("\\bRKK-\\d+\\b", Pattern.CASE_INSENSITIVE);

    public List<String> importTaskKeys(File file) throws Exception {
        try (FileInputStream inputStream = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("В XLSX-файле нет листов");
            }

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            HeaderColumns headerColumns = findHeaderColumns(sheet, formatter, evaluator);
            Set<String> taskKeys = new LinkedHashSet<>();

            for (int rowIndex = headerColumns.rowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String taskType = readCell(row, headerColumns.taskTypeColumnIndex, formatter, evaluator);
                if (taskType.toUpperCase(Locale.ROOT).contains(SUB_TASK_MARKER)) {
                    continue;
                }

                String title = readCell(row, headerColumns.titleColumnIndex, formatter, evaluator);
                Matcher matcher = TASK_KEY_PATTERN.matcher(title);
                if (matcher.find()) {
                    taskKeys.add(matcher.group().toUpperCase(Locale.ROOT));
                }
            }

            return new ArrayList<>(taskKeys);
        }
    }

    private HeaderColumns findHeaderColumns(Sheet sheet, DataFormatter formatter,
                                            FormulaEvaluator evaluator) {
        for (Row row : sheet) {
            int taskTypeColumnIndex = -1;
            int titleColumnIndex = -1;

            for (Cell cell : row) {
                String value = readCell(cell, formatter, evaluator).trim();
                if (TASK_TYPE_HEADER.equalsIgnoreCase(value)) {
                    taskTypeColumnIndex = cell.getColumnIndex();
                } else if (TITLE_HEADER.equalsIgnoreCase(value)) {
                    titleColumnIndex = cell.getColumnIndex();
                }
            }

            if (taskTypeColumnIndex >= 0 && titleColumnIndex >= 0) {
                return new HeaderColumns(row.getRowNum(), taskTypeColumnIndex, titleColumnIndex);
            }
        }

        throw new IllegalArgumentException("Не найдены столбцы \"" + TASK_TYPE_HEADER + "\" и \"" + TITLE_HEADER + "\"");
    }

    private String readCell(Row row, int columnIndex, DataFormatter formatter,
                            FormulaEvaluator evaluator) {
        return readCell(row.getCell(columnIndex), formatter, evaluator);
    }

    private String readCell(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell, evaluator);
    }

    private record HeaderColumns(int rowIndex, int taskTypeColumnIndex, int titleColumnIndex) {
    }
}
