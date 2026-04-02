package org.kveex;

import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.OpenApiPluginConfiguration;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import io.javalin.util.JavalinBindException;
import io.javalin.websocket.WsContext;
import org.kveex.api.ArgsParser;
import org.kveex.api.GetHandler;
import org.kveex.api.PostHandler;
import org.kveex.database.DatabaseController;
import org.kveex.schedule.ScheduleSaver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AkttAPI {
    public static final String ID = "AKTT_API";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    private static int port;
    private static int repeatDelay;
    private static final Set<WsContext> sessions = ConcurrentHashMap.newKeySet();

    static void main(String[] args) {
        setArgs(args);
        Dotenv dotenv = Dotenv.load();
        DatabaseController.initialize(dotenv.get("DATABASE_URL"));
        ScheduleSaver _ = new ScheduleSaver(repeatDelay);
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
                config.registerPlugin(new OpenApiPlugin(AkttAPI::configureOpenApi));
                config.registerPlugin(new SwaggerPlugin());

                config.routes.get("/", GetHandler::showTest);
                config.routes.get("/api/schedule/student/{date}/{groupName}", GetHandler::studentSchedule);
                config.routes.get("/api/schedule/teacher/{date}/{teacherName}", GetHandler::teacherSchedule);
                config.routes.get("/api/schedule/groups/{date}", GetHandler::groupsList);
                config.routes.get("/api/schedule/teachers/{date}", GetHandler::teachersList);
                config.routes.post("/api/certificate-upload", PostHandler::handleCertificate);
                config.routes.post("/api/pdf-upload", PostHandler::handlePdfUpload);

                config.routes.ws("/api/schedule-updates", ws -> {
                    ws.onConnect(ctx -> {
                        sessions.add(ctx);
                        LOGGER.info("Client connected");
                    });
                    ws.onClose(ctx -> {
                        sessions.remove(ctx);
                        LOGGER.info("Client disconnected");
                    });
                });
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

    public static void notifyAboutUpdate() {
        sessions.forEach(session -> session.send("updated"));
    }

    private static void configureOpenApi(OpenApiPluginConfiguration openapi) {
        openapi.withDefinitionConfiguration((_, builder) -> builder.info(info -> {
            info.title("AKTT API");
            info.description("API предоставляющая доступ к некоторым услугам AKTT");
            info.version("2.0");
            info.withLicense(license -> {
                license.name("MIT");
                license.identifier("MIT");
            });
        }));
    }
}