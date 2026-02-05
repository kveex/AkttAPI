package org.kveex.api;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import org.kveex.AkttAPI;
import org.kveex.schedule.ScheduleGroup;
import org.kveex.schedule.ScheduleHandler;
import org.kveex.schedule.ScheduleHandlerV2;
import org.kveex.schedule.ScheduleItem;

import java.util.List;
import java.util.Map;

public class GetHandler {
    public static void showTest(Context context) {
        AkttAPI.LOGGER.info("Сделан запрос на главную страницу");
        context.json(Map.of("message", "Привет от АКТТ REST API"));
    }

    public static void getScheduleBothSubGroups(Context context, ScheduleHandler scheduleHandler) {
        String groupName = context.pathParam("group");
        AkttAPI.LOGGER.info("Запрос расписания для группы {}, на все подгруппы", groupName);
        sendScheduleGroup(scheduleHandler, context, groupName, 0);
    }

    public static void getScheduleDefinedSubGroup(Context context, ScheduleHandler scheduleHandler) {
        String groupName = context.pathParam("group");
        int subGroup = Integer.parseInt(context.pathParam("subGroup"));

        if (subGroup > 2) {
            context.status(HttpStatus.BAD_REQUEST);
            context.json(Map.of("error", "Неверно указанная подгруппа"));
            AkttAPI.LOGGER.error("Был выполнен неправильный запрос, подгруппа указана больше возможной или отрицательной");
            return;
        }
        AkttAPI.LOGGER.info("Запрос расписания для группы {}, {} подгруппы", groupName, subGroup);
        sendScheduleGroup(scheduleHandler, context, groupName, subGroup);
    }

    private static void sendScheduleGroup(ScheduleHandler scheduleHandler, Context context, String groupName, int subGroup) {
        ScheduleGroup scheduleGroup;

        try {
            scheduleGroup = scheduleHandler.createSchedule(groupName, subGroup);
            context.json(scheduleGroup);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.NOT_FOUND);
            context.json(Map.of("error", e.toString()));
            AkttAPI.LOGGER.error(e.toString());
        }
    }

    public static void getGroupsList(Context context, ScheduleHandler scheduleHandler) {
        List<String> groups = scheduleHandler.getAllGroups();
        AkttAPI.LOGGER.info("Запрос на список групп");
        context.json(Map.of("groupsList", groups));
    }

    @OpenApi(
            summary = "Выдаёт расписание для указанной группы и подгруппы",
            operationId = "getScheduleGroupDefinedSubGroup",
            path = "/api/v2/schedule/{group}/{subGroup}",
            pathParams = {
                    @OpenApiParam(
                            name = "group",
                            description = "Название учебной группы, обязательно не заглавными буквами",
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
                            status = "404",
                            description = "Группа не найдена"
                    )
            }
    )
    public static void getScheduleGroupDefinedSubGroup(ScheduleHandlerV2 scheduleHandler, Context context) {
        String groupName = context.pathParam("group");
        int subGroup = Integer.parseInt(context.pathParam("subGroup"));

        try {
            var sch = scheduleHandler.getScheduleGroup(groupName, ScheduleItem.SubGroup.toSubGroup(subGroup));
            AkttAPI.LOGGER.info("Запрос на расписание группы {} для {} подгруппы", groupName, subGroup);
            context.json(sch);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.NOT_FOUND);
            context.json(Map.of("error", e.toString()));
            AkttAPI.LOGGER.error(e.toString());
        }
    }

    @OpenApi(
            summary = "Выдаёт расписание для указанной группы с обеими подгруппами",
            operationId = "getScheduleGroupBothSubGroups",
            path = "/api/v2/schedule/{group}",
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
            var sch = scheduleHandler.getScheduleGroup(groupName);
            AkttAPI.LOGGER.info("Запрос на расписание для группы {} для обеих подгрупп", groupName);
            context.json(sch);
        } catch (IllegalArgumentException e) {
            context.status(HttpStatus.NOT_FOUND);
            context.json(Map.of("error", e.toString()));
            AkttAPI.LOGGER.error(e.toString());
        }
    }

    @OpenApi(
            summary = "Выдаёт расписание всех групп, с обеими подгруппами",
            operationId = "getSchedule",
            path = "/api/v2/schedule/",
            methods = HttpMethod.GET,
            tags = {"Schedule"},
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = {@OpenApiContent(from = ScheduleGroup.class)})
            }
    )
    public static void getSchedule(ScheduleHandlerV2 scheduleHandler, Context context) {
        var sch = scheduleHandler.getSchedule();
        AkttAPI.LOGGER.info("Запрос на полное расписание");
        context.json(Map.of("schedule", sch));
    }

    @OpenApi(
            summary = "Выдаёт список названий всех групп",
            operationId = "getGroupsList",
            path = "/api/v2/schedule/groups",
            methods = HttpMethod.GET,
            tags = {"Schedule"},
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = {@OpenApiContent(from = ScheduleGroup.class)})
            }
    )
    public static void getGroupsList(ScheduleHandlerV2 scheduleHandler, Context context) {
        List<String> groups = scheduleHandler.getGroupsList();
        AkttAPI.LOGGER.info("Запрос на список групп");
        context.json(Map.of("groupsList", groups));
    }
}
