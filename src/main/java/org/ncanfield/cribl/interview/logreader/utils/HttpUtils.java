package org.ncanfield.cribl.interview.logreader.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ncanfield.cribl.interview.logreader.models.LogReadResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HttpUtils {
    //Just using default settings for now
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();

    private static final ObjectMapper mapper = new ObjectMapper();

    private HttpUtils() {
    }

    /**
     * This creates a completable future for the given URL to a specified logs reader server
     *
     * @param url the url, with endpoint and any relevant query parameters, to use
     * @return a {@link CompletableFuture} that will eventually resolve to a {@link LogReadResponse}
     */
    public static CompletableFuture<LogReadResponse> buildFuture (String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .handle((response, ex) -> handleResponse(response, ex, url));
    }

    /**
     * Handler for an HTTP response from a CompletableFuture to a log endpoint and parses it to a {@link LogReadResponse}
     *
     * @param response the HTTP response object, if it exists
     * @param ex the exception, if it exists
     * @param url the URL this call was for
     * @return a {@link LogReadResponse} with either provided data or an error message
     */
    public static LogReadResponse handleResponse(HttpResponse<String> response, Throwable ex, String url) {
        if (ex == null) {
            try {
                return mapper.readValue(response.body(), LogReadResponse.class);
            } catch (JsonProcessingException e) {
                return new LogReadResponse(url, null, List.of("There was an error parsing the response from the server"));
            }
        } else {
            return new LogReadResponse(url, null, List.of("There was an error fetching the response from the server"));
        }
    }

    /**
     * Builds the URL for the aggregator to call for a given server
     *
     * @param server the base URL to use. Should not include the endpoint.
     * @param fileName the file name, if any, to query for
     * @param logLines the log line limite to use, if any
     * @param searchTerm the search term to use, if any
     * @return the URL the aggregator should hit
     */
    public static String makeUrl(String server, String fileName, Integer logLines, String searchTerm) {
        StringBuilder urlBuilder = new StringBuilder(server + "/logs?");
        if (fileName != null) {
            urlBuilder.append(String.format("fileName=%s&", fileName));
        }
        if (logLines != null) {
            urlBuilder.append(String.format("logLines=%s&", logLines));
        }
        if (searchTerm != null) {
            urlBuilder.append(String.format("searchTerm=%s", searchTerm));
        }
        return urlBuilder.toString();
    }
}
