package org.kveex.schedule.parser;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.kveex.AkttAPI;
import org.kveex.schedule.ScheduleItemState;
import org.kveex.schedule.SubGroup;
import org.kveex.schedule.ScheduleGroup;
import org.kveex.schedule.ScheduleItem;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

public class HTMLScheduleParser {
    private static final int GROUP_TIME_COLUMN = 1;
    private static final int GROUP_SUBJECT_COLUMN = 2;
    private static final int GROUP_COLUMN_WIDTH = 3;

    private volatile Document document;
    private static final String URL = "https://aktt.org/raspisaniya/izmenenie-v-raspisanii-dnevnogo-otdeleniya.html";
    private volatile LocalDateTime editTime;
    private static final List<String> staticRoomNames = List.of("библ.", "маст.", "дист.");
    private static final Pattern usualRoomPattern = Pattern.compile("\\d+[аб]?(?:/\\d+[аб]?)?");

    private volatile LocalDate scheduleDate;
    private volatile List<ScheduleGroup> fullSchedule;
    private volatile List<String> groupsList;
    private volatile Set<String> teachersList;

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
            AkttAPI.LOGGER.warn("Не удалось получить время изменения документа, но он всё равно обновлён!");
            this.scheduleDate = collectScheduleDate();
            this.groupsList = collectAllGroups();
            this.fullSchedule = makeSchedule();
            this.teachersList = collectAllTeachers();
            return;
        }

        if (editTime == null) {
            editTime = newEditTime;
            AkttAPI.LOGGER.info("Парсер скачал и прочитал HTML документ, момент изменения: {}", editTime);
        } else if (newEditTime.isAfter(editTime)) {
            AkttAPI.LOGGER.info("Новый HTML документ с моментом изменения: {}", newEditTime);
            editTime = newEditTime;
        } else {
            AkttAPI.LOGGER.info("HTML документ не обновлён, момент изменения: {}", editTime);
            return;
        }

        this.scheduleDate = collectScheduleDate();
        this.groupsList = collectAllGroups();
        this.fullSchedule = makeSchedule();
        this.teachersList = collectAllTeachers();
    }

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public List<ScheduleGroup> getFullSchedule() {
        return fullSchedule;
    }

    public List<String> getGroupsList() {
        return groupsList;
    }

    public Set<String> getTeachersList() {
        return teachersList;
    }

    /**
     * Собирает информацию об изменении документа, сохранённые в метадате
     * @return Дату и время обновления
     */
    private LocalDateTime getScheduleEditDate() {
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

    /**
     * Берёт дату на которое рассчитано расписание
     * @return LocalDate класс с датой месяцом и голом расписания в формате "yyyy-mm-dd"
     */
    private LocalDate collectScheduleDate() {
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
            break;
        }

        for (String part : dateParts) {
            if (part == null) continue;
            if (part.matches("\\d{2}")) {
                day = Integer.parseInt(part);
            } else if (part.matches("\\d{4}г")) {
                year = Integer.parseInt(part.replace("г", ""));
            } else if (months.contains(part)) {
                month = months.indexOf(part) + 1;
            }
        }

        if (day == 1 && month == 1 && year == 1) {
            throw new IllegalStateException("Не удалось получить дату расписания, вероятно указание не было найдено в документе");
        }

        return LocalDate.of(year, month, day);
    }


    /**
     * Проходится по документу и собирает все группы
     * @return список со всеми группами
     */
    private List<String> collectAllGroups() {
        List<String> groups = new ArrayList<>();
        Elements elements = this.document.select("table");
        Elements rows = elements.select("tr");

        for (Element row : rows) {
            Elements cells = row.select("td");

            for (int i = 0; i < 9; i+=3) {
                if (i >= cells.size()) break;
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
    private LinkedHashSet<String> collectAllTeachers() {
        LinkedHashSet<String> teacherNames = new LinkedHashSet<>();

        for (ScheduleGroup scheduleGroup : fullSchedule) {
            for (ScheduleItem scheduleItem : scheduleGroup.scheduleItems()) {
                List<String> teacherName = scheduleItem.teacherNames();
                if (teacherName == null) continue;
                for (String name : teacherName) {
                    if (name.isBlank()) continue;
                    if (name.contains("указан")) continue;
                    teacherNames.add(name);
                }
            }
        }
        return teacherNames;
    }

    private boolean isTeacherExists(String teacherName) {
        return teachersList.contains(teacherName);
    }

    public ScheduleGroup getTeacherScheduleGroup(String teacherName) {
        ScheduleGroup teacherScheduleGroup = new ScheduleGroup(scheduleDate.toString(), null, teacherName);

        if (!isTeacherExists(teacherName)) return teacherScheduleGroup;

        for (ScheduleGroup group : fullSchedule) {
            for (ScheduleItem item : group.scheduleItems()) {
                List<String> teachers = item.teacherNames();
                if (teachers == null || !teachers.contains(teacherName)) continue;
                ScheduleItem newItem = new ScheduleItem(item.time(), item.subjectName(), group.groupName(), teacherName, item.roomNumber(), item.subGroup(), ScheduleItemState.OK, scheduleDate);
                teacherScheduleGroup.add(newItem);
            }
        }

        var teacherScheduleItems = sortScheduleItems(teacherScheduleGroup);

        teacherScheduleGroup.replaceScheduleItems(teacherScheduleItems);

        return teacherScheduleGroup;
    }

    private static @NotNull List<ScheduleItem> sortScheduleItems(ScheduleGroup teacherScheduleGroup) {
        var teacherScheduleItems = teacherScheduleGroup.scheduleItems();
        teacherScheduleItems.sort(Comparator.comparingInt(ScheduleItem::timeToInt));
        return teacherScheduleItems;
    }

    private boolean isGroupExists(String groupName) {
        return groupsList.contains(groupName);
    }

    /**
     * Проходится по документу и собирает расписания всех групп в список
     * @return Лист с объектами содержащими расписание для каждой группы
     */
    private List<ScheduleGroup> makeSchedule() {
        List<ScheduleGroup> scheduleGroups = new ArrayList<>();
        for (String group : groupsList) {
            ScheduleGroup scheduleGroup = buildStudentScheduleGroup(group);
            scheduleGroups.add(scheduleGroup);
        }
        return scheduleGroups;
    }

    public ScheduleGroup getStudentScheduleGroup(String groupName) {
        if (isGroupExists(groupName.toLowerCase())) {
            for (ScheduleGroup group : fullSchedule) {
                if (!group.groupName().equals(groupName.toLowerCase())) continue;
                return group;
            }
        }
        throw new IllegalArgumentException("Группа [%s] не найдена!".formatted(groupName));
    }

    private ScheduleGroup buildStudentScheduleGroup(String groupName) {
        ScheduleGroup scheduleGroup = new ScheduleGroup(scheduleDate.toString(), groupName, null);
        var infoList = getTimeAndInfoForScheduleGroup(groupName);
        for (Pair<String, String> info : infoList) {
            List<ScheduleItem> scheduleItems = buildScheduleItem(groupName, info.getFirst(), info.getSecond());
            scheduleGroup.addAll(scheduleItems);
        }
        return scheduleGroup;
    }

    private List<Pair<String, String>> getTimeAndInfoForScheduleGroup(String groupName) {
        List<Pair<String, String>> timeAndInfoList = new ArrayList<>();

        Elements tables = this.document.select("table");
        if (tables.isEmpty()) return timeAndInfoList;
        Element table = tables.getFirst();

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
    public List<ScheduleItem> buildScheduleItem(String groupName, String time, String info) {
        List<ScheduleItem> result = new ArrayList<>();
        String[] subjects = info.split("\\s*–\\s*");
        String subjectName;
        String teacherName;
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
                roomNumber = roomNumberCombinedWithTeacher;
                doubleRoomNumber = roomNumber.contains("/");
            } else {
                roomNumber = "Не указан";
            }

            ScheduleItem staticCaseItem = checkForStaticCases(time, groupName, subject, roomNumber, itemSubGroup);
            if (staticCaseItem != null) {
                result.add(staticCaseItem);
                continue;
            }


            int subGroupIndex = -1;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].trim().equals("1п") || parts[i].trim().equals("2п")) {
                    subGroupIndex = !subject.contains("замена") ? i : i + 1;
                    break;
                }
            }

            var teacherNameInfo = makeTeacherName(parts, roomNumber, subGroupIndex, lastPartIndex, doubleRoomNumber);

            teacherName = teacherNameInfo.getFirst();
            int subjectNameEndIndex = teacherNameInfo.getSecond();
            for (int i = 0; i < subjectNameEndIndex; i++) {
                String part = parts[i];
                subjectNameBuilder.append(part).append(" ");
            }

            subjectName = subjectNameBuilder.toString().trim();

            if (teacherName.isEmpty()) teacherName = "Не указан";

            ScheduleItem scheduleItem = new ScheduleItem(time, subjectName, groupName, teacherName.trim(), roomNumber, itemSubGroup, ScheduleItemState.OK, scheduleDate);
            result.add(scheduleItem);
        }
        return result;
    }

    /**
     * Метод собирает имя преподавателя и вычисляет конечный индекс для формирования названия предмета
     * @param parts Информация обо всём предмете разделённая на части
     * @param roomNumber Номер кабинета
     * @param subGroupIndex Индекс части с указанием подгруппы в информации
     * @param lastPartIndex Индекс последней части в информации
     * @param doubleRoomNumber Указан ли номер кабинета, как бы через символ "/"
     * @return Пару с фамилией и инициалами преподавателя в виде строки и конечный индекс для формирования названия предмета в виде числа
     */
    private static Pair<String, Integer> makeTeacherName(String[] parts, String roomNumber, int subGroupIndex, int lastPartIndex, boolean doubleRoomNumber) {
        StringBuilder tempTeacherName = new StringBuilder();
        StringBuilder teacherName = new StringBuilder();
        int subjectNameEndIndex;

        // Имя преподавателя формируется до индекса с указанием подгруппы
        if (subGroupIndex != -1) {
            // добавляем только те части, которые существуют: проверяем границы
            for (int i = 1; i <= 2; i++) {
                int idx = subGroupIndex + i;
                if (idx < parts.length) {
                    tempTeacherName.append(parts[idx]).append(" ");
                }
            }
            subjectNameEndIndex = subGroupIndex;

            // Сдвигает формирование имени преподавателя, если номера комнаты нет
        } else if ("Не указан".equals(roomNumber)) {
            int startIndex = lastPartIndex > 3 ? 3 : 1;
            int from = Math.max(0, lastPartIndex - startIndex);
            for (int j = from; j <= lastPartIndex; j++) {
                tempTeacherName.append(parts[j]).append(" ");
            }
            subjectNameEndIndex = from;

            // Когда указан только предмет (маленький массив)
        } else if (parts.length <= 2) {
            subjectNameEndIndex = lastPartIndex;

            // Когда указаны предмет и преподаватель, но номер слился с инициалами
        } else if (parts.length == 3 && roomNumber != null && roomNumber.matches(usualRoomPattern.pattern())) {
            int startIndex = 1;
            int from = Math.max(0, lastPartIndex - startIndex);
            for (int j = from; j <= lastPartIndex; j++) {
                tempTeacherName.append(parts[j]).append(" ");
            }
            subjectNameEndIndex = from;

        } else {
            int startIndex = doubleRoomNumber ? 4 : 2;
            int from = Math.max(0, lastPartIndex - startIndex);
            int to = Math.max(from, lastPartIndex - 1);
            for (int j = from; j <= to; j++) {
                tempTeacherName.append(parts[j]).append(" ");
            }
            subjectNameEndIndex = from;
        }

        // Цикл вычищает номер кабинета из инициалов преподавателя
        String tmp = tempTeacherName.toString().trim();
        if (!tmp.isEmpty()) {
            for (String teacherPart : tmp.split("\\.")) {
                if (teacherPart.matches(usualRoomPattern.pattern())) break;
                if (!teacherPart.isBlank()) {
                    teacherName.append(teacherPart).append(".");
                }
            }
        }

        return new Pair<>(teacherName.toString(), subjectNameEndIndex);
    }

    /**
     * Проверка на особые случаи, которые не поддаются обычному механизму парсинга
     * @param time Время проведения учебной пары
     * @param info Информация об учебной паре (Название предмета, Преподаватель, кабинет)
     * @param roomNumber Кабинет проведения учебной пары
     * @return Класс с информацией об особом случае учебной пары
     */
    private ScheduleItem checkForStaticCases(String time, String groupName, String info, String roomNumber, SubGroup subGroup) {
        String caseText = info.toLowerCase();
        String caseTime = time.toLowerCase();
        String[] parts = info.split(" ");
        StringBuilder teacherName = new StringBuilder();

        if (caseText.contains("нет пары")) {
            return new ScheduleItem(time, "Нет пары", groupName, "Не указан", roomNumber, subGroup, ScheduleItemState.EMPTY, scheduleDate);
        }

        if (caseText.contains("о важном")) {
            return new ScheduleItem(time, "Разговор о важном", groupName, "Не указан", roomNumber, subGroup, ScheduleItemState.OK, scheduleDate);
        }

        if (caseText.contains("лыжи снежинка")) {
            String subjectName = parts[0];
            for (int i = 1; i < parts.length; i++) {
                if (!parts[i].toLowerCase().contains("лыжи") && !parts[i].toLowerCase().contains("снежинка")) {
                    teacherName.append(parts[i]).append(" ");
                }
            }
            return new ScheduleItem(time, subjectName, groupName, teacherName.toString().trim(), "Снежинка", subGroup, ScheduleItemState.OK, scheduleDate);
        }

        if (caseTime.contains("пп") || caseTime.contains("уп")) {
            teacherName = new StringBuilder();
            for (String part : parts) teacherName.append(part).append(" ");

            return new ScheduleItem(time, "Практика", groupName, teacherName.toString().trim(), roomNumber, subGroup, ScheduleItemState.OK, scheduleDate);
        }
        return null;
    }
}
