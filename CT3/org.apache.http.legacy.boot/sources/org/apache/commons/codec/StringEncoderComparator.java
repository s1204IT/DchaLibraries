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
    public int compare(Object o1, Object o2) {
        try {
            Comparable s1 = (Comparable) this.stringEncoder.encode(o1);
            Comparable s2 = (Comparable) this.stringEncoder.encode(o2);
            int compareCode = s1.compareTo(s2);
            return compareCode;
        } catch (EncoderException e) {
            return 0;
        }
    }
}
