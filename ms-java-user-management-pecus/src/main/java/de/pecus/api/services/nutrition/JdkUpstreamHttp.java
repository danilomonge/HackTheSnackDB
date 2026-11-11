package de.pecus.api.services.nutrition;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.springframework.stereotype.Component;

/** {@link UpstreamHttp} on top of the JDK HTTP client (15 s per request, like the former proxy). */
@Component
public class JdkUpstreamHttp implements UpstreamHttp {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public Result get(String url) throws IOException {
        return send(HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).GET().build());
    }

    @Override
    public Result postJson(String url, Map<String, String> headers, String json) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        headers.forEach(builder::header);
        return send(builder.build());
    }

    private Result send(HttpRequest request) throws IOException {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new Result(response.statusCode(), response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        }
    }
}
