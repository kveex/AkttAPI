package org.kveex.api;

import org.kveex.AkttAPI;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;

public class WebhookHandler {
    public static final Set<URI> webhooks = new HashSet<>();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static void notifyAboutUpdate() {
        Set<URI> forRemoval = new HashSet<>();
        webhooks.forEach(webhook -> {
            HttpRequest request;
            try {
                request = HttpRequest.newBuilder()
                        .uri(webhook)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
            } catch (IllegalArgumentException _) {
                AkttAPI.LOGGER.error("Адрес для вебхука [{}] указан неверно! Удалён.", webhook);
                forRemoval.add(webhook);
                return;
            }

            try {
                HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (IOException | InterruptedException e) {
                AkttAPI.LOGGER.error("Не удалось отправить уведомление об обновлении расписания на [{}]. Вебхук удалён", webhook);
                forRemoval.add(webhook);
            }
        });
        webhooks.removeAll(forRemoval);
    }
}
