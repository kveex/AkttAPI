package me.kveex.akttapispringed.service.impl;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import me.kveex.akttapispringed.domain.entity.schedule.*;
import me.kveex.akttapispringed.parser.PDFScheduleParser;
import me.kveex.akttapispringed.repository.GroupRepository;
import me.kveex.akttapispringed.repository.LessonRepository;
import me.kveex.akttapispringed.repository.ScheduleRepository;
import me.kveex.akttapispringed.repository.TeacherRepository;
import me.kveex.akttapispringed.service.ScheduleParserService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ScheduleParserServiceImpl implements ScheduleParserService {
    private static final int GROUP_TIME_COLUMN = 1;
    private static final int GROUP_SUBJECT_COLUMN = 2;
    private static final int GROUP_COLUMN_WIDTH = 3;

    private final ScheduleRepository scheduleRepository;
    private final LessonRepository lessonRepository;
    private final TeacherRepository teacherRepository;
    private final GroupRepository groupRepository;
    private LocalDate savedScheduleDate;

    private static final Pattern roomPattern = Pattern.compile(
            "\\b(?:\\d{1,3}[аб]?|библ\\.|маст\\.|дист\\.)\\b",
            Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE
    );
    private static final Pattern teacherNamePattern = Pattern.compile("[А-ЯЁ][а-яё]* [А-ЯЁ]\\.[А-ЯЁ]\\.");
    private static final Pattern subGroupPattern = Pattern.compile("\\b[12]п\\b", Pattern.UNICODE_CHARACTER_CLASS);

    public record Info(String groupName, String time, String info) { }
    public record Room(String roomName, boolean isInSecondCampus, int roomStartIndex) { }
    private record Pair<A, B>(A first, B second) {
        A getFirst() { return first; }
        B getSecond() { return second; }
    }

    protected ScheduleParserServiceImpl(ScheduleRepository scheduleRepository, TeacherRepository teacherRepository, GroupRepository groupRepository, LessonRepository lessonRepository) {
        this.scheduleRepository = scheduleRepository;
        this.teacherRepository = teacherRepository;
        this.groupRepository = groupRepository;
        this.lessonRepository = lessonRepository;
    }

    public void parsePdf(byte[] bytes) throws IOException {
        PDFScheduleParser parser = new PDFScheduleParser(this, bytes);
        parser.parse();
    }

    public static <T> List<Info> getTimeAndInfoList(Iterable<T> rows,
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
                    String groupCellText = Objects.toString(cells.get(cellIndex)).toLowerCase().trim();
                    if (!groupCellText.isBlank() && !groupCellText.contains("группа")) {
                        currentColumns[columnIndex] = groupCellText.replace("дистант", "").trim();
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
                    if (!info.trim().equals("-")) {
                        infoList.add(new Info(groupName, time, info));
                    }
                }
            }
        }

        return infoList;
    }

    @Transactional
    public void parse(LocalDateTime editTimeStamp, List<String> scheduleDateLines, List<Info> timeAndInfoForScheduleGroup, boolean isWholeScheduleDistant) {
        boolean scheduleExists = this.scheduleRepository.existsByEditTimeStamp(editTimeStamp);
        if (scheduleExists) {
            log.info("Расписание с временем изменения [{}] уже существует, ничего не делаем", editTimeStamp.toString());
            return;
        }

        LocalDate scheduleDate = collectScheduleDate(scheduleDateLines);
        this.savedScheduleDate = scheduleDate;
        List<Lesson> lessons = new ArrayList<>();

        Schedule schedule = Schedule.builder()
                .editTimeStamp(editTimeStamp)
                .scheduleDate(scheduleDate)
                .build();

        this.scheduleRepository.save(schedule);

        for (Info info : timeAndInfoForScheduleGroup) {
            List<Lesson> lessonInfo = buildLessons(info, schedule, isWholeScheduleDistant);
            lessons.addAll(lessonInfo);
        }
        schedule.setLessons(lessons);

        log.info("Добавлено новое расписание на дату: [{}] | Время изменения: [{}]", scheduleDate, editTimeStamp.toString());
    }

    /**
     * Получает дату на которую рассчитано расписание из документа
     * @return LocalDate класс с датой
     */
    private LocalDate collectScheduleDate(List<String> scheduleDateLines) {
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

        for (String line : scheduleDateLines) {
            if (line == null) continue;
            String lineText = line.toLowerCase();
            if (!lineText.contains("расписание на")) continue;

            dateParts = lineText.split(" ");
            break;
        }

        for (String part : dateParts) {
            if (part == null) continue;
            String text = part.toLowerCase().trim();
            String monthText = text;

            try {
                monthText = text.substring(0, 3);
            } catch (StringIndexOutOfBoundsException ignored) {}

            if (text.matches("\\d{1,2}")) {
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

    private List<Lesson> buildLessons(Info info, Schedule schedule, boolean isWholeScheduleDistant) {
        List<Lesson> result = new ArrayList<>();
        String[] subjects = info.info().split("\\s*–\\s*");
        LessonType lessonType = LessonType.NORMAL;

        for (String subject : subjects) {
            List<Room> rooms = findRooms(subject);

            Pair<Subgroup, Integer> subGroup = findSubGroup(subject);
            Subgroup lessonSubgroup = subGroup.getFirst();

            for (var room : rooms) {
                String roomNumber = room.roomName();

                Pair<String, LessonTimeType> timePair = getTime(info.time(), room.isInSecondCampus());
                String lessonTime = timePair.getFirst();
                LessonTimeType lessonTimeType = timePair.getSecond();

                Pair<List<String>, Integer> teachers = findTeacherNames(subject);

                int teacherStartIndex = teachers.getSecond();
                int subGroupStartIndex = subGroup.getSecond();
                int roomStartIndex = room.roomStartIndex();

                int subjectNameEndIndex = subGroupStartIndex != -1 ? subGroupStartIndex :
                        teacherStartIndex != -1 ? teacherStartIndex
                                : roomStartIndex != -1 ? roomStartIndex : subject.length();

                String subjectName = subject.substring(0, subjectNameEndIndex).trim();

                if (isWholeScheduleDistant) {
                    roomNumber = "дист.";
                    lessonType = LessonType.DISTANT;
                }
                List<String> teacherNames = teachers.getFirst();

                Group group = tryGetGroup(info.groupName());

                Optional<List<Lesson>> staticCaseItems = checkForStaticCases(schedule, group, teacherNames, lessonTime, lessonTimeType, subject, roomNumber, lessonSubgroup);
                if (staticCaseItems.isPresent()) {
                    result.addAll(staticCaseItems.get());
                    continue;
                }

                for (String teacherName : teacherNames) {
                    Teacher teacher = tryGetTeacher(teacherName);

                    Lesson lesson = Lesson.builder()
                            .schedule(schedule)
                            .group(group)
                            .teacher(teacher)
                            .lessonTime(lessonTime)
                            .lessonTimeType(lessonTimeType)
                            .subjectName(subjectName)
                            .classroom(roomNumber)
                            .subgroup(lessonSubgroup)
                            .lessonType(lessonType)
                            .build();

                    result.add(lesson);
                }
            }
        }
        return this.lessonRepository.saveAll(result);
    }

    private static List<Room> findRooms(String info) {
        List<Room> rooms = new ArrayList<>();
        int start = info.length();

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

        if (rooms.isEmpty() && !info.toLowerCase().contains("группа")) {
            rooms.add(new Room("Не указан", true, start));
        }

        return rooms;
    }

    private static Pair<Subgroup, Integer> findSubGroup(String info) {
        Matcher matcher = subGroupPattern.matcher(info);

        if (matcher.find()) {
            return new Pair<>(Subgroup.fromString(matcher.group()), matcher.start());
        }

        return new Pair<>(Subgroup.BOTH, -1);
    }

    private Optional<List<Lesson>> checkForStaticCases(Schedule schedule, Group group, List<String> teacherNames, String lessonTime, LessonTimeType lessonTimeType, String info, String room, Subgroup subgroup) {
        List<Lesson> result = new ArrayList<>();
        String caseText = info.toLowerCase();
        String[] parts = info.split(" ");
        boolean isPractice = switch (lessonTimeType) {
            case PRE_DIPLOMA_PRACTICE, PRODUCTION_PRACTICE, LEARNING_PRACTICE -> true;
            default -> false;
        };

        if (caseText.contains("nothing")) {
            return Optional.empty();
        }

        if (caseText.contains("о важном")) {
            Lesson lesson = Lesson.builder()
                    .schedule(schedule)
                    .group(group)
                    .lessonTime(lessonTime)
                    .lessonTimeType(lessonTimeType)
                    .subjectName("Разговоры о важном")
                    .classroom(room)
                    .subgroup(subgroup)
                    .lessonType(LessonType.NORMAL)
                    .build();

            result.add(lesson);

            return Optional.of(result);
        }

        if (caseText.contains("лыжи снежинка")) {
            for (String teacherName : teacherNames) {
                Teacher teacher = tryGetTeacher(teacherName);
                String subjectName = parts[0];

                Lesson lesson = Lesson.builder()
                        .schedule(schedule)
                        .group(group)
                        .teacher(teacher)
                        .lessonTime(lessonTime)
                        .lessonTimeType(lessonTimeType)
                        .subjectName(subjectName)
                        .classroom(room)
                        .subgroup(subgroup)
                        .lessonType(LessonType.NORMAL)
                        .build();

                result.add(lesson);
            }

            return Optional.of(result);
        }

        if (isPractice) {
            for (String teacherName : teacherNames) {
                Teacher teacher = tryGetTeacher(teacherName);

                Lesson lesson = Lesson.builder()
                        .schedule(schedule)
                        .group(group)
                        .teacher(teacher)
                        .lessonTime(lessonTime)
                        .lessonTimeType(lessonTimeType)
                        .subjectName(lessonTime)
                        .classroom(room)
                        .subgroup(subgroup)
                        .lessonType(LessonType.PRACTICE)
                        .build();

                result.add(lesson);
            }

            return Optional.of(result);
        }

        if (caseText.contains("(сам.раб.)")) {
            Lesson lesson = Lesson.builder()
                    .schedule(schedule)
                    .group(group)
                    .lessonTime(lessonTime)
                    .lessonTimeType(lessonTimeType)
                    .subjectName(info)
                    .classroom(room)
                    .subgroup(subgroup)
                    .lessonType(LessonType.NORMAL)
                    .build();

            result.add(lesson);

            return Optional.of(result);
        }

        return Optional.empty();
    }

    private static Pair<List<String>, Integer> findTeacherNames(String info) {
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

    private Group tryGetGroup(String groupName) {
        Optional<Group> existingGroup = this.groupRepository.getGroupByName(groupName);
        if (existingGroup.isEmpty()) {
            Group group = Group.builder().name(groupName).build();
            return this.groupRepository.save(group);
        }
        return existingGroup.get();
    }

    private Teacher tryGetTeacher(String teacherName) {
        Optional<Teacher> existingTeacher = this.teacherRepository.getTeacherByName(teacherName);
        if (existingTeacher.isEmpty()) {
            Teacher teacher = Teacher.builder().name(teacherName).build();
            return this.teacherRepository.save(teacher);
        }
        return existingTeacher.get();
    }

    private Pair<String, LessonTimeType> getTime(String rawTime, boolean isInSecondCampus) {
        boolean todayIsSaturday = savedScheduleDate.getDayOfWeek() == DayOfWeek.SATURDAY;
        return switch (rawTime) {
            case "1,2" -> !todayIsSaturday
                    ? new Pair<>("8:30 - 10:00", LessonTimeType.FIRST)
                    : new Pair<>("8:00 - 9:10", LessonTimeType.FIRST_SHORT);
            case "3,4" -> {
                if (todayIsSaturday) yield new Pair<>("9:20 - 10:30", LessonTimeType.SECOND_SHORT);
                yield !isInSecondCampus
                        ? new Pair<>("10:10 - 10:55 (перерыв) 11:15 - 12:00", LessonTimeType.SECOND)
                        : new Pair<>("10:10 - 11:40", LessonTimeType.SECOND_FULL);
            }
            case "5,6" -> !todayIsSaturday
                    ? new Pair<>("12:10 - 13:40", LessonTimeType.THIRD)
                    : new Pair<>("10:40 - 11:50", LessonTimeType.THIRD_SHORT);
            case "7,8" -> !todayIsSaturday
                    ? new Pair<>("13:50 - 14:20", LessonTimeType.FOURTH)
                    : new Pair<>("12:00 - 13:10", LessonTimeType.FOURTH_SHORT);
            case "УП" -> new Pair<>("Учебная практика", LessonTimeType.LEARNING_PRACTICE);
            case "ПП" -> new Pair<>("Производственная практика", LessonTimeType.PRODUCTION_PRACTICE);
            case "ПДП" -> new Pair<>("Пред дипломная практика", LessonTimeType.PRE_DIPLOMA_PRACTICE);
            default -> new Pair<>(rawTime, LessonTimeType.CUSTOM);
        };
    }
}
