package android.support.v8.renderscript;

import com.mediatek.camera.common.device.v2.Camera2Proxy;

public class Element extends BaseObj {
    DataKind mKind;
    boolean mNormalized;
    int mSize;
    DataType mType;
    int mVectorSize;

    public int getBytesSize() {
        return this.mSize;
    }

    public int getVectorSize() {
        return this.mVectorSize;
    }

    public enum DataType {
        NONE(0, 0),
        FLOAT_32(2, 4),
        FLOAT_64(3, 8),
        SIGNED_8(4, 1),
        SIGNED_16(5, 2),
        SIGNED_32(6, 4),
        SIGNED_64(7, 8),
        UNSIGNED_8(8, 1),
        UNSIGNED_16(9, 2),
        UNSIGNED_32(10, 4),
        UNSIGNED_64(11, 8),
        BOOLEAN(12, 1),
        UNSIGNED_5_6_5(13, 2),
        UNSIGNED_5_5_5_1(14, 2),
        UNSIGNED_4_4_4_4(15, 2),
        MATRIX_4X4(16, 64),
        MATRIX_3X3(17, 36),
        MATRIX_2X2(18, 16),
        RS_ELEMENT(1000),
        RS_TYPE(1001),
        RS_ALLOCATION(1002),
        RS_SAMPLER(1003),
        RS_SCRIPT(1004);

        int mID;
        int mSize;

        DataType(int i, int i2) {
            this.mID = i;
            this.mSize = i2;
        }

        DataType(int i) {
            this.mID = i;
            this.mSize = 4;
            if (RenderScript.sPointerSize == 8) {
                this.mSize = 32;
            }
        }
    }

    public enum DataKind {
        USER(0),
        PIXEL_L(7),
        PIXEL_A(8),
        PIXEL_LA(9),
        PIXEL_RGB(10),
        PIXEL_RGBA(11),
        PIXEL_DEPTH(12),
        PIXEL_YUV(13);

        int mID;

        DataKind(int i) {
            this.mID = i;
        }
    }

    public static Element U8(RenderScript renderScript) {
        if (renderScript.mElement_U8 == null) {
            renderScript.mElement_U8 = createUser(renderScript, DataType.UNSIGNED_8);
        }
        return renderScript.mElement_U8;
    }

    public static Element A_8(RenderScript renderScript) {
        if (renderScript.mElement_A_8 == null) {
            renderScript.mElement_A_8 = createPixel(renderScript, DataType.UNSIGNED_8, DataKind.PIXEL_A);
        }
        return renderScript.mElement_A_8;
    }

    public static Element RGB_565(RenderScript renderScript) {
        if (renderScript.mElement_RGB_565 == null) {
            renderScript.mElement_RGB_565 = createPixel(renderScript, DataType.UNSIGNED_5_6_5, DataKind.PIXEL_RGB);
        }
        return renderScript.mElement_RGB_565;
    }

    public static Element RGBA_4444(RenderScript renderScript) {
        if (renderScript.mElement_RGBA_4444 == null) {
            renderScript.mElement_RGBA_4444 = createPixel(renderScript, DataType.UNSIGNED_4_4_4_4, DataKind.PIXEL_RGBA);
        }
        return renderScript.mElement_RGBA_4444;
    }

    public static Element RGBA_8888(RenderScript renderScript) {
        if (renderScript.mElement_RGBA_8888 == null) {
            renderScript.mElement_RGBA_8888 = createPixel(renderScript, DataType.UNSIGNED_8, DataKind.PIXEL_RGBA);
        }
        return renderScript.mElement_RGBA_8888;
    }

    public static Element U8_4(RenderScript renderScript) {
        if (renderScript.mElement_UCHAR_4 == null) {
            renderScript.mElement_UCHAR_4 = createVector(renderScript, DataType.UNSIGNED_8, 4);
        }
        return renderScript.mElement_UCHAR_4;
    }

    Element(long j, RenderScript renderScript, DataType dataType, DataKind dataKind, boolean z, int i) {
        super(j, renderScript);
        if (dataType != DataType.UNSIGNED_5_6_5 && dataType != DataType.UNSIGNED_4_4_4_4 && dataType != DataType.UNSIGNED_5_5_5_1) {
            if (i == 3) {
                this.mSize = dataType.mSize * 4;
            } else {
                this.mSize = dataType.mSize * i;
            }
        } else {
            this.mSize = dataType.mSize;
        }
        this.mType = dataType;
        this.mKind = dataKind;
        this.mNormalized = z;
        this.mVectorSize = i;
    }

    public long getDummyElement(RenderScript renderScript) {
        return renderScript.nIncElementCreate(this.mType.mID, this.mKind.mID, this.mNormalized, this.mVectorSize);
    }

    static Element createUser(RenderScript renderScript, DataType dataType) {
        DataKind dataKind = DataKind.USER;
        return new Element(renderScript.nElementCreate(dataType.mID, dataKind.mID, false, 1), renderScript, dataType, dataKind, false, 1);
    }

    public static Element createVector(RenderScript renderScript, DataType dataType, int i) {
        if (i < 2 || i > 4) {
            throw new RSIllegalArgumentException("Vector size out of range 2-4.");
        }
        switch (AnonymousClass1.$SwitchMap$android$support$v8$renderscript$Element$DataType[dataType.ordinal()]) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
            case Camera2Proxy.TEMPLATE_RECORD:
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
            case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
            case Camera2Proxy.TEMPLATE_MANUAL:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
                DataKind dataKind = DataKind.USER;
                return new Element(renderScript.nElementCreate(dataType.mID, dataKind.mID, false, i), renderScript, dataType, dataKind, false, i);
            default:
                throw new RSIllegalArgumentException("Cannot create vector of non-primitive type.");
        }
    }

    public static Element createPixel(RenderScript renderScript, DataType dataType, DataKind dataKind) {
        if (dataKind != DataKind.PIXEL_L && dataKind != DataKind.PIXEL_A && dataKind != DataKind.PIXEL_LA && dataKind != DataKind.PIXEL_RGB && dataKind != DataKind.PIXEL_RGBA && dataKind != DataKind.PIXEL_DEPTH && dataKind != DataKind.PIXEL_YUV) {
            throw new RSIllegalArgumentException("Unsupported DataKind");
        }
        if (dataType != DataType.UNSIGNED_8 && dataType != DataType.UNSIGNED_16 && dataType != DataType.UNSIGNED_5_6_5 && dataType != DataType.UNSIGNED_4_4_4_4 && dataType != DataType.UNSIGNED_5_5_5_1) {
            throw new RSIllegalArgumentException("Unsupported DataType");
        }
        if (dataType == DataType.UNSIGNED_5_6_5 && dataKind != DataKind.PIXEL_RGB) {
            throw new RSIllegalArgumentException("Bad kind and type combo");
        }
        if (dataType == DataType.UNSIGNED_5_5_5_1 && dataKind != DataKind.PIXEL_RGBA) {
            throw new RSIllegalArgumentException("Bad kind and type combo");
        }
        if (dataType == DataType.UNSIGNED_4_4_4_4 && dataKind != DataKind.PIXEL_RGBA) {
            throw new RSIllegalArgumentException("Bad kind and type combo");
        }
        if (dataType == DataType.UNSIGNED_16 && dataKind != DataKind.PIXEL_DEPTH) {
            throw new RSIllegalArgumentException("Bad kind and type combo");
        }
        int i = 1;
        switch (AnonymousClass1.$SwitchMap$android$support$v8$renderscript$Element$DataKind[dataKind.ordinal()]) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                i = 2;
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                i = 3;
                break;
            case Camera2Proxy.TEMPLATE_RECORD:
                i = 4;
                break;
        }
        int i2 = i;
        return new Element(renderScript.nElementCreate(dataType.mID, dataKind.mID, true, i2), renderScript, dataType, dataKind, true, i2);
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$android$support$v8$renderscript$Element$DataKind = new int[DataKind.values().length];
        static final int[] $SwitchMap$android$support$v8$renderscript$Element$DataType;

        static {
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataKind[DataKind.PIXEL_LA.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataKind[DataKind.PIXEL_RGB.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataKind[DataKind.PIXEL_RGBA.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            $SwitchMap$android$support$v8$renderscript$Element$DataType = new int[DataType.values().length];
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[DataType.FLOAT_32.ordinal()] = 1;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[DataType.FLOAT_64.ordinal()] = 2;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[DataType.SIGNED_8.ordinal()] = 3;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[DataType.SIGNED_16.ordinal()] = 4;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[DataType.SIGNED_32.ordinal()] = 5;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[DataType.SIGNED_64.ordinal()] = 6;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[DataType.UNSIGNED_8.ordinal()] = 7;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[DataType.UNSIGNED_16.ordinal()] = 8;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[DataType.UNSIGNED_32.ordinal()] = 9;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[DataType.UNSIGNED_64.ordinal()] = 10;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[DataType.BOOLEAN.ordinal()] = 11;
            } catch (NoSuchFieldError e14) {
            }
        }
    }

    public boolean isCompatible(Element element) {
        if (equals(element)) {
            return true;
        }
        return this.mSize == element.mSize && this.mType != DataType.NONE && this.mType == element.mType && this.mVectorSize == element.mVectorSize;
    }
}
