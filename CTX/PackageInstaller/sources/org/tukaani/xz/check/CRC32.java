package org.tukaani.xz.check;

public class CRC32 extends Check {
    private final java.util.zip.CRC32 state = new java.util.zip.CRC32();

    public CRC32() {
        this.size = 4;
        this.name = "CRC32";
    }

    @Override
    public void update(byte[] bArr, int i, int i2) {
        this.state.update(bArr, i, i2);
    }

    @Override
    public byte[] finish() {
        byte[] bArr = {(byte) this.state.getValue(), (byte) (r0 >>> 8), (byte) (r0 >>> 16), (byte) (r0 >>> 24)};
        this.state.reset();
        return bArr;
    }
}
