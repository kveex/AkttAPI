package org.kveex.api;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import org.kveex.AkttAPI;
import org.kveex.schedule.SubGroup;
import org.kveex.schedule.ScheduleGroup;
import org.kveex.schedule.ScheduleHandlerV2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GetHandler {
    public static void showTest(Context context) {
        AkttAPI.LOGGER.info("Сделан запрос на главную страницу");
        context.json(Map.of("message", "Привет от АКТТ REST API"));
    }

    @OpenApi(
            summary = "Выдаёт расписание для указанной группы и подгруппы",
            operationId = "getScheduleGroupDefinedSubGroup",
            path = "/api/schedule/student/{group}/{subGroup}",
            pathParams = {
                    @OpenApiParam(
                            name = "group",
                            description = "Название учебной группы, обязательно со строчными буквами",
                            example = "23-14ис",
                            required = true
                    ),
                    @OpenApiParam(
                            name = "subGroup",
                            description = "Номер подгруппы: 1 - первая, 2 - вторая, 0 - обе",
                            example = "1",
                            required = true
                    )
            },
            methods = HttpMethod.GET,
            tags = {"Schedule"},
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = {@OpenApiContent(from = ScheduleGroup.class)}),
                    @OpenApiResponse(
                            status = "400",
                            description = "Подгруппа указана неверно"
                    ),
                    @OpenApiResponse(
                            status = "404",
                            description = "Группа не найдена"
                    )
            }
    )
    public static void getScheduleGroupDefinedSubGroup(ScheduleHandlerV2 scheduleHandler, Context context) {
        String groupName = context.pathParam("group");
        int subGroup;

        try {
            subGroup = Integer.parseInt(context.pathParam("subGroup"));
        } catch (NumberFormatException e) {
            context.status(HttpStatus.BAD_REQUEST);
            context.json(Map.of("error", "подгруппа должна быть числом!"));
            return;
        }

        try {
            var sch = scheduleHandler.getStudentScheduleGroup(groupName, SubGroup.toSubGroup(subGroup));
            context.status(HttpStatus.OK);
            context.json(sch);
            AkttAPI.LOGGER.info("Запрос на расписание группы {} для {} подгруппы", groupName, subGroup);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.NOT_FOUND);
            context.json(Map.of("error", e.toString()));
            AkttAPI.LOGGER.error(e.toString());
        }
    }

    @OpenApi(
            summary = "Выдаёт расписание для указанной группы с обеими подгруппами",
            operationId = "getScheduleGroupBothSubGroups",
            path = "/api/schedule/student/{group}",
            pathParams = {
                    @OpenApiParam(
                            name = "group",
                            description = "Название учебной группы, обязательно не заглавными буквами",
                            example = "23-14ис",
                            required = true
                    )
            },
            methods = HttpMethod.GET,
            tags = {"Schedule"},
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = {@OpenApiContent(from = ScheduleGroup.class)}),
                    @OpenApiResponse(
                            status = "404",
                            description = "Группа не найдена"
                    )
            }
    )
    public static void getScheduleGroupBothSubGroups(ScheduleHandlerV2 scheduleHandler, Context context) {
        String groupName = context.pathParam("group");
        try {
            var sch = scheduleHandler.getStudentScheduleGroup(groupName);
            context.status(HttpStatus.OK);
            context.json(sch);
            AkttAPI.LOGGER.info("Запрос на расписание для группы {} для обеих подгрупп", groupName);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.NOT_FOUND);
            context.json(Map.of("error", e.toString()));
            AkttAPI.LOGGER.error(e.toString());
        }
    }

    @OpenApi(
            summary = "Выдаёт дату на которую рассчитано расписание",
            operationId = "getScheduleDate",
            path = "/api/schedule/date",
            methods = HttpMethod.GET,
            tags = {"Schedule"},
            responses = {
                    @OpenApiResponse(
                            status = "200"
                    )
            }
    )
    public static void getScheduleDate(ScheduleHandlerV2 scheduleHandler, Context context) {
        String scheduleDate = scheduleHandler.getScheduleDate().toString();
        context.status(HttpStatus.OK);
        context.json(Map.of("scheduleDate", scheduleDate));
        AkttAPI.LOGGER.info("Запрос на дату расписания");
    }

    @OpenApi(
            summary = "Выдаёт расписание всех групп, с обеими подгруппами",
            operationId = "getSchedule",
            path = "/api/schedule/",
            methods = HttpMethod.GET,
            tags = {"Schedule"},
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = {@OpenApiContent(from = ScheduleGroup.class)})
            }
    )
    public static void getSchedule(ScheduleHandlerV2 scheduleHandler, Context context) {
        var schedule = scheduleHandler.getSchedule();
        String scheduleDate = scheduleHandler.getScheduleDate().toString();
        context.status(HttpStatus.OK);
        context.json(Map.of("scheduleDate", scheduleDate, "schedule", schedule));
        AkttAPI.LOGGER.info("Запрос на полное расписание");
    }

    @OpenApi(
            summary = "Выдаёт список названий всех групп",
            operationId = "getGroupsList",
            path = "/api/schedule/groups",
            methods = HttpMethod.GET,
            tags = {"Schedule"},
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = {@OpenApiContent(from = String[].class)})
            }
    )
    public static void getGroupsList(ScheduleHandlerV2 scheduleHandler, Context context) {
        List<String> groups = scheduleHandler.getGroupsList();
        context.status(HttpStatus.OK);
        context.json(Map.of("groupsList", groups));
        AkttAPI.LOGGER.info("Запрос на список групп");
    }

    @OpenApi(
            summary = "Выдаёт список всех преподавателей",
            operationId = "getTeachersList",
            path = "/api/schedule/teachers",
            methods = HttpMethod.GET,
            tags = {"Schedule"},
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = {@OpenApiContent(from = ArrayList.class)}
                    )
            }
    )
    public static void getTeachersList(ScheduleHandlerV2 scheduleHandler, Context context) {
        List<String> teachers = scheduleHandler.getTeachersList();
        context.status(HttpStatus.OK);
        context.json(Map.of("teachersList", teachers));
        AkttAPI.LOGGER.info("Запрос на список преподавателей");
    }

    @OpenApi(
            summary = "Выдаёт расписание для указанного преподавателя",
            operationId = "getTeacherSchedule",
            path = "/api/schedule/teacher/{teacher}",
            pathParams = {
                    @OpenApiParam(
                            name = "teacher",
                            description = "Имя и инициалы преподавателя, должны соответствовать формату",
                            example = "Маликов М.В.",
                            required = true
                    )
            },
            methods = HttpMethod.GET,
            tags = {"Schedule"},
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = {@OpenApiContent(from = ScheduleGroup.class)}),
                    @OpenApiResponse(
                            status = "404",
                            description = "Группа не найдена"
                    )
            }
    )
    public static void getTeacherSchedule(ScheduleHandlerV2 scheduleHandler, Context context) {
        String teacherName = context.pathParam("teacher");
        var sch = scheduleHandler.getTeacherScheduleGroup(teacherName);
        if (sch.scheduleItems().isEmpty() && sch.teacherName() == null) {
            context.status(HttpStatus.NOT_FOUND);
            context.json(Map.of("error", "Преподаватель [%s] не найден".formatted(teacherName)));
            AkttAPI.LOGGER.error("Преподаватель [{}] не найден", teacherName);
        } else {
            context.status(HttpStatus.OK);
            context.json(sch);
            AkttAPI.LOGGER.info("Запрос на расписание для преподавателя {}", teacherName);
        }
    }
}
