package com.alura.literalura.service;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ConsumoAPI {
    private static final int MAX_RETRIES = 10;
    private static final long RETRY_DELAY_MS = 1000;

    public String obtenerDatos(String url) {
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String json = response.body();
                return json;
            } catch (ConnectException e) {
                System.err.println("Error: No se pudo conectar a internet. Intento " + (retries + 1));
                retries++;
                if (retries < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return null;
            }
        }
        System.err.println("Error: No se pudo conectar después de " + MAX_RETRIES + " intentos.");
        return null;
    }
}
