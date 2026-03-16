package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.IsoTypeReader;
import com.googlecode.mp4parser.AbstractBox;
import java.nio.ByteBuffer;

public class OriginalFormatBox extends AbstractBox {
    static final boolean $assertionsDisabled;
    private String dataFormat;

    static {
        $assertionsDisabled = !OriginalFormatBox.class.desiredAssertionStatus();
    }

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
    public void _parseDetails(ByteBuffer content) {
        this.dataFormat = IsoTypeReader.read4cc(content);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        byteBuffer.put(IsoFile.fourCCtoBytes(this.dataFormat));
    }

    public String toString() {
        return "OriginalFormatBox[dataFormat=" + getDataFormat() + "]";
    }
}
