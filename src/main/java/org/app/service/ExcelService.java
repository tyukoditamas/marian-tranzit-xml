package org.app.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.app.dto.HouseConsigment;
import org.app.dto.TranzitDto;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class ExcelService {

    /**
     * Copy the template from resources to the output folder 1:1 (preserves comments, table filters, etc.),
     * then write a couple of sample fields to demonstrate the pipeline.
     *
     * @param templateResourceName e.g. "/template.xlsx"
     * @param outputDir            folder where the XML lives
     * @param outputFileName       e.g. "TransitOutput.xlsx"
     * @param dto                  parsed data
     * @return path to the written xlsx
     */
    public Path cloneTemplateAndWrite(String templateResourceName, Path outputDir, String outputFileName, TranzitDto dto) throws IOException {
        if (!Files.exists(outputDir)) Files.createDirectories(outputDir);
        Path out = outputDir.resolve(outputFileName);

        VerificareTvaClient tvaClient = new VerificareTvaClient();

        // 1) clone bytes 1:1
        try (InputStream in = getClass().getResourceAsStream(templateResourceName)) {
            if (in == null) throw new FileNotFoundException("Resource not found: " + templateResourceName);
            Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
        }

        // 2) write example fields (B2, B3) without disturbing other content
        try (InputStream in = Files.newInputStream(out);
             Workbook wb = new XSSFWorkbook(in)) {

            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : wb.createSheet("Sheet1");
            LocalDate date = convertToLocalDate(dto.getDataMrn());

            setCell(sheet, 1, 1, "MSTL");
            setCell(sheet, 2, 1, date.plusDays(1).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            setCell(sheet, 4, 1, date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            setCell(sheet, 5, 1, dto.getCustomOfficeDeparture());
            setCell(sheet, 6, 1, dto.getContainerNumber());

            int articleRowIndex = 13;
            for (HouseConsigment entry: dto.getHouseConsigmentList()) {
                String cui = entry.getIncadrareTarifara(); // ensure this is actually the CUI (e.g., "RO17581153" or "17581153")
                if (cui != null && !cui.trim().isEmpty()) {
                    try {
                        tvaClient.fetchCompanyNameByCui(cui.trim())
                                .ifPresent(entry::setConsigneName); // or another field
                        Thread.sleep(350L); // be polite; site has free limits
                    } catch (Exception ex) {
                        entry.setConsigneName("");
                    }
                }
                setCell(sheet, articleRowIndex, 2, entry.getConsigneeName().isEmpty()
                        ? entry.getConsigneeNumber() : entry.getConsigneeName());
                setCell(sheet, articleRowIndex, 3, entry.getPacks());
                setCell(sheet, articleRowIndex, 4, "PALLET");
                setCell(sheet, articleRowIndex, 5, entry.getWeight());
                setCell(sheet, articleRowIndex, 6, "KG");
                setCell(sheet, articleRowIndex, 15, dto.getDepartureTransportMeanFirst() + dto.getDepartureTransportMeanSecond());

                String description;
                if (entry.getGoodsDescription().contains("-")) {
                    description = entry.getGoodsDescription().split("-", 2)[0].trim();
                } else {
                    description = entry.getGoodsDescription().trim();
                }
                setCell(sheet, articleRowIndex, 16, description);
                setCell(sheet, articleRowIndex, 17, entry.getConsignorName());
                setCell(sheet, articleRowIndex, 20, entry.getIncadrareTarifara().substring(0, 4));
                articleRowIndex++;
            }

            try (OutputStream os = Files.newOutputStream(out)) {
                wb.write(os);
            }
        }
        return out;
    }

    private static void setCell(Sheet sheet, int rowIndex, int colIndex, String value) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) row = sheet.createRow(rowIndex);
        Cell cell = row.getCell(colIndex);
        if (cell == null) cell = row.createCell(colIndex);
        cell.setCellValue(value == null ? "" : value);
    }

    public static LocalDate convertToLocalDate(String isoDateTime) {
        return LocalDate.parse(isoDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
