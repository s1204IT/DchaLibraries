package org.apache.commons.codec;

import java.util.Comparator;

@Deprecated
public class StringEncoderComparator implements Comparator {
    private StringEncoder stringEncoder;

    public StringEncoderComparator() {
    }

    public StringEncoderComparator(StringEncoder stringEncoder) {
        this.stringEncoder = stringEncoder;
    }

    @Override
    public int compare(Object obj, Object obj2) {
        try {
            return ((Comparable) this.stringEncoder.encode(obj)).compareTo((Comparable) this.stringEncoder.encode(obj2));
        } catch (EncoderException e) {
            return 0;
        }
    }
}
