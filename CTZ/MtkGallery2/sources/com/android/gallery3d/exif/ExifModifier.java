package com.android.gallery3d.exif;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

class ExifModifier {
    private final ByteBuffer mByteBuffer;
    private final ExifInterface mInterface;
    private int mOffsetBase;
    private final List<TagOffset> mTagOffsets = new ArrayList();
    private final ExifData mTagToModified;

    private static class TagOffset {
        final int mOffset;
        final ExifTag mTag;

        TagOffset(ExifTag exifTag, int i) {
            this.mTag = exifTag;
            this.mOffset = i;
        }
    }

    protected ExifModifier(ByteBuffer byteBuffer, ExifInterface exifInterface) throws Throwable {
        ByteBufferInputStream byteBufferInputStream;
        this.mByteBuffer = byteBuffer;
        this.mOffsetBase = byteBuffer.position();
        this.mInterface = exifInterface;
        try {
            byteBufferInputStream = new ByteBufferInputStream(byteBuffer);
            try {
                ExifParser exifParser = ExifParser.parse(byteBufferInputStream, this.mInterface);
                this.mTagToModified = new ExifData(exifParser.getByteOrder());
                this.mOffsetBase += exifParser.getTiffStartPosition();
                this.mByteBuffer.position(0);
                ExifInterface.closeSilently(byteBufferInputStream);
            } catch (Throwable th) {
                th = th;
                ExifInterface.closeSilently(byteBufferInputStream);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            byteBufferInputStream = null;
        }
    }

    protected ByteOrder getByteOrder() {
        return this.mTagToModified.getByteOrder();
    }

    protected boolean commit() throws Throwable {
        ByteBufferInputStream byteBufferInputStream;
        Throwable th;
        IfdData ifdData = null;
        try {
            byteBufferInputStream = new ByteBufferInputStream(this.mByteBuffer);
            try {
                IfdData[] ifdDataArr = {this.mTagToModified.getIfdData(0), this.mTagToModified.getIfdData(1), this.mTagToModified.getIfdData(2), this.mTagToModified.getIfdData(3), this.mTagToModified.getIfdData(4)};
                int i = ifdDataArr[0] != null ? 1 : 0;
                if (ifdDataArr[1] != null) {
                    i |= 2;
                }
                if (ifdDataArr[2] != null) {
                    i |= 4;
                }
                if (ifdDataArr[4] != null) {
                    i |= 8;
                }
                if (ifdDataArr[3] != null) {
                    i |= 16;
                }
                ExifParser exifParser = ExifParser.parse(byteBufferInputStream, i, this.mInterface);
                for (int next = exifParser.next(); next != 5; next = exifParser.next()) {
                    switch (next) {
                        case 0:
                            ifdData = ifdDataArr[exifParser.getCurrentIfd()];
                            if (ifdData == null) {
                                exifParser.skipRemainingTagsInCurrentIfd();
                            }
                            break;
                        case 1:
                            ExifTag tag = exifParser.getTag();
                            ExifTag tag2 = ifdData.getTag(tag.getTagId());
                            if (tag2 != null) {
                                if (tag2.getComponentCount() == tag.getComponentCount() && tag2.getDataType() == tag.getDataType()) {
                                    this.mTagOffsets.add(new TagOffset(tag2, tag.getOffset()));
                                    ifdData.removeTag(tag.getTagId());
                                    if (ifdData.getTagCount() == 0) {
                                        exifParser.skipRemainingTagsInCurrentIfd();
                                    }
                                    break;
                                }
                                ExifInterface.closeSilently(byteBufferInputStream);
                                return false;
                            }
                            continue;
                            break;
                    }
                }
                for (IfdData ifdData2 : ifdDataArr) {
                    if (ifdData2 != null && ifdData2.getTagCount() > 0) {
                        ExifInterface.closeSilently(byteBufferInputStream);
                        return false;
                    }
                }
                modify();
                ExifInterface.closeSilently(byteBufferInputStream);
                return true;
            } catch (Throwable th2) {
                th = th2;
                ExifInterface.closeSilently(byteBufferInputStream);
                throw th;
            }
        } catch (Throwable th3) {
            byteBufferInputStream = null;
            th = th3;
        }
    }

    private void modify() {
        this.mByteBuffer.order(getByteOrder());
        for (TagOffset tagOffset : this.mTagOffsets) {
            writeTagValue(tagOffset.mTag, tagOffset.mOffset);
        }
    }

    private void writeTagValue(ExifTag exifTag, int i) {
        this.mByteBuffer.position(i + this.mOffsetBase);
        int i2 = 0;
        switch (exifTag.getDataType()) {
            case 1:
            case 7:
                byte[] bArr = new byte[exifTag.getComponentCount()];
                exifTag.getBytes(bArr);
                this.mByteBuffer.put(bArr);
                break;
            case 2:
                byte[] stringByte = exifTag.getStringByte();
                if (stringByte.length == exifTag.getComponentCount()) {
                    stringByte[stringByte.length - 1] = 0;
                    this.mByteBuffer.put(stringByte);
                } else {
                    this.mByteBuffer.put(stringByte);
                    this.mByteBuffer.put((byte) 0);
                }
                break;
            case 3:
                int componentCount = exifTag.getComponentCount();
                while (i2 < componentCount) {
                    this.mByteBuffer.putShort((short) exifTag.getValueAt(i2));
                    i2++;
                }
                break;
            case 4:
            case 9:
                int componentCount2 = exifTag.getComponentCount();
                while (i2 < componentCount2) {
                    this.mByteBuffer.putInt((int) exifTag.getValueAt(i2));
                    i2++;
                }
                break;
            case 5:
            case 10:
                int componentCount3 = exifTag.getComponentCount();
                while (i2 < componentCount3) {
                    Rational rational = exifTag.getRational(i2);
                    this.mByteBuffer.putInt((int) rational.getNumerator());
                    this.mByteBuffer.putInt((int) rational.getDenominator());
                    i2++;
                }
                break;
        }
    }

    public void modifyTag(ExifTag exifTag) {
        this.mTagToModified.addTag(exifTag);
    }
}
