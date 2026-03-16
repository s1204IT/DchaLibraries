package com.android.gallery3d.exif;

import java.io.InputStream;
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

        TagOffset(ExifTag tag, int offset) {
            this.mTag = tag;
            this.mOffset = offset;
        }
    }

    protected ExifModifier(ByteBuffer byteBuffer, ExifInterface iRef) throws Throwable {
        this.mByteBuffer = byteBuffer;
        this.mOffsetBase = byteBuffer.position();
        this.mInterface = iRef;
        InputStream is = null;
        try {
            InputStream is2 = new ByteBufferInputStream(byteBuffer);
            try {
                ExifParser parser = ExifParser.parse(is2, this.mInterface);
                this.mTagToModified = new ExifData(parser.getByteOrder());
                this.mOffsetBase += parser.getTiffStartPosition();
                this.mByteBuffer.position(0);
                ExifInterface.closeSilently(is2);
            } catch (Throwable th) {
                th = th;
                is = is2;
                ExifInterface.closeSilently(is);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    protected ByteOrder getByteOrder() {
        return this.mTagToModified.getByteOrder();
    }

    protected boolean commit() throws Throwable {
        InputStream is;
        InputStream is2 = null;
        try {
            is = new ByteBufferInputStream(this.mByteBuffer);
        } catch (Throwable th) {
            th = th;
        }
        try {
            IfdData[] ifdDatas = {this.mTagToModified.getIfdData(0), this.mTagToModified.getIfdData(1), this.mTagToModified.getIfdData(2), this.mTagToModified.getIfdData(3), this.mTagToModified.getIfdData(4)};
            int flag = ifdDatas[0] != null ? 0 | 1 : 0;
            if (ifdDatas[1] != null) {
                flag |= 2;
            }
            if (ifdDatas[2] != null) {
                flag |= 4;
            }
            if (ifdDatas[4] != null) {
                flag |= 8;
            }
            if (ifdDatas[3] != null) {
                flag |= 16;
            }
            ExifParser parser = ExifParser.parse(is, flag, this.mInterface);
            IfdData currIfd = null;
            for (int event = parser.next(); event != 5; event = parser.next()) {
                switch (event) {
                    case 0:
                        currIfd = ifdDatas[parser.getCurrentIfd()];
                        if (currIfd == null) {
                            parser.skipRemainingTagsInCurrentIfd();
                        }
                        break;
                    case 1:
                        ExifTag oldTag = parser.getTag();
                        ExifTag newTag = currIfd.getTag(oldTag.getTagId());
                        if (newTag == null) {
                            continue;
                        } else {
                            if (newTag.getComponentCount() != oldTag.getComponentCount() || newTag.getDataType() != oldTag.getDataType()) {
                                ExifInterface.closeSilently(is);
                                return false;
                            }
                            this.mTagOffsets.add(new TagOffset(newTag, oldTag.getOffset()));
                            currIfd.removeTag(oldTag.getTagId());
                            if (currIfd.getTagCount() == 0) {
                                parser.skipRemainingTagsInCurrentIfd();
                            }
                        }
                        break;
                }
            }
            for (IfdData ifd : ifdDatas) {
                if (ifd != null && ifd.getTagCount() > 0) {
                    ExifInterface.closeSilently(is);
                    return false;
                }
            }
            modify();
            ExifInterface.closeSilently(is);
            return true;
        } catch (Throwable th2) {
            th = th2;
            is2 = is;
            ExifInterface.closeSilently(is2);
            throw th;
        }
    }

    private void modify() {
        this.mByteBuffer.order(getByteOrder());
        for (TagOffset tagOffset : this.mTagOffsets) {
            writeTagValue(tagOffset.mTag, tagOffset.mOffset);
        }
    }

    private void writeTagValue(ExifTag tag, int offset) {
        this.mByteBuffer.position(this.mOffsetBase + offset);
        switch (tag.getDataType()) {
            case 1:
            case 7:
                byte[] buf = new byte[tag.getComponentCount()];
                tag.getBytes(buf);
                this.mByteBuffer.put(buf);
                break;
            case 2:
                byte[] buf2 = tag.getStringByte();
                if (buf2.length == tag.getComponentCount()) {
                    buf2[buf2.length - 1] = 0;
                    this.mByteBuffer.put(buf2);
                } else {
                    this.mByteBuffer.put(buf2);
                    this.mByteBuffer.put((byte) 0);
                }
                break;
            case 3:
                int n = tag.getComponentCount();
                for (int i = 0; i < n; i++) {
                    this.mByteBuffer.putShort((short) tag.getValueAt(i));
                }
                break;
            case 4:
            case 9:
                int n2 = tag.getComponentCount();
                for (int i2 = 0; i2 < n2; i2++) {
                    this.mByteBuffer.putInt((int) tag.getValueAt(i2));
                }
                break;
            case 5:
            case 10:
                int n3 = tag.getComponentCount();
                for (int i3 = 0; i3 < n3; i3++) {
                    Rational v = tag.getRational(i3);
                    this.mByteBuffer.putInt((int) v.getNumerator());
                    this.mByteBuffer.putInt((int) v.getDenominator());
                }
                break;
        }
    }

    public void modifyTag(ExifTag tag) {
        this.mTagToModified.addTag(tag);
    }
}
