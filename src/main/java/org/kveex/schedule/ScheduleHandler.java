package org.kveex.schedule;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.kveex.AkttAPI;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Класс для работы с расписаниями.
 * Класс предоставляет методы для создания и получения расписаний
 */
public class ScheduleHandler {
    //TODO: сделать возможность искать по имени преподавателя и кабинету
    //TODO: метод для собирания списка всех групп
    private static final int GROUP_TIME_COLUMN = 1;
    private static final int GROUP_SUBJECT_COLUMN = 2;
    private static final int GROUP_COLUMN_WIDTH = 3;

    private Document document;
    private static final String URL = "https://aktt.org/raspisaniya/izmenenie-v-raspisanii-dnevnogo-otdeleniya.html";
    private LocalDateTime editTime;

    public ScheduleHandler() throws IOException {
        updateDocument();
        editTime = getScheduleEditDate();
    }

    /**
     * Обновляет имеющийся HTML файл для парсинга
     */
    public void updateDocument() throws IOException {
        this.document = Jsoup.connect(URL).get();
        LocalDateTime newEditTime = getScheduleEditDate();
        if (newEditTime == null) {
            AkttAPI.LOGGER.error("Не удалось получить время изменения документа, но он всё равно обновлён!");
            return;
        }
        if (editTime == null) return;
        if (newEditTime.isAfter(editTime)) {
            AkttAPI.LOGGER.info("HTML скачан, документ обновлён");
            editTime = newEditTime;
        } else {
            AkttAPI.LOGGER.info("HTML скачан, обновлений документа нет");
        }
    }

    /**
     * Собирает информацию об изменении документа, сохранённые в метадате
     * @return Дату и время обновления
     */
    private LocalDateTime getScheduleEditDate() {
        Elements metaData = document.select("meta");
        String contentValue = null;

        for (Element meta : metaData) {
            String metaString = meta.toString();
            if (!metaString.contains("property=\"dateModified\"")) continue;
            String content;
            content = metaString.split(" ")[2];
            content = content.split("=")[1];
            content = content.replace("\"", "");
            content = content.replace(">", "");
            contentValue = content;
        }

        if (contentValue == null) return null;

        OffsetDateTime offsetDateTime = OffsetDateTime.parse(contentValue);
        return offsetDateTime.toLocalDateTime();
    }

    /**
     * Берёт дату на которое рассчитано расписание
     * @return LocalDate класс с датой месяцом и голом расписания в формате "yyyy-mm-dd"
     */
    public LocalDate getScheduleDate() {
        Elements elements = document.select("p");
        String[] dateParts = new String[8];
        List<String> months = List.of(
                "января", "февраля",
                "марта", "апреля", "мая",
                "июня", "июля", "августа",
                "сентября", "октября","ноября",
                "декабря");
        int year = 1;
        int month = 1;
        int day = 1;


        for (Element element : elements) {
            String elementText = element.text().toLowerCase();
            if (!elementText.contains("расписание на")) continue;

            dateParts = elementText.split(" ");
        }

        for (String part : dateParts) {
            if (part.matches("\\d{2}")) {
                day = Integer.parseInt(part);
            } else if (part.matches("\\d{4}г")) {
                year = Integer.parseInt(part.replace("г", ""));
            } else if (months.contains(part)) {
                month = months.indexOf(part) + 1;
            }
        }

        return LocalDate.of(year, month, day);
    }

    /**
     * Создаёт расписание для указанной группы с учётом подгруппы
     * @param groupName Имя группы, например "23-14ИС"
     * @param subGroupNumber Номер подгруппы, может быть 0, для обеих подгрупп и 1 - 2 для конкретных
     * @return Класс со списком классов предметов, каждый из которых содержит в себе информацию об учебной паре
     */
    public ScheduleGroup createSchedule(String groupName, int subGroupNumber) throws IllegalArgumentException{
        String scheduleDate = getScheduleDate().toString();
        ScheduleGroup scheduleGroup = new ScheduleGroup(scheduleDate, groupName);
        Elements tables = this.document.select("table");
        Element scheduleTable = null;

        for (Element table : tables) {
            if (!table.text().contains(groupName)) continue;
            scheduleTable = table;
        }

        if (scheduleTable == null) {
            throw new IllegalArgumentException("Группа [%s] не найдена".formatted(groupName));
        }

        String[] currentGroups = new String[3];

        Elements rows = scheduleTable.select("tr");

        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            Element row = rows.get(rowIndex);
            Elements cells = row.select("td");

            for (int groupIndex = 0; groupIndex < 3; groupIndex++) {
                int cellIndex = groupIndex * GROUP_COLUMN_WIDTH;

                if (cellIndex >= cells.size()) continue;

                String groupCellText = cells.get(cellIndex).text().trim();

                if (!groupCellText.isEmpty() && !groupCellText.equals("&nbsp;"))
                    currentGroups[groupIndex] = groupCellText;

                if (currentGroups[groupIndex] != null && currentGroups[groupIndex].contains(groupName)) {

                    if (cellIndex + GROUP_SUBJECT_COLUMN < cells.size()) {
                        String time = cells.get(cellIndex + GROUP_TIME_COLUMN).text().trim();
                        String info = cells.get(cellIndex + GROUP_SUBJECT_COLUMN).text().trim();

                        if (info.equals("-")) info = "Нет пары";

                        if (!time.isEmpty() && !info.isEmpty()) {
                            ScheduleGroup newScheduleGroup = fillScheduleGroup(groupName, time, info, subGroupNumber);
                            scheduleGroup.combine(newScheduleGroup);
                        }
                    }
                }
            }
        }
        return scheduleGroup;
    }

    /**
     * Метод для создания временной группы, используется только для комбинации с другим scheduleGroup
     * @param groupName Имя группы
     * @param time Время учебной пары
     * @param info Информация об учебной паре (Название предмета, Преподаватель, кабинет)
     * @param subGroupNumber Номер группы (1, 2, 0)
     * @see ScheduleHandler#createSchedule(String, int)
     * @return Группу с предметом или предметами (в зависимости от subGroupNumber и наличия предметов в info)
     */
    private ScheduleGroup fillScheduleGroup(String groupName, String time, String info, int subGroupNumber) {
        String scheduleDate = getScheduleDate().toString();
        ScheduleGroup scheduleGroup = new ScheduleGroup(scheduleDate, groupName);
        ScheduleItem scheduleItem;
        if (subGroupNumber == 0) {
            for (int i = 1; i <= 2; i++) {
                scheduleItem = createScheduleItem(time, info, i);
                if (scheduleItem != null && !scheduleGroup.contains(scheduleItem)) scheduleGroup.add(scheduleItem);
            }
        } else {
            scheduleItem = createScheduleItem(time, info, subGroupNumber);
            if (scheduleItem != null) scheduleGroup.add(scheduleItem);
        }
        return scheduleGroup;
    }

    /**
     * Создаёт Класс с информацией об учебной паре
     * @param time Уроки в которые проходит учебная пара (можно указать не только уроки)
     * @param info Информация об учебной паре (Название предмета, Преподаватель, кабинет)
     * @param subGroup Номер группы (1, 2)
     * @return Класс с информацией об учебной паре
     */
    public static ScheduleItem createScheduleItem(String time, String info, int subGroup) {
        String[] subjects = info.split(" –");
        String subjectName;
        StringBuilder teacherName;
        String roomNumber;
//        int itemSubGroup = 0;
        ScheduleItem.SubGroup itemSubGroup = ScheduleItem.SubGroup.BOTH;

        for (String subject : subjects) {
            String[] parts = subject.split(" ");
            int lastPartIndex = parts.length - 1;
            boolean doubleRoomNumber = false;

            StringBuilder subjectNameBuilder = new StringBuilder();

            for (String part : parts) {
                if (part.trim().equals("1п")) {
                    itemSubGroup = ScheduleItem.SubGroup.FIRST;
                    break;
                } else if (part.trim().contains("2п")) {
                    itemSubGroup = ScheduleItem.SubGroup.SECOND;
                    break;
                }
            }
            int itemSubGroupNum = ScheduleItem.toInt(itemSubGroup);

            if (itemSubGroupNum != 0 && itemSubGroupNum != subGroup && subjects.length == 1) {
                return null;
            }

            if (subGroup != 0 && itemSubGroupNum != 0 && itemSubGroupNum != subGroup) {
                continue;
            }

            if (parts[lastPartIndex].contains("библ") || parts[lastPartIndex].matches("\\d+[аб]?(?:/\\d+[аб]?)?")) {
                roomNumber = parts[lastPartIndex];
                doubleRoomNumber = roomNumber.contains("/");
            } else {
                roomNumber = "Не указан";
            }

            ScheduleItem staticCaseItem = checkForStaticCases(time, subject, roomNumber, itemSubGroup);
            if (staticCaseItem != null) {
                //TODO: сделать полноценную проверку, не только для названия предмета
                return !staticCaseItem.subject().isEmpty() ? staticCaseItem : null;
            }

            int subjectNameEndIndex;

            int subGroupIndex = -1;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].trim().equals("1п") || parts[i].trim().equals("2п")) {
                    subGroupIndex = i;
                    break;
                }
            }

            if (subGroupIndex != -1) {
                teacherName = new StringBuilder(parts[subGroupIndex + 1] + " " + parts[subGroupIndex + 2]);
                subjectNameEndIndex = subGroupIndex;
            } else {
                teacherName = new StringBuilder();
                subjectNameEndIndex = lastPartIndex - 2;
                if (doubleRoomNumber) {
                    teacherName.append(parts[lastPartIndex - 4]).append(" ").append(parts[lastPartIndex - 3]).append(" ");
                    subjectNameEndIndex = lastPartIndex - 4;
                }
                teacherName.append(parts[lastPartIndex - 2]).append(" ").append(parts[lastPartIndex - 1]);

            }

            for (int i = 0; i < subjectNameEndIndex; i++) {
                String part = parts[i];
                if (!part.equals("1п")) subjectNameBuilder.append(parts[i]).append(" ");
            }

            subjectName = subjectNameBuilder.toString().trim();

            if (teacherName.toString().isEmpty()) teacherName = new StringBuilder("Не указан");

            return new ScheduleItem(time, subjectName, teacherName.toString(), roomNumber, itemSubGroup);
        }
        return null;
    }

    /**
     * Проверка на особые случаи, которые не поддаются обычному механизму парсинга
     * @param time Время проведения учебной пары
     * @param info Информация об учебной паре (Название предмета, Преподаватель, кабинет)
     * @param roomNumber Кабинет проведения учебной пары
     * @return Класс с информацией об особом случае учебной пары
     */
    private static ScheduleItem checkForStaticCases(String time, String info, String roomNumber, ScheduleItem.SubGroup subGroup) {
        String caseText = info.toLowerCase();
        String caseTime = time.toLowerCase();
        String[] parts = info.split(" ");
        StringBuilder teacherName = new StringBuilder();

        if (caseText.contains("нет пары")) {
            return new ScheduleItem(time, "", "", roomNumber, subGroup);
        }

        if (caseText.contains("о важном")) {
            return new ScheduleItem(time, "Разговор о важном", "Не указан", roomNumber, subGroup);
        }

        if (caseText.contains("лыжи снежинка")) {
            String subjectName = parts[0];
            for (int i = 1; i < parts.length; i++) {
                if (!parts[i].toLowerCase().contains("лыжи") && !parts[i].toLowerCase().contains("снежинка")) {
                    teacherName.append(parts[i]).append(" ");
                }
            }
            return new ScheduleItem(time, subjectName, teacherName.toString(), "Снежинка", subGroup);
        }

        if (caseTime.contains("пп") || caseTime.contains("уп")) {
            teacherName = new StringBuilder();
            for (String part : parts) teacherName.append(part).append(" ");

            return new ScheduleItem(time, "Практика", teacherName.toString(), roomNumber, subGroup);
        }
        return null;
    }
}
