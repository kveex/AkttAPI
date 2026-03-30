package org.kveex.schedule.parser;

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

    private static final Pattern roomPattern = Pattern.compile(
            "\\b(?:\\d{1,3}[аб]?|библ\\.|маст\\.|дист\\.)\\b",
            Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE
    );
    private static final Pattern teacherNamePattern = Pattern.compile("[А-ЯЁ][а-яё]* [А-ЯЁ]\\.[А-ЯЁ]\\.");
    private static final Pattern subGroupPattern = Pattern.compile("\\b[12]п\\b", Pattern.UNICODE_CHARACTER_CLASS);
    private List<LessonInfo> lessonInfos;

    public record Info(String groupName, String time, String info) { }
    public record Room(String roomName, boolean isInSecondCampus, int roomStartIndex) { }

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

        for (LessonInfo lessonInfo : lessonInfos) {
            List<String> teachers = lessonInfo.teacherNames();
            if (teachers.isEmpty()) continue;
            for (String name : teachers) {
                if (!name.matches(teacherNamePattern.pattern())) continue;
                teacherNames.add(name);
            }
        }

        return teacherNames;
    }

    public List<LessonInfo> buildLessonsList() {
        List<LessonInfo> lessons = new ArrayList<>();

        var infoList = provideTimeAndInfoForScheduleGroup();

        for (Info info : infoList) {
            List<LessonInfo> lessonInfo = buildLessonInfo(info.groupName(), info.time(), info.info());
            lessons.addAll(lessonInfo);
        }

        lessonInfos = lessons;
        return lessons;
    }

    public <T> List<Info> getTimeAndInfoList(Iterable<T> rows,
                                             Function<T, List<String>> rowToCells) {
        List<Info> infoList = new ArrayList<>();

        // Храним текущую группу отдельно для каждой из 3 колонок таблицы
        String[] currentColumns = new String[3];

        for (var row : rows) {
            List<String> cells = rowToCells.apply(row);

            // Обновляем имя группы в каждой колонке, если в ячейке есть значение
            for (int columnIndex = 0; columnIndex < 3; columnIndex++) {
                int cellIndex = columnIndex * GROUP_COLUMN_WIDTH;
                if (cellIndex < cells.size()) {
                    String groupCellText = Objects.toString(cells.get(cellIndex)).trim();
                    if (!groupCellText.isBlank()) {
                        currentColumns[columnIndex] = groupCellText.toLowerCase();
                    }
                }
            }

            // Собираем пары для каждой колонки с уже известной группой
            for (int columnIndex = 0; columnIndex < 3; columnIndex++) {
                int cellIndex = columnIndex * GROUP_COLUMN_WIDTH;
                String groupName = currentColumns[columnIndex];

                if (cellIndex >= cells.size() || groupName == null) {
                    continue;
                }

                // Проверяем, что есть соседние ячейки time/info
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

        return infoList;
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
        LessonState state = LessonState.OK;

        for (String subject : subjects) {
            var rooms = findRooms(subject);

            var subGroup = findSubGroup(subject);
            SubGroup itemSubGroup = subGroup.getFirst();

            for (var room : rooms) {
                String roomNumber = room.roomName();
                LessonTime time = LessonTime.convertFromString(strTime, collectScheduleDate(), room.isInSecondCampus());

                boolean fullDistant = isWholeScheduleDistant();

                LessonInfo staticCaseItem = checkForStaticCases(time, groupName, subject, roomNumber, itemSubGroup);
                if (staticCaseItem != null) {
                    result.add(staticCaseItem);
                    continue;
                }

                var teacher = findTeacherNames(subject);

                int teacherStartIndex = teacher.getSecond();
                int subGroupStartIndex = subGroup.getSecond();
                int roomStartIndex = room.roomStartIndex();

                int subjectNameEndIndex = subGroupStartIndex != -1 ? subGroupStartIndex :
                                teacherStartIndex != -1 ? teacherStartIndex
                                : roomStartIndex != -1 ? roomStartIndex : subject.length();

                String subjectName = subject.substring(0, subjectNameEndIndex).trim();

                if (fullDistant) {
                    roomNumber = "дист.";
                    state = LessonState.DISTANT;
                }
                List<String> teacherNames = teacher.getFirst();
                LessonInfo lesson = new LessonInfo(
                        groupName,
                        teacherNames,
                        time,
                        subjectName,
                        roomNumber,
                        itemSubGroup,
                        state
                );

                result.add(lesson);
            }
        }

        return result;
    }

    public static Pair<SubGroup, Integer> findSubGroup(String info) {
        Matcher matcher = subGroupPattern.matcher(info);

        if (matcher.find()) {
            return new Pair<>(SubGroup.toSubGroup(matcher.group()), matcher.start());
        }

        return new Pair<>(SubGroup.BOTH, -1);
    }

    public static List<Room> findRooms(String info) {
        List<Room> rooms = new ArrayList<>();
        int start = -1;

        Matcher matcher = roomPattern.matcher(info);
        while (matcher.find()) {
            String found = matcher.group();
            boolean isInSecondCampus = false;
            if (found.matches("\\d+[аб]?")) {
                start = matcher.start();
                try {
                    int roomNum = Integer.parseInt(found.replaceAll("[аб]", ""));
                    isInSecondCampus = roomNum > 35 && roomNum <= 85;
                } catch (NumberFormatException ignored) {}
            }
            rooms.add(new Room(found, isInSecondCampus, start));
        }
        return rooms;
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
     * @param room Кабинет проведения учебной пары
     * @return Класс с информацией об особом случае учебной пары
     */
    public LessonInfo checkForStaticCases(LessonTime time, String groupName, String info, String room, SubGroup subGroup) {
        String caseText = info.toLowerCase();
        String[] parts = info.split(" ");
        List<String> teacherNames;

        if (caseText.contains("нет пары")) {
            return null;
        }

        if (caseText.contains("о важном")) {
            return new LessonInfo(
                    groupName,
                    Collections.emptyList(),
                    time,
                    "Разговоры о важном",
                    room,
                    subGroup,
                    LessonState.OK
            );
        }

        if (caseText.contains("лыжи снежинка")) {
            String subjectName = parts[0];
            teacherNames = findTeacherNames(caseText).getFirst();
            return new LessonInfo(
                    groupName,
                    teacherNames,
                    time,
                    subjectName,
                    room,
                    subGroup,
                    LessonState.OK
            );
        }

        if (time.equals(LessonTime.LEARNING_PRACTICE) || time.equals(LessonTime.PRODUCTION_PRACTICE)) {
            teacherNames = findTeacherNames(caseText).getFirst();
            String subjectName = time.equals(LessonTime.LEARNING_PRACTICE) ? "Учебная практика" : "Производственная практика";
            return new LessonInfo(
                    groupName,
                    teacherNames,
                    time,
                    subjectName,
                    room,
                    subGroup,
                    LessonState.OK
            );
        }

        if (caseText.contains("(сам.раб.)")) {
            return new LessonInfo(
                    groupName,
                    Collections.emptyList(),
                    time,
                    info,
                    room,
                    subGroup,
                    LessonState.OK
            );
        }

        return null;
    }
}
