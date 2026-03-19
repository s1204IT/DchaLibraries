package android.icu.text;

import android.icu.lang.UCharacter;

public class BidiTransform {
    private Bidi bidi;
    private int reorderingOptions;
    private int shapingOptions;
    private String text;

    public enum Mirroring {
        OFF,
        ON
    }

    public enum Order {
        LOGICAL,
        VISUAL
    }

    private enum ReorderingScheme {
        LOG_LTR_TO_VIS_LTR {
            @Override
            boolean matches(byte b, Order order, byte b2, Order order2) {
                return BidiTransform.IsLTR(b) && BidiTransform.IsLogical(order) && BidiTransform.IsLTR(b2) && BidiTransform.IsVisual(order2);
            }

            @Override
            void doTransform(BidiTransform bidiTransform) {
                bidiTransform.shapeArabic(0, 0);
                bidiTransform.resolve((byte) 0, 0);
                bidiTransform.reorder();
            }
        },
        LOG_RTL_TO_VIS_LTR {
            @Override
            boolean matches(byte b, Order order, byte b2, Order order2) {
                return BidiTransform.IsRTL(b) && BidiTransform.IsLogical(order) && BidiTransform.IsLTR(b2) && BidiTransform.IsVisual(order2);
            }

            @Override
            void doTransform(BidiTransform bidiTransform) {
                bidiTransform.resolve((byte) 1, 0);
                bidiTransform.reorder();
                bidiTransform.shapeArabic(0, 4);
            }
        },
        LOG_LTR_TO_VIS_RTL {
            @Override
            boolean matches(byte b, Order order, byte b2, Order order2) {
                return BidiTransform.IsLTR(b) && BidiTransform.IsLogical(order) && BidiTransform.IsRTL(b2) && BidiTransform.IsVisual(order2);
            }

            @Override
            void doTransform(BidiTransform bidiTransform) {
                bidiTransform.shapeArabic(0, 0);
                bidiTransform.resolve((byte) 0, 0);
                bidiTransform.reorder();
                bidiTransform.reverse();
            }
        },
        LOG_RTL_TO_VIS_RTL {
            @Override
            boolean matches(byte b, Order order, byte b2, Order order2) {
                return BidiTransform.IsRTL(b) && BidiTransform.IsLogical(order) && BidiTransform.IsRTL(b2) && BidiTransform.IsVisual(order2);
            }

            @Override
            void doTransform(BidiTransform bidiTransform) {
                bidiTransform.resolve((byte) 1, 0);
                bidiTransform.reorder();
                bidiTransform.shapeArabic(0, 4);
                bidiTransform.reverse();
            }
        },
        VIS_LTR_TO_LOG_RTL {
            @Override
            boolean matches(byte b, Order order, byte b2, Order order2) {
                return BidiTransform.IsLTR(b) && BidiTransform.IsVisual(order) && BidiTransform.IsRTL(b2) && BidiTransform.IsLogical(order2);
            }

            @Override
            void doTransform(BidiTransform bidiTransform) {
                bidiTransform.shapeArabic(0, 4);
                bidiTransform.resolve((byte) 1, 5);
                bidiTransform.reorder();
            }
        },
        VIS_RTL_TO_LOG_RTL {
            @Override
            boolean matches(byte b, Order order, byte b2, Order order2) {
                return BidiTransform.IsRTL(b) && BidiTransform.IsVisual(order) && BidiTransform.IsRTL(b2) && BidiTransform.IsLogical(order2);
            }

            @Override
            void doTransform(BidiTransform bidiTransform) {
                bidiTransform.reverse();
                bidiTransform.shapeArabic(0, 4);
                bidiTransform.resolve((byte) 1, 5);
                bidiTransform.reorder();
            }
        },
        VIS_LTR_TO_LOG_LTR {
            @Override
            boolean matches(byte b, Order order, byte b2, Order order2) {
                return BidiTransform.IsLTR(b) && BidiTransform.IsVisual(order) && BidiTransform.IsLTR(b2) && BidiTransform.IsLogical(order2);
            }

            @Override
            void doTransform(BidiTransform bidiTransform) {
                bidiTransform.resolve((byte) 0, 5);
                bidiTransform.reorder();
                bidiTransform.shapeArabic(0, 0);
            }
        },
        VIS_RTL_TO_LOG_LTR {
            @Override
            boolean matches(byte b, Order order, byte b2, Order order2) {
                return BidiTransform.IsRTL(b) && BidiTransform.IsVisual(order) && BidiTransform.IsLTR(b2) && BidiTransform.IsLogical(order2);
            }

            @Override
            void doTransform(BidiTransform bidiTransform) {
                bidiTransform.reverse();
                bidiTransform.resolve((byte) 0, 5);
                bidiTransform.reorder();
                bidiTransform.shapeArabic(0, 0);
            }
        },
        LOG_LTR_TO_LOG_RTL {
            @Override
            boolean matches(byte b, Order order, byte b2, Order order2) {
                return BidiTransform.IsLTR(b) && BidiTransform.IsLogical(order) && BidiTransform.IsRTL(b2) && BidiTransform.IsLogical(order2);
            }

            @Override
            void doTransform(BidiTransform bidiTransform) {
                bidiTransform.shapeArabic(0, 0);
                bidiTransform.resolve((byte) 0, 0);
                bidiTransform.mirror();
                bidiTransform.resolve((byte) 0, 3);
                bidiTransform.reorder();
            }
        },
        LOG_RTL_TO_LOG_LTR {
            @Override
            boolean matches(byte b, Order order, byte b2, Order order2) {
                return BidiTransform.IsRTL(b) && BidiTransform.IsLogical(order) && BidiTransform.IsLTR(b2) && BidiTransform.IsLogical(order2);
            }

            @Override
            void doTransform(BidiTransform bidiTransform) {
                bidiTransform.resolve((byte) 1, 0);
                bidiTransform.mirror();
                bidiTransform.resolve((byte) 1, 3);
                bidiTransform.reorder();
                bidiTransform.shapeArabic(0, 0);
            }
        },
        VIS_LTR_TO_VIS_RTL {
            @Override
            boolean matches(byte b, Order order, byte b2, Order order2) {
                return BidiTransform.IsLTR(b) && BidiTransform.IsVisual(order) && BidiTransform.IsRTL(b2) && BidiTransform.IsVisual(order2);
            }

            @Override
            void doTransform(BidiTransform bidiTransform) {
                bidiTransform.resolve((byte) 0, 0);
                bidiTransform.mirror();
                bidiTransform.shapeArabic(0, 4);
                bidiTransform.reverse();
            }
        },
        VIS_RTL_TO_VIS_LTR {
            @Override
            boolean matches(byte b, Order order, byte b2, Order order2) {
                return BidiTransform.IsRTL(b) && BidiTransform.IsVisual(order) && BidiTransform.IsLTR(b2) && BidiTransform.IsVisual(order2);
            }

            @Override
            void doTransform(BidiTransform bidiTransform) {
                bidiTransform.reverse();
                bidiTransform.resolve((byte) 0, 0);
                bidiTransform.mirror();
                bidiTransform.shapeArabic(0, 4);
            }
        },
        LOG_LTR_TO_LOG_LTR {
            @Override
            boolean matches(byte b, Order order, byte b2, Order order2) {
                return BidiTransform.IsLTR(b) && BidiTransform.IsLogical(order) && BidiTransform.IsLTR(b2) && BidiTransform.IsLogical(order2);
            }

            @Override
            void doTransform(BidiTransform bidiTransform) {
                bidiTransform.resolve((byte) 0, 0);
                bidiTransform.mirror();
                bidiTransform.shapeArabic(0, 0);
            }
        },
        LOG_RTL_TO_LOG_RTL {
            @Override
            boolean matches(byte b, Order order, byte b2, Order order2) {
                return BidiTransform.IsRTL(b) && BidiTransform.IsLogical(order) && BidiTransform.IsRTL(b2) && BidiTransform.IsLogical(order2);
            }

            @Override
            void doTransform(BidiTransform bidiTransform) {
                bidiTransform.resolve((byte) 1, 0);
                bidiTransform.mirror();
                bidiTransform.shapeArabic(4, 0);
            }
        },
        VIS_LTR_TO_VIS_LTR {
            @Override
            boolean matches(byte b, Order order, byte b2, Order order2) {
                return BidiTransform.IsLTR(b) && BidiTransform.IsVisual(order) && BidiTransform.IsLTR(b2) && BidiTransform.IsVisual(order2);
            }

            @Override
            void doTransform(BidiTransform bidiTransform) {
                bidiTransform.resolve((byte) 0, 0);
                bidiTransform.mirror();
                bidiTransform.shapeArabic(0, 4);
            }
        },
        VIS_RTL_TO_VIS_RTL {
            @Override
            boolean matches(byte b, Order order, byte b2, Order order2) {
                return BidiTransform.IsRTL(b) && BidiTransform.IsVisual(order) && BidiTransform.IsRTL(b2) && BidiTransform.IsVisual(order2);
            }

            @Override
            void doTransform(BidiTransform bidiTransform) {
                bidiTransform.reverse();
                bidiTransform.resolve((byte) 0, 0);
                bidiTransform.mirror();
                bidiTransform.shapeArabic(0, 4);
                bidiTransform.reverse();
            }
        };

        abstract void doTransform(BidiTransform bidiTransform);

        abstract boolean matches(byte b, Order order, byte b2, Order order2);
    }

    public String transform(CharSequence charSequence, byte b, Order order, byte b2, Order order2, Mirroring mirroring, int i) {
        if (charSequence == null || order == null || order2 == null || mirroring == null) {
            throw new IllegalArgumentException();
        }
        this.text = charSequence.toString();
        byte[] bArr = {b, b2};
        resolveBaseDirection(bArr);
        ReorderingScheme reorderingSchemeFindMatchingScheme = findMatchingScheme(bArr[0], order, bArr[1], order2);
        if (reorderingSchemeFindMatchingScheme != null) {
            this.bidi = new Bidi();
            this.reorderingOptions = Mirroring.ON.equals(mirroring) ? 2 : 0;
            this.shapingOptions = i & (-5);
            reorderingSchemeFindMatchingScheme.doTransform(this);
        }
        return this.text;
    }

    private void resolveBaseDirection(byte[] bArr) {
        if (!Bidi.IsDefaultLevel(bArr[0])) {
            bArr[0] = (byte) (bArr[0] & 1);
        } else {
            byte baseDirection = Bidi.getBaseDirection(this.text);
            if (baseDirection == 3) {
                baseDirection = bArr[0] == 127 ? (byte) 1 : (byte) 0;
            }
            bArr[0] = baseDirection;
        }
        if (Bidi.IsDefaultLevel(bArr[1])) {
            bArr[1] = bArr[0];
        } else {
            bArr[1] = (byte) (bArr[1] & 1);
        }
    }

    private ReorderingScheme findMatchingScheme(byte b, Order order, byte b2, Order order2) {
        for (ReorderingScheme reorderingScheme : ReorderingScheme.values()) {
            if (reorderingScheme.matches(b, order, b2, order2)) {
                return reorderingScheme;
            }
        }
        return null;
    }

    private void resolve(byte b, int i) {
        this.bidi.setInverse((i & 5) != 0);
        this.bidi.setReorderingMode(i);
        this.bidi.setPara(this.text, b, (byte[]) null);
    }

    private void reorder() {
        this.text = this.bidi.writeReordered(this.reorderingOptions);
        this.reorderingOptions = 0;
    }

    private void reverse() {
        this.text = Bidi.writeReverse(this.text, 0);
    }

    private void mirror() {
        if ((this.reorderingOptions & 2) == 0) {
            return;
        }
        StringBuffer stringBuffer = new StringBuffer(this.text);
        byte[] levels = this.bidi.getLevels();
        int charCount = 0;
        int length = levels.length;
        while (charCount < length) {
            int iCharAt = UTF16.charAt(stringBuffer, charCount);
            if ((levels[charCount] & 1) != 0) {
                UTF16.setCharAt(stringBuffer, charCount, UCharacter.getMirror(iCharAt));
            }
            charCount += UTF16.getCharCount(iCharAt);
        }
        this.text = stringBuffer.toString();
        this.reorderingOptions &= -3;
    }

    private void shapeArabic(int i, int i2) {
        if (i == i2) {
            shapeArabic(i | this.shapingOptions);
        } else {
            shapeArabic(i | (this.shapingOptions & (-25)));
            shapeArabic((this.shapingOptions & (-225)) | i2);
        }
    }

    private void shapeArabic(int i) {
        if (i != 0) {
            try {
                this.text = new ArabicShaping(i).shape(this.text);
            } catch (ArabicShapingException e) {
            }
        }
    }

    private static boolean IsLTR(byte b) {
        return (b & 1) == 0;
    }

    private static boolean IsRTL(byte b) {
        return (b & 1) == 1;
    }

    private static boolean IsLogical(Order order) {
        return Order.LOGICAL.equals(order);
    }

    private static boolean IsVisual(Order order) {
        return Order.VISUAL.equals(order);
    }
}
