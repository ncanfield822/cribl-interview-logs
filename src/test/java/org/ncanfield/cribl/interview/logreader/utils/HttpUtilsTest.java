package org.ncanfield.cribl.interview.logreader.utils;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.ncanfield.cribl.interview.logreader.exception.LogReaderException;
import org.ncanfield.cribl.interview.logreader.models.LogReadResponse;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

public class HttpUtilsTest {
    @Test
    public void urlBuildsCorrectly() {
        String baseUrl = "test";

        String testUrl = HttpUtils.makeUrl(baseUrl, null, null, null);
        assertEquals("test/logs?", testUrl);

        testUrl = HttpUtils.makeUrl(baseUrl, "somefile", null, null);
        assertEquals("test/logs?fileName=somefile&", testUrl);

        testUrl = HttpUtils.makeUrl(baseUrl, "somefile", 1, null);
        assertEquals("test/logs?fileName=somefile&logLines=1&", testUrl);

        testUrl = HttpUtils.makeUrl(baseUrl, "somefile", 1, "blah");
        assertEquals("test/logs?fileName=somefile&logLines=1&searchTerm=blah", testUrl);

        testUrl = HttpUtils.makeUrl(baseUrl, null, 1, "blah");
        assertEquals("test/logs?logLines=1&searchTerm=blah", testUrl);

        testUrl = HttpUtils.makeUrl(baseUrl, null, 1, null);
        assertEquals("test/logs?logLines=1&", testUrl);

        testUrl = HttpUtils.makeUrl(baseUrl, null, null, "blah");
        assertEquals("test/logs?searchTerm=blah", testUrl);
    }

    @Test
    public void handleResponseParsesGoodResponse() {
        HttpResponse<String> httpResponse = Mockito.mock(HttpResponse.class);
        Mockito.when(httpResponse.body()).thenReturn("{\"serverName\":\"MyServer\",\"logFiles\":[{\"fileName\":\"numberFile.txt\",\"filePath\":\"test\\\\resources\\\\numberFile.txt\",\"logLines\":[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\",\"7\",\"8\",\"9\",\"10\"],\"error\":null}],\"errors\":[]}");
        LogReadResponse response = HttpUtils.handleResponse(
                httpResponse,
                null,
                "test/logs"
        );
        assertNotNull(response);
        assertNotNull(response.logFiles());
        assertTrue(response.errors().isEmpty());
        assertEquals(1, response.logFiles().size());
        assertEquals("MyServer", response.serverName());
    }

    @Test
    public void handleResponseHandlesParsingErrors() {
        HttpResponse<String> httpResponse = Mockito.mock(HttpResponse.class);
        Mockito.when(httpResponse.body()).thenReturn("{\"thisisMalformed");
        LogReadResponse response = HttpUtils.handleResponse(
                httpResponse,
                null,
                "test/logs"
        );
        assertNotNull(response);
        assertNull(response.logFiles());
        assertEquals(1, response.errors().size());
        assertEquals("There was an error parsing the response from the server", response.errors().get(0));
        assertEquals("test/logs", response.serverName());
    }

    @Test
    public void handleResponseHandlesException() {
        LogReadResponse response = HttpUtils.handleResponse(
                null,
                new LogReaderException("Test"),
                "test/logs"
        );
        assertNotNull(response);
        assertNull(response.logFiles());
        assertEquals(1, response.errors().size());
        assertEquals("There was an error fetching the response from the server", response.errors().get(0));
        assertEquals("test/logs", response.serverName());
    }
}
