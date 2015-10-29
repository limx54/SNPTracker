/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.samtools.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Implementation of LineReader that is a thin wrapper around BufferedReader.  On Linux, this is faster
 * than AsciiLineReaderImpl.  If you use AsciiLineReader rather than this class, it will detect the OS
 * and delegate to the preferred implementation.
 *
 * @author alecw@broadinstitute.org
 */
public class BufferedLineReader implements LineReader {

    private final BufferedReader reader;
    private int lineNumber = 0;
    private String peekedLine;

    public BufferedLineReader(final InputStream is) {
        reader = new BufferedReader(new InputStreamReader(is));
    }

    public BufferedLineReader(final InputStream is, final int bufferSize) {
        reader = new BufferedReader(new InputStreamReader(is), bufferSize);
    }

    /**
     * Read a line and remove the line terminator
     *
     * @return the line read, or null if EOF has been reached.
     */
    public String readLine() {
        ++lineNumber;
        String ret = null;
        try {

            if (peekedLine != null) {
                ret = peekedLine;
                peekedLine = null;
            } else {
                ret = reader.readLine();
            }
            return ret;
        } catch (IOException e) {
            // throw new RuntimeIOException(e);
            return ret;
        }
    }

    /**
     * @return 1-based number of line most recently read
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Non-destructive one-character look-ahead.
     *
     * @return If not eof, the next character that would be read.  If eof, -1.
     */
    public int peek() {
        if (peekedLine == null) {
            try {
                peekedLine = reader.readLine();
            } catch (IOException e) {
                //throw new RuntimeIOException(e);
            }
        }
        if (peekedLine == null) {
            return -1;
        }
        if (peekedLine.length() == 0) {
            return '\n';
        }
        return peekedLine.charAt(0);
    }

    public void close() {
        peekedLine = null;
        try {
            reader.close();
        } catch (IOException e) {
           // throw new RuntimeIOException(e);
        }
    }
}
