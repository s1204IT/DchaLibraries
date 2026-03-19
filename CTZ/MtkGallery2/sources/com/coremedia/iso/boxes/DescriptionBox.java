package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;

public class DescriptionBox extends AbstractFullBox {
    private String description;
    private String language;

    public DescriptionBox() {
        super("dscp");
    }

    public String getLanguage() {
        return this.language;
    }

    public String getDescription() {
        return this.description;
    }

    @Override
    protected long getContentSize() {
        return 7 + Utf8.utf8StringLengthInBytes(this.description);
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        this.language = IsoTypeReader.readIso639(byteBuffer);
        this.description = IsoTypeReader.readString(byteBuffer);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeIso639(byteBuffer, this.language);
        byteBuffer.put(Utf8.convert(this.description));
        byteBuffer.put((byte) 0);
    }

    public String toString() {
        return "DescriptionBox[language=" + getLanguage() + ";description=" + getDescription() + "]";
    }
}
