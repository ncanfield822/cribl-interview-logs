package org.ncanfield.cribl.interview.logreader.handlers;

import org.junit.jupiter.api.Test;
import org.ncanfield.cribl.interview.logreader.exception.LogReaderException;
import org.ncanfield.cribl.interview.logreader.models.LogFile;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LogReadRequestHandlerTest {
    private static final String TEST_RESOURCE_PATH = new File("src/test/resources").getAbsolutePath();

    @Test
    public void readsLogs() throws LogReaderException {
        File testFile = new File(TEST_RESOURCE_PATH);
        List<LogFile> logFiles = LogReadRequestHandler.readLogs(testFile, 1000, null);

        //Reads nine of them
        assertEquals(9, logFiles.size());

        //Check that the entire works of William Shakesphere was cut off at the defaultLineLimit
        for (LogFile logFile : logFiles) {
            if (logFile.fileName().equals("randomFile2.txt")) {
                assertEquals(1000, logFile.logLines().size());
            }
        }
    }

    @Test
    public void readsUnlimitedLines() throws LogReaderException {
        File testFile = new File(TEST_RESOURCE_PATH + "/secondLevelDir/randomFile2.txt");
        List<LogFile> logFiles = LogReadRequestHandler.readLogs(testFile, -1, null);

        assertEquals(1, logFiles.size());
        assertEquals(196037, logFiles.get(0).logLines().size());
    }

    @Test
    public void readsSpecificLogs() throws LogReaderException {
        File testFile = new File(TEST_RESOURCE_PATH + "/emptyFile.txt");
        List<LogFile> logFiles = LogReadRequestHandler.readLogs(testFile, 1000, null);

        //Reads just that file
        assertEquals(1, logFiles.size());
        assertEquals("emptyFile.txt", logFiles.get(0).fileName());
    }

    @Test
    public void readsLogsXLines() throws LogReaderException {
        //Returns expected lines from one file
        File testFile = new File(TEST_RESOURCE_PATH + "/numberFile.txt");
        List<LogFile> logFiles = LogReadRequestHandler.readLogs(testFile, 4, null);
        assertEquals(1, logFiles.size());
        assertEquals(4, logFiles.get(0).logLines().size());

        for (int i = 1; i <= 4; i ++) {
            assertEquals(String.valueOf(i), logFiles.get(0).logLines().get(i - 1));
        }

        //Returns only one line from each file (or less for the empty file)
        testFile = new File(TEST_RESOURCE_PATH);
        logFiles = LogReadRequestHandler.readLogs(testFile, 1, null);
        assertEquals(9, logFiles.size());
        assertTrue(logFiles.stream().allMatch(logfile -> logfile.logLines().size() <= 1));
    }

    @Test
    public void searchesLogs() throws LogReaderException {
        //Returns only lines containing the word "This"
        File testFile = new File(TEST_RESOURCE_PATH);
        List<LogFile> logFiles = LogReadRequestHandler.readLogs(testFile, 1000, "This");

        //Returns all nine files even ones that did not contain "This"
        assertEquals(9, logFiles.size());
        //Only got lines back with "This" in it and nothing else
        for (LogFile logFile : logFiles) {
            assertTrue(logFile.logLines().stream().allMatch(line -> line.contains("This")));
        }
    }

    @Test
    public void withAllParameters() throws LogReaderException {
        File testFile = new File(TEST_RESOURCE_PATH + "/longLineFile.txt");
        List<LogFile> logFiles = LogReadRequestHandler.readLogs(testFile, 1, "This");

        assertEquals(1, logFiles.size());
        assertEquals(1, logFiles.get(0).logLines().size());
        assertEquals("This is so we can test that the buffer will actually load some lines in.", logFiles.get(0).logLines().get(0));
        assertEquals("longLineFile.txt", logFiles.get(0).fileName());
    }

    @Test
    public void exceptionWhenNoFileExists() {
        File testFile = new File(TEST_RESOURCE_PATH + "/notAFile.txt");
        assertThrows(LogReaderException.class, () -> LogReadRequestHandler.readLogs(testFile, 1000, null));
    }
}
