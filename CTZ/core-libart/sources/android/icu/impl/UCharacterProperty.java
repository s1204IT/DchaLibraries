package android.icu.impl;

import android.icu.impl.ICUBinary;
import android.icu.impl.Normalizer2Impl;
import android.icu.impl.Trie2;
import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;
import android.icu.lang.UScript;
import android.icu.text.Normalizer2;
import android.icu.text.UTF16;
import android.icu.text.UnicodeSet;
import android.icu.util.ICUException;
import android.icu.util.VersionInfo;
import dalvik.system.VMDebug;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.MissingResourceException;

public final class UCharacterProperty {
    static final boolean $assertionsDisabled = false;
    private static final int AGE_SHIFT_ = 24;
    private static final int ALPHABETIC_PROPERTY_ = 8;
    private static final int ASCII_HEX_DIGIT_PROPERTY_ = 7;
    private static final int BLOCK_MASK_ = 130816;
    private static final int BLOCK_SHIFT_ = 8;
    private static final int CGJ = 847;
    private static final int CR = 13;
    private static final int DASH_PROPERTY_ = 1;
    private static final String DATA_FILE_NAME_ = "uprops.icu";
    private static final int DATA_FORMAT = 1431335535;
    private static final int DECOMPOSITION_TYPE_MASK_ = 31;
    private static final int DEFAULT_IGNORABLE_CODE_POINT_PROPERTY_ = 19;
    private static final int DEL = 127;
    private static final int DEPRECATED_PROPERTY_ = 20;
    private static final int DIACRITIC_PROPERTY_ = 10;
    private static final int EAST_ASIAN_MASK_ = 917504;
    private static final int EAST_ASIAN_SHIFT_ = 17;
    private static final int EXTENDER_PROPERTY_ = 11;
    private static final int FIGURESP = 8199;
    private static final int FIRST_NIBBLE_SHIFT_ = 4;
    private static final int GCB_MASK = 992;
    private static final int GCB_SHIFT = 5;
    private static final int GRAPHEME_BASE_PROPERTY_ = 26;
    private static final int GRAPHEME_EXTEND_PROPERTY_ = 13;
    private static final int GRAPHEME_LINK_PROPERTY_ = 14;
    private static final int HAIRSP = 8202;
    private static final int HEX_DIGIT_PROPERTY_ = 6;
    private static final int HYPHEN_PROPERTY_ = 2;
    private static final int IDEOGRAPHIC_PROPERTY_ = 9;
    private static final int IDS_BINARY_OPERATOR_PROPERTY_ = 15;
    private static final int IDS_TRINARY_OPERATOR_PROPERTY_ = 16;
    private static final int ID_CONTINUE_PROPERTY_ = 25;
    private static final int ID_START_PROPERTY_ = 24;
    private static final int INHSWAP = 8298;
    public static final UCharacterProperty INSTANCE;
    private static final int LAST_NIBBLE_MASK_ = 15;
    public static final char LATIN_CAPITAL_LETTER_I_WITH_DOT_ABOVE_ = 304;
    public static final char LATIN_SMALL_LETTER_DOTLESS_I_ = 305;
    public static final char LATIN_SMALL_LETTER_I_ = 'i';
    private static final int LB_MASK = 66060288;
    private static final int LB_SHIFT = 20;
    private static final int LOGICAL_ORDER_EXCEPTION_PROPERTY_ = 21;
    private static final int MATH_PROPERTY_ = 5;
    static final int MY_MASK = 30;
    private static final int NBSP = 160;
    private static final int NL = 133;
    private static final int NNBSP = 8239;
    private static final int NOMDIG = 8303;
    private static final int NONCHARACTER_CODE_POINT_PROPERTY_ = 12;
    private static final int NTV_BASE60_START_ = 768;
    private static final int NTV_DECIMAL_START_ = 1;
    private static final int NTV_DIGIT_START_ = 11;
    private static final int NTV_FRACTION20_START_ = 804;
    private static final int NTV_FRACTION_START_ = 176;
    private static final int NTV_LARGE_START_ = 480;
    private static final int NTV_NONE_ = 0;
    private static final int NTV_NUMERIC_START_ = 21;
    private static final int NTV_RESERVED_START_ = 828;
    private static final int NUMERIC_TYPE_VALUE_SHIFT_ = 6;
    private static final int PATTERN_SYNTAX = 29;
    private static final int PATTERN_WHITE_SPACE = 30;
    private static final int PREPENDED_CONCATENATION_MARK = 31;
    private static final int PROPS_2_EMOJI = 28;
    private static final int PROPS_2_EMOJI_COMPONENT = 27;
    private static final int PROPS_2_EMOJI_MODIFIER = 30;
    private static final int PROPS_2_EMOJI_MODIFIER_BASE = 31;
    private static final int PROPS_2_EMOJI_PRESENTATION = 29;
    private static final int QUOTATION_MARK_PROPERTY_ = 3;
    private static final int RADICAL_PROPERTY_ = 17;
    private static final int RLM = 8207;
    private static final int SB_MASK = 1015808;
    private static final int SB_SHIFT = 15;
    public static final int SCRIPT_MASK_ = 255;
    public static final int SCRIPT_X_MASK = 12583167;
    public static final int SCRIPT_X_WITH_COMMON = 4194304;
    public static final int SCRIPT_X_WITH_INHERITED = 8388608;
    public static final int SCRIPT_X_WITH_OTHER = 12582912;
    public static final int SRC_BIDI = 5;
    public static final int SRC_CASE = 4;
    public static final int SRC_CASE_AND_NORM = 7;
    public static final int SRC_CHAR = 1;
    public static final int SRC_CHAR_AND_PROPSVEC = 6;
    public static final int SRC_COUNT = 12;
    public static final int SRC_NAMES = 3;
    public static final int SRC_NFC = 8;
    public static final int SRC_NFC_CANON_ITER = 11;
    public static final int SRC_NFKC = 9;
    public static final int SRC_NFKC_CF = 10;
    public static final int SRC_NONE = 0;
    public static final int SRC_PROPSVEC = 2;
    private static final int S_TERM_PROPERTY_ = 27;
    private static final int TAB = 9;
    private static final int TERMINAL_PUNCTUATION_PROPERTY_ = 4;
    public static final int TYPE_MASK = 31;
    private static final int UNIFIED_IDEOGRAPH_PROPERTY_ = 18;
    private static final int U_A = 65;
    private static final int U_F = 70;
    private static final int U_FW_A = 65313;
    private static final int U_FW_F = 65318;
    private static final int U_FW_Z = 65338;
    private static final int U_FW_a = 65345;
    private static final int U_FW_f = 65350;
    private static final int U_FW_z = 65370;
    private static final int U_Z = 90;
    private static final int U_a = 97;
    private static final int U_f = 102;
    private static final int U_z = 122;
    private static final int VARIATION_SELECTOR_PROPERTY_ = 28;
    private static final int WB_MASK = 31744;
    private static final int WB_SHIFT = 10;
    private static final int WHITE_SPACE_PROPERTY_ = 0;
    private static final int WJ = 8288;
    private static final int XID_CONTINUE_PROPERTY_ = 23;
    private static final int XID_START_PROPERTY_ = 22;
    private static final int ZWNBSP = 65279;
    BinaryProperty[] binProps;
    IntProperty[] intProps;
    int m_additionalColumnsCount_;
    Trie2_16 m_additionalTrie_;
    int[] m_additionalVectors_;
    int m_maxBlockScriptValue_;
    int m_maxJTGValue_;
    public char[] m_scriptExtensions_;
    public Trie2_16 m_trie_;
    public VersionInfo m_unicodeVersion_;
    private static final int GC_CN_MASK = getMask(0);
    private static final int GC_CC_MASK = getMask(15);
    private static final int GC_CS_MASK = getMask(18);
    private static final int GC_ZS_MASK = getMask(12);
    private static final int GC_ZL_MASK = getMask(13);
    private static final int GC_ZP_MASK = getMask(14);
    private static final int GC_Z_MASK = (GC_ZS_MASK | GC_ZL_MASK) | GC_ZP_MASK;
    private static final int[] gcbToHst = {0, 0, 0, 0, 1, 0, 4, 5, 3, 2};

    static {
        try {
            INSTANCE = new UCharacterProperty();
        } catch (IOException e) {
            throw new MissingResourceException(e.getMessage(), "", "");
        }
    }

    public final int getProperty(int i) {
        return this.m_trie_.get(i);
    }

    public int getAdditional(int i, int i2) {
        if (i2 >= this.m_additionalColumnsCount_) {
            return 0;
        }
        return this.m_additionalVectors_[this.m_additionalTrie_.get(i) + i2];
    }

    public VersionInfo getAge(int i) {
        int additional = getAdditional(i, 0) >> 24;
        return VersionInfo.getInstance((additional >> 4) & 15, additional & 15, 0, 0);
    }

    private static final boolean isgraphPOSIX(int i) {
        return (getMask(UCharacter.getType(i)) & (((GC_CC_MASK | GC_CS_MASK) | GC_CN_MASK) | GC_Z_MASK)) == 0;
    }

    private class BinaryProperty {
        int column;
        int mask;

        BinaryProperty(int i, int i2) {
            this.column = i;
            this.mask = i2;
        }

        BinaryProperty(int i) {
            this.column = i;
            this.mask = 0;
        }

        final int getSource() {
            if (this.mask == 0) {
                return this.column;
            }
            return 2;
        }

        boolean contains(int i) {
            return (UCharacterProperty.this.getAdditional(i, this.column) & this.mask) != 0;
        }
    }

    private class CaseBinaryProperty extends BinaryProperty {
        int which;

        CaseBinaryProperty(int i) {
            super(4);
            this.which = i;
        }

        @Override
        boolean contains(int i) {
            return UCaseProps.INSTANCE.hasBinaryProperty(i, this.which);
        }
    }

    private class NormInertBinaryProperty extends BinaryProperty {
        int which;

        NormInertBinaryProperty(int i, int i2) {
            super(i);
            this.which = i2;
        }

        @Override
        boolean contains(int i) {
            return Norm2AllModes.getN2WithImpl(this.which - 37).isInert(i);
        }
    }

    public boolean hasBinaryProperty(int i, int i2) {
        if (i2 < 0 || 64 <= i2) {
            return false;
        }
        return this.binProps[i2].contains(i);
    }

    public int getType(int i) {
        return getProperty(i) & 31;
    }

    private class IntProperty {
        int column;
        int mask;
        int shift;

        IntProperty(int i, int i2, int i3) {
            this.column = i;
            this.mask = i2;
            this.shift = i3;
        }

        IntProperty(int i) {
            this.column = i;
            this.mask = 0;
        }

        final int getSource() {
            if (this.mask == 0) {
                return this.column;
            }
            return 2;
        }

        int getValue(int i) {
            return (UCharacterProperty.this.getAdditional(i, this.column) & this.mask) >>> this.shift;
        }

        int getMaxValue(int i) {
            return (UCharacterProperty.this.getMaxValues(this.column) & this.mask) >>> this.shift;
        }
    }

    private class BiDiIntProperty extends IntProperty {
        BiDiIntProperty() {
            super(5);
        }

        @Override
        int getMaxValue(int i) {
            return UBiDiProps.INSTANCE.getMaxValue(i);
        }
    }

    private class CombiningClassIntProperty extends IntProperty {
        CombiningClassIntProperty(int i) {
            super(i);
        }

        @Override
        int getMaxValue(int i) {
            return 255;
        }
    }

    private class NormQuickCheckIntProperty extends IntProperty {
        int max;
        int which;

        NormQuickCheckIntProperty(int i, int i2, int i3) {
            super(i);
            this.which = i2;
            this.max = i3;
        }

        @Override
        int getValue(int i) {
            return Norm2AllModes.getN2WithImpl(this.which - 4108).getQuickCheck(i);
        }

        @Override
        int getMaxValue(int i) {
            return this.max;
        }
    }

    public int getIntPropertyValue(int i, int i2) {
        if (i2 < 4096) {
            if (i2 < 0 || i2 >= 64) {
                return 0;
            }
            return this.binProps[i2].contains(i) ? 1 : 0;
        }
        if (i2 < 4118) {
            return this.intProps[i2 - 4096].getValue(i);
        }
        if (i2 == 8192) {
            return getMask(getType(i));
        }
        return 0;
    }

    public int getIntPropertyMaxValue(int i) {
        if (i < 4096) {
            if (i >= 0 && i < 64) {
                return 1;
            }
            return -1;
        }
        if (i < 4118) {
            return this.intProps[i - 4096].getMaxValue(i);
        }
        return -1;
    }

    public final int getSource(int i) {
        if (i < 0) {
            return 0;
        }
        if (i < 64) {
            return this.binProps[i].getSource();
        }
        if (i < 4096) {
            return 0;
        }
        if (i < 4118) {
            return this.intProps[i - 4096].getSource();
        }
        if (i < 16384) {
            if (i != 8192 && i != 12288) {
                return 0;
            }
            return 1;
        }
        if (i >= 16398) {
            return i != 28672 ? 0 : 2;
        }
        switch (i) {
            case 16384:
                return 2;
            case UProperty.BIDI_MIRRORING_GLYPH:
                return 5;
            case UProperty.CASE_FOLDING:
            case UProperty.LOWERCASE_MAPPING:
            case UProperty.SIMPLE_CASE_FOLDING:
            case UProperty.SIMPLE_LOWERCASE_MAPPING:
            case UProperty.SIMPLE_TITLECASE_MAPPING:
            case UProperty.SIMPLE_UPPERCASE_MAPPING:
            case UProperty.TITLECASE_MAPPING:
            case UProperty.UPPERCASE_MAPPING:
                return 4;
            case UProperty.ISO_COMMENT:
            case UProperty.NAME:
            case UProperty.UNICODE_1_NAME:
                return 3;
            default:
                return 0;
        }
    }

    public int getMaxValues(int i) {
        if (i == 0) {
            return this.m_maxBlockScriptValue_;
        }
        if (i == 2) {
            return this.m_maxJTGValue_;
        }
        return 0;
    }

    public static final int getMask(int i) {
        return 1 << i;
    }

    public static int getEuropeanDigit(int i) {
        if (i > 122 && i < U_FW_A) {
            return -1;
        }
        if (i < 65) {
            return -1;
        }
        if ((i > 90 && i < 97) || i > U_FW_z) {
            return -1;
        }
        if (i > U_FW_Z && i < U_FW_a) {
            return -1;
        }
        if (i <= 122) {
            return (i + 10) - (i > 90 ? 97 : 65);
        }
        if (i <= U_FW_Z) {
            return (i + 10) - U_FW_A;
        }
        return (i + 10) - U_FW_a;
    }

    public int digit(int i) {
        int numericTypeValue = getNumericTypeValue(getProperty(i)) - 1;
        if (numericTypeValue <= 9) {
            return numericTypeValue;
        }
        return -1;
    }

    public int getNumericValue(int i) {
        int numericTypeValue = getNumericTypeValue(getProperty(i));
        if (numericTypeValue == 0) {
            return getEuropeanDigit(i);
        }
        if (numericTypeValue < 11) {
            return numericTypeValue - 1;
        }
        if (numericTypeValue < 21) {
            return numericTypeValue - 11;
        }
        if (numericTypeValue < 176) {
            return numericTypeValue - 21;
        }
        if (numericTypeValue < NTV_LARGE_START_) {
            return -2;
        }
        if (numericTypeValue < 768) {
            int i2 = (numericTypeValue >> 5) - 14;
            int i3 = (numericTypeValue & 31) + 2;
            if (i3 >= 9 && (i3 != 9 || i2 > 2)) {
                return -2;
            }
            do {
                i2 *= 10;
                i3--;
            } while (i3 > 0);
            return i2;
        }
        if (numericTypeValue >= NTV_FRACTION20_START_) {
            return numericTypeValue < NTV_RESERVED_START_ ? -2 : -2;
        }
        int i4 = (numericTypeValue >> 2) - 191;
        switch ((numericTypeValue & 3) + 1) {
            case 1:
                return i4 * 60;
            case 2:
                return i4 * 3600;
            case 3:
                return i4 * 216000;
            case 4:
                return i4 * 12960000;
            default:
                return i4;
        }
    }

    public double getUnicodeNumericValue(int i) {
        int numericTypeValue = getNumericTypeValue(getProperty(i));
        if (numericTypeValue == 0) {
            return -1.23456789E8d;
        }
        if (numericTypeValue < 11) {
            return numericTypeValue - 1;
        }
        if (numericTypeValue < 21) {
            return numericTypeValue - 11;
        }
        if (numericTypeValue < 176) {
            return numericTypeValue - 21;
        }
        if (numericTypeValue < NTV_LARGE_START_) {
            return ((double) ((numericTypeValue >> 4) - 12)) / ((double) ((numericTypeValue & 15) + 1));
        }
        if (numericTypeValue < 768) {
            int i2 = (numericTypeValue >> 5) - 14;
            int i3 = (numericTypeValue & 31) + 2;
            double d = i2;
            while (i3 >= 4) {
                d *= 10000.0d;
                i3 -= 4;
            }
            switch (i3) {
                case 1:
                    return d * 10.0d;
                case 2:
                    return d * 100.0d;
                case 3:
                    return d * 1000.0d;
                default:
                    return d;
            }
        }
        if (numericTypeValue < NTV_FRACTION20_START_) {
            int i4 = (numericTypeValue >> 2) - 191;
            switch ((numericTypeValue & 3) + 1) {
                case 1:
                    i4 *= 60;
                    break;
                case 2:
                    i4 *= 3600;
                    break;
                case 3:
                    i4 *= 216000;
                    break;
                case 4:
                    i4 *= 12960000;
                    break;
            }
            return i4;
        }
        if (numericTypeValue >= NTV_RESERVED_START_) {
            return -1.23456789E8d;
        }
        int i5 = numericTypeValue - NTV_FRACTION20_START_;
        return ((double) (((i5 & 3) * 2) + 1)) / ((double) (20 << (i5 >> 2)));
    }

    private static final int getNumericTypeValue(int i) {
        return i >> 6;
    }

    private static final int ntvGetType(int i) {
        if (i == 0) {
            return 0;
        }
        if (i < 11) {
            return 1;
        }
        return i < 21 ? 2 : 3;
    }

    private UCharacterProperty() throws IOException {
        int i = 1;
        int i2 = 0;
        int i3 = 5;
        int i4 = 2;
        int i5 = 8;
        this.binProps = new BinaryProperty[]{new BinaryProperty(1, 256), new BinaryProperty(1, 128), new BinaryProperty(i3) {
            @Override
            boolean contains(int i6) {
                return UBiDiProps.INSTANCE.isBidiControl(i6);
            }
        }, new BinaryProperty(i3) {
            @Override
            boolean contains(int i6) {
                return UBiDiProps.INSTANCE.isMirrored(i6);
            }
        }, new BinaryProperty(1, 2), new BinaryProperty(1, 524288), new BinaryProperty(1, VMDebug.KIND_THREAD_GC_INVOCATIONS), new BinaryProperty(1, 1024), new BinaryProperty(1, 2048), new BinaryProperty(i5) {
            @Override
            boolean contains(int i6) {
                Normalizer2Impl normalizer2Impl = Norm2AllModes.getNFCInstance().impl;
                return normalizer2Impl.isCompNo(normalizer2Impl.getNorm16(i6));
            }
        }, new BinaryProperty(1, 67108864), new BinaryProperty(1, 8192), new BinaryProperty(1, 16384), new BinaryProperty(1, 64), new BinaryProperty(1, 4), new BinaryProperty(1, 33554432), new BinaryProperty(1, 16777216), new BinaryProperty(1, 512), new BinaryProperty(1, 32768), new BinaryProperty(1, 65536), new BinaryProperty(i3) {
            @Override
            boolean contains(int i6) {
                return UBiDiProps.INSTANCE.isJoinControl(i6);
            }
        }, new BinaryProperty(1, 2097152), new CaseBinaryProperty(22), new BinaryProperty(1, 32), new BinaryProperty(1, 4096), new BinaryProperty(1, 8), new BinaryProperty(1, 131072), new CaseBinaryProperty(27), new BinaryProperty(1, 16), new BinaryProperty(1, 262144), new CaseBinaryProperty(30), new BinaryProperty(1, 1), new BinaryProperty(1, SCRIPT_X_WITH_INHERITED), new BinaryProperty(1, 4194304), new CaseBinaryProperty(34), new BinaryProperty(1, 134217728), new BinaryProperty(1, VMDebug.KIND_THREAD_EXT_ALLOCATED_OBJECTS), new NormInertBinaryProperty(8, 37), new NormInertBinaryProperty(9, 38), new NormInertBinaryProperty(8, 39), new NormInertBinaryProperty(9, 40), new BinaryProperty(11) {
            @Override
            boolean contains(int i6) {
                return Norm2AllModes.getNFCInstance().impl.ensureCanonIterData().isCanonSegmentStarter(i6);
            }
        }, new BinaryProperty(1, VMDebug.KIND_THREAD_EXT_ALLOCATED_BYTES), new BinaryProperty(1, VMDebug.KIND_THREAD_EXT_FREED_OBJECTS), new BinaryProperty(6) {
            @Override
            boolean contains(int i6) {
                return UCharacter.isUAlphabetic(i6) || UCharacter.isDigit(i6);
            }
        }, new BinaryProperty(i) {
            @Override
            boolean contains(int i6) {
                return i6 <= 159 ? i6 == 9 || i6 == 32 : UCharacter.getType(i6) == 12;
            }
        }, new BinaryProperty(i) {
            @Override
            boolean contains(int i6) {
                return UCharacterProperty.isgraphPOSIX(i6);
            }
        }, new BinaryProperty(i) {
            @Override
            boolean contains(int i6) {
                return UCharacter.getType(i6) == 12 || UCharacterProperty.isgraphPOSIX(i6);
            }
        }, new BinaryProperty(i) {
            @Override
            boolean contains(int i6) {
                return (i6 <= 102 && i6 >= 65 && (i6 <= 70 || i6 >= 97)) || (i6 >= UCharacterProperty.U_FW_A && i6 <= UCharacterProperty.U_FW_f && (i6 <= UCharacterProperty.U_FW_F || i6 >= UCharacterProperty.U_FW_a)) || UCharacter.getType(i6) == 9;
            }
        }, new CaseBinaryProperty(49), new CaseBinaryProperty(50), new CaseBinaryProperty(51), new CaseBinaryProperty(52), new CaseBinaryProperty(53), new BinaryProperty(7) {
            @Override
            boolean contains(int i6) {
                String decomposition = Norm2AllModes.getNFCInstance().impl.getDecomposition(i6);
                if (decomposition != null) {
                    i6 = decomposition.codePointAt(0);
                    if (Character.charCount(i6) != decomposition.length()) {
                        i6 = -1;
                    }
                } else if (i6 < 0) {
                    return false;
                }
                if (i6 < 0) {
                    return !UCharacter.foldCase(decomposition, true).equals(decomposition);
                }
                UCaseProps uCaseProps = UCaseProps.INSTANCE;
                UCaseProps.dummyStringBuilder.setLength(0);
                if (uCaseProps.toFullFolding(i6, UCaseProps.dummyStringBuilder, 0) < 0) {
                    return false;
                }
                return true;
            }
        }, new CaseBinaryProperty(55), new BinaryProperty(10) {
            @Override
            boolean contains(int i6) {
                Normalizer2Impl normalizer2Impl = Norm2AllModes.getNFKC_CFInstance().impl;
                String strValueOf = UTF16.valueOf(i6);
                normalizer2Impl.compose(strValueOf, 0, strValueOf.length(), false, true, new Normalizer2Impl.ReorderingBuffer(normalizer2Impl, new StringBuilder(), 5));
                return !Normalizer2Impl.UTF16Plus.equal(r0, strValueOf);
            }
        }, new BinaryProperty(2, VMDebug.KIND_THREAD_EXT_ALLOCATED_OBJECTS), new BinaryProperty(2, VMDebug.KIND_THREAD_EXT_ALLOCATED_BYTES), new BinaryProperty(2, VMDebug.KIND_THREAD_EXT_FREED_OBJECTS), new BinaryProperty(2, Integer.MIN_VALUE), new BinaryProperty(2, 134217728), new BinaryProperty(i4) {
            @Override
            boolean contains(int i6) {
                return 127462 <= i6 && i6 <= 127487;
            }
        }, new BinaryProperty(1, Integer.MIN_VALUE)};
        this.intProps = new IntProperty[]{new BiDiIntProperty() {
            @Override
            int getValue(int i6) {
                return UBiDiProps.INSTANCE.getClass(i6);
            }
        }, new IntProperty(0, BLOCK_MASK_, 8), new CombiningClassIntProperty(i5) {
            @Override
            int getValue(int i6) {
                return Normalizer2.getNFDInstance().getCombiningClass(i6);
            }
        }, new IntProperty(2, 31, 0), new IntProperty(0, 917504, 17), new IntProperty(i) {
            @Override
            int getValue(int i6) {
                return UCharacterProperty.this.getType(i6);
            }

            @Override
            int getMaxValue(int i6) {
                return 29;
            }
        }, new BiDiIntProperty() {
            @Override
            int getValue(int i6) {
                return UBiDiProps.INSTANCE.getJoiningGroup(i6);
            }
        }, new BiDiIntProperty() {
            @Override
            int getValue(int i6) {
                return UBiDiProps.INSTANCE.getJoiningType(i6);
            }
        }, new IntProperty(2, LB_MASK, 20), new IntProperty(i) {
            @Override
            int getValue(int i6) {
                return UCharacterProperty.ntvGetType(UCharacterProperty.getNumericTypeValue(UCharacterProperty.this.getProperty(i6)));
            }

            @Override
            int getMaxValue(int i6) {
                return 3;
            }
        }, new IntProperty(i2, 255, i2) {
            @Override
            int getValue(int i6) {
                return UScript.getScript(i6);
            }
        }, new IntProperty(i4) {
            @Override
            int getValue(int i6) {
                int additional = (UCharacterProperty.this.getAdditional(i6, 2) & UCharacterProperty.GCB_MASK) >>> 5;
                if (additional < UCharacterProperty.gcbToHst.length) {
                    return UCharacterProperty.gcbToHst[additional];
                }
                return 0;
            }

            @Override
            int getMaxValue(int i6) {
                return 5;
            }
        }, new NormQuickCheckIntProperty(8, UProperty.NFD_QUICK_CHECK, 1), new NormQuickCheckIntProperty(9, UProperty.NFKD_QUICK_CHECK, 1), new NormQuickCheckIntProperty(8, UProperty.NFC_QUICK_CHECK, 2), new NormQuickCheckIntProperty(9, UProperty.NFKC_QUICK_CHECK, 2), new CombiningClassIntProperty(i5) {
            @Override
            int getValue(int i6) {
                return Norm2AllModes.getNFCInstance().impl.getFCD16(i6) >> 8;
            }
        }, new CombiningClassIntProperty(i5) {
            @Override
            int getValue(int i6) {
                return Norm2AllModes.getNFCInstance().impl.getFCD16(i6) & 255;
            }
        }, new IntProperty(2, GCB_MASK, 5), new IntProperty(2, SB_MASK, 15), new IntProperty(2, WB_MASK, 10), new BiDiIntProperty() {
            @Override
            int getValue(int i6) {
                return UBiDiProps.INSTANCE.getPairedBracketType(i6);
            }
        }};
        if (this.binProps.length != 64) {
            throw new ICUException("binProps.length!=UProperty.BINARY_LIMIT");
        }
        if (this.intProps.length != 22) {
            throw new ICUException("intProps.length!=(UProperty.INT_LIMIT-UProperty.INT_START)");
        }
        ByteBuffer requiredData = ICUBinary.getRequiredData(DATA_FILE_NAME_);
        this.m_unicodeVersion_ = ICUBinary.readHeaderAndDataVersion(requiredData, DATA_FORMAT, new IsAcceptable());
        int i6 = requiredData.getInt();
        requiredData.getInt();
        requiredData.getInt();
        int i7 = requiredData.getInt();
        int i8 = requiredData.getInt();
        this.m_additionalColumnsCount_ = requiredData.getInt();
        int i9 = requiredData.getInt();
        int i10 = requiredData.getInt();
        requiredData.getInt();
        requiredData.getInt();
        this.m_maxBlockScriptValue_ = requiredData.getInt();
        this.m_maxJTGValue_ = requiredData.getInt();
        ICUBinary.skipBytes(requiredData, 16);
        this.m_trie_ = Trie2_16.createFromSerialized(requiredData);
        int i11 = (i6 - 16) * 4;
        int serializedLength = this.m_trie_.getSerializedLength();
        if (serializedLength > i11) {
            throw new IOException("uprops.icu: not enough bytes for main trie");
        }
        ICUBinary.skipBytes(requiredData, i11 - serializedLength);
        ICUBinary.skipBytes(requiredData, (i7 - i6) * 4);
        if (this.m_additionalColumnsCount_ > 0) {
            this.m_additionalTrie_ = Trie2_16.createFromSerialized(requiredData);
            int i12 = (i8 - i7) * 4;
            int serializedLength2 = this.m_additionalTrie_.getSerializedLength();
            if (serializedLength2 > i12) {
                throw new IOException("uprops.icu: not enough bytes for additional-properties trie");
            }
            ICUBinary.skipBytes(requiredData, i12 - serializedLength2);
            this.m_additionalVectors_ = ICUBinary.getInts(requiredData, i9 - i8, 0);
        }
        int i13 = (i10 - i9) * 2;
        if (i13 > 0) {
            this.m_scriptExtensions_ = ICUBinary.getChars(requiredData, i13, 0);
        }
    }

    private static final class IsAcceptable implements ICUBinary.Authenticate {
        private IsAcceptable() {
        }

        @Override
        public boolean isDataVersionAcceptable(byte[] bArr) {
            return bArr[0] == 7;
        }
    }

    public UnicodeSet addPropertyStarts(UnicodeSet unicodeSet) {
        for (Trie2.Range range : this.m_trie_) {
            if (range.leadSurrogate) {
                break;
            }
            unicodeSet.add(range.startCodePoint);
        }
        unicodeSet.add(9);
        unicodeSet.add(10);
        unicodeSet.add(14);
        unicodeSet.add(28);
        unicodeSet.add(32);
        unicodeSet.add(133);
        unicodeSet.add(134);
        unicodeSet.add(127);
        unicodeSet.add(HAIRSP);
        unicodeSet.add(8208);
        unicodeSet.add(INHSWAP);
        unicodeSet.add(8304);
        unicodeSet.add(ZWNBSP);
        unicodeSet.add(65280);
        unicodeSet.add(160);
        unicodeSet.add(161);
        unicodeSet.add(FIGURESP);
        unicodeSet.add(8200);
        unicodeSet.add(NNBSP);
        unicodeSet.add(8240);
        unicodeSet.add(12295);
        unicodeSet.add(12296);
        unicodeSet.add(19968);
        unicodeSet.add(19969);
        unicodeSet.add(20108);
        unicodeSet.add(20109);
        unicodeSet.add(19977);
        unicodeSet.add(19978);
        unicodeSet.add(22235);
        unicodeSet.add(22236);
        unicodeSet.add(20116);
        unicodeSet.add(20117);
        unicodeSet.add(20845);
        unicodeSet.add(20846);
        unicodeSet.add(19971);
        unicodeSet.add(19972);
        unicodeSet.add(20843);
        unicodeSet.add(20844);
        unicodeSet.add(20061);
        unicodeSet.add(20062);
        unicodeSet.add(97);
        unicodeSet.add(123);
        unicodeSet.add(65);
        unicodeSet.add(91);
        unicodeSet.add(U_FW_a);
        unicodeSet.add(65371);
        unicodeSet.add(U_FW_A);
        unicodeSet.add(65339);
        unicodeSet.add(103);
        unicodeSet.add(71);
        unicodeSet.add(65351);
        unicodeSet.add(65319);
        unicodeSet.add(WJ);
        unicodeSet.add(65520);
        unicodeSet.add(65532);
        unicodeSet.add(917504);
        unicodeSet.add(921600);
        unicodeSet.add(CGJ);
        unicodeSet.add(848);
        return unicodeSet;
    }

    public void upropsvec_addPropertyStarts(UnicodeSet unicodeSet) {
        if (this.m_additionalColumnsCount_ > 0) {
            for (Trie2.Range range : this.m_additionalTrie_) {
                if (!range.leadSurrogate) {
                    unicodeSet.add(range.startCodePoint);
                } else {
                    return;
                }
            }
        }
    }
}
