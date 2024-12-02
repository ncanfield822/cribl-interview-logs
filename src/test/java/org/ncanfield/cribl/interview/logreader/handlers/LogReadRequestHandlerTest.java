package org.ncanfield.cribl.interview.logreader.handlers;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.ncanfield.cribl.interview.logreader.exception.LogReaderException;
import org.ncanfield.cribl.interview.logreader.models.LogFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LogReadRequestHandlerTest {
    private static final String TEST_RESOURCE_PATH = new File("src/test/resources").getAbsolutePath();

    @Test
    public void readsLogs() {
        File testFile = new File(TEST_RESOURCE_PATH);
        List<LogFile> logFiles = LogReadRequestHandler.readLogs(testFile, 1000, null, TEST_RESOURCE_PATH.length());

        //Reads eleven of them - does not include the picture or zip files
        assertEquals(11, logFiles.size());

        //Check that the entire works of William Shakesphere was cut off at the defaultLineLimit
        for (LogFile logFile : logFiles) {
            if (logFile.fileName().equals("randomFile2.txt")) {
                assertEquals(1000, logFile.logLines().size());
            }
        }
    }

    @Test
    public void readsUnlimitedLines() {
        File testFile = new File(TEST_RESOURCE_PATH + "/secondLevelDir/randomFile2.txt");
        List<LogFile> logFiles = LogReadRequestHandler.readLogs(testFile, -1, null, TEST_RESOURCE_PATH.length());

        assertEquals(1, logFiles.size());
        assertEquals(153632, logFiles.get(0).logLines().size());
    }

    @Test
    public void emptyLinesForAllWhitespaces() {
        File testFile = new File(TEST_RESOURCE_PATH + "/onlyWhitespaces.txt");
        List<LogFile> logFiles = LogReadRequestHandler.readLogs(testFile, -1, null, TEST_RESOURCE_PATH.length());

        assertEquals(1, logFiles.size());
        assertTrue(logFiles.get(0).logLines().isEmpty());
    }

    @Test
    public void skipsWhitespaces() {
        File testFile = new File(TEST_RESOURCE_PATH + "/whitespaces.log");
        List<LogFile> logFiles = LogReadRequestHandler.readLogs(testFile, -1, null, TEST_RESOURCE_PATH.length());

        assertEquals(1, logFiles.size());
        assertEquals(3, logFiles.get(0).logLines().size());
        assertEquals("Newest Line", logFiles.get(0).logLines().get(0));
        assertEquals("After various whitespace lines", logFiles.get(0).logLines().get(1));
        assertEquals("Oldest line", logFiles.get(0).logLines().get(2));
    }

    @Test
    public void readsSpecificLogs() {
        File testFile = new File(TEST_RESOURCE_PATH + "/emptyFile.txt");
        List<LogFile> logFiles = LogReadRequestHandler.readLogs(testFile, 1000, null, TEST_RESOURCE_PATH.length());

        //Reads just that file
        assertEquals(1, logFiles.size());
        assertEquals("emptyFile.txt", logFiles.get(0).fileName());
    }

    @Test
    public void readsLogsXLines() {
        //Returns expected lines from one file
        File testFile = new File(TEST_RESOURCE_PATH + "/numberFile.txt");
        List<LogFile> logFiles = LogReadRequestHandler.readLogs(testFile, 4, null, TEST_RESOURCE_PATH.length());
        assertEquals(1, logFiles.size());
        assertEquals(4, logFiles.get(0).logLines().size());

        for (int i = 1; i <= 4; i ++) {
            assertEquals(String.valueOf(i), logFiles.get(0).logLines().get(i - 1));
        }

        //Returns only one line from each file (or less for the empty file)
        testFile = new File(TEST_RESOURCE_PATH);
        logFiles = LogReadRequestHandler.readLogs(testFile, 1, null, TEST_RESOURCE_PATH.length());
        assertEquals(11, logFiles.size());
        int filesWithData = 0;

        for (LogFile logFile : logFiles) {
            if (logFile.logLines() != null) {
                assertTrue(logFile.logLines().size() <= 1);
                filesWithData++;
            }
        }

        //Should have been nine text files
        assertEquals(11, filesWithData);
    }

    @Test
    public void searchesLogs() {
        //Returns only lines containing the word "This"
        File testFile = new File(TEST_RESOURCE_PATH);
        List<LogFile> logFiles = LogReadRequestHandler.readLogs(testFile, 1000, "This", TEST_RESOURCE_PATH.length());

        //Returns all fifteen files even ones that did not contain "This"
        assertEquals(11, logFiles.size());
        int filesWithData = 0;
        //Only got lines back with "This" in it and nothing else
        for (LogFile logFile : logFiles) {
            if (!logFile.logLines().isEmpty()) {
                assertTrue(logFile.logLines().stream().allMatch(line -> line.contains("This")));
                filesWithData++;
            }
        }
        assertEquals(6, filesWithData);
    }

    @Test
    public void withAllParameters() {
        File testFile = new File(TEST_RESOURCE_PATH + "/longLineFile.txt");
        List<LogFile> logFiles = LogReadRequestHandler.readLogs(testFile, 1, "This", TEST_RESOURCE_PATH.length());

        assertEquals(1, logFiles.size());
        assertEquals(1, logFiles.get(0).logLines().size());
        assertEquals("This is so we can test that the buffer will actually load some lines in.", logFiles.get(0).logLines().get(0));
        assertEquals("longLineFile.txt", logFiles.get(0).fileName());
    }

    @Test
    public void errorWhenNoFileExists() {
        File testFile = new File(TEST_RESOURCE_PATH + "/notAFile.txt");
        List<LogFile> logFiles = LogReadRequestHandler.readLogs(testFile, 1000, null, TEST_RESOURCE_PATH.length());
        assertEquals(1, logFiles.size());
        assertNull(logFiles.get(0).logLines());
        assertEquals("The specified file does not exist", logFiles.get(0).error());
    }

    @Test
    public void trimsBasePathFromResponse() {
        File testFile = new File(TEST_RESOURCE_PATH + "/secondLevelDir/randomFile.txt");
        List<LogFile> logFiles = LogReadRequestHandler.readLogs(testFile, 1, "This", TEST_RESOURCE_PATH.length());
        assertEquals(1, logFiles.size());

        // This will have different results on windows vs linux
        assertTrue("secondLevelDir/randomFile.txt".equals(logFiles.get(0).filePath()) || "secondLevelDir\\randomFile.txt".equals(logFiles.get(0).filePath()));
    }

    @Test
    public void returnsErrorForNonTextFile() {
        File testFile = new File(TEST_RESOURCE_PATH + "/goatPic.jpg");
        List<LogFile> logFiles = LogReadRequestHandler.readLogs(testFile, 1000, null, TEST_RESOURCE_PATH.length());
        assertEquals(1, logFiles.size());
        assertNull(logFiles.get(0).logLines());
        assertEquals("The specified file is not a text file", logFiles.get(0).error());
    }

    @Test
    public void returnsErrorForUnreadableDir() {
        File testFile = Mockito.mock(File.class);
        Mockito.when(testFile.isDirectory()).thenReturn(true);
        Mockito.when(testFile.exists()).thenReturn(true);
        Mockito.when(testFile.listFiles()).thenReturn(null);
        Mockito.when(testFile.getAbsolutePath()).thenReturn(TEST_RESOURCE_PATH + "/badDir");
        Mockito.when(testFile.getName()).thenReturn("badDir");

        List<LogFile> logFiles = LogReadRequestHandler.readLogs(testFile, 1000, null, TEST_RESOURCE_PATH.length());
        assertEquals(1, logFiles.size());
        assertNull(logFiles.get(0).logLines());
        assertEquals("This directory could not be accessed", logFiles.get(0).error());
    }

    @Test
    public void handlesFileReadException() {
        File testFile = Mockito.mock(File.class);
        Path testPath = Mockito.mock(Path.class);

        Mockito.when(testPath.getFileName()).thenReturn(Path.of("bad.log"));
        Mockito.when(testPath.toString()).thenReturn(TEST_RESOURCE_PATH + "/bad.log");
        //This is called in the actual java methods that support the reader
        Mockito.when(testPath.getFileSystem()).thenThrow(new RuntimeException("Test exception"));

        Mockito.when(testFile.isDirectory()).thenReturn(false);
        Mockito.when(testFile.isFile()).thenReturn(true);
        Mockito.when(testFile.exists()).thenReturn(true);
        Mockito.when(testFile.listFiles()).thenReturn(null);
        Mockito.when(testFile.toPath()).thenReturn(testPath);

        List<LogFile> logFiles = LogReadRequestHandler.readLogs(testFile, 1000, null, TEST_RESOURCE_PATH.length());
        assertEquals(1, logFiles.size());
        assertNull(logFiles.get(0).logLines());
        assertEquals("Encountered an exception reading the file", logFiles.get(0).error());
    }
}
