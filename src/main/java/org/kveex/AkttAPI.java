package org.kveex;

import io.javalin.Javalin;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import io.javalin.util.JavalinBindException;
import org.kveex.api.ArgsParser;
import org.kveex.api.GetHandler;
import org.kveex.api.PostHandler;
import org.kveex.schedule.ScheduleHandlerV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AkttAPI {
    public static final String ID = "AKTT_API";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    private static int port;
    private static int repeatDelay;
    private static ScheduleHandlerV2 scheduleHandler;

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
        try {
            scheduleHandler = new ScheduleHandlerV2(repeatDelay);
        } catch (IOException e) {
            throw new RuntimeException("ScheduleHandler broken!\n" + e);
        }
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
                                    info.setVersion("1.1.1");
                                    info.setDescription("API для взаимодействия");
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
        .get("/api/schedule/groups", ctx -> GetHandler.getGroupsList(scheduleHandler, ctx))
        .get("/api/schedule/student/{group}", ctx -> GetHandler.getScheduleGroupBothSubGroups(scheduleHandler, ctx))
        .get("/api/schedule/student/{group}/{subGroup}", ctx -> GetHandler.getScheduleGroupDefinedSubGroup(scheduleHandler, ctx))
        .get("/api/schedule/teachers", ctx -> GetHandler.getTeachersList(scheduleHandler, ctx))
        .get("/api/schedule/teacher/{teacher}", ctx -> GetHandler.getTeacherSchedule(scheduleHandler, ctx))
        .get("/api/schedule/", ctx -> GetHandler.getSchedule(scheduleHandler, ctx))
        .get("/api/schedule/date", ctx -> GetHandler.getScheduleDate(scheduleHandler, ctx))
        .post("/api/certificate-upload", PostHandler::handleCertificate);

        try {
            app.start(port);
        } catch (JavalinBindException ignored) {
            LOGGER.error("Порт {} уже используется другим процессом!!! Убедитесь, что никакие другие процессы не используют этот порт и попробуйте снова", port);
            System.exit(1);
        }

        LOGGER.info("API запущено на порту: {}", port);
        LOGGER.info("OpenAPI спецификация: http://localhost:{}/openapi", port);
        LOGGER.info("Swagger UI: http://localhost:{}/swagger-ui", port);
    }
}