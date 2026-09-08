package me.kveex.akttapispringed.parser;

import lombok.extern.slf4j.Slf4j;
import me.kveex.akttapispringed.service.impl.ScheduleParserServiceImpl;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
public class PDFScheduleParser implements IScheduleParser {
    // TODO: Сделать полноценный парсинг пдф, сейчас этот код только для теста безопасности
    private final PDDocument document;
    private final ScheduleParserServiceImpl scheduleParserService;

    public PDFScheduleParser(ScheduleParserServiceImpl scheduleParserService, byte[] input) throws IOException {
        this.scheduleParserService = scheduleParserService;
        this.document = Loader.loadPDF(input);
    }

    @Override
    public void parse() {
        PDFTextStripper textStripper = new PDFTextStripper();
        textStripper.setStartPage(1);
        textStripper.setEndPage(1);
        textStripper.setSortByPosition(true);
        try {
            String rawText = textStripper.getText(document);
            log.info(rawText);
        } catch (IOException e) {
            log.error(e.getMessage());
        }

    }

    @Override
    public LocalDateTime scheduleEditDate() {
        return null;
    }

    @Override
    public List<String> scheduleDateLines() {
        return List.of();
    }

    @Override
    public List<ScheduleParserServiceImpl.Info> timeAndInfoForScheduleGroup() {
        return List.of();
    }

    @Override
    public boolean isWholeScheduleDistant() {
        return false;
    }
}
