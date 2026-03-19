package com.googlecode.mp4parser.boxes.mp4.objectdescriptors;

import com.coremedia.iso.Hex;
import com.coremedia.iso.IsoTypeReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

@Descriptor(tags = {4})
public class DecoderConfigDescriptor extends BaseDescriptor {
    private static Logger log = Logger.getLogger(DecoderConfigDescriptor.class.getName());
    AudioSpecificConfig audioSpecificInfo;
    long avgBitRate;
    int bufferSizeDB;
    byte[] configDescriptorDeadBytes;
    DecoderSpecificInfo decoderSpecificInfo;
    long maxBitRate;
    int objectTypeIndication;
    List<ProfileLevelIndicationDescriptor> profileLevelIndicationDescriptors = new ArrayList();
    int streamType;
    int upStream;

    @Override
    public void parseDetail(ByteBuffer byteBuffer) throws IOException {
        int size;
        this.objectTypeIndication = IsoTypeReader.readUInt8(byteBuffer);
        int uInt8 = IsoTypeReader.readUInt8(byteBuffer);
        this.streamType = uInt8 >>> 2;
        this.upStream = (uInt8 >> 1) & 1;
        this.bufferSizeDB = IsoTypeReader.readUInt24(byteBuffer);
        this.maxBitRate = IsoTypeReader.readUInt32(byteBuffer);
        this.avgBitRate = IsoTypeReader.readUInt32(byteBuffer);
        if (byteBuffer.remaining() > 2) {
            int iPosition = byteBuffer.position();
            ?? CreateFrom = ObjectDescriptorFactory.createFrom(this.objectTypeIndication, byteBuffer);
            int iPosition2 = byteBuffer.position() - iPosition;
            Logger logger = log;
            StringBuilder sb = new StringBuilder();
            sb.append((Object) CreateFrom);
            sb.append(" - DecoderConfigDescr1 read: ");
            sb.append(iPosition2);
            sb.append(", size: ");
            sb.append(CreateFrom != 0 ? Integer.valueOf(CreateFrom.getSize()) : null);
            logger.finer(sb.toString());
            if (CreateFrom != 0 && iPosition2 < (size = CreateFrom.getSize())) {
                this.configDescriptorDeadBytes = new byte[size - iPosition2];
                byteBuffer.get(this.configDescriptorDeadBytes);
            }
            if (CreateFrom instanceof DecoderSpecificInfo) {
                this.decoderSpecificInfo = CreateFrom;
            }
            if (CreateFrom instanceof AudioSpecificConfig) {
                this.audioSpecificInfo = CreateFrom;
            }
        }
        while (byteBuffer.remaining() > 2) {
            long jPosition = byteBuffer.position();
            BaseDescriptor baseDescriptorCreateFrom = ObjectDescriptorFactory.createFrom(this.objectTypeIndication, byteBuffer);
            long jPosition2 = ((long) byteBuffer.position()) - jPosition;
            Logger logger2 = log;
            StringBuilder sb2 = new StringBuilder();
            sb2.append(baseDescriptorCreateFrom);
            sb2.append(" - DecoderConfigDescr2 read: ");
            sb2.append(jPosition2);
            sb2.append(", size: ");
            sb2.append(baseDescriptorCreateFrom != null ? Integer.valueOf(baseDescriptorCreateFrom.getSize()) : null);
            logger2.finer(sb2.toString());
            if (baseDescriptorCreateFrom instanceof ProfileLevelIndicationDescriptor) {
                this.profileLevelIndicationDescriptors.add((ProfileLevelIndicationDescriptor) baseDescriptorCreateFrom);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DecoderConfigDescriptor");
        sb.append("{objectTypeIndication=");
        sb.append(this.objectTypeIndication);
        sb.append(", streamType=");
        sb.append(this.streamType);
        sb.append(", upStream=");
        sb.append(this.upStream);
        sb.append(", bufferSizeDB=");
        sb.append(this.bufferSizeDB);
        sb.append(", maxBitRate=");
        sb.append(this.maxBitRate);
        sb.append(", avgBitRate=");
        sb.append(this.avgBitRate);
        sb.append(", decoderSpecificInfo=");
        sb.append(this.decoderSpecificInfo);
        sb.append(", audioSpecificInfo=");
        sb.append(this.audioSpecificInfo);
        sb.append(", configDescriptorDeadBytes=");
        sb.append(Hex.encodeHex(this.configDescriptorDeadBytes != null ? this.configDescriptorDeadBytes : new byte[0]));
        sb.append(", profileLevelIndicationDescriptors=");
        sb.append(this.profileLevelIndicationDescriptors == null ? "null" : Arrays.asList(this.profileLevelIndicationDescriptors).toString());
        sb.append('}');
        return sb.toString();
    }
}
