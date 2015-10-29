/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sf.samtools.util;
/**
 * Static methods that encapsulate the standard SAM way of storing ranges: one-based, with both ends
 * inclusive.
 */
public class CoordMath {

    public static int getLength(final int start, final int end) {
        return (end - start) + 1;
    }

    public static int getStart(final int end, final int length) {
        return end - length + 1;
    }

    public static int getEnd(final int start, final int length) {
        return start + length - 1;
    }

    /**
     * Checks to see if the two sets of coordinates have any overlap.
     */
    public static boolean overlaps(final int start, final int end, final int start2, final int end2) {
        return (start2 >= start && start2 <= end) || (end2 >=start && end2 <= end) ||
                encloses(start2, end2, start, end);
    }

    /** Returns true if the "inner" coords and totally enclosed by the "outer" coords. */
    public static boolean encloses(final int outerStart, final int outerEnd, final int innerStart, final int innerEnd) {
        return innerStart >= outerStart && innerEnd <= outerEnd;
    }

    /**
     * Determines the amount of overlap between two coordinate ranges. Assumes that the two ranges
     * actually do overlap and therefore may produce strange results when they do not!
     */
    public static int getOverlap(final int start, final int end, final int start2, final int end2) {
        return getLength(Math.max(start, start2), Math.min(end, end2));
    }

    /**
     * Determines the read cycle number for the base
     *
     *  @param isNegativeStrand true if the read is negative strand
     *  @param readLength
     *  @param readBaseIndex the 0-based index of the read base in question
     */
    public static int getCycle(boolean isNegativeStrand, int readLength, final int readBaseIndex) {
        return isNegativeStrand ? readLength - readBaseIndex : readBaseIndex + 1;
    }

}
