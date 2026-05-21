package com.empresa.pos.sincronizacion;

import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SyncWorker implements Runnable {
    private static final String SERVER_URL = "https://api.empresa-pos.com/v1/sync";
    private final OkHttpClient client;
    private boolean running = true;
    private int backoffSeconds = 5;

    public SyncWorker() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public void run() {
        System.out.println("SyncWorker started...");
        while (running) {
            try {
                if (checkConnectivity()) {
                    processQueue();
                    backoffSeconds = 5; // Reset backoff on success
                } else {
                    System.out.println("Offline - Waiting to retry...");
                    increaseBackoff();
                }
                
                Thread.sleep(backoffSeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    private boolean checkConnectivity() {
        // Simulación de chequeo de red
        return true; 
    }

    private void processQueue() {
        // Aquí se consultaría la tabla SYNC_QUEUE de pos-data
        // Por ahora es un esqueleto de la lógica
        System.out.println("Checking SYNC_QUEUE for pending records...");
    }

    private void increaseBackoff() {
        backoffSeconds = Math.min(backoffSeconds * 2, 300); // Máximo 5 minutos
    }

    public void stop() {
        this.running = false;
    }
}
