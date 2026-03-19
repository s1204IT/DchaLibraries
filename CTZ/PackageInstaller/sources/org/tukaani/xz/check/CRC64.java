package org.tukaani.xz.check;

public class CRC64 extends Check {
    private static final long[] crcTable = new long[256];
    private long crc = -1;

    static {
        for (int i = 0; i < crcTable.length; i++) {
            long j = i;
            for (int i2 = 0; i2 < 8; i2++) {
                if ((j & 1) == 1) {
                    j = (j >>> 1) ^ (-3932672073523589310L);
                } else {
                    j >>>= 1;
                }
            }
            crcTable[i] = j;
        }
    }

    public CRC64() {
        this.size = 8;
        this.name = "CRC64";
    }

    @Override
    public void update(byte[] bArr, int i, int i2) {
        int i3 = i2 + i;
        while (i < i3) {
            this.crc = crcTable[(bArr[i] ^ ((int) this.crc)) & 255] ^ (this.crc >>> 8);
            i++;
        }
    }

    @Override
    public byte[] finish() {
        long j = ~this.crc;
        this.crc = -1L;
        byte[] bArr = new byte[8];
        for (int i = 0; i < bArr.length; i++) {
            bArr[i] = (byte) (j >> (i * 8));
        }
        return bArr;
    }
}
