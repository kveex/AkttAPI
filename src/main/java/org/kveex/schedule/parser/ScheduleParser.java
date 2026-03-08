package org.kveex.schedule.parser;

import org.jetbrains.annotations.NotNull;
import org.kveex.schedule.ScheduleGroup;
import org.kveex.schedule.ScheduleItem;
import org.kveex.schedule.ScheduleItemState;
import org.kveex.schedule.SubGroup;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

public abstract class ScheduleParser {
    public static final int GROUP_TIME_COLUMN = 1;
    public static final int GROUP_SUBJECT_COLUMN = 2;
    public static final int GROUP_COLUMN_WIDTH = 3;

    private static final List<String> staticRoomNames = List.of("библ.", "маст.", "дист.");
    private static final Pattern usualRoomPattern = Pattern.compile("\\d+[аб]?(?:/\\d+[аб]?)?");

    public abstract ScheduleInfo parse();

    public abstract LocalDate collectScheduleDate();

    /**
     * Узнаёт всё ли расписание рассчитано на дистант, если это указано в заголовке расписания
     * @return True если дистант, иначе False
     */
    public abstract boolean isWholeScheduleDistant();

    public abstract List<String> provideGroupsList();

    public abstract LocalDateTime collectScheduleEditDate();

    public abstract List<Pair<String, String>> provideTimeAndInfoForScheduleGroup(String groupName);

    /**
     * Проходится по документу и собирает все группы
     * @return список со всеми группами
     */
    public <T> List<String> collectAllGroups(
            Iterable<T> rows,
            Function<T, List<String>> rowToCells
    ) {
        List<String> groups = new ArrayList<>();
        int[] columnIndices = new int[]{0, 3, 6};

        for (T row : rows) {
            List<String> cells = rowToCells.apply(row);

            for (int idx : columnIndices) {
                if (idx >= cells.size()) break;
                String group = Objects.toString(cells.get(idx)).toLowerCase().trim();
                if (group.isBlank() || group.contains("группа")) continue;
                if (group.contains("дистант")) group = group.replace("дистант", "").trim();
                groups.add(group);
            }
        }

        return groups;
    }

    /**
     * Проходится по документу и собирает расписания всех групп в список
     * @return Лист с объектами содержащими расписание для каждой группы
     */
    public List<ScheduleGroup> makeSchedule() {
        List<ScheduleGroup> scheduleGroups = new ArrayList<>();
        List<String> groupsList = provideGroupsList();
        for (String group : groupsList) {
            ScheduleGroup scheduleGroup = buildStudentScheduleGroup(group);
            scheduleGroups.add(scheduleGroup);
        }
        return scheduleGroups;
    }

    public ScheduleGroup buildStudentScheduleGroup(String groupName) {
        LocalDate scheduleDate = collectScheduleDate();
        ScheduleGroup scheduleGroup = new ScheduleGroup(scheduleDate.toString(), groupName, null);
        var infoList = provideTimeAndInfoForScheduleGroup(groupName);
        for (Pair<String, String> info : infoList) {
            List<ScheduleItem> scheduleItems = buildScheduleItem(groupName, info.getFirst(), info.getSecond(), scheduleDate);
            scheduleGroup.addAll(scheduleItems);
        }
        return scheduleGroup;
    }

    public <T> List<Pair<String, String>> getTimeAndInfoForScheduleGroup(String groupName,
                                                                         Iterable<T> rows,
                                                                         Function<T, List<String>> rowToCells) {
        List<Pair<String, String>> timeAndInfoList = new ArrayList<>();

        // Массив для хранения текущей группы для каждой из трёх колонок
        String[] currentColumns = new String[3];

        for (var row : rows) {
            List<String> cells = rowToCells.apply(row);

            // Обновляем currentColumns только если ячейка с названием группы не пустая
            for (int columnIndex = 0; columnIndex < 3; columnIndex++) {
                int cellIndex = columnIndex * GROUP_COLUMN_WIDTH;
                if (cellIndex < cells.size()) {
                    String groupCellText = Objects.toString(cells.get(cellIndex)).trim();
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

                String time = Objects.toString(cells.get(cellIndex + GROUP_TIME_COLUMN)).trim();
                String info = Objects.toString(cells.get(cellIndex + GROUP_SUBJECT_COLUMN)).trim();

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

    public static @NotNull List<ScheduleItem> sortScheduleItems(ScheduleGroup teacherScheduleGroup) {
        var teacherScheduleItems = teacherScheduleGroup.scheduleItems();
        teacherScheduleItems.sort(Comparator.comparingInt(ScheduleItem::timeToInt));
        return teacherScheduleItems;
    }

    /**
     * Создаёт Класс с информацией об учебной паре
     * @param time Уроки в которые проходит учебная пара (можно указать не только уроки)
     * @param info Информация об учебной паре (Название предмета, Преподаватель, кабинет)
     * @return Класс с информацией об учебной паре
     */
    public List<ScheduleItem> buildScheduleItem(String groupName, String time, String info, LocalDate scheduleDate) {
        List<ScheduleItem> result = new ArrayList<>();
        String[] subjects = info.split("\\s*–\\s*");
        String subjectName;
        String teacherName;
        String roomNumber;
        SubGroup itemSubGroup = subjects.length > 1 ? SubGroup.FIRST : SubGroup.BOTH;
        ScheduleItemState state = ScheduleItemState.OK;

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
            boolean fullDistant = isWholeScheduleDistant();

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

            ScheduleItem staticCaseItem = checkForStaticCases(time, groupName, subject, roomNumber, itemSubGroup, scheduleDate);
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
                if (part.trim().contains("1п") || part.trim().contains("2п")) continue;
                subjectNameBuilder.append(part).append(" ");
            }

            subjectName = subjectNameBuilder.toString().trim();

            if (teacherName.isEmpty()) teacherName = "Не указан";

            if (fullDistant) {
                roomNumber = "дист.";
                state = ScheduleItemState.DISTANT;
            }

            ScheduleItem scheduleItem = new ScheduleItem(time, subjectName, groupName, teacherName.trim(), roomNumber, itemSubGroup, state, scheduleDate);
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
    public Pair<String, Integer> makeTeacherName(String[] parts, String roomNumber, int subGroupIndex, int lastPartIndex, boolean doubleRoomNumber) {
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
            int startIndex = lastPartIndex >= 4 ? 3 : 1;
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
    public ScheduleItem checkForStaticCases(String time, String groupName, String info, String roomNumber, SubGroup subGroup, LocalDate scheduleDate) {
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
            for (String part : parts) {
                if (part.equals(roomNumber)) continue;
                teacherName.append(part).append(" ");
            }

            return new ScheduleItem(time, "Практика", groupName, teacherName.toString().trim(), roomNumber, subGroup, ScheduleItemState.OK, scheduleDate);
        }

        if (caseText.contains("(сам.раб.)")) {
            return new ScheduleItem(time, info, groupName, "Не указан", roomNumber, subGroup, ScheduleItemState.OK, scheduleDate);
        }

        return null;
    }
}
