package com.coremedia.iso.boxes.sampleentry;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.Utf8;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class VisualSampleEntry extends SampleEntry implements ContainerBox {
    static final boolean $assertionsDisabled = false;
    private String compressorname;
    private int depth;
    private int frameCount;
    private int height;
    private double horizresolution;
    private long[] predefined;
    private double vertresolution;
    private int width;

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
    public void _parseDetails(ByteBuffer byteBuffer) {
        _parseReservedAndDataReferenceIndex(byteBuffer);
        IsoTypeReader.readUInt16(byteBuffer);
        IsoTypeReader.readUInt16(byteBuffer);
        this.predefined[0] = IsoTypeReader.readUInt32(byteBuffer);
        this.predefined[1] = IsoTypeReader.readUInt32(byteBuffer);
        this.predefined[2] = IsoTypeReader.readUInt32(byteBuffer);
        this.width = IsoTypeReader.readUInt16(byteBuffer);
        this.height = IsoTypeReader.readUInt16(byteBuffer);
        this.horizresolution = IsoTypeReader.readFixedPoint1616(byteBuffer);
        this.vertresolution = IsoTypeReader.readFixedPoint1616(byteBuffer);
        IsoTypeReader.readUInt32(byteBuffer);
        this.frameCount = IsoTypeReader.readUInt16(byteBuffer);
        int uInt8 = IsoTypeReader.readUInt8(byteBuffer);
        if (uInt8 > 31) {
            System.out.println("invalid compressor name displayable data: " + uInt8);
            uInt8 = 31;
        }
        byte[] bArr = new byte[uInt8];
        byteBuffer.get(bArr);
        this.compressorname = Utf8.convert(bArr);
        if (uInt8 < 31) {
            byteBuffer.get(new byte[31 - uInt8]);
        }
        this.depth = IsoTypeReader.readUInt16(byteBuffer);
        IsoTypeReader.readUInt16(byteBuffer);
        _parseChildBoxes(byteBuffer);
    }

    @Override
    protected long getContentSize() {
        Iterator<Box> it = this.boxes.iterator();
        long size = 78;
        while (it.hasNext()) {
            size += it.next().getSize();
        }
        return size;
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
        int iUtf8StringLengthInBytes = Utf8.utf8StringLengthInBytes(getCompressorname());
        while (iUtf8StringLengthInBytes < 31) {
            iUtf8StringLengthInBytes++;
            byteBuffer.put((byte) 0);
        }
        IsoTypeWriter.writeUInt16(byteBuffer, getDepth());
        IsoTypeWriter.writeUInt16(byteBuffer, 65535);
        _writeChildBoxes(byteBuffer);
    }
}
