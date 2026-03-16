package com.coremedia.iso.boxes.fragment;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;

public class TrackFragmentBaseMediaDecodeTimeBox extends AbstractFullBox {
    private long baseMediaDecodeTime;

    public TrackFragmentBaseMediaDecodeTimeBox() {
        super("tfdt");
    }

    @Override
    protected long getContentSize() {
        return getVersion() == 0 ? 8L : 12L;
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        if (getVersion() == 1) {
            IsoTypeWriter.writeUInt64(byteBuffer, this.baseMediaDecodeTime);
        } else {
            IsoTypeWriter.writeUInt32(byteBuffer, this.baseMediaDecodeTime);
        }
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        if (getVersion() == 1) {
            this.baseMediaDecodeTime = IsoTypeReader.readUInt64(content);
        } else {
            this.baseMediaDecodeTime = IsoTypeReader.readUInt32(content);
        }
    }

    public String toString() {
        return "TrackFragmentBaseMediaDecodeTimeBox{baseMediaDecodeTime=" + this.baseMediaDecodeTime + '}';
    }
}
