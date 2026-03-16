package android.graphics;

public class ImageFormat {
    public static final int I420 = 19;
    public static final int JPEG = 256;
    public static final int NV16 = 16;
    public static final int NV21 = 17;
    public static final int RAW10 = 37;
    public static final int RAW_SENSOR = 32;
    public static final int RGB_565 = 4;
    public static final int UNKNOWN = 0;
    public static final int UYVY = 22;
    public static final int Y16 = 540422489;
    public static final int Y8 = 538982489;
    public static final int YUV_420_888 = 35;
    public static final int YUY2 = 20;
    public static final int YV12 = 842094169;

    public static int getBitsPerPixel(int format) {
        switch (format) {
            case 4:
            case 16:
            case 20:
            case 22:
            case 32:
            case Y16:
                return 16;
            case 17:
                return 12;
            case 19:
                return 12;
            case 35:
                return 12;
            case 37:
                return 10;
            case Y8:
                return 8;
            case YV12:
                return 12;
            default:
                return -1;
        }
    }

    public static boolean isPublicFormat(int format) {
        switch (format) {
            case 4:
            case 16:
            case 17:
            case 20:
            case 32:
            case 35:
            case 37:
            case 256:
            case YV12:
                return true;
            default:
                return false;
        }
    }
}
