package org.ncanfield.cribl.interview.logreader.endpoints;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.ncanfield.cribl.interview.logreader.config.LogReaderConfig;
import org.ncanfield.cribl.interview.logreader.models.LogFile;
import org.ncanfield.cribl.interview.logreader.models.LogReadResponse;

import java.io.File;

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
    public void readsLogs() {
        //Just make sure the simplest happy path does what we want
        LogReadResponse response = logReader.readLogs(null, null, null);
        //Reads nine of them
        assertEquals(15, response.logFiles().size());
        assertTrue(response.errors().isEmpty());

        //Check that the entire works of William Shakesphere was cut off at the defaultLineLimit
        for (LogFile logFile : response.logFiles()) {
            if (logFile.fileName().equals("randomFile2.txt")) {
                assertEquals(1000, logFile.logLines().size());
            }
        }
    }

    @Test
    public void readsUnlimitedLinesFromConfig() {
        Mockito.when(config.defaultLineLimit()).thenReturn(-1);
        LogReadResponse response = logReader.readLogs("secondLevelDir/randomFile2.txt", null, null);

        assertEquals(1, response.logFiles().size());
        assertEquals(196037, response.logFiles().get(0).logLines().size());
        assertTrue(response.errors().isEmpty());
    }

    @Test
    public void acceptsValidFileParams()  {
        LogReadResponse response = logReader.readLogs("emptyFile.txt", null, null);

        //Reads just that file
        assertEquals(1, response.logFiles().size());
        assertEquals("emptyFile.txt", response.logFiles().get(0).fileName());
        assertTrue(response.errors().isEmpty());

        //Try a subdirectory
        response = logReader.readLogs("secondLevelDir", null, null);

        //Reads both files there
        assertEquals(2, response.logFiles().size());
        assertTrue(response.logFiles().stream().allMatch(logFile -> logFile.fileName().startsWith("randomFile")));
        assertTrue(response.errors().isEmpty());
    }

    @Test
    public void handlesInvalidParams() {
        // Bad filename and invalid lines requested
        LogReadResponse response = logReader.readLogs("thisFileDoesntExist.txt", -1, null);

        assertEquals(2, response.errors().size());
        assertTrue(response.errors().contains("The log files specified do not exist"));
        assertTrue(response.errors().contains("Requested log lines must be > 0"));
        assertNull(response.logFiles());

        // Directory traversal attempt
        response = logReader.readLogs("../../main/java", null, null);

        assertEquals(1, response.errors().size());
        assertEquals("Provided file path is invalid", response.errors().get(0));
        assertNull(response.logFiles());
    }

    @Test
    public void withAllParameters() {
        // Just to check it's passing everything down to the handler like we expect
        LogReadResponse response = logReader.readLogs("longLineFile.txt",  1, "This");

        assertEquals(1, response.logFiles().size());
        assertEquals(1, response.logFiles().get(0).logLines().size());
        assertEquals("This is so we can test that the buffer will actually load some lines in.", response.logFiles().get(0).logLines().get(0));
        assertEquals("longLineFile.txt", response.logFiles().get(0).fileName());
        assertTrue(response.errors().isEmpty());
    }
}
