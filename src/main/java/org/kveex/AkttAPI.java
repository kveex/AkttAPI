package org.kveex;

import io.javalin.Javalin;
import org.kveex.api.ArgsParser;
import org.kveex.api.GetHandler;
import org.kveex.schedule.ScheduleHandler;
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
                .get("/api/schedule/groups", ctx -> GetHandler.gerGroupsList(ctx, scheduleHandler))
                .get("/api/schedule/{group}", ctx -> GetHandler.getScheduleBothSubGroups(ctx, scheduleHandler))
                .get("/api/schedule/{group}/{subGroup}", ctx -> GetHandler.getScheduleDefinedSubGroup(ctx, scheduleHandler));

        app.start(port);
    }
}