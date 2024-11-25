package org.ncanfield.cribl.interview.logreader.utils;

import org.junit.jupiter.api.Test;
import org.ncanfield.cribl.interview.logreader.exception.LogReaderException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReverseFileReaderTest {
    @Test
    public void readsFileInReverse() throws Exception {
        Path numberFilePath = Paths.get(ReverseFileReaderTest.class.getResource("/numberFile.txt").toURI());
        ReverseFileReader rfr = new ReverseFileReader(StandardCharsets.UTF_8, numberFilePath, 4096);
        List<String> lines = readFile(rfr);

        assertEquals(10, lines.size());

        for (int i = 1; i <= 10; i ++) {
            assertEquals(String.valueOf(i), lines.get(i - 1));
        }
    }

    @Test
    public void handlesEmptyLines() throws Exception {
        Path emptyLineFilePath = Paths.get(ReverseFileReaderTest.class.getResource("/fourEmptyLines.txt").toURI());
        ReverseFileReader rfr = new ReverseFileReader(StandardCharsets.UTF_8, emptyLineFilePath, 4096);
        List<String> lines = readFile(rfr);

        assertEquals(4, lines.size());

        assertTrue(lines.stream().allMatch(""::equals));
    }

    @Test
    public void handlesEmptyFile() throws Exception {
        Path emptyLineFilePath = Paths.get(ReverseFileReaderTest.class.getResource("/emptyFile.txt").toURI());
        ReverseFileReader rfr = new ReverseFileReader(StandardCharsets.UTF_8, emptyLineFilePath, 4096);
        List<String> lines = readFile(rfr);

        assertEquals(0, lines.size());
    }

    @Test
    public void handlesVariousNewlines() throws Exception {
        Path rnFilePath  = Paths.get(ReverseFileReaderTest.class.getResource("/rnLineEnd.txt").toURI());
        Path rFilePath = Paths.get(ReverseFileReaderTest.class.getResource("/rLineEnd.txt").toURI());

        ReverseFileReader rnFileReader = new ReverseFileReader(StandardCharsets.UTF_8, rnFilePath, 4096);
        ReverseFileReader rFileReader = new ReverseFileReader(StandardCharsets.UTF_8, rFilePath, 4096);

        List<String> rnLines = readFile(rnFileReader);
        List<String> rLines = readFile(rFileReader);

        // All of them should have read two lines with the last line of the file/first line of the list being "This is the second line"
        // The first line/second in list should read "This line ends in [EOL delimiter]"
        assertEquals(2, rnLines.size());
        assertEquals("This is the second line", rnLines.get(0));
        assertEquals("This line ends in \\r\\n", rnLines.get(1));

        assertEquals(2, rLines.size());
        assertEquals("This is the second line", rLines.get(0));
        assertEquals("This line ends in \\r", rLines.get(1));
    }

    @Test
    public void errorsOnUnsupportedCharset() throws URISyntaxException {
        Path emptyLineFilePath = Paths.get(ReverseFileReaderTest.class.getResource("/fourEmptyLines.txt").toURI());
        assertThrows(LogReaderException.class, () -> new ReverseFileReader(StandardCharsets.UTF_16, emptyLineFilePath, 4096));
    }

    @Test
    public void refillsBuffer() throws Exception {
        Path longFilePath = Paths.get(ReverseFileReaderTest.class.getResource("/longLineFile.txt").toURI());
        ReverseFileReader rfr = new ReverseFileReader(StandardCharsets.UTF_8, longFilePath, 10);

        List<String> lines = readFile(rfr);

        assertEquals(4, lines.size());

        assertEquals("This file has slightly longer lines than the others.", lines.get(3));
        assertEquals("This is so we can test that the buffer will actually load some lines in.", lines.get(2));
        assertEquals("To help with this, we'll also be setting the buffer size pretty low.", lines.get(1));
        assertEquals("Ten will work.", lines.get(0));
    }

    private static List<String> readFile(ReverseFileReader rfr) throws IOException, LogReaderException {
        List<String> lines = new ArrayList<>();

        while (rfr.hasMoreData()) {
            lines.add(rfr.readLine());
        }
        return lines;
    }
}
