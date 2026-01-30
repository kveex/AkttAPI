package org.kveex.schedule;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.kveex.AkttAPI;

import java.io.IOException;

/**
 * Класс для работы с расписаниями.
 * Класс предоставляет методы для создания и получения расписаний
 */
public class ScheduleHandler {
    //TODO: сделать возможность искать по имени преподавателя и кабинету
    //TODO: сделать так, чтобы он брал дату расписания
    private static final int GROUP_TIME_COLUMN = 1;
    private static final int GROUP_SUBJECT_COLUMN = 2;
    private static final int GROUP_COLUMN_WIDTH = 3;

    private Document document;
    private static final String URL = "https://aktt.org/raspisaniya/izmenenie-v-raspisanii-dnevnogo-otdeleniya.html";

    public ScheduleHandler() throws IOException {
        updateDocument();
    }

    public void updateDocument() throws IOException {
        this.document = Jsoup.connect(URL).get();
        AkttAPI.LOGGER.info("Документ обновлён");
    }

    /**
     * Создаёт расписание для указанной группы с учётом подгруппы
     * @param groupName Имя группы, например "23-14ИС"
     * @param subGroupNumber Номер подгруппы, может быть 0, для обеих подгрупп и 1 - 2 для конкретных
     * @return Класс со списком классов предметов, каждый из которых содержит в себе информацию об учебной паре
     */
    public ScheduleGroup createSchedule(String groupName, int subGroupNumber) throws IllegalArgumentException{
        ScheduleGroup scheduleGroup = new ScheduleGroup(groupName);
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
        int itemSubGroup = 0;

        for (String subject : subjects) {
            String[] parts = subject.split(" ");
            int lastPartIndex = parts.length - 1;
            boolean doubleRoomNumber = false;

            StringBuilder subjectNameBuilder = new StringBuilder();

            for (String part : parts) {
                if (part.trim().equals("1п")) {
                    itemSubGroup = 1;
                    break;
                } else if (part.trim().contains("2п")) {
                    itemSubGroup = 2;
                    break;
                }
            }

            if (itemSubGroup != 0 && itemSubGroup != subGroup && subjects.length == 1) {
                return null;
            }

            if (subGroup != 0 && itemSubGroup != 0 && itemSubGroup != subGroup) {
                continue;
            }

            if (parts[lastPartIndex].contains("библ") || parts[lastPartIndex].matches("\\d+[аб]?(?:/\\d+[аб]?)?")) {
                roomNumber = parts[lastPartIndex];
                doubleRoomNumber = roomNumber.contains("/");
            } else {
                roomNumber = "Не указан";
            }

            ScheduleItem staticCaseItem = checkForStaticCases(time, subject, roomNumber);
            if (staticCaseItem != null) {
                //TODO: сделать полноценную проверку, не только для названия предмета
                return !staticCaseItem.getSubject().isEmpty() ? staticCaseItem : null;
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
    private static ScheduleItem checkForStaticCases(String time, String info, String roomNumber) {
        String caseText = info.toLowerCase();
        String caseTime = time.toLowerCase();
        String[] parts = info.split(" ");
        StringBuilder teacherName = new StringBuilder();

        if (caseText.contains("нет пары")) {
            return new ScheduleItem(time, "", "", roomNumber, 0);
        }

        if (caseText.contains("о важном")) {
            return new ScheduleItem(time, "Разговор о важном", "Не указан", roomNumber, 0);
        }

        if (caseText.contains("лыжи снежинка")) {
            String subjectName = parts[0];
            for (int i = 1; i < parts.length; i++) {
                if (!parts[i].toLowerCase().contains("лыжи") && !parts[i].toLowerCase().contains("снежинка")) {
                    teacherName.append(parts[i]).append(" ");
                }
            }
            return new ScheduleItem(time, subjectName, teacherName.toString(), "Снежинка", 0);
        }

        if (caseTime.contains("пп") || caseTime.contains("уп")) {
            teacherName = new StringBuilder();
            for (String part : parts) teacherName.append(part).append(" ");

            return new ScheduleItem(time, "Практика", teacherName.toString(), roomNumber, 0);
        }
        return null;
    }
}
