package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;

public class GenreBox extends AbstractFullBox {
    private String genre;
    private String language;

    public GenreBox() {
        super("gnre");
    }

    public String getLanguage() {
        return this.language;
    }

    public String getGenre() {
        return this.genre;
    }

    @Override
    protected long getContentSize() {
        return 7 + Utf8.utf8StringLengthInBytes(this.genre);
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        this.language = IsoTypeReader.readIso639(byteBuffer);
        this.genre = IsoTypeReader.readString(byteBuffer);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeIso639(byteBuffer, this.language);
        byteBuffer.put(Utf8.convert(this.genre));
        byteBuffer.put((byte) 0);
    }

    public String toString() {
        return "GenreBox[language=" + getLanguage() + ";genre=" + getGenre() + "]";
    }
}
