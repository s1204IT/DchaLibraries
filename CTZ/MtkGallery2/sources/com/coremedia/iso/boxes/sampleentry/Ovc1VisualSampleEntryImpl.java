package com.coremedia.iso.boxes.sampleentry;

import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.boxes.Box;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class Ovc1VisualSampleEntryImpl extends SampleEntry {
    private byte[] vc1Content;

    @Override
    protected long getContentSize() {
        Iterator<Box> it = this.boxes.iterator();
        long size = 8;
        while (it.hasNext()) {
            size += it.next().getSize();
        }
        return size + ((long) this.vc1Content.length);
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        _parseReservedAndDataReferenceIndex(byteBuffer);
        this.vc1Content = new byte[byteBuffer.remaining()];
        byteBuffer.get(this.vc1Content);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        byteBuffer.put(new byte[6]);
        IsoTypeWriter.writeUInt16(byteBuffer, getDataReferenceIndex());
        byteBuffer.put(this.vc1Content);
    }

    protected Ovc1VisualSampleEntryImpl() {
        super("ovc1");
    }
}
