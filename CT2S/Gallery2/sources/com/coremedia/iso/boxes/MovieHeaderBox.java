package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;

public class MovieHeaderBox extends AbstractFullBox {
    private long creationTime;
    private int currentTime;
    private long duration;
    private long[] matrix;
    private long modificationTime;
    private long nextTrackId;
    private int posterTime;
    private int previewDuration;
    private int previewTime;
    private double rate;
    private int selectionDuration;
    private int selectionTime;
    private long timescale;
    private float volume;

    public MovieHeaderBox() {
        super("mvhd");
        this.rate = 1.0d;
        this.volume = 1.0f;
        this.matrix = new long[]{65536, 0, 0, 0, 65536, 0, 0, 0, 1073741824};
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

    public double getRate() {
        return this.rate;
    }

    public float getVolume() {
        return this.volume;
    }

    public long getNextTrackId() {
        return this.nextTrackId;
    }

    @Override
    protected long getContentSize() {
        long contentSize = getVersion() == 1 ? 4 + 28 : 4 + 16;
        return contentSize + 80;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        if (getVersion() == 1) {
            this.creationTime = IsoTypeReader.readUInt64(content);
            this.modificationTime = IsoTypeReader.readUInt64(content);
            this.timescale = IsoTypeReader.readUInt32(content);
            this.duration = IsoTypeReader.readUInt64(content);
        } else {
            this.creationTime = IsoTypeReader.readUInt32(content);
            this.modificationTime = IsoTypeReader.readUInt32(content);
            this.timescale = IsoTypeReader.readUInt32(content);
            this.duration = IsoTypeReader.readUInt32(content);
        }
        this.rate = IsoTypeReader.readFixedPoint1616(content);
        this.volume = IsoTypeReader.readFixedPoint88(content);
        IsoTypeReader.readUInt16(content);
        IsoTypeReader.readUInt32(content);
        IsoTypeReader.readUInt32(content);
        this.matrix = new long[9];
        for (int i = 0; i < 9; i++) {
            this.matrix[i] = IsoTypeReader.readUInt32(content);
        }
        this.previewTime = content.getInt();
        this.previewDuration = content.getInt();
        this.posterTime = content.getInt();
        this.selectionTime = content.getInt();
        this.selectionDuration = content.getInt();
        this.currentTime = content.getInt();
        this.nextTrackId = IsoTypeReader.readUInt32(content);
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("MovieHeaderBox[");
        result.append("creationTime=").append(getCreationTime());
        result.append(";");
        result.append("modificationTime=").append(getModificationTime());
        result.append(";");
        result.append("timescale=").append(getTimescale());
        result.append(";");
        result.append("duration=").append(getDuration());
        result.append(";");
        result.append("rate=").append(getRate());
        result.append(";");
        result.append("volume=").append(getVolume());
        for (int i = 0; i < this.matrix.length; i++) {
            result.append(";");
            result.append("matrix").append(i).append("=").append(this.matrix[i]);
        }
        result.append(";");
        result.append("nextTrackId=").append(getNextTrackId());
        result.append("]");
        return result.toString();
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
        IsoTypeWriter.writeFixedPont1616(byteBuffer, this.rate);
        IsoTypeWriter.writeFixedPont88(byteBuffer, this.volume);
        IsoTypeWriter.writeUInt16(byteBuffer, 0);
        IsoTypeWriter.writeUInt32(byteBuffer, 0L);
        IsoTypeWriter.writeUInt32(byteBuffer, 0L);
        for (int i = 0; i < 9; i++) {
            IsoTypeWriter.writeUInt32(byteBuffer, this.matrix[i]);
        }
        byteBuffer.putInt(this.previewTime);
        byteBuffer.putInt(this.previewDuration);
        byteBuffer.putInt(this.posterTime);
        byteBuffer.putInt(this.selectionTime);
        byteBuffer.putInt(this.selectionDuration);
        byteBuffer.putInt(this.currentTime);
        IsoTypeWriter.writeUInt32(byteBuffer, this.nextTrackId);
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public void setModificationTime(long modificationTime) {
        this.modificationTime = modificationTime;
    }

    public void setTimescale(long timescale) {
        this.timescale = timescale;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setNextTrackId(long nextTrackId) {
        this.nextTrackId = nextTrackId;
    }
}
