package org.kveex;

import io.javalin.Javalin;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import io.javalin.util.JavalinBindException;
import org.kveex.api.ArgsParser;
import org.kveex.api.GetHandler;
import org.kveex.api.PostHandler;
import org.kveex.schedule.ScheduleHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AkttAPI {
    public static final String ID = "AKTT_API";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    private static int port;
    private static int repeatDelay;

    static void main(String[] args) {
        setArgs(args);
        ScheduleHandler.initialize(repeatDelay);
        startApp();
    }

    private static void setArgs(String[] args) {
        ArgsParser argsParser = new ArgsParser();
        argsParser.parse(args);
        port = argsParser.getPort();
        repeatDelay = argsParser.getRepeatDelayMs();
    }

    private static void startApp() {
        Javalin app = Javalin.create(config -> {
                config.registerPlugin(new OpenApiPlugin(openapi -> {
                    openapi.withDefinitionConfiguration((_, builder) -> {
                        builder.info(info -> {
                            info.title("AKTT API");
                            info.description("API предоставляющая доступ к некоторым услугам AKTT");
                            info.version("1.2.1");
                            info.withLicense(license -> {
                                license.name("MIT");
                                license.identifier("MIT");
                            });
                        });
                    });
                }));

                config.registerPlugin(new SwaggerPlugin());

                config.routes.get("/", GetHandler::showTest);
                config.routes.get("/api/schedule/groups", GetHandler::getGroupsList);
                config.routes.get("/api/schedule/student/{group}", GetHandler::getScheduleGroupBothSubGroups);
                config.routes.get("/api/schedule/student/{group}/{subGroup}", GetHandler::getScheduleGroupDefinedSubGroup);
                config.routes.get("/api/schedule/teachers", GetHandler::getTeachersList);
                config.routes.get("/api/schedule/teacher/{teacher}", GetHandler::getTeacherSchedule);
                config.routes.get("/api/schedule/", GetHandler::getSchedule);
                config.routes.get("/api/schedule/date", GetHandler::getScheduleDate);
                config.routes.post("/api/certificate-upload", PostHandler::handleCertificate);
            }
        );

        try {
            app.start(port);
        } catch (JavalinBindException ignored) {
            LOGGER.error("Порт {} уже используется другим процессом!!! Убедитесь, что никакие другие процессы не используют этот порт и попробуйте снова", port);
            System.exit(2);
        }

        LOGGER.info("API запущено на порту: {}", port);
        LOGGER.info("Swagger UI: <server-ip>:{}/swagger", port);
    }
}