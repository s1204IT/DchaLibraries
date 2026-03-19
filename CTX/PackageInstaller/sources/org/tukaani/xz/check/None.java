package org.tukaani.xz.check;

public class None extends Check {
    public None() {
        this.size = 0;
        this.name = "None";
    }

    @Override
    public void update(byte[] bArr, int i, int i2) {
    }

    @Override
    public byte[] finish() {
        return new byte[0];
    }
}
