package android.graphics;

import com.android.internal.telephony.IccCardConstants;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PixelFormat {

    @Deprecated
    public static final int A_8 = 8;

    @Deprecated
    public static final int JPEG = 256;

    @Deprecated
    public static final int LA_88 = 10;

    @Deprecated
    public static final int L_8 = 9;
    public static final int OPAQUE = -1;
    public static final int RGBA_1010102 = 43;

    @Deprecated
    public static final int RGBA_4444 = 7;

    @Deprecated
    public static final int RGBA_5551 = 6;
    public static final int RGBA_8888 = 1;
    public static final int RGBA_F16 = 22;
    public static final int RGBX_8888 = 2;

    @Deprecated
    public static final int RGB_332 = 11;
    public static final int RGB_565 = 4;
    public static final int RGB_888 = 3;
    public static final int TRANSLUCENT = -3;
    public static final int TRANSPARENT = -2;
    public static final int UNKNOWN = 0;

    @Deprecated
    public static final int YCbCr_420_SP = 17;

    @Deprecated
    public static final int YCbCr_422_I = 20;

    @Deprecated
    public static final int YCbCr_422_SP = 16;
    public int bitsPerPixel;
    public int bytesPerPixel;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Format {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Opacity {
    }

    public static void getPixelFormatInfo(int i, PixelFormat pixelFormat) {
        if (i != 20) {
            if (i != 22) {
                if (i != 43) {
                    switch (i) {
                        case 1:
                        case 2:
                            break;
                        case 3:
                            pixelFormat.bitsPerPixel = 24;
                            pixelFormat.bytesPerPixel = 3;
                            return;
                        default:
                            switch (i) {
                                case 6:
                                case 7:
                                case 10:
                                    break;
                                case 8:
                                case 9:
                                case 11:
                                    pixelFormat.bitsPerPixel = 8;
                                    pixelFormat.bytesPerPixel = 1;
                                    return;
                                default:
                                    switch (i) {
                                        case 16:
                                            break;
                                        case 17:
                                            pixelFormat.bitsPerPixel = 12;
                                            pixelFormat.bytesPerPixel = 1;
                                            return;
                                        default:
                                            throw new IllegalArgumentException("unknown pixel format " + i);
                                    }
                                    break;
                            }
                        case 4:
                            pixelFormat.bitsPerPixel = 16;
                            pixelFormat.bytesPerPixel = 2;
                            return;
                    }
                }
                pixelFormat.bitsPerPixel = 32;
                pixelFormat.bytesPerPixel = 4;
                return;
            }
            pixelFormat.bitsPerPixel = 64;
            pixelFormat.bytesPerPixel = 8;
            return;
        }
        pixelFormat.bitsPerPixel = 16;
        pixelFormat.bytesPerPixel = 1;
    }

    public static boolean formatHasAlpha(int i) {
        if (i != 1 && i != 10 && i != 22 && i != 43) {
            switch (i) {
                default:
                    switch (i) {
                        case 6:
                        case 7:
                        case 8:
                            break;
                        default:
                            return false;
                    }
                case -3:
                case -2:
                    return true;
            }
        }
        return true;
    }

    public static boolean isPublicFormat(int i) {
        if (i == 22 || i == 43) {
            return true;
        }
        switch (i) {
            case 1:
            case 2:
            case 3:
            case 4:
                return true;
            default:
                return false;
        }
    }

    public static String formatToString(int i) {
        if (i == 20) {
            return "YCbCr_422_I";
        }
        if (i == 22) {
            return "RGBA_F16";
        }
        if (i == 43) {
            return "RGBA_1010102";
        }
        if (i != 256) {
            switch (i) {
                case -3:
                    return "TRANSLUCENT";
                case -2:
                    return "TRANSPARENT";
                default:
                    switch (i) {
                        case 0:
                            return IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
                        case 1:
                            return "RGBA_8888";
                        case 2:
                            return "RGBX_8888";
                        case 3:
                            return "RGB_888";
                        case 4:
                            return "RGB_565";
                        default:
                            switch (i) {
                                case 6:
                                    return "RGBA_5551";
                                case 7:
                                    return "RGBA_4444";
                                case 8:
                                    return "A_8";
                                case 9:
                                    return "L_8";
                                case 10:
                                    return "LA_88";
                                case 11:
                                    return "RGB_332";
                                default:
                                    switch (i) {
                                        case 16:
                                            return "YCbCr_422_SP";
                                        case 17:
                                            return "YCbCr_420_SP";
                                        default:
                                            return Integer.toString(i);
                                    }
                            }
                    }
            }
        }
        return "JPEG";
    }
}
