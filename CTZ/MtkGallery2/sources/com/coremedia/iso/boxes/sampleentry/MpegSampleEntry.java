package com.coremedia.iso.boxes.sampleentry;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;

public class MpegSampleEntry extends SampleEntry implements ContainerBox {
    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        _parseReservedAndDataReferenceIndex(byteBuffer);
        _parseChildBoxes(byteBuffer);
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

    public String toString() {
        return "MpegSampleEntry" + Arrays.asList(getBoxes());
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        _writeReservedAndDataReferenceIndex(byteBuffer);
        _writeChildBoxes(byteBuffer);
    }
}
