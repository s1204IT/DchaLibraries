package com.googlecode.mp4parser.boxes.mp4.samplegrouping;

import com.coremedia.iso.Hex;
import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class CencSampleEncryptionInformationGroupEntry extends GroupEntry {
    static final boolean $assertionsDisabled = false;
    private int isEncrypted;
    private byte ivSize;
    private byte[] kid = new byte[16];

    @Override
    public void parse(ByteBuffer byteBuffer) {
        this.isEncrypted = IsoTypeReader.readUInt24(byteBuffer);
        this.ivSize = (byte) IsoTypeReader.readUInt8(byteBuffer);
        this.kid = new byte[16];
        byteBuffer.get(this.kid);
    }

    @Override
    public ByteBuffer get() {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(20);
        IsoTypeWriter.writeUInt24(byteBufferAllocate, this.isEncrypted);
        IsoTypeWriter.writeUInt8(byteBufferAllocate, this.ivSize);
        byteBufferAllocate.put(this.kid);
        byteBufferAllocate.rewind();
        return byteBufferAllocate;
    }

    public String toString() {
        return "CencSampleEncryptionInformationGroupEntry{isEncrypted=" + this.isEncrypted + ", ivSize=" + ((int) this.ivSize) + ", kid=" + Hex.encodeHex(this.kid) + '}';
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CencSampleEncryptionInformationGroupEntry cencSampleEncryptionInformationGroupEntry = (CencSampleEncryptionInformationGroupEntry) obj;
        if (this.isEncrypted == cencSampleEncryptionInformationGroupEntry.isEncrypted && this.ivSize == cencSampleEncryptionInformationGroupEntry.ivSize && Arrays.equals(this.kid, cencSampleEncryptionInformationGroupEntry.kid)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * ((this.isEncrypted * 31) + this.ivSize)) + (this.kid != null ? Arrays.hashCode(this.kid) : 0);
    }
}
