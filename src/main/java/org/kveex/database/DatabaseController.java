package org.kveex.database;

import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.kveex.AkttAPI;
import org.kveex.schedule.LessonState;
import org.kveex.schedule.SubGroup;
import org.kveex.schedule.parser.ScheduleInfo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DatabaseController {
    private static DatabaseController INSTANCE = null;
    private DSLContext context;

    private static final Field<?> ID = DSL.field(DSL.name("id"), SQLDataType.INTEGER.autoIncrement().notNull());
    private static final Field<?> SCHEDULE_ID = DSL.field(DSL.name("schedule_id"), SQLDataType.INTEGER.notNull());
    private static final Field<String> NAME = DSL.field(DSL.name("name"), SQLDataType.NVARCHAR.notNull());

    // region Поля для таблицы с расписаниями
    private static final Table<?> SCHEDULES = DSL.table(DSL.name("schedules"));
    private static final Field<LocalDateTime> EDIT_DATE_TIME = DSL.field(DSL.name("edit_date_time"), SQLDataType.LOCALDATETIME.notNull());
    private static final Field<LocalDate> SCHEDULE_DATE = DSL.field(DSL.name("schedule_date"), SQLDataType.LOCALDATE.notNull());
    // endregion

    // region Поля с парами для преподавателей
    // schedule_id сюда
    private static final Table<?> TEACHER_LESSONS = DSL.table(DSL.name("teacher_lessons"));
    // endregion

    // region Поля с парами для студентов
    // schedule_id сюда
    private static final Table<?> GROUP_LESSONS = DSL.table(DSL.name("group_lessons"));
    // endregion

    // region Поля для информации по каждой паре
    private static final Table<?> LESSONS = DSL.table(DSL.name("lessons"));
    private static final Field<Integer> TEACHER_ID = DSL.field(DSL.name("teacher_id"), SQLDataType.INTEGER.nullable(true));
    private static final Field<Integer> GROUP_ID = DSL.field(DSL.name("group_id"), SQLDataType.INTEGER.nullable(true));
    private static final Field<String> SUBJECT_NAME = DSL.field(DSL.name("subject_name"), SQLDataType.NVARCHAR.notNull());
    private static final Field<String> ROOM = DSL.field(DSL.name("room"), SQLDataType.NVARCHAR(3).notNull());
    private static final Field<SubGroup> SUBGROUP = DSL.field(DSL.name("subgroup"), SQLDataType.VARCHAR.asEnumDataType(SubGroup.class).notNull());
    private static final Field<String> TIME = DSL.field(DSL.name("time"), SQLDataType.NVARCHAR.notNull());
    private static final Field<LessonState> STATE = DSL.field(DSL.name("state"), SQLDataType.VARCHAR.asEnumDataType(LessonState.class).notNull());
    // endregion

    // region Поля для списка групп
    private static final Table<?> GROUPS = DSL.table(DSL.name("groups"));
    // endregion

    // region Поля для списка групп
    private static final Table<?> TEACHERS = DSL.table(DSL.name("teachers"));
    // endregion

    private DatabaseController(String url) {
        try (Connection connection = DriverManager.getConnection(url)){
            context = DSL.using(connection, SQLDialect.POSTGRES);
        } catch (SQLException e) {
            AkttAPI.LOGGER.error("Что-то пошло не так при инициализации базы данных! Причина: {} | Не рекомендуется пользоваться API в таком состоянии, исправьте проблему!", e.toString());
        }
        tryCreateTables();
    }

    public static void initialize(String url) {
        if (INSTANCE == null) {
            INSTANCE = new DatabaseController(url);
        } else {
            AkttAPI.LOGGER.warn("Контроллер для базы данных уже инициализирован!");
        }
    }

    public static DatabaseController getInstance() {
        return INSTANCE;
    }

    private void tryCreateTables() {
        var schedulesTable = context.createTableIfNotExists(SCHEDULES)
                .column(ID).primaryKey(ID)
                .column(EDIT_DATE_TIME)
                .column(SCHEDULE_DATE);
//                .column(GROUPS_LIST)
//                .column(TEACHERS_LIST)

        var teacherLessons = context.createTableIfNotExists(TEACHER_LESSONS)
                .column(ID).primaryKey(ID)
                .column(SCHEDULE_ID)
                .column(NAME);

        var groupLessons = context.createTableIfNotExists(GROUP_LESSONS)
                .column(ID).primaryKey(ID)
                .column(SCHEDULE_ID)
                .column(NAME);

        var lessons = context.createTableIfNotExists(LESSONS)
                .column(TEACHER_ID)
                .column(GROUP_ID)
                .column(SUBJECT_NAME)
                .column(ROOM)
                .column(SUBGROUP)
                .column(TIME)
                .column(STATE);

        var groups = context.createTableIfNotExists(GROUPS)
                .column(SCHEDULE_ID)
                .column(NAME);

        var teachers = context.createTableIfNotExists(TEACHERS)
                .column(SCHEDULE_ID)
                .column(NAME);

        schedulesTable.execute();
        teacherLessons.execute();
        groupLessons.execute();
        lessons.execute();
        groups.execute();
        teachers.execute();
    }

    public static void insertSchedule(ScheduleInfo info) {
        DSLContext dslContext = INSTANCE.context;

        dslContext.insertInto(SCHEDULES)
                .set(EDIT_DATE_TIME, info.editDateTime())
                .set(SCHEDULE_DATE, info.scheduleDate())
                .execute();
    }
}
