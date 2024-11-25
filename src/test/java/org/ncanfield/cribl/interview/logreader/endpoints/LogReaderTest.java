package org.ncanfield.cribl.interview.logreader.endpoints;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.ncanfield.cribl.interview.logreader.config.LogReaderConfig;
import org.ncanfield.cribl.interview.logreader.exception.LogReaderException;
import org.ncanfield.cribl.interview.logreader.models.LogFile;
import org.ncanfield.cribl.interview.logreader.models.LogReadResponse;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

public class LogReaderTest {
    @Mock
    private LogReaderConfig config;

    @InjectMocks
    private LogReader logReader;

    @BeforeEach
    public void setup() throws Exception {
        AutoCloseable autoCloseable = MockitoAnnotations.openMocks(this);
        String testResourcesPath = new File("src/test/resources").getAbsolutePath();
        Mockito.when(config.logDir()).thenReturn(testResourcesPath);
        Mockito.when(config.friendlyName()).thenReturn("TestServer");
        Mockito.when(config.defaultLineLimit()).thenReturn(1000);
        autoCloseable.close();
    }

    @Test
    public void readsLogs() throws LogReaderException {
        LogReadResponse response = logReader.readLogs(null, null, null);
        //Reads nine of them
        assertEquals(9, response.logFiles().size());

        //Check that the entire works of William Shakesphere was cut off at the defaultLineLimit
        for (LogFile logFile : response.logFiles()) {
            if (logFile.fileName().equals("randomFile2.txt")) {
                assertEquals(1000, logFile.logLines().size());
            }
        }
    }

    @Test
    public void readsUnlimitedLines() throws LogReaderException, URISyntaxException, IOException {
        Mockito.when(config.defaultLineLimit()).thenReturn(-1);
        LogReadResponse response = logReader.readLogs("secondLevelDir/randomFile2.txt", null, null);

        assertEquals(1, response.logFiles().size());

        assertEquals(196037, response.logFiles().get(0).logLines().size());
    }

    @Test
    public void readsSpecificLogs() throws LogReaderException {
        LogReadResponse response = logReader.readLogs("emptyFile.txt", null, null);

        //Reads just that file
        assertEquals(1, response.logFiles().size());
        assertEquals("emptyFile.txt", response.logFiles().get(0).fileName());

        //Try a subdirctory
        response = logReader.readLogs("secondLevelDir", null, null);

        //Reads both files there
        assertEquals(2, response.logFiles().size());
        assertTrue(response.logFiles().stream().allMatch(logFile -> logFile.fileName().startsWith("randomFile")));
    }

    @Test
    public void handlesInvalidFilename() {
        // Bad filename
        assertThrows(LogReaderException.class, () -> logReader.readLogs("thisFileDoesntExist.txt", null, null));

        // Directory traversal attempt
        assertThrows(LogReaderException.class, () -> logReader.readLogs("../../main/java", null, null));
    }

    @Test
    public void readsLogsXLines() throws LogReaderException {
        //Returns expected lines from one file
        LogReadResponse response = logReader.readLogs("numberFile.txt", 4, null);
        assertEquals(1, response.logFiles().size());
        assertEquals(4, response.logFiles().get(0).logLines().size());

        for (int i = 1; i <= 4; i ++) {
            assertEquals(String.valueOf(i), response.logFiles().get(0).logLines().get(i - 1));
        }

        //Returns only one line from each file (or less for the empty file)
        response = logReader.readLogs(null, 1, null);
        assertEquals(9, response.logFiles().size());
        assertTrue(response.logFiles().stream().allMatch(logfile -> logfile.logLines().size() <= 1));
    }

    @Test
    public void searchesLogs() throws LogReaderException {
        //Returns only lines containing the word "This"
        LogReadResponse response = logReader.readLogs(null,  null, "This");

        //Returns all nine files even ones that did not contain "This"
        assertEquals(9, response.logFiles().size());
        //Only got lines back with "This" in it and nothing else
        for (LogFile logFile : response.logFiles()) {
            assertTrue(logFile.logLines().stream().allMatch(line -> line.contains("This")));
        }
    }

    @Test
    public void withAllParameters() throws LogReaderException {
        LogReadResponse response = logReader.readLogs("longLineFile.txt",  1, "This");

        assertEquals(1, response.logFiles().size());
        assertEquals(1, response.logFiles().get(0).logLines().size());
        assertEquals("This is so we can test that the buffer will actually load some lines in.", response.logFiles().get(0).logLines().get(0));
        assertEquals("longLineFile.txt", response.logFiles().get(0).fileName());
    }
}
