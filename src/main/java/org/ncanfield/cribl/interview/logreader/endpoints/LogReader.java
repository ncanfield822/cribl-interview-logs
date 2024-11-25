package org.ncanfield.cribl.interview.logreader.endpoints;

import org.ncanfield.cribl.interview.logreader.config.LogReaderConfig;
import org.ncanfield.cribl.interview.logreader.exception.LogReaderException;
import org.ncanfield.cribl.interview.logreader.handlers.LogReadRequestHandler;
import org.ncanfield.cribl.interview.logreader.models.LogReadResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@RestController
public class LogReader {
    @Autowired
    private LogReaderConfig config;

    @GetMapping("/logs")
    public LogReadResponse readLogs(@RequestParam(required = false) String fileName,
                                    @RequestParam(required = false) Integer logLines,
                                    @RequestParam(required = false) String searchTerm) throws LogReaderException {
        Path filePath;
        if (fileName != null) {
            filePath = Path.of(config.logDir() + "/" + fileName).normalize();
            if (!filePath.startsWith(config.logDir())) {
                throw new LogReaderException("Provided file path is invalid");
            }
        } else {
            filePath = Path.of(config.logDir());
        }

        if (logLines != null && logLines < 1) {
            throw new LogReaderException("Requested log lines must be > 0");
        }

        return new LogReadResponse(
                config.friendlyName(),
                LogReadRequestHandler.readLogs(
                        filePath,
                        logLines != null ? logLines : config.defaultLineLimit(),
                        searchTerm));
    }
}
