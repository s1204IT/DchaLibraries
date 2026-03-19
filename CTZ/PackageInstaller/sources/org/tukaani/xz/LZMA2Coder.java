package org.tukaani.xz;

abstract class LZMA2Coder implements FilterCoder {
    LZMA2Coder() {
    }

    @Override
    public boolean changesSize() {
        return true;
    }

    @Override
    public boolean nonLastOK() {
        return false;
    }

    @Override
    public boolean lastOK() {
        return true;
    }
}
