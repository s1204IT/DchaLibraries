package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.util.CastUtils;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class SampleAuxiliaryInformationOffsetsBox extends AbstractFullBox {
    private long auxInfoType;
    private long auxInfoTypeParameter;
    private List<Long> offsets;

    public SampleAuxiliaryInformationOffsetsBox() {
        super("saio");
        this.offsets = new LinkedList();
    }

    @Override
    protected long getContentSize() {
        return (getVersion() == 0 ? 4 * this.offsets.size() : this.offsets.size() * 8) + 8 + ((getFlags() & 1) != 1 ? 0 : 8);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        if ((getFlags() & 1) == 1) {
            IsoTypeWriter.writeUInt32(byteBuffer, this.auxInfoType);
            IsoTypeWriter.writeUInt32(byteBuffer, this.auxInfoTypeParameter);
        }
        IsoTypeWriter.writeUInt32(byteBuffer, this.offsets.size());
        for (Long l : this.offsets) {
            if (getVersion() == 0) {
                IsoTypeWriter.writeUInt32(byteBuffer, l.longValue());
            } else {
                IsoTypeWriter.writeUInt64(byteBuffer, l.longValue());
            }
        }
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        if ((getFlags() & 1) == 1) {
            this.auxInfoType = IsoTypeReader.readUInt32(byteBuffer);
            this.auxInfoTypeParameter = IsoTypeReader.readUInt32(byteBuffer);
        }
        int iL2i = CastUtils.l2i(IsoTypeReader.readUInt32(byteBuffer));
        this.offsets.clear();
        for (int i = 0; i < iL2i; i++) {
            if (getVersion() == 0) {
                this.offsets.add(Long.valueOf(IsoTypeReader.readUInt32(byteBuffer)));
            } else {
                this.offsets.add(Long.valueOf(IsoTypeReader.readUInt64(byteBuffer)));
            }
        }
    }
}
