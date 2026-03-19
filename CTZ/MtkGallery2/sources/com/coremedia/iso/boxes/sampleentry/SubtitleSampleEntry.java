package com.coremedia.iso.boxes.sampleentry;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import java.nio.ByteBuffer;

public class SubtitleSampleEntry extends SampleEntry {
    private String imageMimeType;
    private String namespace;
    private String schemaLocation;

    @Override
    protected long getContentSize() {
        return 8 + this.namespace.length() + this.schemaLocation.length() + this.imageMimeType.length() + 3;
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        _parseReservedAndDataReferenceIndex(byteBuffer);
        this.namespace = IsoTypeReader.readString(byteBuffer);
        this.schemaLocation = IsoTypeReader.readString(byteBuffer);
        this.imageMimeType = IsoTypeReader.readString(byteBuffer);
        _parseChildBoxes(byteBuffer);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        _writeReservedAndDataReferenceIndex(byteBuffer);
        IsoTypeWriter.writeUtf8String(byteBuffer, this.namespace);
        IsoTypeWriter.writeUtf8String(byteBuffer, this.schemaLocation);
        IsoTypeWriter.writeUtf8String(byteBuffer, this.imageMimeType);
    }
}
