package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;

public class PerformerBox extends AbstractFullBox {
    private String language;
    private String performer;

    public PerformerBox() {
        super("perf");
    }

    public String getLanguage() {
        return this.language;
    }

    public String getPerformer() {
        return this.performer;
    }

    @Override
    protected long getContentSize() {
        return 6 + Utf8.utf8StringLengthInBytes(this.performer) + 1;
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeIso639(byteBuffer, this.language);
        byteBuffer.put(Utf8.convert(this.performer));
        byteBuffer.put((byte) 0);
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        this.language = IsoTypeReader.readIso639(byteBuffer);
        this.performer = IsoTypeReader.readString(byteBuffer);
    }

    public String toString() {
        return "PerformerBox[language=" + getLanguage() + ";performer=" + getPerformer() + "]";
    }
}
