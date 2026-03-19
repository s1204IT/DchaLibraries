package com.android.internal.telephony.cat;

public class ImageDescriptor {
    public static final int CODING_SCHEME_BASIC = 17;
    public static final int CODING_SCHEME_COLOUR = 33;
    public int mWidth = 0;
    public int mHeight = 0;
    public int mCodingScheme = 0;
    public int mImageId = 0;
    public int mHighOffset = 0;
    public int mLowOffset = 0;
    public int mLength = 0;

    ImageDescriptor() {
    }

    public static ImageDescriptor parse(byte[] bArr, int i) {
        ImageDescriptor imageDescriptor = new ImageDescriptor();
        int i2 = i + 1;
        try {
            imageDescriptor.mWidth = bArr[i] & 255;
            int i3 = i2 + 1;
            imageDescriptor.mHeight = bArr[i2] & 255;
            int i4 = i3 + 1;
            imageDescriptor.mCodingScheme = bArr[i3] & 255;
            int i5 = i4 + 1;
            imageDescriptor.mImageId = (bArr[i4] & 255) << 8;
            int i6 = i5 + 1;
            imageDescriptor.mImageId = (bArr[i5] & 255) | imageDescriptor.mImageId;
            int i7 = i6 + 1;
            imageDescriptor.mHighOffset = bArr[i6] & 255;
            int i8 = i7 + 1;
            imageDescriptor.mLowOffset = bArr[i7] & 255;
            imageDescriptor.mLength = (bArr[i8 + 1] & 255) | ((bArr[i8] & 255) << 8);
            CatLog.d("ImageDescriptor", "parse; Descriptor : " + imageDescriptor.mWidth + ", " + imageDescriptor.mHeight + ", " + imageDescriptor.mCodingScheme + ", 0x" + Integer.toHexString(imageDescriptor.mImageId) + ", " + imageDescriptor.mHighOffset + ", " + imageDescriptor.mLowOffset + ", " + imageDescriptor.mLength);
            return imageDescriptor;
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("ImageDescriptor", "parse; failed parsing image descriptor");
            return null;
        }
    }
}
