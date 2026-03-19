package com.coremedia.iso.boxes.apple;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.util.CastUtils;
import java.nio.ByteBuffer;

public class AppleDataReferenceBox extends AbstractFullBox {
    private String dataReference;
    private int dataReferenceSize;
    private String dataReferenceType;

    public AppleDataReferenceBox() {
        super("rdrf");
    }

    @Override
    protected long getContentSize() {
        return 12 + this.dataReferenceSize;
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        this.dataReferenceType = IsoTypeReader.read4cc(byteBuffer);
        this.dataReferenceSize = CastUtils.l2i(IsoTypeReader.readUInt32(byteBuffer));
        this.dataReference = IsoTypeReader.readString(byteBuffer, this.dataReferenceSize);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        byteBuffer.put(IsoFile.fourCCtoBytes(this.dataReferenceType));
        IsoTypeWriter.writeUInt32(byteBuffer, this.dataReferenceSize);
        byteBuffer.put(Utf8.convert(this.dataReference));
    }
}
