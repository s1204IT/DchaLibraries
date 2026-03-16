package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.util.CastUtils;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SampleAuxiliaryInformationSizesBox extends AbstractFullBox {
    static final boolean $assertionsDisabled;
    private String auxInfoType;
    private String auxInfoTypeParameter;
    private int defaultSampleInfoSize;
    private int sampleCount;
    private List<Short> sampleInfoSizes;

    static {
        $assertionsDisabled = !SampleAuxiliaryInformationSizesBox.class.desiredAssertionStatus();
    }

    public SampleAuxiliaryInformationSizesBox() {
        super("saiz");
        this.sampleInfoSizes = new LinkedList();
    }

    @Override
    protected long getContentSize() {
        int size = 4;
        if ((getFlags() & 1) == 1) {
            size = 4 + 8;
        }
        return size + 5 + (this.defaultSampleInfoSize == 0 ? this.sampleInfoSizes.size() : 0);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        if ((getFlags() & 1) == 1) {
            byteBuffer.put(IsoFile.fourCCtoBytes(this.auxInfoType));
            byteBuffer.put(IsoFile.fourCCtoBytes(this.auxInfoTypeParameter));
        }
        IsoTypeWriter.writeUInt8(byteBuffer, this.defaultSampleInfoSize);
        if (this.defaultSampleInfoSize == 0) {
            IsoTypeWriter.writeUInt32(byteBuffer, this.sampleInfoSizes.size());
            Iterator<Short> it = this.sampleInfoSizes.iterator();
            while (it.hasNext()) {
                short sampleInfoSize = it.next().shortValue();
                IsoTypeWriter.writeUInt8(byteBuffer, sampleInfoSize);
            }
            return;
        }
        IsoTypeWriter.writeUInt32(byteBuffer, this.sampleCount);
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        if ((getFlags() & 1) == 1) {
            this.auxInfoType = IsoTypeReader.read4cc(content);
            this.auxInfoTypeParameter = IsoTypeReader.read4cc(content);
        }
        this.defaultSampleInfoSize = (short) IsoTypeReader.readUInt8(content);
        this.sampleCount = CastUtils.l2i(IsoTypeReader.readUInt32(content));
        this.sampleInfoSizes.clear();
        if (this.defaultSampleInfoSize == 0) {
            for (int i = 0; i < this.sampleCount; i++) {
                this.sampleInfoSizes.add(Short.valueOf((short) IsoTypeReader.readUInt8(content)));
            }
        }
    }

    public String toString() {
        return "SampleAuxiliaryInformationSizesBox{defaultSampleInfoSize=" + this.defaultSampleInfoSize + ", sampleCount=" + this.sampleCount + ", auxInfoType='" + this.auxInfoType + "', auxInfoTypeParameter='" + this.auxInfoTypeParameter + "'}";
    }
}
