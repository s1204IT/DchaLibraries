package org.tukaani.xz;

abstract class BCJCoder implements FilterCoder {
    BCJCoder() {
    }

    public static boolean isBCJFilterID(long j) {
        return j >= 4 && j <= 9;
    }

    @Override
    public boolean changesSize() {
        return false;
    }

    @Override
    public boolean nonLastOK() {
        return true;
    }

    @Override
    public boolean lastOK() {
        return false;
    }
}
