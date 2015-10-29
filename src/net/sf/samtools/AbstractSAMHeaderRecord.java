/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sf.samtools;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Base class for the various concrete records in a SAM header, providing uniform
 * access to the attributes.
 */
public abstract class AbstractSAMHeaderRecord {
    private final Map<String,Object> mAttributes = new HashMap<String, Object>();

    public Object getAttribute(final String key) {
        return mAttributes.get(key);
    }

    /**
     * Set the given value for the attribute named 'key'.  Replaces an existing value, if any.
     * If value is null, the attribute is removed.
     * Supported types are Character, Integer, Float and String.  Byte and Short may also be
     * passed in but they will be converted to Integer.
     * @param key attribute name
     * @param value attribute value
     */
    public void setAttribute(final String key, final Object value) {
        if (value == null) {
            mAttributes.remove(key);
        } else {
            mAttributes.put(key, value);
        }
    }

    /**
     * Returns the Set of attributes.
     */
    public Set<Map.Entry<String,Object>> getAttributes() {
        return mAttributes.entrySet();
    }


    /**
     * Returns the ID tag (or equivalent) for this header record. The
     * default implementation throws a PicardException to indicate "not implemented".
     */
    public String getId() {
        throw new UnsupportedOperationException("Method not implemented for: " + this.getClass());
    }

    /**
     * For use in the equals() method of the concrete class.
     */
    protected boolean attributesEqual(final AbstractSAMHeaderRecord that) {
        return mAttributes.equals(that.mAttributes);
    }

    /**
     * For use in the hashCode() method of the concrete class.
     */
    protected int attributesHashCode() {
        return (mAttributes != null ? mAttributes.hashCode() : 0);
    }

    /**
     * Standard tags are the tags defined in SAM spec.  These do not have type information in the test
     * representation, because the type information is predefined for each tag.
     * @return list of predefined tags for the concrete SAMHeader record type.
     */
    abstract Set<String> getStandardTags();
}
