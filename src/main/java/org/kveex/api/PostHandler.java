package org.kveex.api;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import org.kveex.AkttAPI;
import org.kveex.certificate.CertificateHandler;
import org.kveex.certificate.CertificateItem;
import org.kveex.schedule.ScheduleSaver;
import org.kveex.schedule.parser.ScheduleInfo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class PostHandler {
    @OpenApi(
            summary = "Принимает информацию о заявке на справку",
            operationId = "handleCertificate",
            path = "/api/certificate-upload",
            requestBody = @OpenApiRequestBody(
                    content = @OpenApiContent(
                            from = CertificateItem.class,
                            type = "application/json",
                            example = """
                                    {
                                      "groupName": "23-14ИС",
                                      "course": "FIRST",
                                      "lastName": "Иванов",
                                      "firstName": "Иван",
                                      "middleName": "Иванович",
                                      "requestPlace": "MILITARY_COMMISSARIAT",
                                      "otherRequestPlaceText": null,
                                      "scholarshipInfo": false,
                                      "additionalInfo": "Нужна справка для подачи документов"
                                    }"""
                    )
            ),
            methods = HttpMethod.POST,
            tags = {"Certificate"},
            responses = {
                    @OpenApiResponse(
                            status = "200",
                            content = @OpenApiContent(from = String.class)
                    )
            }
    )
    public static void handleCertificate(Context context) {
        CertificateItem certificateItem = context.bodyAsClass(CertificateItem.class);
        CertificateHandler.sendCertificate(certificateItem);
        context.status(HttpStatus.OK);
        AkttAPI.LOGGER.debug(
                "Заявление на справку отправлено от ({}, {}, {}) {} {} курс",
                certificateItem.lastName(),
                certificateItem.firstName(),
                certificateItem.middleName(),
                certificateItem.groupName(),
                certificateItem.course().toString()
        );
    }

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
                            status = "400",
                            content = @OpenApiContent(from = ScheduleInfo.class)
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
        }
        context.status(HttpStatus.OK);
    }
}
