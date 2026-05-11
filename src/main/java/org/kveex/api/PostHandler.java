package org.kveex.api;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import org.kveex.AkttAPI;
import org.kveex.schedule.ScheduleSaver;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.Map;

public class PostHandler {
    @OpenApi(
            summary = "Принимает PDF файл с расписанием в нём",
            operationId = "handlePdfUpload",
            path = "/api/pdf-upload",
            requestBody = @OpenApiRequestBody(
                    required = true,
                    description = "PDF-файл",
                    content = {
                            @OpenApiContent(
                                    mimeType = "multipart/form-data",
                                    properties = {
                                            @OpenApiContentProperty(
                                                    name = "file",
                                                    type = "string",
                                                    format = "binary"
                                            )
                                    }
                            )
                    }
            ),
            methods = HttpMethod.POST,
            tags = "Schedule",
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            description = "Всё прошло успешно, расписание принято на сервер"
                    ),
                    @OpenApiResponse(
                            status = "400",
                            description = "Что-то не так с файлом или данное расписание уже есть"
                    )
            }
    )
    public static void handlePdfUpload(Context context) {
        byte[] bytes;

        try {
            var uploadedFile = context.uploadedFile("file");
            if (uploadedFile != null) {
                try (var inputStream = uploadedFile.content()) {
                    bytes = inputStream.readAllBytes();
                }
            } else {
                bytes = context.bodyAsBytes();
            }
        } catch (IOException e) {
            AkttAPI.LOGGER.error("Не удалось прочитать загруженный PDF: {}", e.toString());
            context.status(HttpStatus.BAD_REQUEST).result("Не удалось прочитать PDF файл");
            return;
        }

        if (bytes.length == 0) {
            context.status(HttpStatus.BAD_REQUEST).result("PDF файл пустой");
            return;
        }
        try {
            ScheduleSaver.trySavePDF(bytes);
        } catch (SQLException e) {
            context.json(Map.of("error", e.getMessage()));
            context.status(HttpStatus.BAD_REQUEST);
            return;
        }
        context.status(HttpStatus.OK);
    }

    @OpenApi(
            summary = "Принимает вебхук от внешних программ",
            operationId = "addWebhook",
            path = "/api/webhook",
            requestBody = @OpenApiRequestBody(
                    required = true,
                    description = "Вебхук строкой",
                    content = {
                            @OpenApiContent(
                                    mimeType = "text/plain"
                            )
                    }
            ),
            methods = HttpMethod.POST,
            tags = "Schedule",
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            description = "Вебхук принят и добавлен"
                    ),
                    @OpenApiResponse(
                            status = "400",
                            description = "Вебхук отправлен в неправильном формате"
                    )
            }
    )
    public static void addWebHook(Context context) {
        URI uri;

        try {
            uri = URI.create(context.body().trim());
        } catch (IllegalArgumentException e) {
            AkttAPI.LOGGER.error("Вебхук не получилось преобразовать в URI: {}", e.toString());
            context.status(HttpStatus.BAD_REQUEST);
            return;
        }

        String scheme = uri.getScheme();
        if (!uri.isAbsolute() || scheme == null || !(scheme.equals("http") || scheme.equals("https"))) {
            context.status(HttpStatus.BAD_REQUEST);
            return;
        }

        boolean added = WebhookHandler.webhooks.add(uri);
        String message = added ? "Добавлен новый вебхук адрес [%s]".formatted(uri) : "Данный вебхук [%s] уже добавлен".formatted(uri);
        AkttAPI.LOGGER.info(message);
        context.status(HttpStatus.OK);
    }
}
