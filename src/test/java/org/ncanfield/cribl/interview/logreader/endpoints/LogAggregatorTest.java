package org.ncanfield.cribl.interview.logreader.endpoints;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.ncanfield.cribl.interview.logreader.config.LogReaderConfig;
import org.ncanfield.cribl.interview.logreader.models.LogReadResponse;
import org.ncanfield.cribl.interview.logreader.utils.HttpUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LogAggregatorTest {
    @Mock
    private LogReaderConfig config;

    @Mock
    private LogReader logReader;

    @InjectMocks
    private LogAggregator logAggregator;

    private MockedStatic<HttpUtils> mockedHttpUtils;

    @BeforeEach
    public void setup() throws Exception {
        AutoCloseable autoCloseable = MockitoAnnotations.openMocks(this);
        mockedHttpUtils = Mockito.mockStatic(HttpUtils.class);
        mockedHttpUtils.when(() -> HttpUtils.buildFuture(ArgumentMatchers.anyString())).thenReturn(getFuture());
        mockedHttpUtils.when(() -> HttpUtils.makeUrl(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenCallRealMethod();

        String testResourcesPath = new File("src/test/resources").getAbsolutePath();
        Mockito.when(config.logDir()).thenReturn(testResourcesPath);
        Mockito.when(config.friendlyName()).thenReturn("TestServer");
        Mockito.when(config.defaultLineLimit()).thenReturn(1000);
        Mockito.when(config.logServers()).thenReturn(List.of("self", "http://localhost:8080", "https://github.com"));

        Mockito.when(logReader.readLogs(null, null, null))
                        .thenReturn(new LogReadResponse("Test", List.of(), List.of()));
        autoCloseable.close();
    }

    @AfterEach
    public void tearDown() {
        if (mockedHttpUtils != null && Mockito.mockingDetails(HttpUtils.class).isMock()) {
            //Just makes sure a test doesn't accidentally leave this open
            mockedHttpUtils.close();
        }
    }

    @Test
    public void callsAllServers() {
        logAggregator.aggregateLogs(null, null, null);
        Mockito.verify(logReader, Mockito.times(1)).readLogs(null, null, null);
        //Calls both servers and nothing else
        mockedHttpUtils.verify(() -> HttpUtils.buildFuture("http://localhost:8080/logs?"), Mockito.times(1));
        mockedHttpUtils.verify(() -> HttpUtils.buildFuture("https://github.com/logs?"), Mockito.times(1));
        mockedHttpUtils.verify(() -> HttpUtils.buildFuture(ArgumentMatchers.any()), Mockito.times(2));
    }

    private static CompletableFuture<LogReadResponse> getFuture() {
        return CompletableFuture.supplyAsync(() -> new LogReadResponse("Test", List.of(), List.of()));
    }
}
