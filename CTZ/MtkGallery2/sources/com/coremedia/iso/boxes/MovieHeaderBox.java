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
        long j;
        if (getVersion() == 1) {
            j = 32;
        } else {
            j = 20;
        }
        return j + 80;
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
        this.rate = IsoTypeReader.readFixedPoint1616(byteBuffer);
        this.volume = IsoTypeReader.readFixedPoint88(byteBuffer);
        IsoTypeReader.readUInt16(byteBuffer);
        IsoTypeReader.readUInt32(byteBuffer);
        IsoTypeReader.readUInt32(byteBuffer);
        this.matrix = new long[9];
        for (int i = 0; i < 9; i++) {
            this.matrix[i] = IsoTypeReader.readUInt32(byteBuffer);
        }
        this.previewTime = byteBuffer.getInt();
        this.previewDuration = byteBuffer.getInt();
        this.posterTime = byteBuffer.getInt();
        this.selectionTime = byteBuffer.getInt();
        this.selectionDuration = byteBuffer.getInt();
        this.currentTime = byteBuffer.getInt();
        this.nextTrackId = IsoTypeReader.readUInt32(byteBuffer);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MovieHeaderBox[");
        sb.append("creationTime=");
        sb.append(getCreationTime());
        sb.append(";");
        sb.append("modificationTime=");
        sb.append(getModificationTime());
        sb.append(";");
        sb.append("timescale=");
        sb.append(getTimescale());
        sb.append(";");
        sb.append("duration=");
        sb.append(getDuration());
        sb.append(";");
        sb.append("rate=");
        sb.append(getRate());
        sb.append(";");
        sb.append("volume=");
        sb.append(getVolume());
        for (int i = 0; i < this.matrix.length; i++) {
            sb.append(";");
            sb.append("matrix");
            sb.append(i);
            sb.append("=");
            sb.append(this.matrix[i]);
        }
        sb.append(";");
        sb.append("nextTrackId=");
        sb.append(getNextTrackId());
        sb.append("]");
        return sb.toString();
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

    public void setCreationTime(long j) {
        this.creationTime = j;
    }

    public void setModificationTime(long j) {
        this.modificationTime = j;
    }

    public void setTimescale(long j) {
        this.timescale = j;
    }

    public void setDuration(long j) {
        this.duration = j;
    }

    public void setNextTrackId(long j) {
        this.nextTrackId = j;
    }
}
