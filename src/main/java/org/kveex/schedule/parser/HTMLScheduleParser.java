package org.kveex.schedule.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.kveex.AkttAPI;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class HTMLScheduleParser extends ScheduleParser{

    private Document document;
    private static final String URL = "https://aktt.org/raspisaniya/izmenenie-v-raspisanii-dnevnogo-otdeleniya.html";

    public HTMLScheduleParser() {
        updateDocument();
    }

    private void updateDocument() {
        try {
            document = Jsoup.connect(URL).get();
        } catch (IOException e) {
            AkttAPI.LOGGER.error("Документ HTML парсера не обновлён! Причина: {}", e.toString());
        }
    }

    @Override
    public ScheduleInfo parse() {
        updateDocument();
        return new ScheduleInfo(
                collectScheduleEditDate(),
                collectScheduleDate(),
                makeSchedule(),
                provideGroupsList()
        );
    }

    @Override
    public LocalDateTime collectScheduleEditDate() {
        Elements metaData = document.select("meta");
        for (Element meta : metaData) {
            if ("dateModified".equals(meta.attr("property"))) {
                String contentValue = meta.attr("content");
                if (!contentValue.isEmpty()) {
                    try {
                        OffsetDateTime offsetDateTime = OffsetDateTime.parse(contentValue);
                        return offsetDateTime.toLocalDateTime();
                    } catch (DateTimeParseException e) {
                        AkttAPI.LOGGER.warn("Не удалось распарсить дату изменения: {}", e.toString());
                        return null;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public List<Pair<String, String>> provideTimeAndInfoForScheduleGroup(String groupName) {
        Elements tables = this.document.select("table");
        if (tables.isEmpty()) return List.of();
        Element table = tables.getFirst();

        return getTimeAndInfoForScheduleGroup(
                groupName,
                table.select("tr"),
                row -> row.select("td").stream()
                        .map(Element::text)
                        .collect(Collectors.toList())
                );
    }

    @Override
    public LocalDate collectScheduleDate() {
        Elements elements = document.select("p");
        String[] dateParts = new String[8];
        List<String> months = List.of(
                "янв", "фев",
                "мар", "апр", "мая",
                "июн", "июл", "авг",
                "сен", "окт", "ноя",
                "дек");
        int year = 0;
        int month = 0;
        int day = 0;

        for (Element element : elements) {
            String elementText = element.text().toLowerCase();
            if (!elementText.contains("расписание на")) continue;

            dateParts = elementText.split(" ");
            break;
        }

        for (String part : dateParts) {
            if (part == null) continue;
            String text = part.toLowerCase();
            String monthText = text;

            try {
                monthText = text.substring(0, 3);
            } catch (StringIndexOutOfBoundsException ignored) {}

            if (text.matches("\\d{2}")) {
                day = Integer.parseInt(text);
            } else if (text.matches("\\d{4}г?")) {
                year = Integer.parseInt(text.replace("г", ""));
            } else if (months.contains(monthText)) {
                month = months.indexOf(monthText) + 1;
            }
        }

        if (year < 2000 || month < 1 || day < 1) {
            throw new IllegalStateException("Не удалось получить дату расписания, вероятно указание не было найдено в документе");
        }

        return LocalDate.of(year, month, day);
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

    @Override
    public List<String> provideGroupsList() {
        Elements tables = this.document.select("table");
        Element table = tables.getFirst();

        return collectAllGroups(
                table.select("tr"),
                row -> row.select("td").stream()
                        .map(Element::text)
                        .collect(Collectors.toList())
        );
    }
}
