/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.samtools.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * LineReader implementation that detects the OS and selects the preferred implementation for that
 * OS and delegates to it.
 *
 * @author alecw
 */
public class AsciiLineReader implements LineReader {

    private final LineReader readerImpl;
    private static boolean useAsciiLineReaderImpl = false;

    private static boolean isMacOs() {
        return (System.getProperty("os.name").toUpperCase().indexOf("MAC") >= 0);
    }

    private static boolean isWindows() {
        return (System.getProperty("os.name").toUpperCase().indexOf("WINDOWS") >= 0);
    }

    private static boolean isLinux() {
        return (System.getProperty("os.name").toUpperCase().indexOf("LINUX") >= 0);
    }

    /**
     * Prepare to read lines from the given input stream, with default buffer size
     * @param is need not be buffered, because this class does buffered reading
     */
    public AsciiLineReader(File file) throws Exception {
        useAsciiLineReaderImpl = isWindows();
        FileInputStream is = new FileInputStream(file);
        if (useAsciiLineReaderImpl) {
            readerImpl = new AsciiLineReaderImpl(is);
        } else {
            readerImpl = new BufferedLineReader(is);
        }
    }

    /**
     * Prepare to read lines from the given input stream
     * @param is need not be buffered, because this class does buffered reading
     * @param bufferSize
     */
    public AsciiLineReader(final InputStream is, final int bufferSize) {
        if (useAsciiLineReaderImpl) {
            readerImpl = new AsciiLineReaderImpl(is, bufferSize);
        } else {
            readerImpl = new BufferedLineReader(is, bufferSize);
        }
    }

    /**
     * Prepare to read lines from the given input stream
     * @param is need not be buffered, because this class does buffered reading
     * @param bufferSize
     */
    public AsciiLineReader(final InputStream is) {
        if (useAsciiLineReaderImpl) {
            readerImpl = new AsciiLineReaderImpl(is);
        } else {
            readerImpl = new BufferedLineReader(is);
        }
    }

    /**
     * Read a line and remove the line terminator
     *
     * @return the line read, or null if EOF has been reached.
     */
    public String readLine() {
        return readerImpl.readLine();
    }

    /**
     * @return 1-based number of line most recently read
     */
    public int getLineNumber() {
        return readerImpl.getLineNumber();
    }

    /**
     * Non-destructive one-character look-ahead.
     *
     * @return If not eof, the next character that would be read.  If eof, -1.
     */
    public int peek() {
        return readerImpl.peek();
    }

    public void close() {
        readerImpl.close();
    }
}
