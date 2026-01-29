package org.kveex;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Arrays;

/**
 * Класс для работы с расписаниями.
 * Класс предоставляет методы для создания и получения расписаний
 */
public class ScheduleHandler {
    private static final int GROUP_TIME_COLUMN = 1;
    private static final int GROUP_SUBJECT_COLUMN = 2;
    private static final int GROUP_COLUMN_WIDTH = 3;

    private final Document document;

    public ScheduleHandler() throws IOException {
        this.document = Jsoup.connect("https://aktt.org/raspisaniya/izmenenie-v-raspisanii-dnevnogo-otdeleniya.html").get();
    }

    /**
     * Создаёт расписание для указанной группы с учётом подгруппы
     * @param groupName Имя группы, например "23-14ИС"
     * @param subGroupNumber Номер подгруппы, может быть 0, для обеих подгрупп и 1 - 2 для конкретных
     * @return Класс со списком классов предметов, каждый из которых содержит в себе информацию об учебной паре
     */
    public ScheduleGroup createSchedule(String groupName, int subGroupNumber) {
        ScheduleGroup scheduleGroup = new ScheduleGroup(groupName);
        Elements tables = this.document.select("table");
        Element scheduleTable = null;

        for (Element table : tables) {
            if (!table.text().contains(groupName)) continue;
            scheduleTable = table;
        }

        if (scheduleTable == null) {
            String errorString = "Группа [%s] не найдена".formatted(groupName);
            Main.LOGGER.info(errorString);
            return scheduleGroup;
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
    private static ScheduleGroup fillScheduleGroup(String groupName, String time, String info, int subGroupNumber) {
        ScheduleGroup scheduleGroup = new ScheduleGroup(groupName);
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

    public static void printSchedule(@NotNull ScheduleGroup... scheduleGroups) {
        printSchedule(true, scheduleGroups);
    }


    /**
     * Выводит в консоль содержимое расписаний в понятном виде
     * @param goodTime Определяет формат времени (промежуток часов, уроки)
     * @param scheduleGroups Расписания
     */
    public static void printSchedule(boolean goodTime, @NotNull ScheduleGroup... scheduleGroups) {
        for (ScheduleGroup scheduleGroup : scheduleGroups) {
            StringBuilder scheduleText = new StringBuilder("Расписание для группы ").append(scheduleGroup.groupName()).append(":\n");

            if (!scheduleGroup.isEmpty()) {
                for (ScheduleItem scheduleItem : scheduleGroup.scheduleItems()) {
                    String time = goodTime ? scheduleItem.goodTime() : scheduleItem.time();
                    String subject = scheduleItem.subject();
                    String teacherName = scheduleItem.teacherName();
                    String roomNumber = scheduleItem.roomNumber();

                    if (subject.equals("-")) subject = "Нет пары";

                    if (!time.isEmpty() && !subject.isEmpty()) {
                        scheduleText.append(time).append(" | ").append(subject).append("\n");
                        scheduleText.append("Учитель: ").append(teacherName).append("\n");
                        scheduleText.append("Кабинет: ").append(roomNumber).append("\n");
                    }
                }
            } else {
                scheduleText.append("   Расписания нет");
            }

            Main.LOGGER.info(scheduleText.toString());
        }
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
        String subjectName = "";
        StringBuilder teacherName = new StringBuilder();
        String roomNumber = "";

        for (String subject : subjects) {
            String[] parts = subject.split(" ");
            int lastPartIndex = parts.length - 1;

            StringBuilder subjectNameBuilder = new StringBuilder();

            int subGroupIndex = -1;
            int targetSubGroup = 0;
            for (String part : parts) {
                if (part.trim().equals("1п")) {
                    targetSubGroup = 1;
                    subGroupIndex = Arrays.asList(parts).indexOf(part);
                    break;
                } else if (part.trim().contains("2п")) {
                    targetSubGroup = 2;
                    subGroupIndex = Arrays.asList(parts).indexOf(part);
                    break;
                }
            }

            if (targetSubGroup != 0 && targetSubGroup != subGroup && subjects.length == 1) {
                return null;
            }

            if (parts[lastPartIndex].contains("библ") || parts[lastPartIndex].matches("\\d+б?а?")) {
                roomNumber = parts[lastPartIndex];
            } else {
                roomNumber = "Не указан";
            }

            ScheduleItem staticCaseItem = checkForStaticCases(time, subject, roomNumber);
            if (staticCaseItem != null) {
                return !staticCaseItem.subject().isEmpty() ? staticCaseItem : null;
            }

            int subjectNameEndIndex;

            if (subGroupIndex != -1) {
                teacherName = new StringBuilder(parts[subGroupIndex + 1] + " " + parts[subGroupIndex + 2]);
                subjectNameEndIndex = subGroupIndex;
            } else {
                teacherName = new StringBuilder(parts[lastPartIndex - 2] + " " + parts[lastPartIndex - 1]);
                subjectNameEndIndex = lastPartIndex - 2;
            }

            for (int i = 0; i < subjectNameEndIndex; i++) {
                String part = parts[i];
                if (!part.equals("1п")) subjectNameBuilder.append(parts[i]).append(" ");
            }

            subjectName = subjectNameBuilder.toString();

            if (subGroup == 1) break;

            if (teacherName.isEmpty()) teacherName = new StringBuilder("Не указан");

        }
        return new ScheduleItem(time, subjectName, teacherName.toString(), roomNumber);
    }

    /**
     * Проверка на особые случаи, которые не поддаются обычному механизму парсинга
     * @param time Время проведения учебной пары
     * @param info Информация об учебной паре (Название предмета, Преподаватель, кабинет)
     * @param roomNumber Кабинет проведения учебной пары
     * @return Класс с информацией об особом случае учебной пары
     */
    private static ScheduleItem checkForStaticCases(String time, String info, String roomNumber) {
        String caseText = info.toLowerCase();
        String caseTime = time.toLowerCase();
        String[] parts = info.split(" ");
        StringBuilder teacherName = new StringBuilder();

        if (caseText.contains("нет пары")) {
            return new ScheduleItem(time, "", "", roomNumber);
        }

        if (caseText.contains("о важном")) {
            return new ScheduleItem(time, "Разговор о важном", "Не указан", roomNumber);
        }

        if (caseText.contains("лыжи снежинка")) {
            String subjectName = parts[0];
            for (int i = 1; i < parts.length; i++) {
                if (!parts[i].toLowerCase().contains("лыжи") && !parts[i].toLowerCase().contains("снежинка")) {
                    teacherName.append(parts[i]).append(" ");
                }
            }
            return new ScheduleItem(time, subjectName, teacherName.toString(), "Снежинка");
        }

        if (caseTime.contains("пп") || caseTime.contains("уп")) {
            teacherName = new StringBuilder();
            for (String part : parts) teacherName.append(part).append(" ");

            return new ScheduleItem(time, "Практика", teacherName.toString(), roomNumber);
        }
        return null;
    }
}
