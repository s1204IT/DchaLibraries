package org.tukaani.xz.common;

public class Util {
    public static int getVLISize(long j) {
        int i = 0;
        do {
            i++;
            j >>= 7;
        } while (j != 0);
        return i;
    }
}
