/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sf.samtools;

/**
 * @author alecw@broadinstitute.org
 */
public class SAMException extends RuntimeException {
    public SAMException() {
    }

    public SAMException(final String s) {
        super(s);
    }

    public SAMException(final String s, final Throwable throwable) {
        super(s, throwable);
    }

    public SAMException(final Throwable throwable) {
        super(throwable);
    }
}
