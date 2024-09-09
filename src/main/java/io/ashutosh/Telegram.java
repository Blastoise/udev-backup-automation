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
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class Telegram {
    final String urlString;
    final String telegramReceiverID;
    final ObjectMapper objectMapper;

    public Telegram(String telegramReceiverID, String token) {
        this.urlString = String.format("https://api.telegram.org/bot%s", token);
        this.telegramReceiverID = telegramReceiverID;
        this.objectMapper = new ObjectMapper();
    }

    private boolean checkForSuccessfulRequest(String response) throws JsonProcessingException {
        return this.objectMapper.readTree(response).get("ok").asBoolean();
    }

    public void sendMessageUpdate(List<String> copiedFiles) throws IOException, InterruptedException {
        String methodName = "/sendMessage";

        Map<String, String> jsonBody = new HashMap<>();
        jsonBody.put("chat_id", this.telegramReceiverID);
        jsonBody.put("parse_mode", "HTML");
        jsonBody.put("protect_content", Boolean.toString(true));

        StringBuilder text = new StringBuilder();
        text.append("<b>Backup Successful</b> ✅\n\n");
        if (!copiedFiles.isEmpty()) {
            text.append("<b><i>List of files that were backed up in Hard Disk:</i></b>\n");
            IntStream.range(0, copiedFiles.size()).forEach(idx -> text.append(idx + 1).append(". ").append(copiedFiles.get(idx)).append("\n"));
        } else {
            text.append("<b><i>All files are already up to date!</i></b>");
        }
        jsonBody.put("text", text.toString());

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

        if(checkForSuccessfulRequest(response.body()))
            System.out.println("Message containing the list of files that were backed up has been sent");
        else
            System.out.println("Unable to send message update");
    }

    public void sendVideoUpdate(String videoFileName) throws IOException {
        String methodName = "/sendVideo";

        InputStream inputStream = new FileInputStream(Paths.get("").toAbsolutePath().resolve(videoFileName).toString());
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

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String response = client.execute(httpPost, new BasicHttpClientResponseHandler());
            if(checkForSuccessfulRequest(response))
                System.out.println("Video recording of pCloud backup has been sent");
            else
                System.out.println("Unable to send video recording of pCloud backup");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
