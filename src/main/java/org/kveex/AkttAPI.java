package org.kveex;

import io.javalin.Javalin;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import org.kveex.api.ArgsParser;
import org.kveex.api.GetHandler;
import org.kveex.api.PostHandler;
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
        var app = Javalin.create(config -> {
                config.showJavalinBanner = false;
                config.registerPlugin(new OpenApiPlugin(pluginConfig ->
                        pluginConfig.withDocumentationPath("/openapi")
                                .withDefinitionConfiguration(
                                        (version, definition) -> definition.withInfo(
                                                info -> {
                                    info.setTitle("AKTT Schedule API");
                                    info.setVersion("1.0");
                                    info.setDescription("API для получения расписания занятий");
                                }))
                        )
                );

                config.registerPlugin(new SwaggerPlugin(pluginConfig -> {
                    pluginConfig.setUiPath("/swagger-ui");
                    pluginConfig.setDocumentationPath("/openapi");
                    pluginConfig.setTitle("AKTT API by kveex");
                }));
            }
        )
        .get("/", GetHandler::showTest)
        .get("/api/schedule/groups", ctx -> GetHandler.getGroupsList(ctx, scheduleHandler))
        .get("/api/schedule/{group}", ctx -> GetHandler.getScheduleBothSubGroups(ctx, scheduleHandler))
        .get("/api/schedule/{group}/{subGroup}", ctx -> GetHandler.getScheduleDefinedSubGroup(ctx, scheduleHandler))
        .get("api/v2/schedule/groups", ctx -> GetHandler.getGroupsList(scheduleHandlerV2, ctx))
        .get("/api/v2/schedule/{group}", ctx -> GetHandler.getScheduleGroupBothSubGroups(scheduleHandlerV2, ctx))
        .get("/api/v2/schedule/{group}/{subGroup}", ctx -> GetHandler.getScheduleGroupDefinedSubGroup(scheduleHandlerV2, ctx))
        .get("/api/v2/schedule/", ctx -> GetHandler.getSchedule(scheduleHandlerV2, ctx))
        .post("/api/v2/certificate-upload", PostHandler::handleCertificate);

        app.start(port);

        LOGGER.info("API запущено на порту: {}", port);
        LOGGER.info("OpenAPI спецификация: http://localhost:{}/openapi", port);
        LOGGER.info("Swagger UI: http://localhost:{}/swagger-ui", port);
    }
}