package java.lang.invoke;

import sun.invoke.util.Wrapper;

final class MethodTypeForm {
    static final boolean $assertionsDisabled = false;
    public static final int ERASE = 1;
    public static final int INTS = 4;
    public static final int LONGS = 5;
    public static final int NO_CHANGE = 0;
    public static final int RAW_RETURN = 6;
    public static final int UNWRAP = 3;
    public static final int WRAP = 2;
    final long argCounts;
    final int[] argToSlotTable;
    final MethodType basicType;
    final MethodType erasedType;
    final long primCounts;
    final int[] slotToArgTable;

    public MethodType erasedType() {
        return this.erasedType;
    }

    public MethodType basicType() {
        return this.basicType;
    }

    private boolean assertIsBasicType() {
        return true;
    }

    protected MethodTypeForm(MethodType methodType) {
        ?? r10;
        int i;
        Class<?> cls;
        int i2;
        int i3;
        int[] iArr;
        int[] iArr2;
        this.erasedType = methodType;
        Class<?>[] clsArrPtypes = methodType.ptypes();
        int length = clsArrPtypes.length;
        int i4 = 0;
        Class<?>[] clsArr = clsArrPtypes;
        int i5 = 0;
        int i6 = 0;
        for (int i7 = 0; i7 < clsArrPtypes.length; i7++) {
            Class<?> cls2 = clsArrPtypes[i7];
            if (cls2 != Object.class) {
                i6++;
                Wrapper wrapperForPrimitiveType = Wrapper.forPrimitiveType(cls2);
                i5 = wrapperForPrimitiveType.isDoubleWord() ? i5 + 1 : i5;
                if (wrapperForPrimitiveType.isSubwordOrInt() && cls2 != Integer.TYPE) {
                    clsArr = clsArr == clsArrPtypes ? (Class[]) clsArr.clone() : clsArr;
                    clsArr[i7] = Integer.TYPE;
                }
            }
        }
        int i8 = length + i5;
        Class<?> clsReturnType = methodType.returnType();
        if (clsReturnType == Object.class) {
            r10 = 0;
            i = 0;
            cls = clsReturnType;
            i2 = 1;
            i3 = 1;
        } else {
            Wrapper wrapperForPrimitiveType2 = Wrapper.forPrimitiveType(clsReturnType);
            boolean zIsDoubleWord = wrapperForPrimitiveType2.isDoubleWord();
            if (wrapperForPrimitiveType2.isSubwordOrInt() && clsReturnType != Integer.TYPE) {
                cls = Integer.TYPE;
            } else {
                cls = clsReturnType;
            }
            if (clsReturnType == Void.TYPE) {
                i2 = 0;
                i3 = 0;
                i = 1;
                r10 = zIsDoubleWord;
            } else {
                i3 = 1;
                i2 = 1 + (zIsDoubleWord ? 1 : 0);
                i = 1;
                r10 = zIsDoubleWord;
            }
        }
        if (clsArrPtypes == clsArr && cls == clsReturnType) {
            this.basicType = methodType;
            if (i5 != 0) {
                int[] iArr3 = new int[i8 + 1];
                iArr2 = new int[1 + length];
                iArr2[0] = i8;
                int i9 = i8;
                while (i4 < clsArrPtypes.length) {
                    if (Wrapper.forBasicType(clsArrPtypes[i4]).isDoubleWord()) {
                        i9--;
                    }
                    i9--;
                    i4++;
                    iArr3[i9] = i4;
                    iArr2[i4] = i9;
                }
                iArr = iArr3;
            } else if (i6 != 0) {
                MethodTypeForm methodTypeFormForm = MethodType.genericMethodType(length).form();
                iArr = methodTypeFormForm.slotToArgTable;
                iArr2 = methodTypeFormForm.argToSlotTable;
            } else {
                int i10 = length + 1;
                iArr = new int[i10];
                iArr2 = new int[i10];
                iArr2[0] = length;
                int i11 = length;
                while (i4 < length) {
                    i11--;
                    i4++;
                    iArr[i11] = i4;
                    iArr2[i4] = i11;
                }
            }
            this.primCounts = pack(r10, i, i5, i6);
            this.argCounts = pack(i2, i3, i8, length);
            this.argToSlotTable = iArr2;
            this.slotToArgTable = iArr;
            if (i8 >= 256) {
                throw MethodHandleStatics.newIllegalArgumentException("too many arguments");
            }
            return;
        }
        this.basicType = MethodType.makeImpl(cls, clsArr, true);
        MethodTypeForm methodTypeFormForm2 = this.basicType.form();
        this.primCounts = methodTypeFormForm2.primCounts;
        this.argCounts = methodTypeFormForm2.argCounts;
        this.argToSlotTable = methodTypeFormForm2.argToSlotTable;
        this.slotToArgTable = methodTypeFormForm2.slotToArgTable;
    }

    private static long pack(int i, int i2, int i3, int i4) {
        return (((long) ((i << 16) | i2)) << 32) | ((long) ((i3 << 16) | i4));
    }

    private static char unpack(long j, int i) {
        return (char) (j >> ((3 - i) * 16));
    }

    public int parameterCount() {
        return unpack(this.argCounts, 3);
    }

    public int parameterSlotCount() {
        return unpack(this.argCounts, 2);
    }

    public int returnCount() {
        return unpack(this.argCounts, 1);
    }

    public int returnSlotCount() {
        return unpack(this.argCounts, 0);
    }

    public int primitiveParameterCount() {
        return unpack(this.primCounts, 3);
    }

    public int longPrimitiveParameterCount() {
        return unpack(this.primCounts, 2);
    }

    public int primitiveReturnCount() {
        return unpack(this.primCounts, 1);
    }

    public int longPrimitiveReturnCount() {
        return unpack(this.primCounts, 0);
    }

    public boolean hasPrimitives() {
        if (this.primCounts != 0) {
            return true;
        }
        return $assertionsDisabled;
    }

    public boolean hasNonVoidPrimitives() {
        if (this.primCounts == 0) {
            return $assertionsDisabled;
        }
        if (primitiveParameterCount() != 0) {
            return true;
        }
        if (primitiveReturnCount() == 0 || returnCount() == 0) {
            return $assertionsDisabled;
        }
        return true;
    }

    public boolean hasLongPrimitives() {
        if ((longPrimitiveParameterCount() | longPrimitiveReturnCount()) != 0) {
            return true;
        }
        return $assertionsDisabled;
    }

    public int parameterToArgSlot(int i) {
        return this.argToSlotTable[1 + i];
    }

    public int argSlotToParameter(int i) {
        return this.slotToArgTable[i] - 1;
    }

    static MethodTypeForm findForm(MethodType methodType) {
        MethodType methodTypeCanonicalize = canonicalize(methodType, 1, 1);
        if (methodTypeCanonicalize == null) {
            return new MethodTypeForm(methodType);
        }
        return methodTypeCanonicalize.form();
    }

    public static MethodType canonicalize(MethodType methodType, int i, int i2) {
        Class<?>[] clsArrPtypes = methodType.ptypes();
        Class<?>[] clsArrCanonicalizeAll = canonicalizeAll(clsArrPtypes, i2);
        Class<?> clsReturnType = methodType.returnType();
        Class<?> clsCanonicalize = canonicalize(clsReturnType, i);
        if (clsArrCanonicalizeAll == null && clsCanonicalize == null) {
            return null;
        }
        if (clsCanonicalize != null) {
            clsReturnType = clsCanonicalize;
        }
        if (clsArrCanonicalizeAll == null) {
            clsArrCanonicalizeAll = clsArrPtypes;
        }
        return MethodType.makeImpl(clsReturnType, clsArrCanonicalizeAll, true);
    }

    static Class<?> canonicalize(Class<?> cls, int i) {
        if (cls != Object.class) {
            if (!cls.isPrimitive()) {
                if (i == 1) {
                    return Object.class;
                }
                if (i == 3) {
                    Class<?> clsAsPrimitiveType = Wrapper.asPrimitiveType(cls);
                    if (clsAsPrimitiveType != cls) {
                        return clsAsPrimitiveType;
                    }
                } else if (i == 6) {
                    return Object.class;
                }
            } else if (cls == Void.TYPE) {
                if (i == 2) {
                    return Void.class;
                }
                if (i == 6) {
                    return Integer.TYPE;
                }
            } else {
                if (i == 2) {
                    return Wrapper.asWrapperType(cls);
                }
                switch (i) {
                    case 4:
                        if (cls == Integer.TYPE || cls == Long.TYPE) {
                            return null;
                        }
                        if (cls == Double.TYPE) {
                            return Long.TYPE;
                        }
                        return Integer.TYPE;
                    case 5:
                        if (cls == Long.TYPE) {
                            return null;
                        }
                        return Long.TYPE;
                    case 6:
                        if (cls == Integer.TYPE || cls == Long.TYPE || cls == Float.TYPE || cls == Double.TYPE) {
                            return null;
                        }
                        return Integer.TYPE;
                }
            }
        }
        return null;
    }

    static Class<?>[] canonicalizeAll(Class<?>[] clsArr, int i) {
        int length = clsArr.length;
        Class<?>[] clsArr2 = null;
        for (int i2 = 0; i2 < length; i2++) {
            Class<?> clsCanonicalize = canonicalize(clsArr[i2], i);
            if (clsCanonicalize == Void.TYPE) {
                clsCanonicalize = null;
            }
            if (clsCanonicalize != null) {
                if (clsArr2 == null) {
                    clsArr2 = (Class[]) clsArr.clone();
                }
                clsArr2[i2] = clsCanonicalize;
            }
        }
        return clsArr2;
    }

    public String toString() {
        return "Form" + ((Object) this.erasedType);
    }
}
