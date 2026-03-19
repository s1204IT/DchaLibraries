package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.IsoTypeReader;
import com.googlecode.mp4parser.AbstractBox;
import java.nio.ByteBuffer;

public class OriginalFormatBox extends AbstractBox {
    static final boolean $assertionsDisabled = false;
    private String dataFormat;

    public OriginalFormatBox() {
        super("frma");
        this.dataFormat = "    ";
    }

    public String getDataFormat() {
        return this.dataFormat;
    }

    @Override
    protected long getContentSize() {
        return 4L;
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        this.dataFormat = IsoTypeReader.read4cc(byteBuffer);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        byteBuffer.put(IsoFile.fourCCtoBytes(this.dataFormat));
    }

    public String toString() {
        return "OriginalFormatBox[dataFormat=" + getDataFormat() + "]";
    }
}
