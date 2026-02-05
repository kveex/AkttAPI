package org.kveex;

import io.javalin.Javalin;
import org.kveex.api.ArgsParser;
import org.kveex.api.GetHandler;
import org.kveex.schedule.ScheduleHandler;
import org.kveex.schedule.ScheduleHandlerV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class AkttAPI {
    public static final String ID = "AKTT_API";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    private static int port;
    private static int repeatDelay;
    private static ScheduleHandler scheduleHandler;
    private static ScheduleHandlerV2 scheduleHandlerV2;

    public static void main(String[] args) {
        setArgs(args);
        makeScheduleHandler();
        startApp();
    }

    private static void setArgs(String[] args) {
        ArgsParser argsParser = new ArgsParser();
        argsParser.parse(args);
        port = argsParser.getPort();
        repeatDelay = argsParser.getRepeatDelayMs();
    }

    private static void makeScheduleHandler() {
        Timer timer = new Timer();
        try {
            scheduleHandler = new ScheduleHandler();
            scheduleHandlerV2 = new ScheduleHandlerV2();
        } catch (IOException e) {
            throw new RuntimeException("ScheduleHandler broken!\n" + e);
        }
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    scheduleHandler.updateDocument();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, repeatDelay);
    }

    private static void startApp() {
        var app = Javalin.create(config -> config.showJavalinBanner = false)
                .get("/", GetHandler::showTest)
                .get("/api/schedule/groups", ctx -> GetHandler.getGroupsList(ctx, scheduleHandler))
                .get("/api/schedule/{group}", ctx -> GetHandler.getScheduleBothSubGroups(ctx, scheduleHandler))
                .get("/api/schedule/{group}/{subGroup}", ctx -> GetHandler.getScheduleDefinedSubGroup(ctx, scheduleHandler))
                .get("/api/v2/schedule/{group}", ctx -> GetHandler.getScheduleGroupBothSubGroups(scheduleHandlerV2, ctx))
                .get("/api/v2/schedule/{group}/{subGroup}", ctx -> GetHandler.getScheduleGroupDefinedSubGroup(scheduleHandlerV2, ctx))
                .get("/api/v2/schedule/", ctx -> GetHandler.getSchedule(scheduleHandlerV2, ctx));

        app.start(port);
    }
}