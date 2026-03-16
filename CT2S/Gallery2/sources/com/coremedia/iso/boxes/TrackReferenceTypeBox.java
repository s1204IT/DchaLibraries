package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractBox;
import java.nio.ByteBuffer;

public class TrackReferenceTypeBox extends AbstractBox {
    private long[] trackIds;

    @Override
    public void _parseDetails(ByteBuffer content) {
        int count = content.remaining() / 4;
        this.trackIds = new long[count];
        for (int i = 0; i < count; i++) {
            this.trackIds[i] = IsoTypeReader.readUInt32(content);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        long[] arr$ = this.trackIds;
        for (long trackId : arr$) {
            IsoTypeWriter.writeUInt32(byteBuffer, trackId);
        }
    }

    @Override
    protected long getContentSize() {
        return this.trackIds.length * 4;
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("TrackReferenceTypeBox[type=").append(getType());
        for (int i = 0; i < this.trackIds.length; i++) {
            buffer.append(";trackId");
            buffer.append(i);
            buffer.append("=");
            buffer.append(this.trackIds[i]);
        }
        buffer.append("]");
        return buffer.toString();
    }
}
