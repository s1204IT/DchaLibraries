package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;

public class MediaHeaderBox extends AbstractFullBox {
    private long creationTime;
    private long duration;
    private String language;
    private long modificationTime;
    private long timescale;

    public MediaHeaderBox() {
        super("mdhd");
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public long getModificationTime() {
        return this.modificationTime;
    }

    public long getTimescale() {
        return this.timescale;
    }

    public long getDuration() {
        return this.duration;
    }

    public String getLanguage() {
        return this.language;
    }

    @Override
    protected long getContentSize() {
        long j;
        if (getVersion() == 1) {
            j = 32;
        } else {
            j = 20;
        }
        return j + 2 + 2;
    }

    public void setCreationTime(long j) {
        this.creationTime = j;
    }

    public void setTimescale(long j) {
        this.timescale = j;
    }

    public void setDuration(long j) {
        this.duration = j;
    }

    public void setLanguage(String str) {
        this.language = str;
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        if (getVersion() == 1) {
            this.creationTime = IsoTypeReader.readUInt64(byteBuffer);
            this.modificationTime = IsoTypeReader.readUInt64(byteBuffer);
            this.timescale = IsoTypeReader.readUInt32(byteBuffer);
            this.duration = IsoTypeReader.readUInt64(byteBuffer);
        } else {
            this.creationTime = IsoTypeReader.readUInt32(byteBuffer);
            this.modificationTime = IsoTypeReader.readUInt32(byteBuffer);
            this.timescale = IsoTypeReader.readUInt32(byteBuffer);
            this.duration = IsoTypeReader.readUInt32(byteBuffer);
        }
        this.language = IsoTypeReader.readIso639(byteBuffer);
        IsoTypeReader.readUInt16(byteBuffer);
    }

    public String toString() {
        return "MediaHeaderBox[creationTime=" + getCreationTime() + ";modificationTime=" + getModificationTime() + ";timescale=" + getTimescale() + ";duration=" + getDuration() + ";language=" + getLanguage() + "]";
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        if (getVersion() == 1) {
            IsoTypeWriter.writeUInt64(byteBuffer, this.creationTime);
            IsoTypeWriter.writeUInt64(byteBuffer, this.modificationTime);
            IsoTypeWriter.writeUInt32(byteBuffer, this.timescale);
            IsoTypeWriter.writeUInt64(byteBuffer, this.duration);
        } else {
            IsoTypeWriter.writeUInt32(byteBuffer, this.creationTime);
            IsoTypeWriter.writeUInt32(byteBuffer, this.modificationTime);
            IsoTypeWriter.writeUInt32(byteBuffer, this.timescale);
            IsoTypeWriter.writeUInt32(byteBuffer, this.duration);
        }
        IsoTypeWriter.writeIso639(byteBuffer, this.language);
        IsoTypeWriter.writeUInt16(byteBuffer, 0);
    }
}
