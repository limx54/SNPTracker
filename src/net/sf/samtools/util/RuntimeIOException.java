/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sf.samtools.util;

 

/**
 * Thrown by various IO classes to indicate IOException without having to clutter the API with throws clauses
 */
public class RuntimeIOException extends Exception {
    public RuntimeIOException() {
    }

    public RuntimeIOException(final String s) {
        super(s);
    }

    public RuntimeIOException(final String s, final Throwable throwable) {
        super(s, throwable);
    }

    public RuntimeIOException(final Throwable throwable) {
        super(throwable);
    }
}
