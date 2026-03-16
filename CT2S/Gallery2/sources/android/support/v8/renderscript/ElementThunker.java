package android.support.v8.renderscript;

import android.renderscript.Element;
import android.support.v4.app.FragmentManagerImpl;
import android.support.v4.app.NotificationCompat;
import android.support.v8.renderscript.Element;

class ElementThunker extends Element {
    android.renderscript.Element mN;

    @Override
    android.renderscript.Element getNObj() {
        return this.mN;
    }

    @Override
    public int getBytesSize() {
        try {
            return this.mN.getBytesSize();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public int getVectorSize() {
        try {
            return this.mN.getVectorSize();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    static Element.DataKind convertKind(Element.DataKind cdk) {
        switch (AnonymousClass1.$SwitchMap$android$support$v8$renderscript$Element$DataKind[cdk.ordinal()]) {
            case 1:
                return Element.DataKind.USER;
            case 2:
                return Element.DataKind.PIXEL_L;
            case 3:
                return Element.DataKind.PIXEL_A;
            case 4:
                return Element.DataKind.PIXEL_LA;
            case 5:
                return Element.DataKind.PIXEL_RGB;
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                return Element.DataKind.PIXEL_RGBA;
            default:
                return null;
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$android$support$v8$renderscript$Element$DataKind;
        static final int[] $SwitchMap$android$support$v8$renderscript$Element$DataType = new int[Element.DataType.values().length];

        static {
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.NONE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.FLOAT_32.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.FLOAT_64.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.SIGNED_8.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.SIGNED_16.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.SIGNED_32.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.SIGNED_64.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.UNSIGNED_8.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.UNSIGNED_16.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.UNSIGNED_32.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.UNSIGNED_64.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.BOOLEAN.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.MATRIX_4X4.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.MATRIX_3X3.ordinal()] = 14;
            } catch (NoSuchFieldError e14) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.MATRIX_2X2.ordinal()] = 15;
            } catch (NoSuchFieldError e15) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.RS_ELEMENT.ordinal()] = 16;
            } catch (NoSuchFieldError e16) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.RS_TYPE.ordinal()] = 17;
            } catch (NoSuchFieldError e17) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.RS_ALLOCATION.ordinal()] = 18;
            } catch (NoSuchFieldError e18) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.RS_SAMPLER.ordinal()] = 19;
            } catch (NoSuchFieldError e19) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataType[Element.DataType.RS_SCRIPT.ordinal()] = 20;
            } catch (NoSuchFieldError e20) {
            }
            $SwitchMap$android$support$v8$renderscript$Element$DataKind = new int[Element.DataKind.values().length];
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataKind[Element.DataKind.USER.ordinal()] = 1;
            } catch (NoSuchFieldError e21) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataKind[Element.DataKind.PIXEL_L.ordinal()] = 2;
            } catch (NoSuchFieldError e22) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataKind[Element.DataKind.PIXEL_A.ordinal()] = 3;
            } catch (NoSuchFieldError e23) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataKind[Element.DataKind.PIXEL_LA.ordinal()] = 4;
            } catch (NoSuchFieldError e24) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataKind[Element.DataKind.PIXEL_RGB.ordinal()] = 5;
            } catch (NoSuchFieldError e25) {
            }
            try {
                $SwitchMap$android$support$v8$renderscript$Element$DataKind[Element.DataKind.PIXEL_RGBA.ordinal()] = 6;
            } catch (NoSuchFieldError e26) {
            }
        }
    }

    static Element.DataType convertType(Element.DataType cdt) {
        switch (AnonymousClass1.$SwitchMap$android$support$v8$renderscript$Element$DataType[cdt.ordinal()]) {
            case 1:
                return Element.DataType.NONE;
            case 2:
                return Element.DataType.FLOAT_32;
            case 3:
                return Element.DataType.FLOAT_64;
            case 4:
                return Element.DataType.SIGNED_8;
            case 5:
                return Element.DataType.SIGNED_16;
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                return Element.DataType.SIGNED_32;
            case 7:
                return Element.DataType.SIGNED_64;
            case NotificationCompat.FLAG_ONLY_ALERT_ONCE:
                return Element.DataType.UNSIGNED_8;
            case 9:
                return Element.DataType.UNSIGNED_16;
            case 10:
                return Element.DataType.UNSIGNED_32;
            case 11:
                return Element.DataType.UNSIGNED_64;
            case 12:
                return Element.DataType.BOOLEAN;
            case 13:
                return Element.DataType.MATRIX_4X4;
            case 14:
                return Element.DataType.MATRIX_3X3;
            case 15:
                return Element.DataType.MATRIX_2X2;
            case NotificationCompat.FLAG_AUTO_CANCEL:
                return Element.DataType.RS_ELEMENT;
            case 17:
                return Element.DataType.RS_TYPE;
            case 18:
                return Element.DataType.RS_ALLOCATION;
            case 19:
                return Element.DataType.RS_SAMPLER;
            case 20:
                return Element.DataType.RS_SCRIPT;
            default:
                return null;
        }
    }

    @Override
    public boolean isComplex() {
        try {
            return this.mN.isComplex();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public int getSubElementCount() {
        try {
            return this.mN.getSubElementCount();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public Element getSubElement(int index) {
        try {
            return new ElementThunker(this.mRS, this.mN.getSubElement(index));
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public String getSubElementName(int index) {
        try {
            return this.mN.getSubElementName(index);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public int getSubElementArraySize(int index) {
        try {
            return this.mN.getSubElementArraySize(index);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public int getSubElementOffsetBytes(int index) {
        try {
            return this.mN.getSubElementOffsetBytes(index);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public Element.DataType getDataType() {
        return this.mType;
    }

    @Override
    public Element.DataKind getDataKind() {
        return this.mKind;
    }

    ElementThunker(RenderScript rs, android.renderscript.Element e) {
        super(0, rs);
        this.mN = e;
    }

    static Element create(RenderScript rs, Element.DataType dt) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        android.renderscript.Element e = null;
        try {
            switch (AnonymousClass1.$SwitchMap$android$support$v8$renderscript$Element$DataType[dt.ordinal()]) {
                case 2:
                    e = android.renderscript.Element.F32(rst.mN);
                    break;
                case 3:
                    e = android.renderscript.Element.F64(rst.mN);
                    break;
                case 4:
                    e = android.renderscript.Element.I8(rst.mN);
                    break;
                case 5:
                    e = android.renderscript.Element.I16(rst.mN);
                    break;
                case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                    e = android.renderscript.Element.I32(rst.mN);
                    break;
                case 7:
                    e = android.renderscript.Element.I64(rst.mN);
                    break;
                case NotificationCompat.FLAG_ONLY_ALERT_ONCE:
                    e = android.renderscript.Element.U8(rst.mN);
                    break;
                case 9:
                    e = android.renderscript.Element.U16(rst.mN);
                    break;
                case 10:
                    e = android.renderscript.Element.U32(rst.mN);
                    break;
                case 11:
                    e = android.renderscript.Element.U64(rst.mN);
                    break;
                case 12:
                    e = android.renderscript.Element.BOOLEAN(rst.mN);
                    break;
                case 13:
                    e = android.renderscript.Element.MATRIX_4X4(rst.mN);
                    break;
                case 14:
                    e = android.renderscript.Element.MATRIX_3X3(rst.mN);
                    break;
                case 15:
                    e = android.renderscript.Element.MATRIX_2X2(rst.mN);
                    break;
                case NotificationCompat.FLAG_AUTO_CANCEL:
                    e = android.renderscript.Element.ELEMENT(rst.mN);
                    break;
                case 17:
                    e = android.renderscript.Element.TYPE(rst.mN);
                    break;
                case 18:
                    e = android.renderscript.Element.ALLOCATION(rst.mN);
                    break;
                case 19:
                    e = android.renderscript.Element.SAMPLER(rst.mN);
                    break;
                case 20:
                    e = android.renderscript.Element.SCRIPT(rst.mN);
                    break;
            }
            return new ElementThunker(rs, e);
        } catch (android.renderscript.RSRuntimeException e2) {
            throw ExceptionThunker.convertException(e2);
        }
    }

    public static Element createVector(RenderScript rs, Element.DataType dt, int size) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        try {
            android.renderscript.Element e = android.renderscript.Element.createVector(rst.mN, convertType(dt), size);
            return new ElementThunker(rs, e);
        } catch (android.renderscript.RSRuntimeException exc) {
            throw ExceptionThunker.convertException(exc);
        }
    }

    public static Element createPixel(RenderScript rs, Element.DataType dt, Element.DataKind dk) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        try {
            android.renderscript.Element e = android.renderscript.Element.createPixel(rst.mN, convertType(dt), convertKind(dk));
            return new ElementThunker(rs, e);
        } catch (android.renderscript.RSRuntimeException exc) {
            throw ExceptionThunker.convertException(exc);
        }
    }

    @Override
    public boolean isCompatible(Element e) {
        ElementThunker et = (ElementThunker) e;
        try {
            return et.mN.isCompatible(this.mN);
        } catch (android.renderscript.RSRuntimeException exc) {
            throw ExceptionThunker.convertException(exc);
        }
    }

    static class BuilderThunker {
        Element.Builder mN;

        public BuilderThunker(RenderScript rs) {
            RenderScriptThunker rst = (RenderScriptThunker) rs;
            try {
                this.mN = new Element.Builder(rst.mN);
            } catch (android.renderscript.RSRuntimeException e) {
                throw ExceptionThunker.convertException(e);
            }
        }

        public void add(Element e, String name, int arraySize) {
            ElementThunker et = (ElementThunker) e;
            try {
                this.mN.add(et.mN, name, arraySize);
            } catch (android.renderscript.RSRuntimeException exc) {
                throw ExceptionThunker.convertException(exc);
            }
        }

        public Element create(RenderScript rs) {
            try {
                android.renderscript.Element e = this.mN.create();
                return new ElementThunker(rs, e);
            } catch (android.renderscript.RSRuntimeException exc) {
                throw ExceptionThunker.convertException(exc);
            }
        }
    }
}
