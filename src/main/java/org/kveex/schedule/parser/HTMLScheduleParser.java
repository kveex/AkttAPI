package org.kveex.schedule.parser;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.kveex.AkttAPI;
import org.kveex.schedule.SubGroup;
import org.kveex.schedule.ScheduleGroup;
import org.kveex.schedule.ScheduleItem;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class HTMLScheduleParser {
    private static final int GROUP_TIME_COLUMN = 1;
    private static final int GROUP_SUBJECT_COLUMN = 2;
    private static final int GROUP_COLUMN_WIDTH = 3;

    private Document document;
    private static final String URL = "https://aktt.org/raspisaniya/izmenenie-v-raspisanii-dnevnogo-otdeleniya.html";
    private LocalDateTime editTime;
    private static final List<String> staticRoomNames = List.of("библ.", "маст.", "дист.");

    public HTMLScheduleParser() throws IOException {
        updateDocument();
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

        if (editTime == null) {
            editTime = getScheduleEditDate();
            AkttAPI.LOGGER.info("Парсер скачал и прочитал HTML документ, момент изменения: {}", editTime);
            return;
        }

        if (newEditTime.isAfter(editTime)) {
            AkttAPI.LOGGER.info("Новый HTML документ с моментом изменения: {}", newEditTime);
            editTime = newEditTime;
        } else {
            AkttAPI.LOGGER.info("HTML документ не обновлён, момент изменения: {}", editTime);
        }
    }

    /**
     * Собирает информацию об изменении документа, сохранённые в метадате
     * @return Дату и время обновления
     */
    public LocalDateTime getScheduleEditDate() {
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
     * Проходится по документу и собирает все группы
     * @return список со всеми группами
     */
    public List<String> getAllGroups() {
        List<String> groups = new ArrayList<>();
        Elements elements = this.document.select("table");
        Elements rows = elements.select("tr");

        for (Element row : rows) {
            Elements cells = row.select("td");

            for (int i = 0; i < 9; i+=3) {
                String group = cells.get(i).text().toLowerCase();
                if (group.isBlank() || group.contains("группа")) continue;
                if (group.contains("дистант")) group = group.replace("дистант", "").trim();
                groups.add(group);
            }
        }

        return groups;
    }

    /**
     * Собирает имена преподавателей из всех групп
     * @return Список
     */
    public List<String> getAllTeachers() {
        List<String> teacherNames = new ArrayList<>();
        List<ScheduleGroup> scheduleGroups = getSchedule();

        for (ScheduleGroup scheduleGroup : scheduleGroups) {
            for (ScheduleItem scheduleItem : scheduleGroup.scheduleItems()) {
                List<String> teacherName = scheduleItem.teacherNames();
                if (teacherName == null) continue;
                for (String name : teacherName) {
                    if (name.isBlank()) continue;
                    if (name.contains("указан")) continue;
                    if (teacherNames.contains(name)) continue;
                    teacherNames.add(name);
                }
            }
        }
        return teacherNames;
    }

    private boolean isTeacherExists(String teacherName) {
        return getAllTeachers().contains(teacherName);
    }

    public ScheduleGroup getTeacherScheduleGroup(String teacherName) {
        var schedule = getSchedule();
        ScheduleGroup teacherScheduleGroup = new ScheduleGroup(getScheduleDate().toString(), teacherName);

        if (!isTeacherExists(teacherName)) return teacherScheduleGroup;

        for (ScheduleGroup group : schedule) {
            for (ScheduleItem item : group.scheduleItems()) {
                if (!item.teacherOrGroupName().equals(teacherName)) continue;
                ScheduleItem newItem = new ScheduleItem(item.time(), item.subjectName(), group.groupOrTeacherName(), item.roomNumber(), item.subGroup());
                teacherScheduleGroup.add(newItem);
            }
        }

        var teacherScheduleItems = sortScheduleItems(teacherScheduleGroup);

        teacherScheduleGroup.replaceScheduleItems(teacherScheduleItems);

        return teacherScheduleGroup;
    }

    private static @NotNull List<ScheduleItem> sortScheduleItems(ScheduleGroup teacherScheduleGroup) {
        var teacherScheduleItems = teacherScheduleGroup.scheduleItems();

        for (ScheduleItem item : teacherScheduleItems) {
            int value = item.timeToInt();

            for (int i = 0; i < teacherScheduleItems.size() - 1; i++) {
                if (value < teacherScheduleItems.get(i).timeToInt()) {
                    ScheduleItem tempItem = teacherScheduleItems.get(i);
                    teacherScheduleItems.set(i, teacherScheduleItems.get(i + 1));
                    teacherScheduleItems.set(i + 1, tempItem);
                    break;
                }
            }
        }
        return teacherScheduleItems;
    }

    private boolean isGroupExists(String groupName) {
        return getAllGroups().contains(groupName);
    }

    /**
     * Проходится по документу и собирает расписания всех групп в список
     * @return Лист с объектами содержащими расписание для каждой группы
     */
    public List<ScheduleGroup> getSchedule() {
        List<ScheduleGroup> scheduleGroups = new ArrayList<>();
        List<String> groupsList = getAllGroups();
        for (String group : groupsList) {
            ScheduleGroup scheduleGroup = buildStudentScheduleGroup(group);
            scheduleGroups.add(scheduleGroup);
        }
        return scheduleGroups;
    }

    public ScheduleGroup getStudentScheduleGroup(String groupName) {
        if (isGroupExists(groupName.toLowerCase())) {
            return buildStudentScheduleGroup(groupName.toLowerCase());
        }
        throw new IllegalArgumentException("Группа [%s] не найдена!".formatted(groupName));
    }

    private ScheduleGroup buildStudentScheduleGroup(String groupName) {
        String scheduleDate = getScheduleDate().toString();
        ScheduleGroup scheduleGroup = new ScheduleGroup(scheduleDate, groupName);
        var infoList = getTimeAndInfoForScheduleGroup(groupName);
        for (Pair<String, String> info : infoList) {
            List<ScheduleItem> scheduleItems = buildScheduleItem(info.getFirst(), info.getSecond());
            scheduleGroup.addAll(scheduleItems);
        }
        return scheduleGroup;
    }

    private List<Pair<String, String>> getTimeAndInfoForScheduleGroup(String groupName) {
        List<Pair<String, String>> timeAndInfoList = new ArrayList<>();
        Element table = this.document.select("table").getFirst();

        // Массив для хранения текущей группы для каждой из трёх колонок
        String[] currentColumns = new String[3];

        Elements rows = table.select("tr");

        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            Element row = rows.get(rowIndex);
            Elements cells = row.select("td");

            // Обновляем currentColumns только если ячейка с названием группы не пустая
            for (int columnIndex = 0; columnIndex < 3; columnIndex++) {
                int cellIndex = columnIndex * GROUP_COLUMN_WIDTH;
                if (cellIndex < cells.size()) {
                    String groupCellText = cells.get(cellIndex).text().trim();
                    if (!groupCellText.isBlank()) {
                        currentColumns[columnIndex] = groupCellText.toLowerCase();
                    }
                }
            }

            // Теперь собираем данные для нашей группы
            for (int columnIndex = 0; columnIndex < 3; columnIndex++) {
                int cellIndex = columnIndex * GROUP_COLUMN_WIDTH;

                // Пропускаем если нет ячейки или группа не наша
                if (cellIndex >= cells.size() ||
                        currentColumns[columnIndex] == null ||
                        !currentColumns[columnIndex].contains(groupName)) {
                    continue;
                }

                // Проверяем, что есть соседние ячейки
                if (cellIndex + GROUP_SUBJECT_COLUMN >= cells.size()) {
                    continue;
                }

                String time = cells.get(cellIndex + GROUP_TIME_COLUMN).text().trim();
                String info = cells.get(cellIndex + GROUP_SUBJECT_COLUMN).text().trim();

                if (!time.isBlank() && !info.isBlank()) {
                    if (info.trim().equals("-")) {
                        info = "нет пары";
                    }
                    timeAndInfoList.add(new Pair<>(time, info));
                }
            }
        }
        return timeAndInfoList;
    }

    /**
     * Создаёт Класс с информацией об учебной паре
     * @param time Уроки в которые проходит учебная пара (можно указать не только уроки)
     * @param info Информация об учебной паре (Название предмета, Преподаватель, кабинет)
     * @return Класс с информацией об учебной паре
     */
    public static List<ScheduleItem> buildScheduleItem(String time, String info) {
        List<ScheduleItem> result = new ArrayList<>();
        String[] subjects = info.split("\\s*–\\s*");
        String subjectName;
        StringBuilder teacherName = new StringBuilder();
        String roomNumber;
        SubGroup itemSubGroup = subjects.length > 1 ? SubGroup.FIRST : SubGroup.BOTH;

        for (String subject : subjects) {
            String[] parts = subject.split(" ");
            int lastPartIndex = parts.length - 1;
            boolean doubleRoomNumber = false;

            StringBuilder subjectNameBuilder = new StringBuilder();

            for (String part : parts) {
                if (part.trim().equals("1п")) {
                    itemSubGroup = SubGroup.FIRST;
                    break;
                } else if (part.trim().contains("2п")) {
                    itemSubGroup = SubGroup.SECOND;
                    break;
                }
            }

            Pattern usualRoomPattern = Pattern.compile("\\d+[аб]?(?:/\\d+[аб]?)?");

            boolean haveStaticName = staticRoomNames.contains(parts[lastPartIndex]);
            boolean matchesUsualPattern = parts[lastPartIndex].matches(usualRoomPattern.pattern());
            String roomNumberCombinedWithTeacher = "";

            for (String teacherPart : parts[lastPartIndex].split("\\.")) {
                if (!teacherPart.matches(usualRoomPattern.pattern())) continue;
                roomNumberCombinedWithTeacher = teacherPart;
            }

            if (haveStaticName || matchesUsualPattern) {
                roomNumber = parts[lastPartIndex];
                doubleRoomNumber = roomNumber.contains("/");
            } else if (!roomNumberCombinedWithTeacher.isEmpty()) {
                //TODO: Попробовать сделать так, чтобы кабинет исключался из части с названием преподавателя
                roomNumber = roomNumberCombinedWithTeacher;
                doubleRoomNumber = roomNumber.contains("/");
            } else {
                roomNumber = "Не указан";
            }

            ScheduleItem staticCaseItem = checkForStaticCases(time, subject, roomNumber, itemSubGroup);
            if (staticCaseItem != null) {
                return Collections.singletonList(staticCaseItem);
            }

            int subjectNameEndIndex;

            int subGroupIndex = -1;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].trim().equals("1п") || parts[i].trim().equals("2п")) {
                    subGroupIndex = !subject.contains("замена") ? i : i + 1;
                    break;
                }
            }

            if (subGroupIndex != -1) {
                for (int i = 1; i <= 2; i++) {
                    String part = parts[subGroupIndex + i];
                    teacherName.append(part).append(" ");
                }
                subjectNameEndIndex = subGroupIndex;
            } else if (roomNumber.equals("Не указан")) {
                int startIndex = lastPartIndex > 3 ? 3 : 1;
                for (int i = startIndex; i >= 0; i--) {
                    String part = parts[lastPartIndex - i];
                    teacherName.append(part).append(" ");
                }
                subjectNameEndIndex = lastPartIndex - startIndex;

            } else if (parts.length <= 2) {
                subjectNameEndIndex = lastPartIndex;
            } else if (parts.length == 3 && roomNumber.matches(usualRoomPattern.pattern())) {
                int startIndex = 1;
                for (int i = startIndex; i >= 0; i--) {
                    String part = parts[lastPartIndex - i];
                    teacherName.append(part).append(" ");
                }
                subjectNameEndIndex = lastPartIndex - startIndex;
            } else {
                int startIndex = doubleRoomNumber ? 4 : 2;
                for (int i = startIndex; i > 0; i--) {
                    String part = parts[lastPartIndex - i];
                    teacherName.append(part).append(" ");
                }
                subjectNameEndIndex = lastPartIndex - startIndex;
            }

            for (int i = 0; i < subjectNameEndIndex; i++) {
                String part = parts[i];
                subjectNameBuilder.append(part).append(" ");
            }

            subjectName = subjectNameBuilder.toString().trim();

            if (teacherName.toString().isEmpty()) teacherName = new StringBuilder("Не указан");

            ScheduleItem scheduleItem = new ScheduleItem(time, subjectName, teacherName.toString().trim(), roomNumber, itemSubGroup);
            result.add(scheduleItem);
        }
        return result;
    }

    /**
     * Проверка на особые случаи, которые не поддаются обычному механизму парсинга
     * @param time Время проведения учебной пары
     * @param info Информация об учебной паре (Название предмета, Преподаватель, кабинет)
     * @param roomNumber Кабинет проведения учебной пары
     * @return Класс с информацией об особом случае учебной пары
     */
    private static ScheduleItem checkForStaticCases(String time, String info, String roomNumber, SubGroup subGroup) {
        String caseText = info.toLowerCase();
        String caseTime = time.toLowerCase();
        String[] parts = info.split(" ");
        StringBuilder teacherName = new StringBuilder();

        if (caseText.contains("нет пары")) {
            return new ScheduleItem(time, "Нет пары", "Не указан", roomNumber, subGroup);
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
