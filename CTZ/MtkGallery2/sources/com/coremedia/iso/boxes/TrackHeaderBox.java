package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;

public class TrackHeaderBox extends AbstractFullBox {
    private int alternateGroup;
    private long creationTime;
    private long duration;
    private double height;
    private int layer;
    private long[] matrix;
    private long modificationTime;
    private long trackId;
    private float volume;
    private double width;

    public TrackHeaderBox() {
        super("tkhd");
        this.matrix = new long[]{65536, 0, 0, 0, 65536, 0, 0, 0, 1073741824};
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public long getModificationTime() {
        return this.modificationTime;
    }

    public long getTrackId() {
        return this.trackId;
    }

    public long getDuration() {
        return this.duration;
    }

    public int getLayer() {
        return this.layer;
    }

    public int getAlternateGroup() {
        return this.alternateGroup;
    }

    public float getVolume() {
        return this.volume;
    }

    public long[] getMatrix() {
        return this.matrix;
    }

    public double getWidth() {
        return this.width;
    }

    public double getHeight() {
        return this.height;
    }

    @Override
    protected long getContentSize() {
        long j;
        if (getVersion() == 1) {
            j = 36;
        } else {
            j = 24;
        }
        return j + 60;
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        if (getVersion() == 1) {
            this.creationTime = IsoTypeReader.readUInt64(byteBuffer);
            this.modificationTime = IsoTypeReader.readUInt64(byteBuffer);
            this.trackId = IsoTypeReader.readUInt32(byteBuffer);
            IsoTypeReader.readUInt32(byteBuffer);
            this.duration = IsoTypeReader.readUInt64(byteBuffer);
        } else {
            this.creationTime = IsoTypeReader.readUInt32(byteBuffer);
            this.modificationTime = IsoTypeReader.readUInt32(byteBuffer);
            this.trackId = IsoTypeReader.readUInt32(byteBuffer);
            IsoTypeReader.readUInt32(byteBuffer);
            this.duration = IsoTypeReader.readUInt32(byteBuffer);
        }
        IsoTypeReader.readUInt32(byteBuffer);
        IsoTypeReader.readUInt32(byteBuffer);
        this.layer = IsoTypeReader.readUInt16(byteBuffer);
        this.alternateGroup = IsoTypeReader.readUInt16(byteBuffer);
        this.volume = IsoTypeReader.readFixedPoint88(byteBuffer);
        IsoTypeReader.readUInt16(byteBuffer);
        this.matrix = new long[9];
        for (int i = 0; i < 9; i++) {
            this.matrix[i] = IsoTypeReader.readUInt32(byteBuffer);
        }
        this.width = IsoTypeReader.readFixedPoint1616(byteBuffer);
        this.height = IsoTypeReader.readFixedPoint1616(byteBuffer);
    }

    @Override
    public void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        if (getVersion() == 1) {
            IsoTypeWriter.writeUInt64(byteBuffer, this.creationTime);
            IsoTypeWriter.writeUInt64(byteBuffer, this.modificationTime);
            IsoTypeWriter.writeUInt32(byteBuffer, this.trackId);
            IsoTypeWriter.writeUInt32(byteBuffer, 0L);
            IsoTypeWriter.writeUInt64(byteBuffer, this.duration);
        } else {
            IsoTypeWriter.writeUInt32(byteBuffer, this.creationTime);
            IsoTypeWriter.writeUInt32(byteBuffer, this.modificationTime);
            IsoTypeWriter.writeUInt32(byteBuffer, this.trackId);
            IsoTypeWriter.writeUInt32(byteBuffer, 0L);
            IsoTypeWriter.writeUInt32(byteBuffer, this.duration);
        }
        IsoTypeWriter.writeUInt32(byteBuffer, 0L);
        IsoTypeWriter.writeUInt32(byteBuffer, 0L);
        IsoTypeWriter.writeUInt16(byteBuffer, this.layer);
        IsoTypeWriter.writeUInt16(byteBuffer, this.alternateGroup);
        IsoTypeWriter.writeFixedPont88(byteBuffer, this.volume);
        IsoTypeWriter.writeUInt16(byteBuffer, 0);
        for (int i = 0; i < 9; i++) {
            IsoTypeWriter.writeUInt32(byteBuffer, this.matrix[i]);
        }
        IsoTypeWriter.writeFixedPont1616(byteBuffer, this.width);
        IsoTypeWriter.writeFixedPont1616(byteBuffer, this.height);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TrackHeaderBox[");
        sb.append("creationTime=");
        sb.append(getCreationTime());
        sb.append(";");
        sb.append("modificationTime=");
        sb.append(getModificationTime());
        sb.append(";");
        sb.append("trackId=");
        sb.append(getTrackId());
        sb.append(";");
        sb.append("duration=");
        sb.append(getDuration());
        sb.append(";");
        sb.append("layer=");
        sb.append(getLayer());
        sb.append(";");
        sb.append("alternateGroup=");
        sb.append(getAlternateGroup());
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
        sb.append("width=");
        sb.append(getWidth());
        sb.append(";");
        sb.append("height=");
        sb.append(getHeight());
        sb.append("]");
        return sb.toString();
    }

    public void setCreationTime(long j) {
        this.creationTime = j;
    }

    public void setModificationTime(long j) {
        this.modificationTime = j;
    }

    public void setTrackId(long j) {
        this.trackId = j;
    }

    public void setDuration(long j) {
        this.duration = j;
    }

    public void setLayer(int i) {
        this.layer = i;
    }

    public void setAlternateGroup(int i) {
        this.alternateGroup = i;
    }

    public void setVolume(float f) {
        this.volume = f;
    }

    public void setMatrix(long[] jArr) {
        this.matrix = jArr;
    }

    public void setWidth(double d) {
        this.width = d;
    }

    public void setHeight(double d) {
        this.height = d;
    }

    public boolean isEnabled() {
        return (getFlags() & 1) > 0;
    }

    public boolean isInMovie() {
        return (getFlags() & 2) > 0;
    }

    public boolean isInPreview() {
        return (getFlags() & 4) > 0;
    }

    public boolean isInPoster() {
        return (getFlags() & 8) > 0;
    }
}
