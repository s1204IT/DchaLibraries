package org.tukaani.xz.delta;

abstract class DeltaCoder {
    final int distance;
    final byte[] history = new byte[256];
    int pos = 0;

    DeltaCoder(int i) {
        if (i < 1 || i > 256) {
            throw new IllegalArgumentException();
        }
        this.distance = i;
    }
}
