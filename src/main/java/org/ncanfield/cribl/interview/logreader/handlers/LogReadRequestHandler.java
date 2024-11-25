package org.ncanfield.cribl.interview.logreader.handlers;

import org.ncanfield.cribl.interview.logreader.exception.LogReaderException;
import org.ncanfield.cribl.interview.logreader.models.LogFile;
import org.ncanfield.cribl.interview.logreader.utils.ReverseFileReader;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class LogReadRequestHandler {
    private static Logger LOGGER = Logger.getLogger("LogReadRequestHandler");

    /**
     * Searches logFile for logs and parses any found there. If logFile is a directory, it will recursively search it.
     *
     * @param logFile the file/directory to search
     * @param maxLines the max lines per file to return, or -1 for unlimited
     * @param searchTerm the search term to use, or null to return any lines
     * @return A list of {@link LogFile} for each file checked
     * @throws LogReaderException if the specified file does not exist
     */
    public static List<LogFile> readLogs(File logFile, Integer maxLines, String searchTerm, Integer basePathSize) throws LogReaderException {
        List<LogFile> logs = new ArrayList<>();

        if (!logFile.exists()) {
            throw new LogReaderException("The log files specified do not exist");
        }

        if (logFile.isDirectory()) {
            logs.addAll(readDirectory(logFile, maxLines, searchTerm, basePathSize));
        } else if (logFile.isFile()) {
            logs.add(readFile(logFile.toPath(), maxLines, searchTerm, basePathSize));
        }

        return logs;
    }

    /**
     * Recursively searches logDir and parses logs found there
     *
     * @param logDir the directory to search
     * @param maxLines the max lines per file to return, or -1 for unlimited
     * @param searchTerm the search term to use, or null to return any ines
     * @return A list of {@link LogFile} for each file checked
     */
    private static List<LogFile> readDirectory(File logDir, Integer maxLines, String searchTerm, Integer basePathSize) {
        List<LogFile> logs = new ArrayList<>();
        for (File logFile : logDir.listFiles()) {
            if (logFile.isDirectory()) {
                logs.addAll(readDirectory(logFile, maxLines, searchTerm, basePathSize));
            } else if (logFile.isFile()) {
                logs.add(readFile(logFile.toPath(), maxLines, searchTerm, basePathSize));
            }
        }
        return logs;
    }

    /**
     * Reads the file specified by filePath until it's hit the end of the file or maxLines, slecting only lines containing
     * searchTerm if provided
     *
     * @param filePath the path of the file to parse
     * @param maxLines the number of lines to return maximum, or -1 for unlimited
     * @param searchTerm the term to search for, or null to return all lines
     * @return a {@link LogFile} object containing the lines found or an error message
     */
    private static LogFile readFile(Path filePath, Integer maxLines, String searchTerm, Integer basePathSize) {
        LogFile logFile;
        ReverseFileReader reverseFileReader = null;
        String fileName = filePath.getFileName().toString();
        try {
            List<String> logLines = new ArrayList<>();
            boolean limitLines = maxLines > 0;
            boolean doSearch = searchTerm != null;
            reverseFileReader = new ReverseFileReader(StandardCharsets.UTF_8, filePath, 4096);
            // Keep parsing the file while it has more data and either we're not limiting lines or have kept below it
            while (reverseFileReader.hasMoreData() &&
                    (!limitLines || logLines.size() < maxLines)) {
                String logLine = reverseFileReader.readLine();
                // We want this line if it exists and we're either not searching or it contains the search term
                if (logLine != null && (!doSearch || logLine.contains(searchTerm))) {
                    logLines.add(logLine);
                }
            }

            logFile = new LogFile(fileName, filePath.toString().substring(basePathSize + 1), logLines, null);
        } catch (Exception e) {
            LOGGER.info("Exception reading file: " + e.getMessage());
            logFile = new LogFile(fileName, filePath.toString().substring(basePathSize + 1), null, "Encountered an exception reading the file");
        } finally {
            if (reverseFileReader != null) {
                try {
                    reverseFileReader.close();
                } catch (Exception e) {
                    LOGGER.info("Exception closing reverse file reader: " + e.getMessage());
                }
            }
        }
        return logFile;
    }
}
