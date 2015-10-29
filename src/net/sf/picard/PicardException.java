/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.picard;

/**
 *
 * @author Broadinstitute
 */
public class PicardException extends RuntimeException {

    public PicardException() {
    }

    public PicardException(final String s) {
        super(s);
    }

    public PicardException(final String s, final Throwable throwable) {
        super(s, throwable);
    }

    public PicardException(final Throwable throwable) {
        super(throwable);
    }
}
