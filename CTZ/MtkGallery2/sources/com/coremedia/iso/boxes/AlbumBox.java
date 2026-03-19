package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;

public class AlbumBox extends AbstractFullBox {
    private String albumTitle;
    private String language;
    private int trackNumber;

    public AlbumBox() {
        super("albm");
    }

    public String getLanguage() {
        return this.language;
    }

    public String getAlbumTitle() {
        return this.albumTitle;
    }

    public int getTrackNumber() {
        return this.trackNumber;
    }

    @Override
    protected long getContentSize() {
        return 6 + Utf8.utf8StringLengthInBytes(this.albumTitle) + 1 + (this.trackNumber == -1 ? 0 : 1);
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        this.language = IsoTypeReader.readIso639(byteBuffer);
        this.albumTitle = IsoTypeReader.readString(byteBuffer);
        if (byteBuffer.remaining() > 0) {
            this.trackNumber = IsoTypeReader.readUInt8(byteBuffer);
        } else {
            this.trackNumber = -1;
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeIso639(byteBuffer, this.language);
        byteBuffer.put(Utf8.convert(this.albumTitle));
        byteBuffer.put((byte) 0);
        if (this.trackNumber != -1) {
            IsoTypeWriter.writeUInt8(byteBuffer, this.trackNumber);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AlbumBox[language=");
        sb.append(getLanguage());
        sb.append(";");
        sb.append("albumTitle=");
        sb.append(getAlbumTitle());
        if (this.trackNumber >= 0) {
            sb.append(";trackNumber=");
            sb.append(getTrackNumber());
        }
        sb.append("]");
        return sb.toString();
    }
}
