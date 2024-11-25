package org.ncanfield.cribl.interview.logreader.endpoints;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ncanfield.cribl.interview.logreader.config.LogReaderConfig;
import org.ncanfield.cribl.interview.logreader.models.LogAggregateResponse;
import org.ncanfield.cribl.interview.logreader.models.LogReadResponse;
import org.ncanfield.cribl.interview.logreader.utils.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
public class LogAggregator {
    @Autowired
    private LogReaderConfig config;

    @Autowired
    private LogReader logReader;

    @GetMapping("/aggregate")
    public LogAggregateResponse aggregateLogs(@RequestParam(required = false) String fileName,
                                              @RequestParam(required = false) Integer logLines,
                                              @RequestParam(required = false) String searchTerm) {
        List<String> errors = new ArrayList<>();
        // Other parameters may be valid on individual machines, this is just stopping definite invalid ones
        if (logLines != null &&logLines < 1) {
            errors.add("Requested log lines must be > 0");
            return new LogAggregateResponse(null, errors);
        }

        List<CompletableFuture<LogReadResponse>> futures = new ArrayList<>();
        for (String server : config.logServers()) {
            if ("self".equalsIgnoreCase(server)) {
                futures.add(CompletableFuture.supplyAsync(() -> logReader.readLogs(fileName, logLines, searchTerm)));
            } else {
                final String fullUrl = HttpUtils.makeUrl(server, fileName, logLines, searchTerm);
                futures.add(HttpUtils.buildFuture(fullUrl));
            }
        }

        // Wait for them to all complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return new LogAggregateResponse(futures.stream().map(CompletableFuture::join).toList(), errors);
    }

}
