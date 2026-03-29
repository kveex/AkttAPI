package org.kveex.api;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import org.kveex.AkttAPI;
import org.kveex.schedule.SubGroup;
import org.kveex.schedule.LessonGroup;
import org.kveex.schedule.ScheduleHandler;

import java.util.List;
import java.util.Map;

public class GetHandler {
    public static void showTest(Context context) {
        AkttAPI.LOGGER.info("Сделан запрос на главную страницу");
        context.json(Map.of("message", "Привет от АКТТ REST API"));
    }
//
//    @OpenApi(
//            summary = "Выдаёт расписание для указанной группы и подгруппы",
//            operationId = "getScheduleGroupDefinedSubGroup",
//            path = "/api/schedule/student/{group}/{subGroup}",
//            pathParams = {
//                    @OpenApiParam(
//                            name = "group",
//                            description = "Название учебной группы, обязательно со строчными буквами",
//                            example = "23-14ис",
//                            required = true
//                    ),
//                    @OpenApiParam(
//                            name = "subGroup",
//                            type = int.class,
//                            description = "Номер подгруппы: 1 - первая, 2 - вторая, 0 - обе",
//                            example = "1",
//                            required = true
//                    )
//            },
//            responses = {
//                    @OpenApiResponse(
//                            status = "200",
//                            content = {@OpenApiContent(from = ScheduleGroup.class)}),
//                    @OpenApiResponse(
//                            status = "400",
//                            description = "Подгруппа указана неверно"
//                    ),
//                    @OpenApiResponse(
//                            status = "404",
//                            description = "Группа не найдена"
//                    )
//            }
//    )
//    public static void getScheduleGroupDefinedSubGroup(Context context) {
//        var scheduleHandler = ScheduleHandler.getInstance();
//        String groupName = context.pathParam("group");
//        int subGroup;
//
//        try {
//            subGroup = Integer.parseInt(context.pathParam("subGroup"));
//        } catch (NumberFormatException e) {
//            context.status(HttpStatus.BAD_REQUEST);
//            context.json(Map.of("error", "подгруппа должна быть числом!"));
//            return;
//        }
//
//        try {
//            var scheduleGroup = scheduleHandler.getStudentScheduleGroup(groupName, SubGroup.toSubGroup(subGroup));
//            context.status(HttpStatus.OK);
//            context.json(scheduleGroup);
//            AkttAPI.LOGGER.info("Запрос на расписание группы {} для {} подгруппы", groupName, subGroup);
//        } catch (IllegalArgumentException e) {
//            context.status(HttpStatus.NOT_FOUND);
//            context.json(Map.of("error", e.toString()));
//            AkttAPI.LOGGER.error(e.toString());
//        }
//    }
//
//    @OpenApi(
//            summary = "Выдаёт расписание для указанной группы с обеими подгруппами",
//            operationId = "getScheduleGroupBothSubGroups",
//            path = "/api/schedule/student/{group}",
//            pathParams = {
//                    @OpenApiParam(
//                            name = "group",
//                            description = "Название учебной группы, обязательно не заглавными буквами",
//                            example = "23-14ис",
//                            required = true
//                    )
//            },
//            methods = HttpMethod.GET,
//            tags = {"Schedule"},
//            responses = {
//                    @OpenApiResponse(
//                            status = "200",
//                            content = {@OpenApiContent(from = ScheduleGroup.class)}),
//                    @OpenApiResponse(
//                            status = "404",
//                            description = "Группа не найдена"
//                    )
//            }
//    )
//    public static void getScheduleGroupBothSubGroups(Context context) {
//        String groupName = context.pathParam("group");
//        try {
//            var sch = ScheduleHandler.getInstance().getStudentScheduleGroup(groupName);
//            context.status(HttpStatus.OK);
//            context.json(sch);
//            AkttAPI.LOGGER.info("Запрос на расписание для группы {} для обеих подгрупп", groupName);
//        } catch (IllegalArgumentException e) {
//            context.status(HttpStatus.NOT_FOUND);
//            context.json(Map.of("error", e.toString()));
//            AkttAPI.LOGGER.error(e.toString());
//        }
//    }

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
    public static void getScheduleDate(Context context) {
        String scheduleDate = ScheduleHandler.getInstance().getScheduleDate().toString();
        context.status(HttpStatus.OK);
        context.json(Map.of("scheduleDate", scheduleDate));
        AkttAPI.LOGGER.info("Запрос на дату расписания");
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
                            content = {@OpenApiContent(from = String[].class)}
                    )
            }
    )
    public static void getTeachersList(Context context) {
        var scheduleHandler = ScheduleHandler.getInstance();
        List<String> teachers = scheduleHandler.getTeachersList();
        context.status(HttpStatus.OK);
        context.json(Map.of("teachersList", teachers));
        AkttAPI.LOGGER.info("Запрос на список преподавателей");
    }
}
