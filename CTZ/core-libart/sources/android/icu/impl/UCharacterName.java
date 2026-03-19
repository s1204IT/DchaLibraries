package android.icu.impl;

import android.icu.lang.UCharacter;
import android.icu.lang.UCharacterEnums;
import android.icu.text.UnicodeSet;
import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;

public final class UCharacterName {
    static final int EXTENDED_CATEGORY_ = 33;
    private static final String FILE_NAME_ = "unames.icu";
    private static final int GROUP_MASK_ = 31;
    private static final int GROUP_SHIFT_ = 5;
    public static final UCharacterName INSTANCE;
    private static final int LEAD_SURROGATE_ = 31;
    public static final int LINES_PER_GROUP_ = 32;
    private static final int NON_CHARACTER_ = 30;
    private static final int OFFSET_HIGH_OFFSET_ = 1;
    private static final int OFFSET_LOW_OFFSET_ = 2;
    private static final int SINGLE_NIBBLE_MAX_ = 11;
    private static final int TRAIL_SURROGATE_ = 32;
    private static final String[] TYPE_NAMES_;
    private static final String UNKNOWN_TYPE_NAME_ = "unknown";
    private AlgorithmName[] m_algorithm_;
    private char[] m_groupinfo_;
    private byte[] m_groupstring_;
    private int m_maxISOCommentLength_;
    private int m_maxNameLength_;
    private byte[] m_tokenstring_;
    private char[] m_tokentable_;
    public int m_groupcount_ = 0;
    int m_groupsize_ = 0;
    private char[] m_groupoffsets_ = new char[33];
    private char[] m_grouplengths_ = new char[33];
    private int[] m_nameSet_ = new int[8];
    private int[] m_ISOCommentSet_ = new int[8];
    private StringBuffer m_utilStringBuffer_ = new StringBuffer();
    private int[] m_utilIntBuffer_ = new int[2];

    static {
        try {
            INSTANCE = new UCharacterName();
            TYPE_NAMES_ = new String[]{"unassigned", "uppercase letter", "lowercase letter", "titlecase letter", "modifier letter", "other letter", "non spacing mark", "enclosing mark", "combining spacing mark", "decimal digit number", "letter number", "other number", "space separator", "line separator", "paragraph separator", "control", "format", "private use area", "surrogate", "dash punctuation", "start punctuation", "end punctuation", "connector punctuation", "other punctuation", "math symbol", "currency symbol", "modifier symbol", "other symbol", "initial punctuation", "final punctuation", "noncharacter", "lead surrogate", "trail surrogate"};
        } catch (IOException e) {
            throw new MissingResourceException("Could not construct UCharacterName. Missing unames.icu", "", "");
        }
    }

    public String getName(int i, int i2) {
        if (i < 0 || i > 1114111 || i2 > 4) {
            return null;
        }
        String algName = getAlgName(i, i2);
        if (algName == null || algName.length() == 0) {
            if (i2 == 2) {
                return getExtendedName(i);
            }
            return getGroupName(i, i2);
        }
        return algName;
    }

    public int getCharFromName(int i, String str) {
        int length;
        if (i >= 4 || str == null || str.length() == 0) {
            return -1;
        }
        int extendedChar = getExtendedChar(str.toLowerCase(Locale.ENGLISH), i);
        if (extendedChar >= -1) {
            return extendedChar;
        }
        String upperCase = str.toUpperCase(Locale.ENGLISH);
        if (i == 0 || i == 2) {
            if (this.m_algorithm_ != null) {
                length = this.m_algorithm_.length;
            } else {
                length = 0;
            }
            for (int i2 = length - 1; i2 >= 0; i2--) {
                int i3 = this.m_algorithm_[i2].getChar(upperCase);
                if (i3 >= 0) {
                    return i3;
                }
            }
        }
        if (i == 2) {
            int groupChar = getGroupChar(upperCase, 0);
            if (groupChar == -1) {
                return getGroupChar(upperCase, 3);
            }
            return groupChar;
        }
        return getGroupChar(upperCase, i);
    }

    public int getGroupLengths(int r10, char[] r11, char[] r12) {
        r10 = r10 * r9.m_groupsize_;
        r10 = android.icu.impl.UCharacterUtility.toInt(r9.m_groupinfo_[r10 + 1], r9.m_groupinfo_[r10 + 2]);
        r0 = 0;
        r11[0] = 0;
        r2 = 65535;
        while (r0 < 32) {
            r4 = r9.m_groupstring_[r10];
            r6 = r0;
            r0 = 4;
            while (r0 >= 0) {
                r7 = (byte) ((r4 >> r0) & 15);
                if (r2 == 65535 && r7 > 11) {
                    r2 = (char) ((r7 - 12) << 4);
                } else {
                    if (r2 != 65535) {
                        r12[r6] = (char) ((r2 | r7) + 12);
                    } else {
                        r12[r6] = (char) r7;
                    }
                    if (r6 < 32) {
                        r11[r6 + 1] = (char) (r11[r6] + r12[r6]);
                    }
                    r6 = r6 + 1;
                    r2 = 65535;
                }
                r0 = r0 - 4;
            }
            r10 = r10 + 1;
            r0 = r6;
        }
        return r10;
    }

    public String getGroupName(int i, int i2, int i3) {
        int iSkipByteSubString;
        int i4 = 0;
        if (i3 != 0 && i3 != 2) {
            if (59 >= this.m_tokentable_.length || this.m_tokentable_[59] == 65535) {
                int i5 = i3 == 4 ? 2 : i3;
                while (true) {
                    iSkipByteSubString = UCharacterUtility.skipByteSubString(this.m_groupstring_, i, i2, (byte) 59) + i;
                    i2 -= iSkipByteSubString - i;
                    i5--;
                    if (i5 <= 0) {
                        break;
                    }
                    i = iSkipByteSubString;
                }
                i = iSkipByteSubString;
            } else {
                i2 = 0;
            }
        }
        synchronized (this.m_utilStringBuffer_) {
            this.m_utilStringBuffer_.setLength(0);
            while (i4 < i2) {
                byte b = this.m_groupstring_[i + i4];
                i4++;
                if (b >= this.m_tokentable_.length) {
                    if (b == 59) {
                        break;
                    }
                    this.m_utilStringBuffer_.append((int) b);
                } else {
                    char[] cArr = this.m_tokentable_;
                    int i6 = b & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                    char c = cArr[i6];
                    if (c == 65534) {
                        c = this.m_tokentable_[(b << 8) | (this.m_groupstring_[i + i4] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED)];
                        i4++;
                    }
                    if (c == 65535) {
                        if (b == 59) {
                            if (this.m_utilStringBuffer_.length() != 0 || i3 != 2) {
                                break;
                            }
                        } else {
                            this.m_utilStringBuffer_.append((char) i6);
                        }
                    } else {
                        UCharacterUtility.getNullTermByteSubString(this.m_utilStringBuffer_, this.m_tokenstring_, c);
                    }
                }
            }
            if (this.m_utilStringBuffer_.length() > 0) {
                return this.m_utilStringBuffer_.toString();
            }
            return null;
        }
    }

    public String getExtendedName(int i) {
        String name = getName(i, 0);
        if (name == null) {
            return getExtendedOr10Name(i);
        }
        return name;
    }

    public int getGroup(int i) {
        int i2 = this.m_groupcount_;
        int codepointMSB = getCodepointMSB(i);
        int i3 = 0;
        while (i3 < i2 - 1) {
            int i4 = (i3 + i2) >> 1;
            if (codepointMSB < getGroupMSB(i4)) {
                i2 = i4;
            } else {
                i3 = i4;
            }
        }
        return i3;
    }

    public String getExtendedOr10Name(int i) {
        String str;
        String string;
        int type = getType(i);
        if (type >= TYPE_NAMES_.length) {
            str = UNKNOWN_TYPE_NAME_;
        } else {
            str = TYPE_NAMES_[type];
        }
        synchronized (this.m_utilStringBuffer_) {
            this.m_utilStringBuffer_.setLength(0);
            this.m_utilStringBuffer_.append('<');
            this.m_utilStringBuffer_.append(str);
            this.m_utilStringBuffer_.append('-');
            String upperCase = Integer.toHexString(i).toUpperCase(Locale.ENGLISH);
            for (int length = 4 - upperCase.length(); length > 0; length--) {
                this.m_utilStringBuffer_.append('0');
            }
            this.m_utilStringBuffer_.append(upperCase);
            this.m_utilStringBuffer_.append('>');
            string = this.m_utilStringBuffer_.toString();
        }
        return string;
    }

    public int getGroupMSB(int i) {
        if (i >= this.m_groupcount_) {
            return -1;
        }
        return this.m_groupinfo_[i * this.m_groupsize_];
    }

    public static int getCodepointMSB(int i) {
        return i >> 5;
    }

    public static int getGroupLimit(int i) {
        return (i << 5) + 32;
    }

    public static int getGroupMin(int i) {
        return i << 5;
    }

    public static int getGroupOffset(int i) {
        return i & 31;
    }

    public static int getGroupMinFromCodepoint(int i) {
        return i & (-32);
    }

    public int getAlgorithmLength() {
        return this.m_algorithm_.length;
    }

    public int getAlgorithmStart(int i) {
        return this.m_algorithm_[i].m_rangestart_;
    }

    public int getAlgorithmEnd(int i) {
        return this.m_algorithm_[i].m_rangeend_;
    }

    public String getAlgorithmName(int i, int i2) {
        String string;
        synchronized (this.m_utilStringBuffer_) {
            this.m_utilStringBuffer_.setLength(0);
            this.m_algorithm_[i].appendName(i2, this.m_utilStringBuffer_);
            string = this.m_utilStringBuffer_.toString();
        }
        return string;
    }

    public synchronized String getGroupName(int i, int i2) {
        int codepointMSB = getCodepointMSB(i);
        int group = getGroup(i);
        if (codepointMSB == this.m_groupinfo_[this.m_groupsize_ * group]) {
            int i3 = i & 31;
            return getGroupName(getGroupLengths(group, this.m_groupoffsets_, this.m_grouplengths_) + this.m_groupoffsets_[i3], this.m_grouplengths_[i3], i2);
        }
        return null;
    }

    public int getMaxCharNameLength() {
        if (initNameSetsLengths()) {
            return this.m_maxNameLength_;
        }
        return 0;
    }

    public int getMaxISOCommentLength() {
        if (initNameSetsLengths()) {
            return this.m_maxISOCommentLength_;
        }
        return 0;
    }

    public void getCharNameCharacters(UnicodeSet unicodeSet) {
        convert(this.m_nameSet_, unicodeSet);
    }

    public void getISOCommentCharacters(UnicodeSet unicodeSet) {
        convert(this.m_ISOCommentSet_, unicodeSet);
    }

    static final class AlgorithmName {
        static final int TYPE_0_ = 0;
        static final int TYPE_1_ = 1;
        private char[] m_factor_;
        private byte[] m_factorstring_;
        private String m_prefix_;
        private int m_rangeend_;
        private int m_rangestart_;
        private byte m_type_;
        private byte m_variant_;
        private StringBuffer m_utilStringBuffer_ = new StringBuffer();
        private int[] m_utilIntBuffer_ = new int[256];

        AlgorithmName() {
        }

        boolean setInfo(int i, int i2, byte b, byte b2) {
            if (i < 0 || i > i2 || i2 > 1114111) {
                return false;
            }
            if (b == 0 || b == 1) {
                this.m_rangestart_ = i;
                this.m_rangeend_ = i2;
                this.m_type_ = b;
                this.m_variant_ = b2;
                return true;
            }
            return false;
        }

        boolean setFactor(char[] cArr) {
            if (cArr.length == this.m_variant_) {
                this.m_factor_ = cArr;
                return true;
            }
            return false;
        }

        boolean setPrefix(String str) {
            if (str != null && str.length() > 0) {
                this.m_prefix_ = str;
                return true;
            }
            return false;
        }

        boolean setFactorString(byte[] bArr) {
            this.m_factorstring_ = bArr;
            return true;
        }

        boolean contains(int i) {
            return this.m_rangestart_ <= i && i <= this.m_rangeend_;
        }

        void appendName(int i, StringBuffer stringBuffer) {
            stringBuffer.append(this.m_prefix_);
            switch (this.m_type_) {
                case 0:
                    stringBuffer.append(Utility.hex(i, this.m_variant_));
                    return;
                case 1:
                    int i2 = i - this.m_rangestart_;
                    int[] iArr = this.m_utilIntBuffer_;
                    synchronized (this.m_utilIntBuffer_) {
                        for (int i3 = this.m_variant_ - 1; i3 > 0; i3--) {
                            int i4 = this.m_factor_[i3] & 255;
                            iArr[i3] = i2 % i4;
                            i2 /= i4;
                        }
                        iArr[0] = i2;
                        stringBuffer.append(getFactorString(iArr, this.m_variant_));
                        break;
                    }
                    return;
                default:
                    return;
            }
        }

        int getChar(String str) {
            int length = this.m_prefix_.length();
            if (str.length() < length || !this.m_prefix_.equals(str.substring(0, length))) {
                return -1;
            }
            switch (this.m_type_) {
                case 0:
                    try {
                        int i = Integer.parseInt(str.substring(length), 16);
                        if (this.m_rangestart_ <= i) {
                            if (i <= this.m_rangeend_) {
                                return i;
                            }
                        }
                        return -1;
                    } catch (NumberFormatException e) {
                        return -1;
                    }
                case 1:
                    for (int i2 = this.m_rangestart_; i2 <= this.m_rangeend_; i2++) {
                        int i3 = i2 - this.m_rangestart_;
                        int[] iArr = this.m_utilIntBuffer_;
                        synchronized (this.m_utilIntBuffer_) {
                            for (int i4 = this.m_variant_ - 1; i4 > 0; i4--) {
                                int i5 = this.m_factor_[i4] & 255;
                                iArr[i4] = i3 % i5;
                                i3 /= i5;
                            }
                            iArr[0] = i3;
                            if (compareFactorString(iArr, this.m_variant_, str, length)) {
                                return i2;
                            }
                        }
                    }
                    return -1;
                default:
                    return -1;
            }
        }

        int add(int[] iArr, int i) {
            int iAdd = UCharacterName.add(iArr, this.m_prefix_);
            switch (this.m_type_) {
                case 0:
                    iAdd += this.m_variant_;
                    break;
                case 1:
                    for (int i2 = this.m_variant_ - 1; i2 > 0; i2--) {
                        int length = 0;
                        int nullTermByteSubString = 0;
                        for (int i3 = this.m_factor_[i2]; i3 > 0; i3--) {
                            synchronized (this.m_utilStringBuffer_) {
                                this.m_utilStringBuffer_.setLength(0);
                                nullTermByteSubString = UCharacterUtility.getNullTermByteSubString(this.m_utilStringBuffer_, this.m_factorstring_, nullTermByteSubString);
                                UCharacterName.add(iArr, this.m_utilStringBuffer_);
                                if (this.m_utilStringBuffer_.length() > length) {
                                    length = this.m_utilStringBuffer_.length();
                                }
                            }
                        }
                        iAdd += length;
                    }
                    break;
            }
            if (iAdd > i) {
                return iAdd;
            }
            return i;
        }

        private String getFactorString(int[] iArr, int i) {
            String string;
            int length = this.m_factor_.length;
            if (iArr == null || i != length) {
                return null;
            }
            synchronized (this.m_utilStringBuffer_) {
                this.m_utilStringBuffer_.setLength(0);
                int i2 = length - 1;
                int nullTermByteSubString = 0;
                for (int i3 = 0; i3 <= i2; i3++) {
                    char c = this.m_factor_[i3];
                    nullTermByteSubString = UCharacterUtility.getNullTermByteSubString(this.m_utilStringBuffer_, this.m_factorstring_, UCharacterUtility.skipNullTermByteSubString(this.m_factorstring_, nullTermByteSubString, iArr[i3]));
                    if (i3 != i2) {
                        nullTermByteSubString = UCharacterUtility.skipNullTermByteSubString(this.m_factorstring_, nullTermByteSubString, (c - iArr[i3]) - 1);
                    }
                }
                string = this.m_utilStringBuffer_.toString();
            }
            return string;
        }

        private boolean compareFactorString(int[] iArr, int i, String str, int i2) {
            int length = this.m_factor_.length;
            if (iArr == null || i != length) {
                return false;
            }
            int i3 = length - 1;
            int iCompareNullTermByteSubString = i2;
            int iSkipNullTermByteSubString = 0;
            for (int i4 = 0; i4 <= i3; i4++) {
                char c = this.m_factor_[i4];
                iSkipNullTermByteSubString = UCharacterUtility.skipNullTermByteSubString(this.m_factorstring_, iSkipNullTermByteSubString, iArr[i4]);
                iCompareNullTermByteSubString = UCharacterUtility.compareNullTermByteSubString(str, this.m_factorstring_, iCompareNullTermByteSubString, iSkipNullTermByteSubString);
                if (iCompareNullTermByteSubString < 0) {
                    return false;
                }
                if (i4 != i3) {
                    iSkipNullTermByteSubString = UCharacterUtility.skipNullTermByteSubString(this.m_factorstring_, iSkipNullTermByteSubString, c - iArr[i4]);
                }
            }
            if (iCompareNullTermByteSubString != str.length()) {
                return false;
            }
            return true;
        }
    }

    boolean setToken(char[] cArr, byte[] bArr) {
        if (cArr != null && bArr != null && cArr.length > 0 && bArr.length > 0) {
            this.m_tokentable_ = cArr;
            this.m_tokenstring_ = bArr;
            return true;
        }
        return false;
    }

    boolean setAlgorithm(AlgorithmName[] algorithmNameArr) {
        if (algorithmNameArr != null && algorithmNameArr.length != 0) {
            this.m_algorithm_ = algorithmNameArr;
            return true;
        }
        return false;
    }

    boolean setGroupCountSize(int i, int i2) {
        if (i <= 0 || i2 <= 0) {
            return false;
        }
        this.m_groupcount_ = i;
        this.m_groupsize_ = i2;
        return true;
    }

    boolean setGroup(char[] cArr, byte[] bArr) {
        if (cArr != null && bArr != null && cArr.length > 0 && bArr.length > 0) {
            this.m_groupinfo_ = cArr;
            this.m_groupstring_ = bArr;
            return true;
        }
        return false;
    }

    private UCharacterName() throws IOException {
        new UCharacterNameReader(ICUBinary.getRequiredData(FILE_NAME_)).read(this);
    }

    private String getAlgName(int i, int i2) {
        if (i2 == 0 || i2 == 2) {
            synchronized (this.m_utilStringBuffer_) {
                this.m_utilStringBuffer_.setLength(0);
                for (int length = this.m_algorithm_.length - 1; length >= 0; length--) {
                    if (this.m_algorithm_[length].contains(i)) {
                        this.m_algorithm_[length].appendName(i, this.m_utilStringBuffer_);
                        return this.m_utilStringBuffer_.toString();
                    }
                }
                return null;
            }
        }
        return null;
    }

    private synchronized int getGroupChar(String str, int i) {
        for (int i2 = 0; i2 < this.m_groupcount_; i2++) {
            int groupChar = getGroupChar(getGroupLengths(i2, this.m_groupoffsets_, this.m_grouplengths_), this.m_grouplengths_, str, i);
            if (groupChar != -1) {
                return (this.m_groupinfo_[i2 * this.m_groupsize_] << 5) | groupChar;
            }
        }
        return -1;
    }

    private int getGroupChar(int i, char[] cArr, String str, int i2) {
        int iSkipByteSubString;
        int length = str.length();
        int i3 = i;
        for (int i4 = 0; i4 <= 32; i4++) {
            char c = cArr[i4];
            if (i2 != 0) {
                if (i2 != 2) {
                    int i5 = i2 != 4 ? i2 : 2;
                    while (true) {
                        iSkipByteSubString = UCharacterUtility.skipByteSubString(this.m_groupstring_, i3, c, (byte) 59) + i3;
                        c -= iSkipByteSubString - i3;
                        i5--;
                        if (i5 <= 0) {
                            break;
                        }
                        i3 = iSkipByteSubString;
                    }
                } else {
                    iSkipByteSubString = i3;
                }
            }
            int i6 = 0;
            int iCompareNullTermByteSubString = 0;
            while (i6 < c && iCompareNullTermByteSubString != -1 && iCompareNullTermByteSubString < length) {
                byte b = this.m_groupstring_[iSkipByteSubString + i6];
                i6++;
                if (b >= this.m_tokentable_.length) {
                    iCompareNullTermByteSubString = str.charAt(iCompareNullTermByteSubString) != (b & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) ? -1 : iCompareNullTermByteSubString + 1;
                } else {
                    char[] cArr2 = this.m_tokentable_;
                    int i7 = b & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                    char c2 = cArr2[i7];
                    if (c2 == 65534) {
                        c2 = this.m_tokentable_[(b << 8) | (this.m_groupstring_[iSkipByteSubString + i6] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED)];
                        i6++;
                    }
                    if (c2 == 65535) {
                        int i8 = iCompareNullTermByteSubString + 1;
                        if (str.charAt(iCompareNullTermByteSubString) == i7) {
                            iCompareNullTermByteSubString = i8;
                        }
                    } else {
                        iCompareNullTermByteSubString = UCharacterUtility.compareNullTermByteSubString(str, this.m_tokenstring_, iCompareNullTermByteSubString, c2);
                    }
                }
            }
            if (length == iCompareNullTermByteSubString && (i6 == c || this.m_groupstring_[i6 + iSkipByteSubString] == 59)) {
                return i4;
            }
            i3 = iSkipByteSubString + c;
        }
        return -1;
    }

    private static int getType(int i) {
        if (UCharacterUtility.isNonCharacter(i)) {
            return 30;
        }
        int type = UCharacter.getType(i);
        if (type == 18) {
            if (i <= 56319) {
                return 31;
            }
            return 32;
        }
        return type;
    }

    private static int getExtendedChar(String str, int i) {
        int iLastIndexOf;
        int i2 = 0;
        if (str.charAt(0) == '<') {
            if (i == 2) {
                int length = str.length() - 1;
                if (str.charAt(length) == '>' && (iLastIndexOf = str.lastIndexOf(45)) >= 0) {
                    int i3 = iLastIndexOf + 1;
                    try {
                        int i4 = Integer.parseInt(str.substring(i3, length), 16);
                        String strSubstring = str.substring(1, i3 - 1);
                        int length2 = TYPE_NAMES_.length;
                        while (true) {
                            if (i2 >= length2) {
                                break;
                            }
                            if (strSubstring.compareTo(TYPE_NAMES_[i2]) != 0) {
                                i2++;
                            } else if (getType(i4) == i2) {
                                return i4;
                            }
                        }
                    } catch (NumberFormatException e) {
                        return -1;
                    }
                }
            }
            return -1;
        }
        return -2;
    }

    private static void add(int[] iArr, char c) {
        int i = c >>> 5;
        iArr[i] = (1 << (c & 31)) | iArr[i];
    }

    private static boolean contains(int[] iArr, char c) {
        return (iArr[c >>> 5] & (1 << (c & 31))) != 0;
    }

    private static int add(int[] iArr, String str) {
        int length = str.length();
        for (int i = length - 1; i >= 0; i--) {
            add(iArr, str.charAt(i));
        }
        return length;
    }

    private static int add(int[] iArr, StringBuffer stringBuffer) {
        int length = stringBuffer.length();
        for (int i = length - 1; i >= 0; i--) {
            add(iArr, stringBuffer.charAt(i));
        }
        return length;
    }

    private int addAlgorithmName(int i) {
        for (int length = this.m_algorithm_.length - 1; length >= 0; length--) {
            int iAdd = this.m_algorithm_[length].add(this.m_nameSet_, i);
            if (iAdd > i) {
                i = iAdd;
            }
        }
        return i;
    }

    private int addExtendedName(int i) {
        for (int length = TYPE_NAMES_.length - 1; length >= 0; length--) {
            int iAdd = 9 + add(this.m_nameSet_, TYPE_NAMES_[length]);
            if (iAdd > i) {
                i = iAdd;
            }
        }
        return i;
    }

    private int[] addGroupName(int i, int i2, byte[] bArr, int[] iArr) {
        int i3 = 0;
        int i4 = 0;
        while (i3 < i2) {
            char c = (char) (this.m_groupstring_[i + i3] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
            i3++;
            if (c == ';') {
                break;
            }
            if (c >= this.m_tokentable_.length) {
                add(iArr, c);
                i4++;
            } else {
                char c2 = this.m_tokentable_[c & 255];
                if (c2 == 65534) {
                    c = (char) ((c << '\b') | (this.m_groupstring_[i + i3] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED));
                    c2 = this.m_tokentable_[c];
                    i3++;
                }
                if (c2 == 65535) {
                    add(iArr, c);
                    i4++;
                } else {
                    byte bAdd = bArr[c];
                    if (bAdd == 0) {
                        synchronized (this.m_utilStringBuffer_) {
                            this.m_utilStringBuffer_.setLength(0);
                            UCharacterUtility.getNullTermByteSubString(this.m_utilStringBuffer_, this.m_tokenstring_, c2);
                            bAdd = (byte) add(iArr, this.m_utilStringBuffer_);
                        }
                        bArr[c] = bAdd;
                    }
                    i4 += bAdd;
                }
            }
        }
        this.m_utilIntBuffer_[0] = i4;
        this.m_utilIntBuffer_[1] = i3;
        return this.m_utilIntBuffer_;
    }

    private void addGroupName(int i) {
        char[] cArr = new char[34];
        char[] cArr2 = new char[34];
        byte[] bArr = new byte[this.m_tokentable_.length];
        int i2 = i;
        int i3 = 0;
        int i4 = 0;
        while (i3 < this.m_groupcount_) {
            int groupLengths = getGroupLengths(i3, cArr, cArr2);
            int i5 = i4;
            for (int i6 = 0; i6 < 32; i6++) {
                int i7 = cArr[i6] + groupLengths;
                char c = cArr2[i6];
                if (c != 0) {
                    int[] iArrAddGroupName = addGroupName(i7, c, bArr, this.m_nameSet_);
                    if (iArrAddGroupName[0] > i2) {
                        i2 = iArrAddGroupName[0];
                    }
                    int i8 = i7 + iArrAddGroupName[1];
                    if (iArrAddGroupName[1] < c) {
                        int i9 = c - iArrAddGroupName[1];
                        int[] iArrAddGroupName2 = addGroupName(i8, i9, bArr, this.m_nameSet_);
                        if (iArrAddGroupName2[0] > i2) {
                            i2 = iArrAddGroupName2[0];
                        }
                        int i10 = i8 + iArrAddGroupName2[1];
                        if (iArrAddGroupName2[1] < i9) {
                            int i11 = i9 - iArrAddGroupName2[1];
                            if (addGroupName(i10, i11, bArr, this.m_ISOCommentSet_)[1] > i5) {
                                i5 = i11;
                            }
                        }
                    }
                }
            }
            i3++;
            i4 = i5;
        }
        this.m_maxISOCommentLength_ = i4;
        this.m_maxNameLength_ = i2;
    }

    private boolean initNameSetsLengths() {
        if (this.m_maxNameLength_ > 0) {
            return true;
        }
        for (int length = "0123456789ABCDEF<>-".length() - 1; length >= 0; length--) {
            add(this.m_nameSet_, "0123456789ABCDEF<>-".charAt(length));
        }
        this.m_maxNameLength_ = addAlgorithmName(0);
        this.m_maxNameLength_ = addExtendedName(this.m_maxNameLength_);
        addGroupName(this.m_maxNameLength_);
        return true;
    }

    private void convert(int[] iArr, UnicodeSet unicodeSet) {
        unicodeSet.clear();
        if (!initNameSetsLengths()) {
            return;
        }
        for (char c = 255; c > 0; c = (char) (c - 1)) {
            if (contains(iArr, c)) {
                unicodeSet.add(c);
            }
        }
    }
}
