package com.coremedia.iso.boxes.sampleentry;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.Utf8;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;
import java.nio.ByteBuffer;

public class VisualSampleEntry extends SampleEntry implements ContainerBox {
    static final boolean $assertionsDisabled;
    private String compressorname;
    private int depth;
    private int frameCount;
    private int height;
    private double horizresolution;
    private long[] predefined;
    private double vertresolution;
    private int width;

    static {
        $assertionsDisabled = !VisualSampleEntry.class.desiredAssertionStatus();
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public double getHorizresolution() {
        return this.horizresolution;
    }

    public double getVertresolution() {
        return this.vertresolution;
    }

    public int getFrameCount() {
        return this.frameCount;
    }

    public String getCompressorname() {
        return this.compressorname;
    }

    public int getDepth() {
        return this.depth;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        _parseReservedAndDataReferenceIndex(content);
        long tmp = IsoTypeReader.readUInt16(content);
        if (!$assertionsDisabled && 0 != tmp) {
            throw new AssertionError("reserved byte not 0");
        }
        long tmp2 = IsoTypeReader.readUInt16(content);
        if (!$assertionsDisabled && 0 != tmp2) {
            throw new AssertionError("reserved byte not 0");
        }
        this.predefined[0] = IsoTypeReader.readUInt32(content);
        this.predefined[1] = IsoTypeReader.readUInt32(content);
        this.predefined[2] = IsoTypeReader.readUInt32(content);
        this.width = IsoTypeReader.readUInt16(content);
        this.height = IsoTypeReader.readUInt16(content);
        this.horizresolution = IsoTypeReader.readFixedPoint1616(content);
        this.vertresolution = IsoTypeReader.readFixedPoint1616(content);
        long tmp3 = IsoTypeReader.readUInt32(content);
        if (!$assertionsDisabled && 0 != tmp3) {
            throw new AssertionError("reserved byte not 0");
        }
        this.frameCount = IsoTypeReader.readUInt16(content);
        int compressornameDisplayAbleData = IsoTypeReader.readUInt8(content);
        if (compressornameDisplayAbleData > 31) {
            System.out.println("invalid compressor name displayable data: " + compressornameDisplayAbleData);
            compressornameDisplayAbleData = 31;
        }
        byte[] bytes = new byte[compressornameDisplayAbleData];
        content.get(bytes);
        this.compressorname = Utf8.convert(bytes);
        if (compressornameDisplayAbleData < 31) {
            byte[] zeros = new byte[31 - compressornameDisplayAbleData];
            content.get(zeros);
        }
        this.depth = IsoTypeReader.readUInt16(content);
        long tmp4 = IsoTypeReader.readUInt16(content);
        if (!$assertionsDisabled && 65535 != tmp4) {
            throw new AssertionError();
        }
        _parseChildBoxes(content);
    }

    @Override
    protected long getContentSize() {
        long contentSize = 78;
        for (Box boxe : this.boxes) {
            contentSize += boxe.getSize();
        }
        return contentSize;
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        _writeReservedAndDataReferenceIndex(byteBuffer);
        IsoTypeWriter.writeUInt16(byteBuffer, 0);
        IsoTypeWriter.writeUInt16(byteBuffer, 0);
        IsoTypeWriter.writeUInt32(byteBuffer, this.predefined[0]);
        IsoTypeWriter.writeUInt32(byteBuffer, this.predefined[1]);
        IsoTypeWriter.writeUInt32(byteBuffer, this.predefined[2]);
        IsoTypeWriter.writeUInt16(byteBuffer, getWidth());
        IsoTypeWriter.writeUInt16(byteBuffer, getHeight());
        IsoTypeWriter.writeFixedPont1616(byteBuffer, getHorizresolution());
        IsoTypeWriter.writeFixedPont1616(byteBuffer, getVertresolution());
        IsoTypeWriter.writeUInt32(byteBuffer, 0L);
        IsoTypeWriter.writeUInt16(byteBuffer, getFrameCount());
        IsoTypeWriter.writeUInt8(byteBuffer, Utf8.utf8StringLengthInBytes(getCompressorname()));
        byteBuffer.put(Utf8.convert(getCompressorname()));
        int a = Utf8.utf8StringLengthInBytes(getCompressorname());
        while (a < 31) {
            a++;
            byteBuffer.put((byte) 0);
        }
        IsoTypeWriter.writeUInt16(byteBuffer, getDepth());
        IsoTypeWriter.writeUInt16(byteBuffer, 65535);
        _writeChildBoxes(byteBuffer);
    }
}
