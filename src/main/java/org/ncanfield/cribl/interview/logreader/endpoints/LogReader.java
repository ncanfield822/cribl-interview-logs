package org.ncanfield.cribl.interview.logreader.endpoints;

import org.ncanfield.cribl.interview.logreader.config.LogReaderConfig;
import org.ncanfield.cribl.interview.logreader.exception.LogReaderException;
import org.ncanfield.cribl.interview.logreader.handlers.LogReadRequestHandler;
import org.ncanfield.cribl.interview.logreader.models.LogFile;
import org.ncanfield.cribl.interview.logreader.models.LogReadResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RestController
public class LogReader {
    @Autowired
    private LogReaderConfig config;

    @GetMapping("/logs")
    public LogReadResponse readLogs(@RequestParam(required = false) String fileName,
                                    @RequestParam(required = false) Integer logLines,
                                    @RequestParam(required = false) String searchTerm) {
        Path filePath;
        List<String> errorMessages = new ArrayList<>();
        List<LogFile> logFiles = null;
        if (fileName != null) {
            filePath = Path.of(config.logDir() + "/" + fileName).normalize();
            if (!filePath.startsWith(config.logDir())) {
                errorMessages.add("Provided file path is invalid");
                //Don't even go further for this one.
                return new LogReadResponse(
                        config.friendlyName(),
                        null,
                        errorMessages);
            }
        } else {
            filePath = Path.of(config.logDir());
        }

        if (logLines != null && logLines < 1) {
            errorMessages.add("Requested log lines must be > 0");
        }

        File logFile = filePath.toFile();

        // We validate the dir on startup, this should only happen if an invalid path is passed in the API
        if (!logFile.exists()){
            errorMessages.add("The log files specified do not exist");
        } else if (!logFile.canRead()) {
            errorMessages.add("The log files specified cannot be read");
        }

        if (errorMessages.isEmpty()) {
            logFiles = LogReadRequestHandler.readLogs(
                    logFile,
                    logLines != null ? logLines : config.defaultLineLimit(),
                    searchTerm,
                    config.logDir().length());
        }

        return new LogReadResponse(
                config.friendlyName(),
                logFiles,
                errorMessages);
    }
}
