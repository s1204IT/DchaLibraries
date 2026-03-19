package android.filterfw.format;

import android.filterfw.core.MutableFrameFormat;
import android.graphics.Bitmap;

public class ImageFormat {
    public static final int COLORSPACE_GRAY = 1;
    public static final String COLORSPACE_KEY = "colorspace";
    public static final int COLORSPACE_RGB = 2;
    public static final int COLORSPACE_RGBA = 3;
    public static final int COLORSPACE_YUV = 4;

    public static MutableFrameFormat create(int i, int i2, int i3, int i4, int i5) {
        MutableFrameFormat mutableFrameFormat = new MutableFrameFormat(2, i5);
        mutableFrameFormat.setDimensions(i, i2);
        mutableFrameFormat.setBytesPerSample(i4);
        mutableFrameFormat.setMetaValue(COLORSPACE_KEY, Integer.valueOf(i3));
        if (i5 == 1) {
            mutableFrameFormat.setObjectClass(Bitmap.class);
        }
        return mutableFrameFormat;
    }

    public static MutableFrameFormat create(int i, int i2, int i3, int i4) {
        return create(i, i2, i3, bytesPerSampleForColorspace(i3), i4);
    }

    public static MutableFrameFormat create(int i, int i2) {
        return create(0, 0, i, bytesPerSampleForColorspace(i), i2);
    }

    public static MutableFrameFormat create(int i) {
        return create(0, 0, i, bytesPerSampleForColorspace(i), 0);
    }

    public static int bytesPerSampleForColorspace(int i) {
        switch (i) {
            case 1:
                return 1;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 3;
            default:
                throw new RuntimeException("Unknown colorspace id " + i + "!");
        }
    }
}
