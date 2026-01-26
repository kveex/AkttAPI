package org.kveex;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public class ScheduleHandler {
    // TODO: Разделить вывод расписания по подгруппам с опцией выбора выводить так или нет
    //  так же заменять время второй пары в зависимости от корпуса
    private static final LocalDate today = LocalDate.now();
    private static final DayOfWeek dayOfWeek = today.getDayOfWeek();
    private static final List<String> standardSubjectsTime = List.of("8:30 - 10:00", "[10:10 - 10:45 () 11:15 - 12:00]", "12:10 - 13:40", "13:50 - 15:20", "15:30 - 17:00");
    private static final String alternativeSubjectsTime = "10:10 - 11:40";
    private static final List<String> saturdaySubjectsTime = List.of("8:00 - 9:10", "9:20 - 10:30", "10:40 - 11:50", "12:00 - 13:10", "13:20 - 14:30");

    public static ScheduleGroup fillSchedule(Document document, String groupName, int subGroupNumber) {
        ScheduleGroup scheduleGroup = new ScheduleGroup(groupName);
        Elements tables = document.select("table");
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

        List<String> timeList = dayOfWeek != DayOfWeek.SATURDAY ? standardSubjectsTime : saturdaySubjectsTime;
        int timeCounter = 0;

        Elements rows = scheduleTable.select("tr");

        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            Element row = rows.get(rowIndex);
            Elements cells = row.select("td");

            for (int groupIndex = 0; groupIndex < 3; groupIndex++) {
                int cellIndex = groupIndex * 3;

                if (cellIndex >= cells.size()) continue;

                String groupCellText = cells.get(cellIndex).text().trim();

                if (!groupCellText.isEmpty() && !groupCellText.equals("&nbsp;"))
                    currentGroups[groupIndex] = groupCellText;

                if (currentGroups[groupIndex] != null && currentGroups[groupIndex].contains(groupName)) {

                    if (cellIndex + 2 < cells.size()) {
                        String time = timeList.get(timeCounter).trim();
                        String subject = cells.get(cellIndex + 2).text().trim();

                        if (subject.equals("-")) subject = "Нет пары";

                        if (!time.isEmpty() && !subject.isEmpty()) {
                            ScheduleItem scheduleItem = new ScheduleItem(time, subject);
                            scheduleGroup.add(scheduleItem);
                        }
                        timeCounter += 1;
                    }
                }
            }
        }
        return scheduleGroup;
    }

    public static void printSchedule(ScheduleGroup scheduleGroup) {
        StringBuilder scheduleText = new StringBuilder("Расписание для группы ").append(scheduleGroup.getGroupName()).append(":\n");

        if (!scheduleGroup.isEmpty()) {
            for (ScheduleItem scheduleItem : scheduleGroup.getScheduleItems()) {
                String time = scheduleItem.time();
                String subject = scheduleItem.subject();

                if (subject.equals("-")) subject = "Нет пары";

                if (!time.isEmpty() && !subject.isEmpty()) {
                    scheduleText.append(time).append(" | ").append(subject).append("\n");
                }
            }
        } else {
            scheduleText.append("   Расписания нет");
        }

        Main.LOGGER.info(scheduleText.toString());
    }
}
