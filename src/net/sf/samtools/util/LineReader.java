/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sf.samtools.util;

/**
 * Interface allows for implementations that read lines from a String, an ASCII file, or somewhere else.
 */
public interface LineReader {

    /**
     * Read a line and remove the line terminator
     * @return the line read, or null if EOF has been reached.
     */
    String readLine();

    /**
     * @return 1-based number of line most recently read
     */
    int getLineNumber();

    /**
     * Non-destructive one-character look-ahead.
     * @return If not eof, the next character that would be read.  If eof, -1.
     */
    int peek();

    public void close();
}
