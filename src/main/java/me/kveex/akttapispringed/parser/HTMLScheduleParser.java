package me.kveex.akttapispringed.parser;

import lombok.extern.slf4j.Slf4j;
import me.kveex.akttapispringed.service.ScheduleParserService;
import me.kveex.akttapispringed.service.impl.ScheduleParserServiceImpl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class HTMLScheduleParser implements IScheduleParser {
    private Document document;
    private static final String URL = "https://aktt.org/raspisaniya/izmenenie-v-raspisanii-dnevnogo-otdeleniya.html";
    private final ScheduleParserService scheduleParserService;

    public HTMLScheduleParser(ScheduleParserService scheduleParserService) {
        this.scheduleParserService = scheduleParserService;
    }

    @Scheduled(fixedRate = 30, timeUnit = TimeUnit.MINUTES, initialDelay = 1)
    private void updateDocument() {
        Document newDocument = null;
        try {
            newDocument = Jsoup.connect(URL).get();
        }
        catch (ConnectException e) {
            log.error("Ошибка подключения к сайту АКТТ! Проверьте подключение к интернету или настройки сети!");

        }
        catch (IOException e) {
            log.error("Документ HTML парсера не обновлён! Причина: {}", e.toString());
        }
        if (newDocument != null) document = newDocument;
        this.parse();
    }

    @Override
    public void parse() {
        this.scheduleParserService.parse(
                scheduleEditDate(),
                scheduleDateLines(),
                timeAndInfoForScheduleGroup(),
                isWholeScheduleDistant()
        );
    }

    @Override
    public LocalDateTime scheduleEditDate() {
        Elements metaData = document.select("meta");
        for (Element meta : metaData) {
            if ("dateModified".equals(meta.attr("property"))) {
                String contentValue = meta.attr("content");
                if (!contentValue.isEmpty()) {
                    try {
                        OffsetDateTime offsetDateTime = OffsetDateTime.parse(contentValue);
                        return offsetDateTime.toLocalDateTime();
                    } catch (DateTimeParseException e) {
                        log.warn("Не удалось распарсить дату изменения: {}", e.toString());
                        return null;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public List<String> scheduleDateLines() {
        Elements scheduleLines = document.select("p");
        List<String> lines = new ArrayList<>();
        for (Element line : scheduleLines) {
            String text = line.text();
            if (!text.isBlank()) {
                lines.add(text);
            }
        }
        return lines;
    }

    @Override
    public List<ScheduleParserServiceImpl.Info> timeAndInfoForScheduleGroup() {
        Elements tables = this.document.select("table");
        if (tables.isEmpty()) return List.of();
        Element table = tables.getFirst();

        return ScheduleParserServiceImpl.getTimeAndInfoList(
                table.select("tr"),
                row -> row.select("td").stream()
                        .map(Element::text)
                        .collect(Collectors.toList())
        );
    }

    @Override
    public boolean isWholeScheduleDistant() {
        Elements elements = document.select("p");
        String[] dateParts = new String[10];

        for (Element element : elements) {
            String elementText = element.text().toLowerCase();
            if (!elementText.contains("расписание на")) continue;

            dateParts = elementText.split(" ");
            break;
        }

        for (String part : dateParts) {
            if (part.contains("дист")) {
                return true;
            }
        }

        return false;
    }
}
