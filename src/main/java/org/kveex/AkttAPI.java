package org.kveex;

import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.OpenApiPluginConfiguration;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import io.javalin.util.JavalinBindException;
import org.kveex.api.GetHandler;
import org.kveex.api.PostHandler;
import org.kveex.database.DatabaseController;
import org.kveex.schedule.ScheduleSaver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AkttAPI {
    public static final String ID = "AKTT_API";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    private static final int minimumRepeatDelayMs = 30 * 60 * 1000;
    private static String databaseUrl;
    private static int port;
    private static int repeatDelay;

    static void main() {
        setArgs();
        DatabaseController.initialize(databaseUrl);
        ScheduleSaver _ = new ScheduleSaver(repeatDelay);
        startApp();
    }

    private static void setArgs() {
        Dotenv dotenv = Dotenv.load();

        if (dotenv.get("DATABASE_URL") == null) throw new IllegalStateException("Не указана ссылка подключения к базе данных!");

        databaseUrl = dotenv.get("DATABASE_URL");
        port = dotenv.get("PORT") == null ? 16311 : Integer.parseInt(dotenv.get("PORT"));
        repeatDelay = dotenv.get("REPEAT_DELAY_MINUTES") == null ? minimumRepeatDelayMs : Integer.parseInt(dotenv.get("REPEAT_DELAY_MINUTES")) * 60 * 1000;
        if (repeatDelay < minimumRepeatDelayMs) {
            repeatDelay = minimumRepeatDelayMs;
            LOGGER.warn("Промежуток между проверками расписания не может быть меньше получаса! Установлен стандартный промежуток (30 минут)");
        }
    }

    private static void startApp() {
        Javalin app = Javalin.create(config -> {
                config.registerPlugin(new OpenApiPlugin(AkttAPI::configureOpenApi));
                config.registerPlugin(new SwaggerPlugin());

                config.routes.get("/", GetHandler::showTest);
                config.routes.get("/api/schedule/student/{groupName}", GetHandler::studentSchedule);
                config.routes.get("/api/schedule/teacher/{teacherName}", GetHandler::teacherSchedule);
                config.routes.get("/api/schedule/groups", GetHandler::groupsList);
                config.routes.get("/api/schedule/teachers", GetHandler::teachersList);
//                config.routes.get("/api/schedule/forceNotify", GetHandler::forceNotify);
                config.routes.post("/api/pdf-upload", PostHandler::handlePdfUpload);
                config.routes.post("/api/webhook", PostHandler::addWebHook);
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
