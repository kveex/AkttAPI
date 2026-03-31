package org.kveex.api;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import org.kveex.AkttAPI;
import org.kveex.database.DatabaseController;

import java.util.List;
import java.util.Map;

public class GetHandler {
    public static void showTest(Context context) {
        AkttAPI.LOGGER.info("Сделан запрос на главную страницу");
        context.json(Map.of("message", "Привет от АКТТ REST API"));
    }

    public static void groupsList(Context context) {
        var databaseController = DatabaseController.getInstance();

    }
}
