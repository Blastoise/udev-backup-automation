package io.ashutosh;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class Telegram {

    static final Logger logger = LoggerFactory.getLogger(Telegram.class);

    final String urlString;
    final String telegramReceiverID;
    final ObjectMapper objectMapper;

    public Telegram(String telegramReceiverID, String token) {
        this.urlString = String.format("https://api.telegram.org/bot%s", token);
        this.telegramReceiverID = telegramReceiverID;
        this.objectMapper = new ObjectMapper();
    }

    private boolean checkForSuccessfulRequest(String response) {
        try {
            return this.objectMapper.readTree(response).get("ok").asBoolean();
        } catch (JsonProcessingException e) {
            logger.error("Unable to determine whether request was successful. Marking it as a failed request", e);
            return false;
        }
    }

    public void sendMessageUpdate(String message) {
        try {
            String methodName = "/sendMessage";

            Map<String, String> jsonBody = new HashMap<>();
            jsonBody.put("chat_id", this.telegramReceiverID);
            jsonBody.put("parse_mode", "HTML");
            jsonBody.put("protect_content", Boolean.toString(true));
            jsonBody.put("text", message);

            String requestBody = this.objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(jsonBody);

            HttpRequest request = HttpRequest.newBuilder(URI.create(this.urlString + methodName))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = HttpClient.newBuilder()
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (checkForSuccessfulRequest(response.body()))
                logger.info("Message update related to backup has been sent");
            else
                logger.error("Unsuccessful Telegram Request. Response received: {}", response.body());
        } catch (IOException | InterruptedException e) {
            logger.error("Something went wrong while sending message update! Unable to send HTTP POST request.", e);
        }
    }

    public void sendVideoUpdate(String outputDir, String videoFileName) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String methodName = "/sendVideo";

            InputStream inputStream = new FileInputStream(Paths.get(outputDir).toAbsolutePath().resolve(videoFileName).toString());
            HttpEntity entity = MultipartEntityBuilder.create()
                    .addTextBody("chat_id", this.telegramReceiverID)
                    .addTextBody("caption", "Video Recording of pCloud backup: <b>" +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("E, d LLL YYYY HH:mm:ss")) + "</b>")
                    .addTextBody("parse_mode", "HTML")
                    .addTextBody("show_caption_above_media", Boolean.toString(true))
                    .addTextBody("protect_content", Boolean.toString(true))
                    .addBinaryBody("video", inputStream, ContentType.create("video/mp4"), videoFileName)
                    .build();

            HttpPost httpPost = new HttpPost(this.urlString + methodName);
            httpPost.setEntity(entity);

            String response = client.execute(httpPost, new BasicHttpClientResponseHandler());
            if (checkForSuccessfulRequest(response))
                logger.info("Video recording of pCloud backup has been sent");
            else
                logger.error("Unsuccessful Telegram Request while sending video of pCloud backup! Response Received: {}", response);
        } catch (IOException e) {
            logger.error("Something went wrong while sending video update! Unable to send HTTP POST request.", e);
        }
    }
}
