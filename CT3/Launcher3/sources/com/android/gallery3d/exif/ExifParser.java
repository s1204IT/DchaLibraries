package com.android.gallery3d.exif;

import android.util.Log;
import com.android.launcher3.compat.PackageInstallerCompat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.TreeMap;

class ExifParser {
    private int mApp1End;
    private boolean mContainExifData;
    private byte[] mDataAboveIfd0;
    private int mIfd0Position;
    private int mIfdType;
    private ImageEvent mImageEvent;
    private final ExifInterface mInterface;
    private ExifTag mJpegSizeTag;
    private boolean mNeedToParseOffsetsInCurrentIfd;
    private final int mOptions;
    private ExifTag mStripSizeTag;
    private ExifTag mTag;
    private int mTiffStartPosition;
    private final CountedDataInputStream mTiffStream;
    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    private static final short TAG_EXIF_IFD = ExifInterface.getTrueTagKey(ExifInterface.TAG_EXIF_IFD);
    private static final short TAG_GPS_IFD = ExifInterface.getTrueTagKey(ExifInterface.TAG_GPS_IFD);
    private static final short TAG_INTEROPERABILITY_IFD = ExifInterface.getTrueTagKey(ExifInterface.TAG_INTEROPERABILITY_IFD);
    private static final short TAG_JPEG_INTERCHANGE_FORMAT = ExifInterface.getTrueTagKey(ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT);
    private static final short TAG_JPEG_INTERCHANGE_FORMAT_LENGTH = ExifInterface.getTrueTagKey(ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH);
    private static final short TAG_STRIP_OFFSETS = ExifInterface.getTrueTagKey(ExifInterface.TAG_STRIP_OFFSETS);
    private static final short TAG_STRIP_BYTE_COUNTS = ExifInterface.getTrueTagKey(ExifInterface.TAG_STRIP_BYTE_COUNTS);
    private int mIfdStartOffset = 0;
    private int mNumOfTagInIfd = 0;
    private int mOffsetToApp1EndFromSOF = 0;
    private final TreeMap<Integer, Object> mCorrespondingEvent = new TreeMap<>();

    private boolean isIfdRequested(int ifdType) {
        switch (ifdType) {
            case PackageInstallerCompat.STATUS_INSTALLED:
                return (this.mOptions & 1) != 0;
            case PackageInstallerCompat.STATUS_INSTALLING:
                return (this.mOptions & 2) != 0;
            case PackageInstallerCompat.STATUS_FAILED:
                return (this.mOptions & 4) != 0;
            case 3:
                return (this.mOptions & 16) != 0;
            case 4:
                return (this.mOptions & 8) != 0;
            default:
                return false;
        }
    }

    private boolean isThumbnailRequested() {
        return (this.mOptions & 32) != 0;
    }

    private ExifParser(InputStream inputStream, int options, ExifInterface iRef) throws ExifInvalidFormatException, IOException {
        this.mContainExifData = false;
        if (inputStream == null) {
            throw new IOException("Null argument inputStream to ExifParser");
        }
        this.mInterface = iRef;
        this.mContainExifData = seekTiffData(inputStream);
        this.mTiffStream = new CountedDataInputStream(inputStream);
        this.mOptions = options;
        if (!this.mContainExifData) {
            return;
        }
        parseTiffHeader();
        long offset = this.mTiffStream.readUnsignedInt();
        if (offset > 2147483647L) {
            throw new ExifInvalidFormatException("Invalid offset " + offset);
        }
        this.mIfd0Position = (int) offset;
        this.mIfdType = 0;
        if (!isIfdRequested(0) && !needToParseOffsetsInCurrentIfd()) {
            return;
        }
        registerIfd(0, offset);
        if (offset == 8) {
            return;
        }
        this.mDataAboveIfd0 = new byte[((int) offset) - 8];
        read(this.mDataAboveIfd0);
    }

    protected static ExifParser parse(InputStream inputStream, ExifInterface iRef) throws ExifInvalidFormatException, IOException {
        return new ExifParser(inputStream, 63, iRef);
    }

    protected int next() throws ExifInvalidFormatException, IOException {
        if (!this.mContainExifData) {
            return 5;
        }
        int offset = this.mTiffStream.getReadByteCount();
        int endOfTags = this.mIfdStartOffset + 2 + (this.mNumOfTagInIfd * 12);
        if (offset < endOfTags) {
            this.mTag = readTag();
            if (this.mTag == null) {
                return next();
            }
            if (!this.mNeedToParseOffsetsInCurrentIfd) {
                return 1;
            }
            checkOffsetOrImageTag(this.mTag);
            return 1;
        }
        if (offset == endOfTags) {
            if (this.mIfdType == 0) {
                long ifdOffset = readUnsignedLong();
                if ((isIfdRequested(1) || isThumbnailRequested()) && ifdOffset != 0) {
                    registerIfd(1, ifdOffset);
                }
            } else {
                int offsetSize = this.mCorrespondingEvent.size() > 0 ? this.mCorrespondingEvent.firstEntry().getKey().intValue() - this.mTiffStream.getReadByteCount() : 4;
                if (offsetSize < 4) {
                    Log.w("ExifParser", "Invalid size of link to next IFD: " + offsetSize);
                } else {
                    long ifdOffset2 = readUnsignedLong();
                    if (ifdOffset2 != 0) {
                        Log.w("ExifParser", "Invalid link to next IFD: " + ifdOffset2);
                    }
                }
            }
        }
        while (this.mCorrespondingEvent.size() != 0) {
            Map.Entry<Integer, Object> entry = this.mCorrespondingEvent.pollFirstEntry();
            Object event = entry.getValue();
            try {
                skipTo(entry.getKey().intValue());
                if (event instanceof IfdEvent) {
                    this.mIfdType = ((IfdEvent) event).ifd;
                    this.mNumOfTagInIfd = this.mTiffStream.readUnsignedShort();
                    this.mIfdStartOffset = entry.getKey().intValue();
                    if ((this.mNumOfTagInIfd * 12) + this.mIfdStartOffset + 2 > this.mApp1End) {
                        Log.w("ExifParser", "Invalid size of IFD " + this.mIfdType);
                        return 5;
                    }
                    this.mNeedToParseOffsetsInCurrentIfd = needToParseOffsetsInCurrentIfd();
                    if (((IfdEvent) event).isRequested) {
                        return 0;
                    }
                    skipRemainingTagsInCurrentIfd();
                } else {
                    if (event instanceof ImageEvent) {
                        this.mImageEvent = (ImageEvent) event;
                        return this.mImageEvent.type;
                    }
                    ExifTagEvent tagEvent = (ExifTagEvent) event;
                    this.mTag = tagEvent.tag;
                    if (this.mTag.getDataType() != 7) {
                        readFullTagValue(this.mTag);
                        checkOffsetOrImageTag(this.mTag);
                    }
                    if (tagEvent.isRequested) {
                        return 2;
                    }
                }
            } catch (IOException e) {
                Log.w("ExifParser", "Failed to skip to data at: " + entry.getKey() + " for " + event.getClass().getName() + ", the file may be broken.");
            }
        }
        return 5;
    }

    protected void skipRemainingTagsInCurrentIfd() throws ExifInvalidFormatException, IOException {
        int endOfTags = this.mIfdStartOffset + 2 + (this.mNumOfTagInIfd * 12);
        int offset = this.mTiffStream.getReadByteCount();
        if (offset > endOfTags) {
            return;
        }
        if (this.mNeedToParseOffsetsInCurrentIfd) {
            while (offset < endOfTags) {
                this.mTag = readTag();
                offset += 12;
                if (this.mTag != null) {
                    checkOffsetOrImageTag(this.mTag);
                }
            }
        } else {
            skipTo(endOfTags);
        }
        long ifdOffset = readUnsignedLong();
        if (this.mIfdType != 0) {
            return;
        }
        if ((!isIfdRequested(1) && !isThumbnailRequested()) || ifdOffset <= 0) {
            return;
        }
        registerIfd(1, ifdOffset);
    }

    private boolean needToParseOffsetsInCurrentIfd() {
        switch (this.mIfdType) {
            case PackageInstallerCompat.STATUS_INSTALLED:
                if (isIfdRequested(2) || isIfdRequested(4) || isIfdRequested(3)) {
                    return true;
                }
                return isIfdRequested(1);
            case PackageInstallerCompat.STATUS_INSTALLING:
                return isThumbnailRequested();
            case PackageInstallerCompat.STATUS_FAILED:
                return isIfdRequested(3);
            default:
                return false;
        }
    }

    protected ExifTag getTag() {
        return this.mTag;
    }

    protected int getCurrentIfd() {
        return this.mIfdType;
    }

    protected int getStripIndex() {
        return this.mImageEvent.stripIndex;
    }

    protected int getStripSize() {
        if (this.mStripSizeTag == null) {
            return 0;
        }
        return (int) this.mStripSizeTag.getValueAt(0);
    }

    protected int getCompressedImageSize() {
        if (this.mJpegSizeTag == null) {
            return 0;
        }
        return (int) this.mJpegSizeTag.getValueAt(0);
    }

    private void skipTo(int offset) throws IOException {
        this.mTiffStream.skipTo(offset);
        while (!this.mCorrespondingEvent.isEmpty() && this.mCorrespondingEvent.firstKey().intValue() < offset) {
            this.mCorrespondingEvent.pollFirstEntry();
        }
    }

    protected void registerForTagValue(ExifTag tag) {
        if (tag.getOffset() < this.mTiffStream.getReadByteCount()) {
            return;
        }
        this.mCorrespondingEvent.put(Integer.valueOf(tag.getOffset()), new ExifTagEvent(tag, true));
    }

    private void registerIfd(int ifdType, long offset) {
        this.mCorrespondingEvent.put(Integer.valueOf((int) offset), new IfdEvent(ifdType, isIfdRequested(ifdType)));
    }

    private void registerCompressedImage(long offset) {
        this.mCorrespondingEvent.put(Integer.valueOf((int) offset), new ImageEvent(3));
    }

    private void registerUncompressedStrip(int stripIndex, long offset) {
        this.mCorrespondingEvent.put(Integer.valueOf((int) offset), new ImageEvent(4, stripIndex));
    }

    private ExifTag readTag() throws ExifInvalidFormatException, IOException {
        short tagId = this.mTiffStream.readShort();
        short dataFormat = this.mTiffStream.readShort();
        long numOfComp = this.mTiffStream.readUnsignedInt();
        if (numOfComp > 2147483647L) {
            throw new ExifInvalidFormatException("Number of component is larger then Integer.MAX_VALUE");
        }
        if (!ExifTag.isValidType(dataFormat)) {
            Log.w("ExifParser", String.format("Tag %04x: Invalid data type %d", Short.valueOf(tagId), Short.valueOf(dataFormat)));
            this.mTiffStream.skip(4L);
            return null;
        }
        ExifTag tag = new ExifTag(tagId, dataFormat, (int) numOfComp, this.mIfdType, ((int) numOfComp) != 0);
        int dataSize = tag.getDataSize();
        if (dataSize > 4) {
            long offset = this.mTiffStream.readUnsignedInt();
            if (offset > 2147483647L) {
                throw new ExifInvalidFormatException("offset is larger then Integer.MAX_VALUE");
            }
            if (offset < this.mIfd0Position && dataFormat == 7) {
                byte[] buf = new byte[(int) numOfComp];
                System.arraycopy(this.mDataAboveIfd0, ((int) offset) - 8, buf, 0, (int) numOfComp);
                tag.setValue(buf);
            } else {
                tag.setOffset((int) offset);
            }
        } else {
            boolean defCount = tag.hasDefinedCount();
            tag.setHasDefinedCount(false);
            readFullTagValue(tag);
            tag.setHasDefinedCount(defCount);
            this.mTiffStream.skip(4 - dataSize);
            tag.setOffset(this.mTiffStream.getReadByteCount() - 4);
        }
        return tag;
    }

    private void checkOffsetOrImageTag(ExifTag tag) {
        if (tag.getComponentCount() == 0) {
            return;
        }
        short tid = tag.getTagId();
        int ifd = tag.getIfd();
        if (tid == TAG_EXIF_IFD && checkAllowed(ifd, ExifInterface.TAG_EXIF_IFD)) {
            if (isIfdRequested(2) || isIfdRequested(3)) {
                registerIfd(2, tag.getValueAt(0));
                return;
            }
            return;
        }
        if (tid == TAG_GPS_IFD && checkAllowed(ifd, ExifInterface.TAG_GPS_IFD)) {
            if (isIfdRequested(4)) {
                registerIfd(4, tag.getValueAt(0));
                return;
            }
            return;
        }
        if (tid == TAG_INTEROPERABILITY_IFD && checkAllowed(ifd, ExifInterface.TAG_INTEROPERABILITY_IFD)) {
            if (isIfdRequested(3)) {
                registerIfd(3, tag.getValueAt(0));
                return;
            }
            return;
        }
        if (tid == TAG_JPEG_INTERCHANGE_FORMAT && checkAllowed(ifd, ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT)) {
            if (isThumbnailRequested()) {
                registerCompressedImage(tag.getValueAt(0));
                return;
            }
            return;
        }
        if (tid == TAG_JPEG_INTERCHANGE_FORMAT_LENGTH && checkAllowed(ifd, ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH)) {
            if (isThumbnailRequested()) {
                this.mJpegSizeTag = tag;
                return;
            }
            return;
        }
        if (tid != TAG_STRIP_OFFSETS || !checkAllowed(ifd, ExifInterface.TAG_STRIP_OFFSETS)) {
            if (tid == TAG_STRIP_BYTE_COUNTS && checkAllowed(ifd, ExifInterface.TAG_STRIP_BYTE_COUNTS) && isThumbnailRequested() && tag.hasValue()) {
                this.mStripSizeTag = tag;
                return;
            }
            return;
        }
        if (isThumbnailRequested()) {
            if (!tag.hasValue()) {
                this.mCorrespondingEvent.put(Integer.valueOf(tag.getOffset()), new ExifTagEvent(tag, false));
                return;
            }
            for (int i = 0; i < tag.getComponentCount(); i++) {
                if (tag.getDataType() == 3) {
                    registerUncompressedStrip(i, tag.getValueAt(i));
                } else {
                    registerUncompressedStrip(i, tag.getValueAt(i));
                }
            }
        }
    }

    private boolean checkAllowed(int ifd, int tagId) {
        int info = this.mInterface.getTagInfo().get(tagId);
        if (info == 0) {
            return false;
        }
        return ExifInterface.isIfdAllowed(info, ifd);
    }

    protected void readFullTagValue(ExifTag tag) throws IOException {
        short type = tag.getDataType();
        if (type == 2 || type == 7 || type == 1) {
            int size = tag.getComponentCount();
            if (this.mCorrespondingEvent.size() > 0 && this.mCorrespondingEvent.firstEntry().getKey().intValue() < this.mTiffStream.getReadByteCount() + size) {
                Object event = this.mCorrespondingEvent.firstEntry().getValue();
                if (event instanceof ImageEvent) {
                    Log.w("ExifParser", "Thumbnail overlaps value for tag: \n" + tag.toString());
                    Map.Entry<Integer, Object> entry = this.mCorrespondingEvent.pollFirstEntry();
                    Log.w("ExifParser", "Invalid thumbnail offset: " + entry.getKey());
                } else {
                    if (event instanceof IfdEvent) {
                        Log.w("ExifParser", "Ifd " + ((IfdEvent) event).ifd + " overlaps value for tag: \n" + tag.toString());
                    } else if (event instanceof ExifTagEvent) {
                        Log.w("ExifParser", "Tag value for tag: \n" + ((ExifTagEvent) event).tag.toString() + " overlaps value for tag: \n" + tag.toString());
                    }
                    int size2 = this.mCorrespondingEvent.firstEntry().getKey().intValue() - this.mTiffStream.getReadByteCount();
                    Log.w("ExifParser", "Invalid size of tag: \n" + tag.toString() + " setting count to: " + size2);
                    tag.forceSetComponentCount(size2);
                }
            }
        }
        switch (tag.getDataType()) {
            case PackageInstallerCompat.STATUS_INSTALLING:
            case 7:
                byte[] buf = new byte[tag.getComponentCount()];
                read(buf);
                tag.setValue(buf);
                break;
            case PackageInstallerCompat.STATUS_FAILED:
                tag.setValue(readString(tag.getComponentCount()));
                break;
            case 3:
                int[] value = new int[tag.getComponentCount()];
                int n = value.length;
                for (int i = 0; i < n; i++) {
                    value[i] = readUnsignedShort();
                }
                tag.setValue(value);
                break;
            case 4:
                long[] value2 = new long[tag.getComponentCount()];
                int n2 = value2.length;
                for (int i2 = 0; i2 < n2; i2++) {
                    value2[i2] = readUnsignedLong();
                }
                tag.setValue(value2);
                break;
            case 5:
                Rational[] value3 = new Rational[tag.getComponentCount()];
                int n3 = value3.length;
                for (int i3 = 0; i3 < n3; i3++) {
                    value3[i3] = readUnsignedRational();
                }
                tag.setValue(value3);
                break;
            case 9:
                int[] value4 = new int[tag.getComponentCount()];
                int n4 = value4.length;
                for (int i4 = 0; i4 < n4; i4++) {
                    value4[i4] = readLong();
                }
                tag.setValue(value4);
                break;
            case 10:
                Rational[] value5 = new Rational[tag.getComponentCount()];
                int n5 = value5.length;
                for (int i5 = 0; i5 < n5; i5++) {
                    value5[i5] = readRational();
                }
                tag.setValue(value5);
                break;
        }
    }

    private void parseTiffHeader() throws ExifInvalidFormatException, IOException {
        short byteOrder = this.mTiffStream.readShort();
        if (18761 == byteOrder) {
            this.mTiffStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        } else if (19789 == byteOrder) {
            this.mTiffStream.setByteOrder(ByteOrder.BIG_ENDIAN);
        } else {
            throw new ExifInvalidFormatException("Invalid TIFF header");
        }
        if (this.mTiffStream.readShort() == 42) {
        } else {
            throw new ExifInvalidFormatException("Invalid TIFF header");
        }
    }

    private boolean seekTiffData(InputStream inputStream) throws ExifInvalidFormatException, IOException {
        CountedDataInputStream dataStream = new CountedDataInputStream(inputStream);
        if (dataStream.readShort() != -40) {
            throw new ExifInvalidFormatException("Invalid JPEG format");
        }
        for (short marker = dataStream.readShort(); marker != -39 && !JpegHeader.isSofMarker(marker); marker = dataStream.readShort()) {
            int length = dataStream.readUnsignedShort();
            if (marker == -31 && length >= 8) {
                int header = dataStream.readInt();
                short headerTail = dataStream.readShort();
                length -= 6;
                if (header == 1165519206 && headerTail == 0) {
                    this.mTiffStartPosition = dataStream.getReadByteCount();
                    this.mApp1End = length;
                    this.mOffsetToApp1EndFromSOF = this.mTiffStartPosition + this.mApp1End;
                    return true;
                }
            }
            if (length < 2 || length - 2 != dataStream.skip(length - 2)) {
                Log.w("ExifParser", "Invalid JPEG format.");
                return false;
            }
        }
        return false;
    }

    protected int read(byte[] buffer) throws IOException {
        return this.mTiffStream.read(buffer);
    }

    protected String readString(int n) throws IOException {
        return readString(n, US_ASCII);
    }

    protected String readString(int n, Charset charset) throws IOException {
        if (n > 0) {
            return this.mTiffStream.readString(n, charset);
        }
        return "";
    }

    protected int readUnsignedShort() throws IOException {
        return this.mTiffStream.readShort() & 65535;
    }

    protected long readUnsignedLong() throws IOException {
        return ((long) readLong()) & 4294967295L;
    }

    protected Rational readUnsignedRational() throws IOException {
        long nomi = readUnsignedLong();
        long denomi = readUnsignedLong();
        return new Rational(nomi, denomi);
    }

    protected int readLong() throws IOException {
        return this.mTiffStream.readInt();
    }

    protected Rational readRational() throws IOException {
        int nomi = readLong();
        int denomi = readLong();
        return new Rational(nomi, denomi);
    }

    private static class ImageEvent {
        int stripIndex;
        int type;

        ImageEvent(int type) {
            this.stripIndex = 0;
            this.type = type;
        }

        ImageEvent(int type, int stripIndex) {
            this.type = type;
            this.stripIndex = stripIndex;
        }
    }

    private static class IfdEvent {
        int ifd;
        boolean isRequested;

        IfdEvent(int ifd, boolean isInterestedIfd) {
            this.ifd = ifd;
            this.isRequested = isInterestedIfd;
        }
    }

    private static class ExifTagEvent {
        boolean isRequested;
        ExifTag tag;

        ExifTagEvent(ExifTag tag, boolean isRequireByUser) {
            this.tag = tag;
            this.isRequested = isRequireByUser;
        }
    }

    protected ByteOrder getByteOrder() {
        return this.mTiffStream.getByteOrder();
    }
}
