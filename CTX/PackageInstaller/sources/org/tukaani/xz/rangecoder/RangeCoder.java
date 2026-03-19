package org.tukaani.xz.rangecoder;

import java.util.Arrays;

public abstract class RangeCoder {
    public static final void initProbs(short[] sArr) {
        Arrays.fill(sArr, (short) 1024);
    }
}
