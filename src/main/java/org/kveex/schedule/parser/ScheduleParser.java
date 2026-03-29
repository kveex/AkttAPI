package org.kveex.schedule.parser;

import org.jetbrains.annotations.NotNull;
import org.kveex.schedule.Lesson;
import org.kveex.schedule.LessonGroup;
import org.kveex.schedule.LessonState;
import org.kveex.schedule.SubGroup;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ScheduleParser {
    public static final int GROUP_TIME_COLUMN = 1;
    public static final int GROUP_SUBJECT_COLUMN = 2;
    public static final int GROUP_COLUMN_WIDTH = 3;

    private static final List<String> staticRoomNames = List.of("библ.", "маст.", "дист.");
    private static final Pattern roomPattern = Pattern.compile("^\\d{1,3}[аб]?(?:/\\d{1,3}[аб]?)?$");
    private static final Pattern teacherNamePattern = Pattern.compile("[А-ЯЁ][а-яё]* [А-ЯЁ]\\.[А-ЯЁ]\\.");
    private static List<LessonGroup> studentsSchedule;

    public abstract ScheduleInfo parse();

    /**
     * Узнаёт всё ли расписание рассчитано на дистант, если это указано в заголовке расписания
     * @return True если дистант, иначе False
     */
    public abstract boolean isWholeScheduleDistant();

    public abstract Set<String> provideGroupsList();

    public abstract LocalDateTime collectScheduleEditDate();

    public abstract List<String> provideScheduleDateLines();
    public abstract List<Info> provideTimeAndInfoForScheduleGroup();

    /**
     * Проходится по документу и собирает все группы
     * @return список со всеми группами
     */
    public <T> Set<String> collectAllGroups(
            Iterable<T> rows,
            Function<T, List<String>> rowToCells
    ) {
        Set<String> groups = new HashSet<>();
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

    public LocalDate collectScheduleDate() {
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

        List<String> lines = provideScheduleDateLines();
        for (String line : lines) {
            if (line == null) continue;
            String lineText = line.toLowerCase();
            if (!lineText.contains("расписание на")) continue;

            dateParts = lineText.split(" ");
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

    /**
     * Собирает имена преподавателей из всех групп
     * @return Список
     */
    public LinkedHashSet<String> collectAllTeachers() {
        LinkedHashSet<String> teacherNames = new LinkedHashSet<>();

        for (LessonGroup lessonGroup : studentsSchedule) {
            for (Lesson lesson : lessonGroup.lessons()) {
                List<String> teacherName = lesson.teacherNames();
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

//    public LessonGroup buildStudentScheduleGroup(String groupName) {
//        LocalDate scheduleDate = collectScheduleDate();
//        LessonGroup lessonGroup = new LessonGroup(scheduleDate.toString(), groupName, null);
//        var infoList = provideTimeAndInfoForScheduleGroup(groupName);
//        for (Pair<String, String> info : infoList) {
//            List<Lesson> lessons = buildScheduleItem(groupName, info.getFirst(), info.getSecond(), scheduleDate);
//            lessonGroup.addAll(lessons);
//        }
//        return lessonGroup;
//    }

    public List<LessonInfo> buildLessonsList() {
        LocalDate scheduleDate = collectScheduleDate();
        List<LessonInfo> lessons = new ArrayList<>();

        var infoList = provideTimeAndInfoForScheduleGroup();

        for (Info info : infoList) {
            List<LessonInfo> lessonInfo = buildLessonInfo(info.groupName(), info.time(), info.info());
            lessons.addAll(lessonInfo);
        }

       return lessons;
    }

    public <T> List<Info> getTimeAndInfoList(Iterable<T> rows,
                                             Function<T, List<String>> rowToCells) {
        List<Info> infoList = new ArrayList<>();

        List<String> checkedGroups = new ArrayList<>();

//        for (String groupName : groups) {
        // Массив для хранения текущей группы для каждой из трёх колонок
        String[] currentColumns = new String[3];

        for (var row : rows) {
                List<String> cells = rowToCells.apply(row);
                String groupName = "";

                // Обновляем currentColumns только если ячейка с названием группы не пустая
                for (int columnIndex = 0; columnIndex < 3; columnIndex++) {
                    int cellIndex = columnIndex * GROUP_COLUMN_WIDTH;
                    if (cellIndex < cells.size()) {
                        String groupCellText = Objects.toString(cells.get(cellIndex)).trim();
                        if (!groupCellText.isBlank() && !checkedGroups.contains(groupCellText)) {
                            currentColumns[columnIndex] = groupCellText.toLowerCase();
                            groupName = groupCellText.toLowerCase();
                            checkedGroups.add(groupName);
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
                        infoList.add(new Info(groupName, time, info));
                    }
                }
            }
//        }
        return infoList;
    }

    public static @NotNull List<Lesson> sortScheduleItems(LessonGroup teacherLessonGroup) {
        var teacherScheduleItems = teacherLessonGroup.lessons();
        teacherScheduleItems.sort(Comparator.comparingInt(Lesson::timeToInt));
        return teacherScheduleItems;
    }

    /**
     * Создаёт Класс с информацией об учебной паре
     * @param strTime Уроки в которые проходит учебная пара (можно указать не только уроки)
     * @param info Информация об учебной паре (Название предмета, Преподаватель, кабинет)
     * @return Класс с информацией об учебной паре
     */
    public List<LessonInfo> buildLessonInfo(String groupName, String strTime, String info) {
        List<LessonInfo> result = new ArrayList<>();
        String[] subjects = info.split("\\s*–\\s*");
        String subjectName;
        String teacherName;
        String roomNumber;
        SubGroup itemSubGroup = subjects.length > 1 ? SubGroup.FIRST : SubGroup.BOTH;
        LessonState state = LessonState.OK;

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
            boolean matchesUsualPattern = parts[lastPartIndex].matches(roomPattern.pattern());
            boolean fullDistant = isWholeScheduleDistant();

            String roomNumberCombinedWithTeacher = "";

            for (String teacherPart : parts[lastPartIndex].split("\\.")) {
                if (!teacherPart.matches(roomPattern.pattern())) continue;
                roomNumberCombinedWithTeacher = teacherPart;
            }

//            if (haveStaticName || matchesUsualPattern) {
//                roomNumber = parts[lastPartIndex];
//                doubleRoomNumber = roomNumber.contains("/");
//            } else if (!roomNumberCombinedWithTeacher.isEmpty()) {
//                roomNumber = roomNumberCombinedWithTeacher;
//                doubleRoomNumber = roomNumber.contains("/");
//            } else {
//                roomNumber = "Не указан";
//            }
            var roomsInfo = findRooms(subject);

            List<String> rooms = new ArrayList<>();

            LessonInfo staticCaseItem = checkForStaticCases(time, groupName, subject, rooms, itemSubGroup, 0);
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
                state = LessonState.DISTANT;
            }

            Lesson lesson = new Lesson(time, subjectName, groupName, teacherName.trim(), roomNumber, itemSubGroup, state);
            result.add(lesson);
        }
        return result;
    }

    //FIXME: Переделать так, чтобы пара с разделением кабинетов типа "25а/61" разделялось на две, даже если название одинаковое
    public Pair<List<String>, Boolean> findRooms(String info) {
        List<String> rooms = new ArrayList<>();
        boolean isInSecondCampus = false;

        Matcher matcher = roomPattern.matcher(info);
        while (matcher.find()) {
            String room = matcher.group();
            try {
                int roomNum = Integer.parseInt(room.replace("а", "").replace("б", ""));
                isInSecondCampus = roomNum > 35 && roomNum <= 85;
            } catch (NumberFormatException _) {}
            rooms.add(room);
        }

        return new Pair<>(rooms, isInSecondCampus);
    }

    /**
     * Метод собирает имя преподавателя и вычисляет конечный индекс для формирования названия предмета
     * @param info Полная информация о паре
     * @return Пару с фамилией и инициалами преподавателя в виде строки и конечный индекс для формирования названия предмета в виде числа
     */
    public static Pair<List<String>, Integer> findTeacherNames(String info) {
        List<String> result = new ArrayList<>();
        int start = -1;

        Matcher matcher = teacherNamePattern.matcher(info);
        while (matcher.find()) {
            if (start == -1) {
                start = matcher.start();
            }
            result.add(matcher.group());
        }

        return new Pair<>(result, start);
    }

    /**
     * Проверка на особые случаи, которые не поддаются обычному механизму парсинга
     * @param time Время проведения учебной пары
     * @param info Информация об учебной паре (Название предмета, Преподаватель, кабинет)
     * @param rooms Кабинеты проведения учебной пары
     * @return Класс с информацией об особом случае учебной пары
     */
    public LessonInfo checkForStaticCases(LessonTime time, String groupName, String info, List<String> rooms, SubGroup subGroup, int position) {
        String caseText = info.toLowerCase();
        String[] parts = info.split(" ");
        List<String> teacherNames;

        if (caseText.contains("нет пары")) {
//            return new LessonInfo(time, "Нет пары", groupName, "Не указан", roomNumber, subGroup, LessonState.EMPTY, scheduleDate);
            return null;
        }

        if (caseText.contains("о важном")) {
//            return new LessonInfo(time, "Разговор о важном", groupName, "Не указан", roomNumber, subGroup, LessonState.OK, scheduleDate);
            return new LessonInfo(groupName,
                    Collections.singletonList("Не указан"),
                    time,
                    "Разговоры о важном",
                    rooms,
                    subGroup,
                    LessonState.OK,
                    position);
        }

        if (caseText.contains("лыжи снежинка")) {
            String subjectName = parts[0];
            teacherNames = findTeacherNames(caseText).getFirst();
            return new LessonInfo(groupName,
                    teacherNames,
                    time,
                    subjectName,
                    rooms,
                    subGroup,
                    LessonState.OK,
                    position);
        }

        if (time.equals(LessonTime.LEARNING_PRACTICE) || time.equals(LessonTime.PRODUCTION_PRACTICE)) {
            teacherNames = findTeacherNames(caseText).getFirst();
            String subjectName = time.equals(LessonTime.LEARNING_PRACTICE) ? "Учебная практика" : "Производственная практика";
            return new LessonInfo(groupName, teacherNames, time, subjectName, rooms, subGroup, LessonState.OK, position);
        }

        if (caseText.contains("(сам.раб.)")) {
            return new LessonInfo(groupName, Collections.emptyList(), time, info, rooms, subGroup, LessonState.OK, position);
        }

        return null;
    }
}
