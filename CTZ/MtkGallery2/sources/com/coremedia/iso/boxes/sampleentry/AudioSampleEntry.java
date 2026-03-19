package com.coremedia.iso.boxes.sampleentry;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class AudioSampleEntry extends SampleEntry implements ContainerBox {
    private long bytesPerFrame;
    private long bytesPerPacket;
    private long bytesPerSample;
    private int channelCount;
    private int compressionId;
    private int packetSize;
    private int reserved1;
    private long reserved2;
    private long sampleRate;
    private int sampleSize;
    private long samplesPerPacket;
    private int soundVersion;
    private byte[] soundVersion2Data;

    public long getSampleRate() {
        return this.sampleRate;
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        _parseReservedAndDataReferenceIndex(byteBuffer);
        this.soundVersion = IsoTypeReader.readUInt16(byteBuffer);
        this.reserved1 = IsoTypeReader.readUInt16(byteBuffer);
        this.reserved2 = IsoTypeReader.readUInt32(byteBuffer);
        this.channelCount = IsoTypeReader.readUInt16(byteBuffer);
        this.sampleSize = IsoTypeReader.readUInt16(byteBuffer);
        this.compressionId = IsoTypeReader.readUInt16(byteBuffer);
        this.packetSize = IsoTypeReader.readUInt16(byteBuffer);
        this.sampleRate = IsoTypeReader.readUInt32(byteBuffer);
        if (!this.type.equals("mlpa")) {
            this.sampleRate >>>= 16;
        }
        if (this.soundVersion > 0) {
            this.samplesPerPacket = IsoTypeReader.readUInt32(byteBuffer);
            this.bytesPerPacket = IsoTypeReader.readUInt32(byteBuffer);
            this.bytesPerFrame = IsoTypeReader.readUInt32(byteBuffer);
            this.bytesPerSample = IsoTypeReader.readUInt32(byteBuffer);
        }
        if (this.soundVersion == 2) {
            this.soundVersion2Data = new byte[20];
            byteBuffer.get(20);
        }
        _parseChildBoxes(byteBuffer);
    }

    @Override
    protected long getContentSize() {
        long size = 28 + (this.soundVersion > 0 ? 16L : 0L) + (this.soundVersion == 2 ? 20L : 0L);
        Iterator<Box> it = this.boxes.iterator();
        while (it.hasNext()) {
            size += it.next().getSize();
        }
        return size;
    }

    public String toString() {
        return "AudioSampleEntry{bytesPerSample=" + this.bytesPerSample + ", bytesPerFrame=" + this.bytesPerFrame + ", bytesPerPacket=" + this.bytesPerPacket + ", samplesPerPacket=" + this.samplesPerPacket + ", packetSize=" + this.packetSize + ", compressionId=" + this.compressionId + ", soundVersion=" + this.soundVersion + ", sampleRate=" + this.sampleRate + ", sampleSize=" + this.sampleSize + ", channelCount=" + this.channelCount + ", boxes=" + getBoxes() + '}';
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        _writeReservedAndDataReferenceIndex(byteBuffer);
        IsoTypeWriter.writeUInt16(byteBuffer, this.soundVersion);
        IsoTypeWriter.writeUInt16(byteBuffer, this.reserved1);
        IsoTypeWriter.writeUInt32(byteBuffer, this.reserved2);
        IsoTypeWriter.writeUInt16(byteBuffer, this.channelCount);
        IsoTypeWriter.writeUInt16(byteBuffer, this.sampleSize);
        IsoTypeWriter.writeUInt16(byteBuffer, this.compressionId);
        IsoTypeWriter.writeUInt16(byteBuffer, this.packetSize);
        if (this.type.equals("mlpa")) {
            IsoTypeWriter.writeUInt32(byteBuffer, getSampleRate());
        } else {
            IsoTypeWriter.writeUInt32(byteBuffer, getSampleRate() << 16);
        }
        if (this.soundVersion > 0) {
            IsoTypeWriter.writeUInt32(byteBuffer, this.samplesPerPacket);
            IsoTypeWriter.writeUInt32(byteBuffer, this.bytesPerPacket);
            IsoTypeWriter.writeUInt32(byteBuffer, this.bytesPerFrame);
            IsoTypeWriter.writeUInt32(byteBuffer, this.bytesPerSample);
        }
        if (this.soundVersion == 2) {
            byteBuffer.put(this.soundVersion2Data);
        }
        _writeChildBoxes(byteBuffer);
    }
}
