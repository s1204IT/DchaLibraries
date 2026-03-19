package com.googlecode.mp4parser.boxes.adobe;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.sampleentry.SampleEntry;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class ActionMessageFormat0SampleEntryBox extends SampleEntry {
    public ActionMessageFormat0SampleEntryBox() {
        super("amf0");
    }

    @Override
    protected long getContentSize() {
        Iterator<Box> it = this.boxes.iterator();
        long size = 8;
        while (it.hasNext()) {
            size += it.next().getSize();
        }
        return size;
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        _parseReservedAndDataReferenceIndex(byteBuffer);
        _parseChildBoxes(byteBuffer);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        _writeReservedAndDataReferenceIndex(byteBuffer);
        _writeChildBoxes(byteBuffer);
    }
}
