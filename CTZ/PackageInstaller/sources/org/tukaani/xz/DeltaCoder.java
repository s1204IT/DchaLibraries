package org.tukaani.xz;

abstract class DeltaCoder implements FilterCoder {
    DeltaCoder() {
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
