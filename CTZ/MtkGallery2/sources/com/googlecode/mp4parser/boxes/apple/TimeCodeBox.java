package com.googlecode.mp4parser.boxes.apple;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.sampleentry.SampleEntry;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class TimeCodeBox extends SampleEntry {
    byte[] data;

    public TimeCodeBox() {
        super("tmcd");
    }

    @Override
    protected long getContentSize() {
        Iterator<Box> it = this.boxes.iterator();
        long size = 26;
        while (it.hasNext()) {
            size += it.next().getSize();
        }
        return size;
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        _parseReservedAndDataReferenceIndex(byteBuffer);
        this.data = new byte[18];
        byteBuffer.get(this.data);
        _parseChildBoxes(byteBuffer);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        _writeReservedAndDataReferenceIndex(byteBuffer);
        byteBuffer.put(this.data);
        _writeChildBoxes(byteBuffer);
    }
}
