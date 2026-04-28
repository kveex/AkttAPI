package org.kveex.database;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.kveex.AkttAPI;
import org.kveex.schedule.LessonState;
import org.kveex.schedule.SubGroup;
import org.kveex.schedule.parser.LessonInfo;
import org.kveex.schedule.parser.LessonTime;
import org.kveex.schedule.parser.ScheduleInfo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class DatabaseController implements AutoCloseable {
    private static DatabaseController INSTANCE;

    private static final Schema PUBLIC_SCHEMA = DSL.schema(DSL.name("public"));

    private static final Table<?> SCHEDULES = DSL.table(DSL.name("public", "schedules"));
    private static final Table<?> GROUPS = DSL.table(DSL.name("public", "groups"));
    private static final Table<?> TEACHERS = DSL.table(DSL.name("public", "teachers"));
    private static final Table<?> LESSONS = DSL.table(DSL.name("public", "lessons"));
    private static final Table<?> LESSON_TEACHERS = DSL.table(DSL.name("public", "lesson_teachers"));

    private static final Field<Long> SCHEDULES_ID = DSL.field(DSL.name("id"), SQLDataType.BIGINT.nullable(false));
    private static final Field<LocalDateTime> EDIT_DATE_TIME = DSL.field(DSL.name("edit_date_time"), SQLDataType.LOCALDATETIME.nullable(false));
    private static final Field<LocalDate> SCHEDULE_DATE = DSL.field(DSL.name("schedule_date"), SQLDataType.LOCALDATE.nullable(false));

    private static final Field<Long> GROUPS_ID = DSL.field(DSL.name("id"), SQLDataType.BIGINT.nullable(false));
    private static final Field<Long> GROUPS_SCHEDULE_ID = DSL.field(DSL.name("schedule_id"), SQLDataType.BIGINT.nullable(false));
    private static final Field<String> GROUPS_NAME = DSL.field(DSL.name("name"), SQLDataType.NVARCHAR.nullable(false));

    private static final Field<Long> TEACHERS_ID = DSL.field(DSL.name("id"), SQLDataType.BIGINT.nullable(false));
    private static final Field<Long> TEACHERS_SCHEDULE_ID = DSL.field(DSL.name("schedule_id"), SQLDataType.BIGINT.nullable(false));
    private static final Field<String> TEACHERS_NAME = DSL.field(DSL.name("name"), SQLDataType.NVARCHAR.nullable(false));

    private static final DataType<SubGroup> SUBGROUP_TYPE = SQLDataType.VARCHAR
            .asEnumDataType(SubGroup.class);
    private static final DataType<LessonState> LESSON_STATE_TYPE = SQLDataType.VARCHAR
            .asEnumDataType(LessonState.class);
    private static final DataType<LessonTime> LESSON_TIME_TYPE = SQLDataType.NVARCHAR
            .asEnumDataType(LessonTime.class);

    private static final Field<Long> LESSONS_ID = DSL.field(DSL.name("id"), SQLDataType.BIGINT.nullable(false));
    private static final Field<Long> LESSONS_SCHEDULE_ID = DSL.field(DSL.name("schedule_id"), SQLDataType.BIGINT.nullable(false));
    private static final Field<Long> LESSONS_TEACHER_ID = DSL.field(DSL.name("teacher_id"), SQLDataType.BIGINT);
    private static final Field<Long> LESSONS_GROUP_ID = DSL.field(DSL.name("group_id"), SQLDataType.BIGINT.nullable(false));
    private static final Field<String> SUBJECT_NAME = DSL.field(DSL.name("subject_name"), SQLDataType.NVARCHAR.nullable(false));
    private static final Field<String> ROOM = DSL.field(DSL.name("room"), SQLDataType.NVARCHAR.nullable(false));
    private static final Field<SubGroup> SUBGROUP = DSL.field(DSL.name("subgroup"), SUBGROUP_TYPE.nullable(false));
    private static final Field<LessonTime> TIME = DSL.field(DSL.name("time"), LESSON_TIME_TYPE.nullable(false));
    private static final Field<LessonState> STATE = DSL.field(DSL.name("state"), LESSON_STATE_TYPE.nullable(false));
    private static final Field<String> CUSTOM_TIME = DSL.field(DSL.name("custom_name"), SQLDataType.NVARCHAR.nullable(true));

    private static final Field<Long> LESSON_TEACHERS_LESSON_ID = DSL.field(DSL.name("lesson_link_id"), SQLDataType.BIGINT.nullable(false));
    private static final Field<Long> LESSON_TEACHERS_TEACHER_ID = DSL.field(DSL.name("teacher_link_id"), SQLDataType.BIGINT.nullable(false));

    private final Connection connection;
    private final DSLContext context;

    private DatabaseController(String url) {
        try {
            connection = DriverManager.getConnection(url);
            context = DSL.using(connection, SQLDialect.POSTGRES);
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось инициализировать подключение к базе данных", e);
        }

        tryCreateSchemaObjects();
    }

    public static synchronized void initialize(String url) {
        if (INSTANCE == null) {
            INSTANCE = new DatabaseController(url);
            return;
        }

        AkttAPI.LOGGER.warn("Контроллер для базы данных уже инициализирован!");
    }

    public static DatabaseController getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("DatabaseController не был инициализирован");
        }
        return INSTANCE;
    }

    private void tryCreateSchemaObjects() {
        context.createSchemaIfNotExists(PUBLIC_SCHEMA).execute();

        createTypes();
        createSchedulesTable();
        createGroupsTable();
        createTeachersTable();
        createLessonsTable();
        createLessonTeachersTable();
        createIndexes();
        migrateLessonTeachers();
    }

    private void createTypes() {
        context.createTypeIfNotExists(SubGroup.BOTH.getName())
                .asEnum(SubGroup.FIRST.getLiteral(),
                        SubGroup.SECOND.getLiteral(),
                        SubGroup.BOTH.getLiteral())
                .execute();

        context.createTypeIfNotExists(LessonState.OK.getName())
                .asEnum(LessonState.OK.getLiteral(),
                        LessonState.DISTANT.getLiteral(),
                        LessonState.PRACTICE.getLiteral(),
                        LessonState.EMPTY.getLiteral())
                .execute();

        context.createTypeIfNotExists(LessonTime.FIRST.getName())
                .asEnum(LessonTime.FIRST.getLiteral(),
                        LessonTime.FIRST_SHORT.getLiteral(),
                        LessonTime.SECOND.getLiteral(),
                        LessonTime.SECOND_FULL.getLiteral(),
                        LessonTime.SECOND_SHORT.getLiteral(),
                        LessonTime.THIRD.getLiteral(),
                        LessonTime.THIRD_SHORT.getLiteral(),
                        LessonTime.FOURTH.getLiteral(),
                        LessonTime.FOURTH_SHORT.getLiteral(),
                        LessonTime.PRODUCTION_PRACTICE.getLiteral(),
                        LessonTime.LEARNING_PRACTICE.getLiteral(),
                        LessonTime.PRE_DIPLOMA_PRACTICE.getLiteral(),
                        LessonTime.CUSTOM.getLiteral())
                .execute();
    }

    private void createSchedulesTable() {
        context.createTableIfNotExists(SCHEDULES)
                .column(SCHEDULES_ID, SQLDataType.BIGINT.generatedByDefaultAsIdentity().nullable(false))
                .primaryKey(SCHEDULES_ID)
                .column(EDIT_DATE_TIME)
                .unique(EDIT_DATE_TIME)
                .column(SCHEDULE_DATE)
                .execute();
    }

    private void createGroupsTable() {
        context.createTableIfNotExists(GROUPS)
                .column(GROUPS_ID, SQLDataType.BIGINT.generatedByDefaultAsIdentity().nullable(false))
                .primaryKey(GROUPS_ID)
                .column(GROUPS_SCHEDULE_ID)
                .column(GROUPS_NAME)
                .constraints(
                        DSL.constraint("uk_groups_schedule_name").unique(GROUPS_SCHEDULE_ID, GROUPS_NAME),
                        DSL.constraint("fk_groups_schedule_id_schedules_id")
                                .foreignKey(GROUPS_SCHEDULE_ID)
                                .references(SCHEDULES, SCHEDULES_ID)
                                .onDeleteCascade()
                )
                .execute();
    }

    private void createTeachersTable() {
        context.createTableIfNotExists(TEACHERS)
                .column(TEACHERS_ID, SQLDataType.BIGINT.generatedByDefaultAsIdentity().nullable(false))
                .primaryKey(TEACHERS_ID)
                .column(TEACHERS_SCHEDULE_ID)
                .column(TEACHERS_NAME)
                .constraints(
                        DSL.constraint("uk_teachers_schedule_name").unique(TEACHERS_SCHEDULE_ID, TEACHERS_NAME),
                        DSL.constraint("fk_teachers_schedule_id_schedules_id")
                                .foreignKey(TEACHERS_SCHEDULE_ID)
                                .references(SCHEDULES, SCHEDULES_ID)
                                .onDeleteCascade()
                )
                .execute();
    }

    private void createLessonsTable() {
        context.createTableIfNotExists(LESSONS)
                .column(LESSONS_ID, SQLDataType.BIGINT.generatedByDefaultAsIdentity().nullable(false))
                .primaryKey(LESSONS_ID)
                .column(LESSONS_SCHEDULE_ID)
                .column(LESSONS_TEACHER_ID)
                .column(LESSONS_GROUP_ID)
                .column(SUBJECT_NAME)
                .column(ROOM)
                .column(SUBGROUP)
                .column(TIME)
                .column(STATE)
                .column(CUSTOM_TIME)
                .constraints(
                        DSL.constraint("fk_lessons_schedule_id_schedules_id")
                                .foreignKey(LESSONS_SCHEDULE_ID)
                                .references(SCHEDULES, SCHEDULES_ID)
                                .onDeleteCascade(),
                        DSL.constraint("fk_lessons_teacher_id_teachers_id")
                                .foreignKey(LESSONS_TEACHER_ID)
                                .references(TEACHERS, TEACHERS_ID)
                                .onDeleteSetNull(),
                        DSL.constraint("fk_lessons_group_id_groups_id")
                                .foreignKey(LESSONS_GROUP_ID)
                                .references(GROUPS, GROUPS_ID)
                                .onDeleteCascade()
                )
                .execute();
    }

    private void createLessonTeachersTable() {
        context.createTableIfNotExists(LESSON_TEACHERS)
                .column(LESSON_TEACHERS_LESSON_ID)
                .column(LESSON_TEACHERS_TEACHER_ID)
                .constraints(
                        DSL.constraint("pk_lesson_teachers")
                                .primaryKey(LESSON_TEACHERS_LESSON_ID, LESSON_TEACHERS_TEACHER_ID),
                        DSL.constraint("fk_lesson_teachers_lesson_id_lessons_id")
                                .foreignKey(LESSON_TEACHERS_LESSON_ID)
                                .references(LESSONS, LESSONS_ID)
                                .onDeleteCascade(),
                        DSL.constraint("fk_lesson_teachers_teacher_id_teachers_id")
                                .foreignKey(LESSON_TEACHERS_TEACHER_ID)
                                .references(TEACHERS, TEACHERS_ID)
                                .onDeleteCascade()
                )
                .execute();
    }

    private void createIndexes() {
        context.createIndexIfNotExists("idx_groups_schedule_id")
                .on(GROUPS, GROUPS_SCHEDULE_ID)
                .execute();

        context.createIndexIfNotExists("idx_teachers_schedule_id")
                .on(TEACHERS, TEACHERS_SCHEDULE_ID)
                .execute();

        context.createIndexIfNotExists("idx_lessons_schedule_id")
                .on(LESSONS, LESSONS_SCHEDULE_ID)
                .execute();

        context.createIndexIfNotExists("idx_lessons_group_id")
                .on(LESSONS, LESSONS_GROUP_ID)
                .execute();

        context.createIndexIfNotExists("idx_lessons_teacher_id")
                .on(LESSONS, LESSONS_TEACHER_ID)
                .execute();

        context.createIndexIfNotExists("idx_lesson_teachers_lesson_id")
                .on(LESSON_TEACHERS, LESSON_TEACHERS_LESSON_ID)
                .execute();

        context.createIndexIfNotExists("idx_lesson_teachers_teacher_id")
                .on(LESSON_TEACHERS, LESSON_TEACHERS_TEACHER_ID)
                .execute();
    }

    private void migrateLessonTeachers() {
        context.insertInto(LESSON_TEACHERS, LESSON_TEACHERS_LESSON_ID, LESSON_TEACHERS_TEACHER_ID)
                .select(
                        context.select(LESSONS_ID, LESSONS_TEACHER_ID)
                                .from(LESSONS)
                                .where(LESSONS_TEACHER_ID.isNotNull())
                )
                .onConflictDoNothing()
                .execute();
    }

    public void insertSchedule(ScheduleInfo info) throws DataAccessException {
        context.insertInto(SCHEDULES)
                .set(EDIT_DATE_TIME, info.editDateTime())
                .set(SCHEDULE_DATE, info.scheduleDate())
                .execute();

        var response = context.select(SCHEDULES_ID)
                .from(SCHEDULES)
                .where(EDIT_DATE_TIME.eq(info.editDateTime()))
                .fetch();
        long scheduleId = response.getValue(0, SCHEDULES_ID);

        fillGroupTeacherLists(scheduleId, info);
        fillLessons(scheduleId, info);

        AkttAPI.LOGGER.info("ID расписания: {}", scheduleId);
    }

    private void fillGroupTeacherLists(long scheduleId, ScheduleInfo info) {
        var groups_insert = context.insertInto(GROUPS, GROUPS_SCHEDULE_ID, GROUPS_NAME);
        var teachers_insert = context.insertInto(TEACHERS, TEACHERS_SCHEDULE_ID, TEACHERS_NAME);

        for (String groupName : info.groupsList()) {
            groups_insert = groups_insert.values(scheduleId, groupName);
        }

        for (String teacherName : info.teachersList()) {
            teachers_insert = teachers_insert.values(scheduleId, teacherName);
        }

        int g = groups_insert.execute();
        int t = teachers_insert.execute();

        AkttAPI.LOGGER.info("Количество вставленных [групп|преподавателей]: [{}|{}]", g, t);
    }

    private void fillLessons(long scheduleId, ScheduleInfo info) {
        List<Query> queries = new ArrayList<>();

        Map<String, Long> teachers = context.select(TEACHERS_NAME, TEACHERS_ID)
                .from(TEACHERS)
                .where(TEACHERS_SCHEDULE_ID.eq(scheduleId))
                .fetchMap(TEACHERS_NAME, TEACHERS_ID);

        Map<String, Long> groups = context.select(GROUPS_NAME, GROUPS_ID)
                .from(GROUPS)
                .where(GROUPS_SCHEDULE_ID.eq(scheduleId))
                .fetchMap(GROUPS_NAME, GROUPS_ID);

        for (LessonInfo lessonInfo : info.lessons()) {
            Long groupId = groups.get(lessonInfo.groupName());
            if (groupId == null) {
                throw new IllegalStateException("Не найдена группа в БД: \"" + lessonInfo.groupName() + "\"");
            }

            List<String> teacherNames = lessonInfo.teacherNames() == null ? List.of() : lessonInfo.teacherNames();
            String customTime = lessonInfo.time().equals(LessonTime.CUSTOM) ? lessonInfo.time().getCustomTime() : null;

            Long lessonId = context.insertInto(LESSONS)
                    .set(LESSONS_SCHEDULE_ID, scheduleId)
                    .set(LESSONS_GROUP_ID, groupId)
                    .set(SUBJECT_NAME, lessonInfo.subjectName())
                    .set(ROOM, lessonInfo.room())
                    .set(SUBGROUP, lessonInfo.subGroup())
                    .set(TIME, lessonInfo.time())
                    .set(STATE, lessonInfo.state())
                    .set(CUSTOM_TIME, customTime)
                    .returningResult(LESSONS_ID)
                    .fetchOne(LESSONS_ID);

            if (lessonId == null) {
                throw new IllegalStateException("Не удалось сохранить урок: \"" + lessonInfo.subjectName() + "\"");
            }

            for (String teacherName : teacherNames) {
                Long teacherId = teachers.get(teacherName);
                if (teacherId == null) {
                    AkttAPI.LOGGER.warn("Для урока '{}' не найден преподаватель: {}",
                            lessonInfo.subjectName(), teacherName);
                    continue;
                }

                Query query = context.insertInto(LESSON_TEACHERS)
                        .set(LESSON_TEACHERS_LESSON_ID, lessonId)
                        .set(LESSON_TEACHERS_TEACHER_ID, teacherId)
                        .onConflictDoNothing();
                queries.add(query);
            }
        }

        if (!queries.isEmpty()) {
            context.batch(queries).execute();
        }
    }


    public List<LessonInfo> getLessonsForGroup(LocalDate scheduleDate, String groupName, SubGroup subGroup) {
        Optional<Long> scheduleIdOpt = getScheduleID(scheduleDate);

        if (scheduleIdOpt.isEmpty()) return Collections.emptyList();
        long scheduleId = scheduleIdOpt.get();

        Optional<Long> groupIdOpt = context.select(GROUPS_ID)
                .from(GROUPS)
                .where(GROUPS_SCHEDULE_ID.eq(scheduleId))
                .and(GROUPS_NAME.eq(groupName))
                .fetchOptional(GROUPS_ID);

        if (groupIdOpt.isEmpty()) return Collections.emptyList();
        long groupId = groupIdOpt.get();

        Map<Long, List<String>> teacherNamesByLessonId = getTeacherNamesByLessonId(scheduleId);

        Condition subgroupCondition = (subGroup == SubGroup.BOTH)
                ? DSL.trueCondition()
                : SUBGROUP.in(subGroup, SubGroup.BOTH);

        return context.select(
                        LESSONS_ID,
                        LESSONS_TEACHER_ID,
                        TIME,
                        SUBJECT_NAME,
                        ROOM,
                        SUBGROUP,
                        STATE,
                        CUSTOM_TIME
                )
                .from(LESSONS)
                .where(LESSONS_SCHEDULE_ID.eq(scheduleId))
                .and(LESSONS_GROUP_ID.eq(groupId))
                .and(subgroupCondition)
                .fetch(r -> {
                    Long lessonId = r.get(LESSONS_ID);
                    Long teacherId = r.get(LESSONS_TEACHER_ID);
                    List<String> teacherNames = teacherNamesByLessonId.getOrDefault(lessonId, List.of());

                    if (teacherNames.isEmpty() && teacherId != null) {
                        teacherNames = getTeacherNamesForLegacyLesson(scheduleId, teacherId);
                    }

                    return new LessonInfo(
                            groupName,
                            teacherNames,
                            r.get(TIME),
                            r.get(SUBJECT_NAME),
                            r.get(ROOM),
                            r.get(SUBGROUP),
                            r.get(STATE),
                            r.get(CUSTOM_TIME)
                    );
                });
    }

    public List<LessonInfo> getLessonsForTeacher(LocalDate scheduleDate, String teacherName) {
        Optional<Long> scheduleIdOpt = getScheduleID(scheduleDate);

        if (scheduleIdOpt.isEmpty()) return Collections.emptyList();
        long scheduleId = scheduleIdOpt.get();

        Optional<Long> teacherIdOpt = context.select(TEACHERS_ID)
                .from(TEACHERS)
                .where(TEACHERS_SCHEDULE_ID.eq(scheduleId))
                .and(TEACHERS_NAME.eq(teacherName))
                .fetchOptional(TEACHERS_ID);

        if (teacherIdOpt.isEmpty()) return Collections.emptyList();
        long teacherId = teacherIdOpt.get();

        Map<Long, String> groupsById = context.select(GROUPS_ID, GROUPS_NAME)
                .from(GROUPS)
                .where(GROUPS_SCHEDULE_ID.eq(scheduleId))
                .fetchMap(GROUPS_ID, GROUPS_NAME);

        Map<Long, List<String>> teacherNamesByLessonId = getTeacherNamesByLessonId(scheduleId);

        return context.select(
                        LESSONS_ID,
                        LESSONS_GROUP_ID,
                        LESSONS_TEACHER_ID,
                        TIME,
                        SUBJECT_NAME,
                        ROOM,
                        SUBGROUP,
                        STATE,
                        CUSTOM_TIME
                )
                .from(LESSONS)
                .leftJoin(LESSON_TEACHERS)
                .on(LESSON_TEACHERS_LESSON_ID.eq(LESSONS_ID))
                .where(LESSONS_SCHEDULE_ID.eq(scheduleId))
                .and(
                        LESSON_TEACHERS_TEACHER_ID.eq(teacherId)
                                .or(LESSONS_TEACHER_ID.eq(teacherId))
                )
                .fetch(r -> {
                    Long lessonId = r.get(LESSONS_ID);
                    Long groupId = r.get(LESSONS_GROUP_ID);
                    String groupName = groupId == null ? null : groupsById.get(groupId);
                    List<String> teacherNames = teacherNamesByLessonId.getOrDefault(lessonId, List.of());

                    if (teacherNames.isEmpty()) {
                        teacherNames = getTeacherNamesForLegacyLesson(scheduleId, r.get(LESSONS_TEACHER_ID));
                    }

                    return new LessonInfo(
                            groupName,
                            teacherNames,
                            r.get(TIME),
                            r.get(SUBJECT_NAME),
                            r.get(ROOM),
                            r.get(SUBGROUP),
                            r.get(STATE),
                            r.get(CUSTOM_TIME)
                    );
                });
    }

    private Map<Long, List<String>> getTeacherNamesByLessonId(long scheduleId) {
        Map<Long, List<String>> result = new HashMap<>();

        Result<Record2<Long, String>> records = context.select(LESSON_TEACHERS_LESSON_ID, TEACHERS_NAME)
                .from(LESSON_TEACHERS)
                .join(TEACHERS)
                .on(LESSON_TEACHERS_TEACHER_ID.eq(TEACHERS_ID))
                .where(TEACHERS_SCHEDULE_ID.eq(scheduleId))
                .orderBy(LESSON_TEACHERS_LESSON_ID.asc(), TEACHERS_NAME.asc())
                .fetch();

        for (Record2<Long, String> record : records) {
            Long lessonId = record.get(LESSON_TEACHERS_LESSON_ID);
            String teacherName = record.get(TEACHERS_NAME);
            result.computeIfAbsent(lessonId, ignored -> new ArrayList<>()).add(teacherName);
        }

        return result;
    }

    private List<String> getTeacherNamesForLegacyLesson(long scheduleId, Long teacherId) {
        if (teacherId == null) {
            return List.of();
        }

        String teacherName = context.select(TEACHERS_NAME)
                .from(TEACHERS)
                .where(TEACHERS_SCHEDULE_ID.eq(scheduleId))
                .and(TEACHERS_ID.eq(teacherId))
                .fetchOptional(TEACHERS_NAME)
                .orElse(null);

        return teacherName == null ? List.of() : List.of(teacherName);
    }

    public Optional<LocalDate> getLatestScheduleDate() {
        return context.select(SCHEDULE_DATE)
                .from(SCHEDULES)
                .orderBy(SCHEDULE_DATE.desc(), EDIT_DATE_TIME.desc())
                .limit(1)
                .fetchOptional(SCHEDULE_DATE);
    }

    private Optional<Long> getScheduleID(LocalDate scheduleDate) {
        return context.select(SCHEDULES_ID)
                .from(SCHEDULES)
                .where(SCHEDULE_DATE.eq(scheduleDate))
                .orderBy(EDIT_DATE_TIME.desc())
                .limit(1)
                .fetchOptional(SCHEDULES_ID);
    }

    public List<String> getGroupsList(LocalDate scheduleDate) {
        var scheduleId = getScheduleID(scheduleDate);
        return scheduleId.map(aLong -> List.of(context.select(GROUPS_NAME).from(GROUPS).where(GROUPS_SCHEDULE_ID.eq(aLong)).fetchArray(GROUPS_NAME))).orElse(Collections.emptyList());
    }

    public List<String> getTeachersList(LocalDate scheduleDate) {
        var scheduleId = getScheduleID(scheduleDate);
        return scheduleId.map(aLong -> List.of(context.select(TEACHERS_NAME).from(TEACHERS).where(TEACHERS_SCHEDULE_ID.eq(aLong)).fetchArray(TEACHERS_NAME))).orElse(Collections.emptyList());
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
