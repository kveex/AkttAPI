package org.kveex.api;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import org.kveex.AkttAPI;
import org.kveex.database.DatabaseController;
import org.kveex.schedule.SubGroup;
import org.kveex.schedule.parser.LessonInfo;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class GetHandler {
    public static void showTest(Context context) {
        AkttAPI.LOGGER.info("Сделан запрос на главную страницу");
        context.json(Map.of("message", "Привет от АКТТ API"));
    }

    @OpenApi(
            summary = "Выдаёт список пар для указанной группы, с опциональным указанием подгруппы",
            operationId = "studentSchedule",
            path = "/api/schedule/student/{date}/{groupName}",
            pathParams = {
                    @OpenApiParam(
                            name = "date",
                            description = "Дата расписания в формате yyyy-mm-dd",
                            required = true
                    ),
                    @OpenApiParam(
                            name = "groupName",
                            description = "Название учебной группы, регистро-независимое",
                            example = "23-14ИС",
                            required = true
                    )
            },
            queryParams = {
                    @OpenApiParam(
                            name = "subgroup",
                            description = "Подгруппа, указанной учебной группы, в виде числа (1, 2, любое)",
                            example = "2"
                    )
            },
            methods = HttpMethod.GET,
            tags = {"Schedule"},
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = @OpenApiContent(from = ArrayList.class)
                    ),
                    @OpenApiResponse(
                            status = "400",
                            description = "Дата указана в неверном формате"
                    ),
                    @OpenApiResponse(
                            status = "404",
                            description = "Расписание для указанной группы не было найдено"
                    )
            }
    )
    public static void studentSchedule(Context context) {
        var databaseController = DatabaseController.getInstance();
        String groupName = context.pathParam("groupName").toLowerCase();

        SubGroup subGroup = getSubGroup(context);
        Optional<LocalDate> scheduleDate = getScheduleDate(context);

        if (scheduleDate.isEmpty()) return;

        List<LessonInfo> lessonInfoList = databaseController.getLessonsForGroup(scheduleDate.get(), groupName, subGroup);

        if (lessonInfoList.isEmpty()) {
            context.json(Map.of("error", "Расписание для группы [%s] не найдено".formatted(groupName)));
            context.status(HttpStatus.NOT_FOUND);
            return;
        }

        context.json(lessonInfoList);
        context.status(HttpStatus.OK);
    }

    @OpenApi(
            summary = "Выдаёт список пар для указанного преподавателя, с опциональным указанием подгруппы",
            operationId = "teacherSchedule",
            path = "/api/schedule/teacher/{date}/{teacherName}",
            pathParams = {
                    @OpenApiParam(
                            name = "date",
                            description = "Дата расписания в формате yyyy-mm-dd",
                            required = true
                    ),
                    @OpenApiParam(
                            name = "teacherName",
                            description = "Имя и инициалы преподавателя, регистро-зависимое",
                            example = "Маликова Н.А.",
                            required = true
                    )
            },
            methods = HttpMethod.GET,
            tags = {"Schedule"},
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = @OpenApiContent(from = ArrayList.class)
                    ),
                    @OpenApiResponse(
                            status = "400",
                            description = "Дата указана в неверном формате"
                    ),
                    @OpenApiResponse(
                            status = "404",
                            description = "Расписание для указанного преподавателя не было найдено"
                    )
            }
    )
    public static void teacherSchedule(Context context) {
        var databaseController = DatabaseController.getInstance();
        String teacherName = context.pathParam("teacherName");

        Optional<LocalDate> scheduleDate = getScheduleDate(context);

        if (scheduleDate.isEmpty()) return;

        List<LessonInfo> lessonInfoList = databaseController.getLessonsForTeacher(scheduleDate.get(), teacherName);

        if (lessonInfoList.isEmpty()) {
            context.json(Map.of("error", "Расписание для преподавателя [%s] не найдено".formatted(teacherName)));
            context.status(HttpStatus.NOT_FOUND);
            return;
        }

        context.json(lessonInfoList);
        context.status(HttpStatus.OK);
    }

    @OpenApi(
            summary = "Выдаёт список групп для которых есть расписание",
            operationId = "groupsList",
            path = "/api/schedule/groups/{date}",
            pathParams = {
                    @OpenApiParam(
                            name = "date",
                            description = "Дата расписания в формате yyyy-mm-dd",
                            required = true
                    )
            },
            methods = HttpMethod.GET,
            tags = {"Schedule"},
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = @OpenApiContent(from = ArrayList.class)
                    ),
                    @OpenApiResponse(
                            status = "400",
                            description = "Дата указана в неверном формате"
                    ),
                    @OpenApiResponse(
                            status = "404",
                            description = "Нет списка групп для указанной даты"
                    )
            }
    )
    public static void groupsList(Context context) {
        var databaseController = DatabaseController.getInstance();
        Optional<LocalDate> scheduleDate = getScheduleDate(context);

        if (scheduleDate.isEmpty()) return;

        List<String> groupsList = databaseController.getGroupsList(scheduleDate.get());

        if (groupsList.isEmpty()) {
            context.json(Map.of("error", "На указанную дату [%s] не получилось получить список групп!".formatted(scheduleDate.get().toString())));
            context.status(HttpStatus.NOT_FOUND);
            return;
        }

        context.json(groupsList);
        context.status(HttpStatus.OK);
    }

    @OpenApi(
            summary = "Выдаёт список преподавателей для которых есть расписание",
            operationId = "teachersList",
            path = "/api/schedule/teachers/{date}",
            pathParams = {
                    @OpenApiParam(
                            name = "date",
                            description = "Дата расписания в формате yyyy-mm-dd",
                            required = true
                    )
            },
            methods = HttpMethod.GET,
            tags = {"Schedule"},
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = @OpenApiContent(from = ArrayList.class)
                    ),
                    @OpenApiResponse(
                            status = "400",
                            description = "Дата указана в неверном формате"
                    ),
                    @OpenApiResponse(
                            status = "404",
                            description = "Нет списка преподавателей для указанной даты"
                    )
            }
    )
    public static void teachersList(Context context) {
        var databaseController = DatabaseController.getInstance();
        Optional<LocalDate> scheduleDate = getScheduleDate(context);

        if (scheduleDate.isEmpty()) return;

        List<String> teachersList = databaseController.getTeachersList(scheduleDate.get());

        if (teachersList.isEmpty()) {
            context.json(Map.of("error", "На указанную дату [%s] не получилось получить список преподавателей!".formatted(scheduleDate.get().toString())));
            context.status(HttpStatus.NOT_FOUND);
            return;
        }

        context.json(teachersList);
        context.status(HttpStatus.OK);
    }

    private static SubGroup getSubGroup(Context context) {
        String strSubGroup = context.queryParam("subgroup");

        SubGroup subGroup = SubGroup.BOTH;

        if (strSubGroup != null) {
            subGroup = SubGroup.toSubGroup(strSubGroup);
        }

        return subGroup;
    }

    private static Optional<LocalDate> getScheduleDate(Context context) {
        String strDate = context.pathParam("date");

        try {
            return Optional.of(LocalDate.parse(strDate));
        } catch (DateTimeParseException _) {
            String errorStr = "Ошибка во время парсинга даты перед запросом расписания!";
            AkttAPI.LOGGER.error(errorStr);
            context.json(Map.of("error", errorStr));
            context.status(HttpStatus.BAD_REQUEST);
            return Optional.empty();
        }
    }
}
