package com.googlecode.mp4parser.boxes.mp4.objectdescriptors;

import com.coremedia.iso.IsoTypeReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Descriptor(tags = {3})
public class ESDescriptor extends BaseDescriptor {
    private static Logger log = Logger.getLogger(ESDescriptor.class.getName());
    int URLFlag;
    String URLString;
    DecoderConfigDescriptor decoderConfigDescriptor;
    int dependsOnEsId;
    int esId;
    int oCREsId;
    int oCRstreamFlag;
    int remoteODFlag;
    SLConfigDescriptor slConfigDescriptor;
    int streamDependenceFlag;
    int streamPriority;
    int URLLength = 0;
    List<BaseDescriptor> otherDescriptors = new ArrayList();

    @Override
    public void parseDetail(ByteBuffer byteBuffer) throws IOException {
        this.esId = IsoTypeReader.readUInt16(byteBuffer);
        int uInt8 = IsoTypeReader.readUInt8(byteBuffer);
        this.streamDependenceFlag = uInt8 >>> 7;
        this.URLFlag = (uInt8 >>> 6) & 1;
        this.oCRstreamFlag = (uInt8 >>> 5) & 1;
        this.streamPriority = uInt8 & 31;
        if (this.streamDependenceFlag == 1) {
            this.dependsOnEsId = IsoTypeReader.readUInt16(byteBuffer);
        }
        if (this.URLFlag == 1) {
            this.URLLength = IsoTypeReader.readUInt8(byteBuffer);
            this.URLString = IsoTypeReader.readString(byteBuffer, this.URLLength);
        }
        if (this.oCRstreamFlag == 1) {
            this.oCREsId = IsoTypeReader.readUInt16(byteBuffer);
        }
        int sizeBytes = getSizeBytes() + 1 + 2 + 1 + (this.streamDependenceFlag == 1 ? 2 : 0) + (this.URLFlag == 1 ? this.URLLength + 1 : 0) + (this.oCRstreamFlag == 1 ? 2 : 0);
        int iPosition = byteBuffer.position();
        if (getSize() > sizeBytes + 2) {
            ?? CreateFrom = ObjectDescriptorFactory.createFrom(-1, byteBuffer);
            long jPosition = byteBuffer.position() - iPosition;
            Logger logger = log;
            StringBuilder sb = new StringBuilder();
            sb.append((Object) CreateFrom);
            sb.append(" - ESDescriptor1 read: ");
            sb.append(jPosition);
            sb.append(", size: ");
            sb.append(CreateFrom != 0 ? Integer.valueOf(CreateFrom.getSize()) : null);
            logger.finer(sb.toString());
            if (CreateFrom != 0) {
                int size = CreateFrom.getSize();
                byteBuffer.position(iPosition + size);
                sizeBytes += size;
            } else {
                sizeBytes = (int) (((long) sizeBytes) + jPosition);
            }
            if (CreateFrom instanceof DecoderConfigDescriptor) {
                this.decoderConfigDescriptor = CreateFrom;
            }
        }
        int iPosition2 = byteBuffer.position();
        if (getSize() > sizeBytes + 2) {
            ?? CreateFrom2 = ObjectDescriptorFactory.createFrom(-1, byteBuffer);
            long jPosition2 = byteBuffer.position() - iPosition2;
            Logger logger2 = log;
            StringBuilder sb2 = new StringBuilder();
            sb2.append((Object) CreateFrom2);
            sb2.append(" - ESDescriptor2 read: ");
            sb2.append(jPosition2);
            sb2.append(", size: ");
            sb2.append(CreateFrom2 != 0 ? Integer.valueOf(CreateFrom2.getSize()) : null);
            logger2.finer(sb2.toString());
            if (CreateFrom2 != 0) {
                int size2 = CreateFrom2.getSize();
                byteBuffer.position(iPosition2 + size2);
                sizeBytes += size2;
            } else {
                sizeBytes = (int) (((long) sizeBytes) + jPosition2);
            }
            if (CreateFrom2 instanceof SLConfigDescriptor) {
                this.slConfigDescriptor = CreateFrom2;
            }
        } else {
            log.warning("SLConfigDescriptor is missing!");
        }
        while (getSize() - sizeBytes > 2) {
            int iPosition3 = byteBuffer.position();
            BaseDescriptor baseDescriptorCreateFrom = ObjectDescriptorFactory.createFrom(-1, byteBuffer);
            long jPosition3 = byteBuffer.position() - iPosition3;
            Logger logger3 = log;
            StringBuilder sb3 = new StringBuilder();
            sb3.append(baseDescriptorCreateFrom);
            sb3.append(" - ESDescriptor3 read: ");
            sb3.append(jPosition3);
            sb3.append(", size: ");
            sb3.append(baseDescriptorCreateFrom != null ? Integer.valueOf(baseDescriptorCreateFrom.getSize()) : null);
            logger3.finer(sb3.toString());
            if (baseDescriptorCreateFrom != null) {
                int size3 = baseDescriptorCreateFrom.getSize();
                byteBuffer.position(iPosition3 + size3);
                sizeBytes += size3;
            } else {
                sizeBytes = (int) (((long) sizeBytes) + jPosition3);
            }
            this.otherDescriptors.add(baseDescriptorCreateFrom);
        }
    }

    @Override
    public String toString() {
        return "ESDescriptor{esId=" + this.esId + ", streamDependenceFlag=" + this.streamDependenceFlag + ", URLFlag=" + this.URLFlag + ", oCRstreamFlag=" + this.oCRstreamFlag + ", streamPriority=" + this.streamPriority + ", URLLength=" + this.URLLength + ", URLString='" + this.URLString + "', remoteODFlag=" + this.remoteODFlag + ", dependsOnEsId=" + this.dependsOnEsId + ", oCREsId=" + this.oCREsId + ", decoderConfigDescriptor=" + this.decoderConfigDescriptor + ", slConfigDescriptor=" + this.slConfigDescriptor + '}';
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ESDescriptor eSDescriptor = (ESDescriptor) obj;
        if (this.URLFlag != eSDescriptor.URLFlag || this.URLLength != eSDescriptor.URLLength || this.dependsOnEsId != eSDescriptor.dependsOnEsId || this.esId != eSDescriptor.esId || this.oCREsId != eSDescriptor.oCREsId || this.oCRstreamFlag != eSDescriptor.oCRstreamFlag || this.remoteODFlag != eSDescriptor.remoteODFlag || this.streamDependenceFlag != eSDescriptor.streamDependenceFlag || this.streamPriority != eSDescriptor.streamPriority) {
            return false;
        }
        if (this.URLString == null ? eSDescriptor.URLString != null : !this.URLString.equals(eSDescriptor.URLString)) {
            return false;
        }
        if (this.decoderConfigDescriptor == null ? eSDescriptor.decoderConfigDescriptor != null : !this.decoderConfigDescriptor.equals(eSDescriptor.decoderConfigDescriptor)) {
            return false;
        }
        if (this.otherDescriptors == null ? eSDescriptor.otherDescriptors != null : !this.otherDescriptors.equals(eSDescriptor.otherDescriptors)) {
            return false;
        }
        if (this.slConfigDescriptor == null ? eSDescriptor.slConfigDescriptor == null : this.slConfigDescriptor.equals(eSDescriptor.slConfigDescriptor)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * ((((((((((((((((((((((this.esId * 31) + this.streamDependenceFlag) * 31) + this.URLFlag) * 31) + this.oCRstreamFlag) * 31) + this.streamPriority) * 31) + this.URLLength) * 31) + (this.URLString != null ? this.URLString.hashCode() : 0)) * 31) + this.remoteODFlag) * 31) + this.dependsOnEsId) * 31) + this.oCREsId) * 31) + (this.decoderConfigDescriptor != null ? this.decoderConfigDescriptor.hashCode() : 0)) * 31) + (this.slConfigDescriptor != null ? this.slConfigDescriptor.hashCode() : 0))) + (this.otherDescriptors != null ? this.otherDescriptors.hashCode() : 0);
    }
}
