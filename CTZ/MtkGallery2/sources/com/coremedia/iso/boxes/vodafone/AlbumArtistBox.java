package com.coremedia.iso.boxes.vodafone;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;

public class AlbumArtistBox extends AbstractFullBox {
    private String albumArtist;
    private String language;

    public AlbumArtistBox() {
        super("albr");
    }

    public String getLanguage() {
        return this.language;
    }

    public String getAlbumArtist() {
        return this.albumArtist;
    }

    @Override
    protected long getContentSize() {
        return 6 + Utf8.utf8StringLengthInBytes(this.albumArtist) + 1;
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        this.language = IsoTypeReader.readIso639(byteBuffer);
        this.albumArtist = IsoTypeReader.readString(byteBuffer);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeIso639(byteBuffer, this.language);
        byteBuffer.put(Utf8.convert(this.albumArtist));
        byteBuffer.put((byte) 0);
    }

    public String toString() {
        return "AlbumArtistBox[language=" + getLanguage() + ";albumArtist=" + getAlbumArtist() + "]";
    }
}
