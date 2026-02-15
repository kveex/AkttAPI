package org.kveex.api;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import org.kveex.AkttAPI;
import org.kveex.certificate.CertificateHandler;
import org.kveex.certificate.CertificateItem;

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
}
