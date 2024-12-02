package org.ncanfield.cribl.interview.logreader.utils;

import org.ncanfield.cribl.interview.logreader.exception.LogReaderException;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

public class ReverseFileReader implements Closeable {
    private final Charset charset;
    private final int bufferSize;
    private final List<byte[]> newLines;
    private final SeekableByteChannel fileChannel;
    private long remainingBytes;
    private int bufferOffset;
    private int lastNewline;
    private byte[] buffer;

    /**
     * Creates a new ReverseFileReader. Only UTF-8 and single byte encodings are currently supported
     *
     * @param charset the charset to use, only UTF-8 and single byte encodings are supported
     * @param filePath the path to the file to read
     * @param bufferSize the buffer size to use when reading the file
     * @throws IOException if there's an exception loading the file in
     * @throws LogReaderException if an unsupported charset is passed
     */
    public ReverseFileReader(Charset charset, Path filePath, int bufferSize) throws IOException, LogReaderException {
        if (charset.newEncoder().maxBytesPerChar() != 1f && !StandardCharsets.UTF_8.equals(charset)) {
            throw new LogReaderException("Only single byte encodings and UTF-8 are supported at the moment");
        }

        this.charset = charset;
        this.bufferSize = bufferSize;
        newLines = List.of(
                "\r\n".getBytes(charset),
                "\n".getBytes(charset),
                "\r".getBytes(charset)
        );

        fileChannel = Files.newByteChannel(filePath, StandardOpenOption.READ);
        remainingBytes = fileChannel.size();
        buffer = fillBuffer(null);
    }

    public boolean hasMoreData() {
        return bufferOffset + remainingBytes >= 0;
    }

    /**
     * Reads the next line up in the file, or null
     *
     * @return the next line up in the file, or null if there are no more
     * @throws IOException if there's an exception accessing
     * @throws LogReaderException if there's an issue parsing the file
     */
    public String readLine() throws IOException, LogReaderException {
        String line = null;
        while (bufferOffset > -1) {
            //We don't want to split up \r\n newlines.
            if (remainingBytes > 0 && bufferOffset < newLines.get(0).length) {
                // We can refill the buffer and add in the
                 buffer = fillBuffer(Arrays.copyOfRange(buffer, 0, lastNewline));
                 // Just refilled the buffer, start scanning again.
                 continue;
            }

            int newLineBytes = findNewline();

            // We've found a newline or reached the top of the file
            if (newLineBytes > 0 || (
                            remainingBytes == 0 &&
                            bufferOffset == 0)
            ) {
                // If newline bytes are 0, we're here because we need to read the rest of the file from the start
                int startIndex = newLineBytes == 0 ? 0 : bufferOffset + 1;
                byte[] lineBytes = Arrays.copyOfRange(buffer, startIndex, lastNewline);
                line = new String(lineBytes, charset);
                // Move both the buffer offset and last new line to just before the found newline
                // Move it at least one if this is the EOF so that the hasMoreData check returns false
                bufferOffset -= Math.max(newLineBytes, 1);
                lastNewline = bufferOffset + 1;
                // We found a line and can break the loop
                break;
            }

            // If nothing was found, move the buffer offset back one
            bufferOffset -= 1;
        }
        return line;
    }

    /**
     * Checks for a newline, if it finds it returns the number of bytes in it
     *
     * @return size of the newline or 0 if it didn't find any
     */
    private int findNewline() {
        int matchSize = 0;
        for (byte[] newLine : newLines) {
            // If there's not even enough bytes left for this newline it cannot match
            if (bufferOffset < newLine.length - 1) {
                continue;
            }
            // Checks the newline character(s) against the buffer
            // We add 1 to the buffer to capture the right number of characters
           if (Arrays.equals(
                   newLine, 0, newLine.length,
                   buffer, bufferOffset + 1 - newLine.length, bufferOffset + 1)) {
               matchSize = newLine.length;
               break;
           }
        }
        return matchSize;
    }

    /**
     * Loads new data into the buffer, adding spillover from the last line if needed
     *
     * @param spillover remaining bytes from the prior buffer
     * @return an array of bytes for the buffer
     * @throws IOException if there is an error reading from the file
     * @throws LogReaderException if the expected bytes could not be read from the file
     */
    private byte[] fillBuffer(byte[] spillover) throws IOException, LogReaderException {
        byte[] newBuffer;
        int dataSize = spillover != null ? spillover.length : 0;

        if (remainingBytes > bufferSize) {
            // Enough bytes are left to fill the buffer
            dataSize += bufferSize;

            // Move the offset to the end of the latest data
            bufferOffset = bufferSize - 1;
            newBuffer = new byte[dataSize];
            remainingBytes -= bufferSize;
            fileChannel.position(remainingBytes);
            int readCount = fileChannel.read(ByteBuffer.wrap(newBuffer, 0, bufferSize));
            if (readCount != bufferSize) {
                throw new LogReaderException("Could not read requested bytes");
            }
        } else {
            // Read the rest of the bytes
            dataSize += remainingBytes;

            // Move the offset to the end of the latest data
            bufferOffset = Long.valueOf(remainingBytes).intValue() - 1;
            newBuffer = new byte[dataSize];
            fileChannel.position(0);
            int readCount = fileChannel.read(ByteBuffer.wrap(newBuffer, 0, Long.valueOf(remainingBytes).intValue()));
            if (readCount != remainingBytes) {
                throw new LogReaderException("Could not read requested bytes");
            }
            remainingBytes = 0L;
        }

        if (spillover != null) {
            // Append spillover
            System.arraycopy(spillover, 0, newBuffer, dataSize - spillover.length, spillover.length);
            //Just in case the spillover contained a newline near the start
            bufferOffset += Math.min(spillover.length, newLines.get(0).length);
        }

        // Last newline was at the end of this buffer
        lastNewline = newBuffer.length;

        return newBuffer;
    }

    @Override
    public void close() throws IOException {
        fileChannel.close();
    }
}
