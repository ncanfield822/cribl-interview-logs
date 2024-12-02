package org.ncanfield.cribl.interview.logreader.handlers;

import org.ncanfield.cribl.interview.logreader.models.LogFile;
import org.ncanfield.cribl.interview.logreader.utils.ReverseFileReader;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class LogReadRequestHandler {
    private static final Logger LOGGER = Logger.getLogger("LogReadRequestHandler");

    /**
     * Searches logFile for logs and parses any found there. If logFile is a directory, it will recursively search it.
     *
     * @param logFile the file/directory to search
     * @param maxLines the max lines per file to return, or -1 for unlimited
     * @param searchTerm the search term to use, or null to return any lines
     * @return A list of {@link LogFile} for each file checked
     */
    public static List<LogFile> readLogs(File logFile, Integer maxLines, String searchTerm, Integer basePathSize) {
        List<LogFile> logs = new ArrayList<>();

        if (!logFile.exists()) {
            logs.add(new LogFile(logFile.getName(), logFile.getAbsolutePath().substring(basePathSize + 1), null, "The specified file does not exist"));
        } else if (logFile.isDirectory()) {
            if (logFile.listFiles() == null) {
                // If an unreadable dir was specifically requested, return an error
                logs.add(new LogFile(logFile.getName(), logFile.getAbsolutePath().substring(basePathSize + 1), null, "This directory could not be accessed"));
            } else {
                logs.addAll(readDirectory(logFile, maxLines, searchTerm, basePathSize));
            }
        } else if (logFile.isFile()) {
            if (isReadableFile(logFile.toPath())) {
                logs.add(readFile(logFile.toPath(), maxLines, searchTerm, basePathSize));
            } else {
                // This should only happen if a user specifies a zip file or the like.
                logs.add(new LogFile(logFile.getName(), logFile.getAbsolutePath().substring(basePathSize + 1), null, "The specified file is not a text file"));
            }
        }

        return logs;
    }

    /**
     * Recursively searches logDir and parses logs found there, or an empty list if it cannot access the directory.
     * <p>
     * If the directory cannot be accessed the list will be empty.
     *
     * @param logDir the directory to search
     * @param maxLines the max lines per file to return, or -1 for unlimited
     * @param searchTerm the search term to use, or null to return any ines
     * @return A list of {@link LogFile} for each file checked, or an empty list if the directory cannot be accessed
     */
    private static List<LogFile> readDirectory(File logDir, Integer maxLines, String searchTerm, Integer basePathSize) {
        List<LogFile> logs = new ArrayList<>();
        File[] logFiles = logDir.listFiles();

        if (logFiles == null) {
            LOGGER.info("Cannot access directory" + logDir.getAbsolutePath());
            // We can't read anything here anyways
            return logs;
        }

        for (File logFile : logFiles) {
            if (logFile.isDirectory()) {
                logs.addAll(readDirectory(logFile, maxLines, searchTerm, basePathSize));
            } else if (logFile.isFile() && isReadableFile(logFile.toPath())) {
                //This gets skipped if the file isn't a log/text file
                logs.add(readFile(logFile.toPath(), maxLines, searchTerm, basePathSize));
            }
        }
        return logs;
    }

    /**
     * Reads the file specified by filePath until it's hit the end of the file or maxLines, selecting only lines containing
     * searchTerm if provided. Returns a logfile with an error message if an exception is encountered reading the file,
     * or null if it is not a .log, .txt, or other file type of 'text/plain'
     *
     * @param filePath the path of the file to parse
     * @param maxLines the number of lines to return maximum, or -1 for unlimited
     * @param searchTerm the term to search for, or null to return all lines
     * @return a {@link LogFile} object containing the lines found, an error message, or null if it's not a readable file
     */
    private static LogFile readFile(Path filePath, Integer maxLines, String searchTerm, Integer basePathSize) {
        LogFile logFile;
        ReverseFileReader reverseFileReader = null;
        String fileName = filePath.getFileName().toString();
        try {
            if (!isReadableFile(filePath)) {
                //If it's a file we likely can't read, return null.
                return null;
            }
            List<String> logLines = new ArrayList<>();
            boolean limitLines = maxLines > 0;
            reverseFileReader = new ReverseFileReader(StandardCharsets.UTF_8, filePath, 4096);
            // Keep parsing the file while it has more data and either we're not limiting lines or have kept below it
            while (reverseFileReader.hasMoreData() &&
                    (!limitLines || logLines.size() < maxLines)) {
                String logLine = reverseFileReader.readLine();
                // We want this line if it exists and we're either not searching or it contains the search term
                if (shouldAddLine(logLine, searchTerm)) {
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

    /**
     * Checks if this app can read filePath.
     * <p/>
     * For this to be true, the file name must end with .log, .txt. or be of type text/plain
     *
     * @param filePath the file path to check
     * @return true if this file can be read as a log
     */
    private static boolean isReadableFile(Path filePath) {
        boolean isPlainText = false;

        try {
            isPlainText = "text/plain".equals(Files.probeContentType(filePath));
        } catch (Exception e) {
            // If we get an exception here assuming it's not plaintext
            LOGGER.info("Exception checking file content type: " + e.getMessage());
        }

        return filePath.getFileName().toString().endsWith(".log") ||
                filePath.getFileName().toString().endsWith(".txt") ||
                isPlainText;
    }

    /**
     * Checks if a log line should be added to results.
     * <p/>
     * This is true if the logLine is not null, not blank, and either the searchTerm is null or is present in the logLine
     *
     * @param logLine the log line to check
     * @param searchTerm the search term to use, or null for none
     * @return true if the log line should be added
     */
    private static boolean shouldAddLine(String logLine, String searchTerm) {
        return logLine != null &&
                !logLine.isBlank() &&
                (searchTerm == null || logLine.contains(searchTerm));
    }
}
