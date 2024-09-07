package io.ashutosh;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class Telegram {
    final String urlString;
    final String telegramReceiverID;

    public Telegram(String telegramReceiverID, String token) {
        this.urlString = String.format("https://api.telegram.org/bot%s", token);
        this.telegramReceiverID = telegramReceiverID;
    }

    public void sendNotification(List<String> copiedFiles) throws IOException, InterruptedException {
        String methodName = "/sendMessage";

        Map<String, String> jsonBody = new HashMap<>();
        jsonBody.put("chat_id", this.telegramReceiverID);
        jsonBody.put("parse_mode", "HTML");

        StringBuilder text = new StringBuilder();
        text.append("<b>Backup Successful</b> ✅\n\n");
        if (!copiedFiles.isEmpty()) {
            text.append("<b><i>List of files that were backed up in Hard Disk:</i></b>\n");
            IntStream.range(0, copiedFiles.size()).forEach(idx -> text.append(idx + 1).append(". ").append(copiedFiles.get(idx)).append("\n"));
        } else {
            text.append("<b><i>All files are already up to date!</i></b>");
        }
        jsonBody.put("text", text.toString());

        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(jsonBody);

        HttpRequest request = HttpRequest.newBuilder(URI.create(this.urlString + methodName))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = HttpClient.newBuilder()
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());

    }
}
