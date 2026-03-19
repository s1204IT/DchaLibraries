package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;

public class AuthorBox extends AbstractFullBox {
    private String author;
    private String language;

    public AuthorBox() {
        super("auth");
    }

    public String getLanguage() {
        return this.language;
    }

    public String getAuthor() {
        return this.author;
    }

    @Override
    protected long getContentSize() {
        return 7 + Utf8.utf8StringLengthInBytes(this.author);
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        this.language = IsoTypeReader.readIso639(byteBuffer);
        this.author = IsoTypeReader.readString(byteBuffer);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeIso639(byteBuffer, this.language);
        byteBuffer.put(Utf8.convert(this.author));
        byteBuffer.put((byte) 0);
    }

    public String toString() {
        return "AuthorBox[language=" + getLanguage() + ";author=" + getAuthor() + "]";
    }
}
