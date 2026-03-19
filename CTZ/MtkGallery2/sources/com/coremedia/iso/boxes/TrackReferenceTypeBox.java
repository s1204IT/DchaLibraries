package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractBox;
import java.nio.ByteBuffer;

public class TrackReferenceTypeBox extends AbstractBox {
    private long[] trackIds;

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        int iRemaining = byteBuffer.remaining() / 4;
        this.trackIds = new long[iRemaining];
        for (int i = 0; i < iRemaining; i++) {
            this.trackIds[i] = IsoTypeReader.readUInt32(byteBuffer);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        for (long j : this.trackIds) {
            IsoTypeWriter.writeUInt32(byteBuffer, j);
        }
    }

    @Override
    protected long getContentSize() {
        return this.trackIds.length * 4;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TrackReferenceTypeBox[type=");
        sb.append(getType());
        for (int i = 0; i < this.trackIds.length; i++) {
            sb.append(";trackId");
            sb.append(i);
            sb.append("=");
            sb.append(this.trackIds[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
