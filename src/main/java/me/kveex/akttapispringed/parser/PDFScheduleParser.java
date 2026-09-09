package me.kveex.akttapispringed.parser;

import lombok.extern.slf4j.Slf4j;
import me.kveex.akttapispringed.service.impl.ScheduleParserServiceImpl;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Класс сгенерирован ИИ, возможно будет заменён на более правильный и удобный формат добавления расписаний заранее
@Slf4j
public class PDFScheduleParser implements IScheduleParser {

    private final ScheduleParserServiceImpl scheduleParserService;
    private final byte[] bytes;

    private PDDocument document;

    private List<String> scheduleLines = List.of();
    private List<ScheduleParserServiceImpl.Info> infos = List.of();

    /**
     * Группа вида:
     * 25-41АВТ
     * 25-38БУХ
     * 24-37ТМ
     * 22-13ИС
     */
    private static final Pattern GROUP_PATTERN = Pattern.compile(
            "\\b\\d{2}-\\d{2}[А-ЯЁA-Z]{2,}\\b",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    /**
     * Реальные значения первого столбца времени из PDF.
     */
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "^(1,2|3,4|5,6|7,8|УП(?:\\.\\d+)?|ПП|ПДП|8[.,]00)\\b",
            Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> IGNORE_LINES = Set.of(
            "группа",
            "уроки",
            "наименование дисциплин",
            "практика",
            "экзаменационная сессия"
    );

    public PDFScheduleParser(
            ScheduleParserServiceImpl scheduleParserService,
            byte[] bytes
    ) {
        this.scheduleParserService = scheduleParserService;
        this.bytes = bytes;
    }

    @Override
    public void parse() {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            this.document = document;

            this.scheduleLines = extractScheduleLines(document);
            this.infos = extractInfos(document);

            if (infos.isEmpty()) {
                throw new IllegalStateException(
                        "PDF распарсен, но занятия не обнаружены"
                );
            }

            scheduleParserService.parse(
                    scheduleEditDate(),
                    scheduleDateLines(),
                    timeAndInfoForScheduleGroup(),
                    isWholeScheduleDistant()
            );
        } catch (IOException e) {
            log.info("Что-то случилось с PDF файлом: {}", e.getMessage());
        }
    }

    @Override
    public LocalDateTime scheduleEditDate() {
        PDDocumentInformation metadata = document.getDocumentInformation();

        Calendar modificationDate = metadata.getModificationDate();

        if (modificationDate != null) {
            return modificationDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        }

        Calendar creationDate = metadata.getCreationDate();

        if (creationDate != null) {
            return creationDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        }

        log.warn(
                "У PDF отсутствуют CreationDate и ModDate"
        );

        return null;
    }

    @Override
    public List<String> scheduleDateLines() {
        return scheduleLines;
    }

    @Override
    public List<ScheduleParserServiceImpl.Info> timeAndInfoForScheduleGroup() {
        return infos;
    }

    @Override
    public boolean isWholeScheduleDistant() {
        for (String line : scheduleLines) {
            String text = normalize(line).toLowerCase(Locale.ROOT);

            if (text.contains("расписание на")
                    && text.contains("дист")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Обычное извлечение текста.
     * Используется только для метаданных документа:
     * дата расписания и т.п.
     */
    private List<String> extractScheduleLines(
            PDDocument document
    ) throws IOException {

        PDFTextStripper stripper = new PDFTextStripper();

        stripper.setSortByPosition(true);
        stripper.setWordSeparator(" ");
        stripper.setLineSeparator("\n");

        String text = stripper.getText(document);

        List<String> result = new ArrayList<>();

        for (String line : text.split("\\R")) {
            String normalized = normalize(line);

            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }

        return result;
    }

    /**
     * Получаем три колонки PDF отдельно.
     * Координаты определены по-исходному testAktt.pdf.
     */
    private List<ScheduleParserServiceImpl.Info> extractInfos(
            PDDocument document
    ) throws IOException {

        List<ScheduleParserServiceImpl.Info> result =
                new ArrayList<>();

        for (PDPage page : document.getPages()) {

            float pageHeight = page.getCropBox().getHeight();

            /*
             * В testAktt.pdf:
             *
             * column 0: ~ 40..200
             * column 1: ~ 205..395
             * column 2: ~ 395..585
             */
            Rectangle2D[] columns = {
                    new Rectangle2D.Float(
                            40,
                            35,
                            160,
                            pageHeight - 40
                    ),

                    new Rectangle2D.Float(
                            205,
                            35,
                            185,
                            pageHeight - 40
                    ),

                    new Rectangle2D.Float(
                            395,
                            35,
                            190,
                            pageHeight - 40
                    )
            };

            for (int i = 0; i < columns.length; i++) {

                PDFTextStripperByArea stripper =
                        new PDFTextStripperByArea();

                stripper.setSortByPosition(true);
                stripper.setWordSeparator(" ");
                stripper.setLineSeparator("\n");

                String regionName = "column-" + i;

                stripper.addRegion(
                        regionName,
                        columns[i]
                );

                stripper.extractRegions(page);

                String columnText =
                        stripper.getTextForRegion(regionName);

                parseColumn(
                        columnText,
                        result
                );
            }
        }

        return result;
    }

    /**
     * Разбор одной из трёх колонок.
     */
    private void parseColumn(
            String text,
            List<ScheduleParserServiceImpl.Info> result
    ) {

        String currentGroup = null;
        String currentTime = null;

        StringBuilder currentInfo = new StringBuilder();

        for (String rawLine : text.split("\\R")) {

            String line = normalize(rawLine);

            if (line.isBlank()) {
                continue;
            }

            if (shouldIgnore(line)) {
                continue;
            }

            /*
             * Ищем новую группу.
             */
            Matcher groupMatcher =
                    GROUP_PATTERN.matcher(line);

            if (groupMatcher.find()) {

                flush(
                        currentGroup,
                        currentTime,
                        currentInfo,
                        result
                );

                currentTime = null;
                currentInfo.setLength(0);

                currentGroup =
                        groupMatcher.group().toLowerCase(Locale.ROOT);

                /*
                 * Убираем группу.
                 */
                line = normalize(
                        line.substring(groupMatcher.end())
                );

                if (line.isBlank()) {
                    continue;
                }
            }

            if (currentGroup == null) {
                continue;
            }

            /*
             * Ищем начало новой пары.
             */
            Matcher timeMatcher =
                    TIME_PATTERN.matcher(line);

            if (timeMatcher.find()) {

                flush(
                        currentGroup,
                        currentTime,
                        currentInfo,
                        result
                );

                currentTime =
                        normalizeTime(
                                timeMatcher.group()
                        );

                currentInfo.setLength(0);

                String info =
                        normalize(
                                line.substring(
                                        timeMatcher.end()
                                )
                        );

                if (!info.isBlank()) {
                    currentInfo.append(info);
                }

                continue;
            }

            /*
             * Если времени нет, это продолжение
             * предыдущей записи.
             */
            if (currentTime != null) {

                if (!currentInfo.isEmpty()) {
                    currentInfo.append(' ');
                }

                currentInfo.append(line);
            }
        }

        /*
         * Последняя запись колонки.
         */
        flush(
                currentGroup,
                currentTime,
                currentInfo,
                result
        );
    }

    private void flush(
            String group,
            String time,
            StringBuilder info,
            List<ScheduleParserServiceImpl.Info> result
    ) {

        if (group == null
                || time == null
                || info == null) {
            return;
        }

        String value =
                normalize(info.toString());

        if (value.isBlank()) {
            return;
        }

        if ("-".equals(value)) {
            return;
        }

        result.add(
                new ScheduleParserServiceImpl.Info(
                        group,
                        time,
                        value
                )
        );
    }

    private boolean shouldIgnore(String line) {

        String normalized =
                normalize(line).toLowerCase(Locale.ROOT);

        if (IGNORE_LINES.contains(normalized)) {
            return true;
        }

        /*
         * PDF иногда разрывает заголовок:
         *
         * Наименование
         * дисциплин
         */
        return normalized.equals("наименование")
                || normalized.equals("дисциплин");
    }

    private String normalizeTime(String time) {
        return time.replace('.', ',');
    }

    private static String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}