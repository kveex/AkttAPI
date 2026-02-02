package org.kveex.api;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.kveex.AkttAPI;
import org.kveex.schedule.ScheduleGroup;
import org.kveex.schedule.ScheduleHandler;

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

    public static void gerGroupsList(Context context, ScheduleHandler scheduleHandler) {
        List<String> groups = scheduleHandler.getAllGroups();
        AkttAPI.LOGGER.info("Запрос на список групп");
        context.json(Map.of("groupsList", groups));
    }
}
