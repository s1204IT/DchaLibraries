package com.coremedia.iso.boxes.h264;

import com.coremedia.iso.Hex;
import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractBox;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.BitReaderBuffer;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.BitWriterBuffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AvcConfigurationBox extends AbstractBox {
    public AVCDecoderConfigurationRecord avcDecoderConfigurationRecord;

    public AvcConfigurationBox() {
        super("avcC");
        this.avcDecoderConfigurationRecord = new AVCDecoderConfigurationRecord();
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        this.avcDecoderConfigurationRecord = new AVCDecoderConfigurationRecord(byteBuffer);
    }

    @Override
    public long getContentSize() {
        return this.avcDecoderConfigurationRecord.getContentSize();
    }

    @Override
    public void getContent(ByteBuffer byteBuffer) {
        this.avcDecoderConfigurationRecord.getContent(byteBuffer);
    }

    public static class AVCDecoderConfigurationRecord {
        public int avcLevelIndication;
        public int avcProfileIndication;
        public int bitDepthChromaMinus8;
        public int bitDepthChromaMinus8PaddingBits;
        public int bitDepthLumaMinus8;
        public int bitDepthLumaMinus8PaddingBits;
        public int chromaFormat;
        public int chromaFormatPaddingBits;
        public int configurationVersion;
        public boolean hasExts;
        public int lengthSizeMinusOne;
        public int lengthSizeMinusOnePaddingBits;
        public int numberOfSequenceParameterSetsPaddingBits;
        public List<byte[]> pictureParameterSets;
        public int profileCompatibility;
        public List<byte[]> sequenceParameterSetExts;
        public List<byte[]> sequenceParameterSets;

        public AVCDecoderConfigurationRecord() {
            this.sequenceParameterSets = new ArrayList();
            this.pictureParameterSets = new ArrayList();
            this.hasExts = true;
            this.chromaFormat = 1;
            this.bitDepthLumaMinus8 = 0;
            this.bitDepthChromaMinus8 = 0;
            this.sequenceParameterSetExts = new ArrayList();
            this.lengthSizeMinusOnePaddingBits = 60;
            this.numberOfSequenceParameterSetsPaddingBits = 7;
            this.chromaFormatPaddingBits = 31;
            this.bitDepthLumaMinus8PaddingBits = 31;
            this.bitDepthChromaMinus8PaddingBits = 31;
        }

        public AVCDecoderConfigurationRecord(ByteBuffer byteBuffer) {
            this.sequenceParameterSets = new ArrayList();
            this.pictureParameterSets = new ArrayList();
            this.hasExts = true;
            this.chromaFormat = 1;
            this.bitDepthLumaMinus8 = 0;
            this.bitDepthChromaMinus8 = 0;
            this.sequenceParameterSetExts = new ArrayList();
            this.lengthSizeMinusOnePaddingBits = 60;
            this.numberOfSequenceParameterSetsPaddingBits = 7;
            this.chromaFormatPaddingBits = 31;
            this.bitDepthLumaMinus8PaddingBits = 31;
            this.bitDepthChromaMinus8PaddingBits = 31;
            this.configurationVersion = IsoTypeReader.readUInt8(byteBuffer);
            this.avcProfileIndication = IsoTypeReader.readUInt8(byteBuffer);
            this.profileCompatibility = IsoTypeReader.readUInt8(byteBuffer);
            this.avcLevelIndication = IsoTypeReader.readUInt8(byteBuffer);
            BitReaderBuffer bitReaderBuffer = new BitReaderBuffer(byteBuffer);
            this.lengthSizeMinusOnePaddingBits = bitReaderBuffer.readBits(6);
            this.lengthSizeMinusOne = bitReaderBuffer.readBits(2);
            this.numberOfSequenceParameterSetsPaddingBits = bitReaderBuffer.readBits(3);
            int bits = bitReaderBuffer.readBits(5);
            for (int i = 0; i < bits; i++) {
                byte[] bArr = new byte[IsoTypeReader.readUInt16(byteBuffer)];
                byteBuffer.get(bArr);
                this.sequenceParameterSets.add(bArr);
            }
            long uInt8 = IsoTypeReader.readUInt8(byteBuffer);
            for (int i2 = 0; i2 < uInt8; i2++) {
                byte[] bArr2 = new byte[IsoTypeReader.readUInt16(byteBuffer)];
                byteBuffer.get(bArr2);
                this.pictureParameterSets.add(bArr2);
            }
            if (byteBuffer.remaining() < 4) {
                this.hasExts = false;
            }
            if (this.hasExts && (this.avcProfileIndication == 100 || this.avcProfileIndication == 110 || this.avcProfileIndication == 122 || this.avcProfileIndication == 144)) {
                BitReaderBuffer bitReaderBuffer2 = new BitReaderBuffer(byteBuffer);
                this.chromaFormatPaddingBits = bitReaderBuffer2.readBits(6);
                this.chromaFormat = bitReaderBuffer2.readBits(2);
                this.bitDepthLumaMinus8PaddingBits = bitReaderBuffer2.readBits(5);
                this.bitDepthLumaMinus8 = bitReaderBuffer2.readBits(3);
                this.bitDepthChromaMinus8PaddingBits = bitReaderBuffer2.readBits(5);
                this.bitDepthChromaMinus8 = bitReaderBuffer2.readBits(3);
                long uInt82 = IsoTypeReader.readUInt8(byteBuffer);
                for (int i3 = 0; i3 < uInt82; i3++) {
                    byte[] bArr3 = new byte[IsoTypeReader.readUInt16(byteBuffer)];
                    byteBuffer.get(bArr3);
                    this.sequenceParameterSetExts.add(bArr3);
                }
                return;
            }
            this.chromaFormat = -1;
            this.bitDepthLumaMinus8 = -1;
            this.bitDepthChromaMinus8 = -1;
        }

        public void getContent(ByteBuffer byteBuffer) {
            IsoTypeWriter.writeUInt8(byteBuffer, this.configurationVersion);
            IsoTypeWriter.writeUInt8(byteBuffer, this.avcProfileIndication);
            IsoTypeWriter.writeUInt8(byteBuffer, this.profileCompatibility);
            IsoTypeWriter.writeUInt8(byteBuffer, this.avcLevelIndication);
            BitWriterBuffer bitWriterBuffer = new BitWriterBuffer(byteBuffer);
            bitWriterBuffer.writeBits(this.lengthSizeMinusOnePaddingBits, 6);
            bitWriterBuffer.writeBits(this.lengthSizeMinusOne, 2);
            bitWriterBuffer.writeBits(this.numberOfSequenceParameterSetsPaddingBits, 3);
            bitWriterBuffer.writeBits(this.pictureParameterSets.size(), 5);
            for (byte[] bArr : this.sequenceParameterSets) {
                IsoTypeWriter.writeUInt16(byteBuffer, bArr.length);
                byteBuffer.put(bArr);
            }
            IsoTypeWriter.writeUInt8(byteBuffer, this.pictureParameterSets.size());
            for (byte[] bArr2 : this.pictureParameterSets) {
                IsoTypeWriter.writeUInt16(byteBuffer, bArr2.length);
                byteBuffer.put(bArr2);
            }
            if (this.hasExts) {
                if (this.avcProfileIndication == 100 || this.avcProfileIndication == 110 || this.avcProfileIndication == 122 || this.avcProfileIndication == 144) {
                    BitWriterBuffer bitWriterBuffer2 = new BitWriterBuffer(byteBuffer);
                    bitWriterBuffer2.writeBits(this.chromaFormatPaddingBits, 6);
                    bitWriterBuffer2.writeBits(this.chromaFormat, 2);
                    bitWriterBuffer2.writeBits(this.bitDepthLumaMinus8PaddingBits, 5);
                    bitWriterBuffer2.writeBits(this.bitDepthLumaMinus8, 3);
                    bitWriterBuffer2.writeBits(this.bitDepthChromaMinus8PaddingBits, 5);
                    bitWriterBuffer2.writeBits(this.bitDepthChromaMinus8, 3);
                    for (byte[] bArr3 : this.sequenceParameterSetExts) {
                        IsoTypeWriter.writeUInt16(byteBuffer, bArr3.length);
                        byteBuffer.put(bArr3);
                    }
                }
            }
        }

        public long getContentSize() {
            Iterator<byte[]> it = this.sequenceParameterSets.iterator();
            long length = 6;
            while (it.hasNext()) {
                length = length + 2 + ((long) it.next().length);
            }
            long length2 = length + 1;
            Iterator<byte[]> it2 = this.pictureParameterSets.iterator();
            while (it2.hasNext()) {
                length2 = length2 + 2 + ((long) it2.next().length);
            }
            if (this.hasExts && (this.avcProfileIndication == 100 || this.avcProfileIndication == 110 || this.avcProfileIndication == 122 || this.avcProfileIndication == 144)) {
                length2 += 4;
                Iterator<byte[]> it3 = this.sequenceParameterSetExts.iterator();
                while (it3.hasNext()) {
                    length2 = length2 + 2 + ((long) it3.next().length);
                }
            }
            return length2;
        }

        public List<String> getSequenceParameterSetsAsStrings() {
            ArrayList arrayList = new ArrayList(this.sequenceParameterSets.size());
            Iterator<byte[]> it = this.sequenceParameterSets.iterator();
            while (it.hasNext()) {
                arrayList.add(Hex.encodeHex(it.next()));
            }
            return arrayList;
        }

        public List<String> getPictureParameterSetsAsStrings() {
            ArrayList arrayList = new ArrayList(this.pictureParameterSets.size());
            Iterator<byte[]> it = this.pictureParameterSets.iterator();
            while (it.hasNext()) {
                arrayList.add(Hex.encodeHex(it.next()));
            }
            return arrayList;
        }
    }
}
