package com.coremedia.iso.boxes.sampleentry;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MpegSampleEntry extends SampleEntry implements ContainerBox {
    @Override
    public void _parseDetails(ByteBuffer content) {
        _parseReservedAndDataReferenceIndex(content);
        _parseChildBoxes(content);
    }

    @Override
    protected long getContentSize() {
        long contentSize = 8;
        for (Box boxe : this.boxes) {
            contentSize += boxe.getSize();
        }
        return contentSize;
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
