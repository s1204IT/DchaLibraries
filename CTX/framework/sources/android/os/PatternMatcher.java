package android.os;

import android.os.Parcelable;
import android.util.proto.ProtoOutputStream;
import java.util.Arrays;

public class PatternMatcher implements Parcelable {
    private static final int MAX_PATTERN_STORAGE = 2048;
    private static final int NO_MATCH = -1;
    private static final int PARSED_MODIFIER_ONE_OR_MORE = -8;
    private static final int PARSED_MODIFIER_RANGE_START = -5;
    private static final int PARSED_MODIFIER_RANGE_STOP = -6;
    private static final int PARSED_MODIFIER_ZERO_OR_MORE = -7;
    private static final int PARSED_TOKEN_CHAR_ANY = -4;
    private static final int PARSED_TOKEN_CHAR_SET_INVERSE_START = -2;
    private static final int PARSED_TOKEN_CHAR_SET_START = -1;
    private static final int PARSED_TOKEN_CHAR_SET_STOP = -3;
    public static final int PATTERN_ADVANCED_GLOB = 3;
    public static final int PATTERN_LITERAL = 0;
    public static final int PATTERN_PREFIX = 1;
    public static final int PATTERN_SIMPLE_GLOB = 2;
    private static final String TAG = "PatternMatcher";
    private static final int TOKEN_TYPE_ANY = 1;
    private static final int TOKEN_TYPE_INVERSE_SET = 3;
    private static final int TOKEN_TYPE_LITERAL = 0;
    private static final int TOKEN_TYPE_SET = 2;
    private final int[] mParsedPattern;
    private final String mPattern;
    private final int mType;
    private static final int[] sParsedPatternScratch = new int[2048];
    public static final Parcelable.Creator<PatternMatcher> CREATOR = new Parcelable.Creator<PatternMatcher>() {
        @Override
        public PatternMatcher createFromParcel(Parcel parcel) {
            return new PatternMatcher(parcel);
        }

        @Override
        public PatternMatcher[] newArray(int i) {
            return new PatternMatcher[i];
        }
    };

    public PatternMatcher(String str, int i) {
        this.mPattern = str;
        this.mType = i;
        if (this.mType == 3) {
            this.mParsedPattern = parseAndVerifyAdvancedPattern(str);
        } else {
            this.mParsedPattern = null;
        }
    }

    public final String getPath() {
        return this.mPattern;
    }

    public final int getType() {
        return this.mType;
    }

    public boolean match(String str) {
        return matchPattern(str, this.mPattern, this.mParsedPattern, this.mType);
    }

    public String toString() {
        String str = "? ";
        switch (this.mType) {
            case 0:
                str = "LITERAL: ";
                break;
            case 1:
                str = "PREFIX: ";
                break;
            case 2:
                str = "GLOB: ";
                break;
            case 3:
                str = "ADVANCED: ";
                break;
        }
        return "PatternMatcher{" + str + this.mPattern + "}";
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1138166333441L, this.mPattern);
        protoOutputStream.write(1159641169922L, this.mType);
        protoOutputStream.end(jStart);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mPattern);
        parcel.writeInt(this.mType);
        parcel.writeIntArray(this.mParsedPattern);
    }

    public PatternMatcher(Parcel parcel) {
        this.mPattern = parcel.readString();
        this.mType = parcel.readInt();
        this.mParsedPattern = parcel.createIntArray();
    }

    static boolean matchPattern(String str, String str2, int[] iArr, int i) {
        if (str == null) {
            return false;
        }
        if (i == 0) {
            return str2.equals(str);
        }
        if (i == 1) {
            return str.startsWith(str2);
        }
        if (i == 2) {
            return matchGlobPattern(str2, str);
        }
        if (i != 3) {
            return false;
        }
        return matchAdvancedPattern(iArr, str);
    }

    static boolean matchGlobPattern(String str, String str2) {
        int length = str.length();
        if (length <= 0) {
            return str2.length() <= 0;
        }
        int length2 = str2.length();
        int i = 0;
        char cCharAt = str.charAt(0);
        int i2 = 0;
        while (i2 < length && i < length2) {
            i2++;
            char cCharAt2 = i2 < length ? str.charAt(i2) : (char) 0;
            boolean z = cCharAt == '\\';
            if (z) {
                i2++;
                cCharAt = i2 < length ? str.charAt(i2) : (char) 0;
            } else {
                char c = cCharAt2;
                cCharAt2 = cCharAt;
                cCharAt = c;
            }
            if (cCharAt == '*') {
                if (!z && cCharAt2 == '.') {
                    if (i2 >= length - 1) {
                        return true;
                    }
                    int i3 = i2 + 1;
                    char cCharAt3 = str.charAt(i3);
                    if (cCharAt3 == '\\') {
                        i3++;
                        cCharAt3 = i3 < length ? str.charAt(i3) : (char) 0;
                    }
                    while (str2.charAt(i) != cCharAt3 && (i = i + 1) < length2) {
                    }
                    if (i == length2) {
                        return false;
                    }
                    i2 = i3 + 1;
                    cCharAt = i2 < length ? str.charAt(i2) : (char) 0;
                    i++;
                } else {
                    while (str2.charAt(i) == cCharAt2 && (i = i + 1) < length2) {
                    }
                    i2++;
                    cCharAt = i2 < length ? str.charAt(i2) : (char) 0;
                }
            } else {
                if (cCharAt2 != '.' && str2.charAt(i) != cCharAt2) {
                    return false;
                }
                i++;
            }
        }
        if (i2 < length || i < length2) {
            return i2 == length + (-2) && str.charAt(i2) == '.' && str.charAt(i2 + 1) == '*';
        }
        return true;
    }

    static synchronized int[] parseAndVerifyAdvancedPattern(String str) {
        int i;
        int i2;
        boolean z;
        int i3;
        int i4;
        int i5;
        int i6;
        int length = str.length();
        int i7 = 0;
        i = 0;
        boolean z2 = false;
        boolean z3 = false;
        boolean z4 = false;
        while (i7 < length) {
            if (i > 2045) {
                throw new IllegalArgumentException("Pattern is too large!");
            }
            char cCharAt = str.charAt(i7);
            if (cCharAt == '.') {
                if (z2) {
                    i2 = i;
                } else {
                    i2 = i + 1;
                    sParsedPatternScratch[i] = -4;
                }
                z = false;
                if (!z2) {
                }
                i7++;
            } else if (cCharAt != '{') {
                if (cCharAt != '}') {
                    switch (cCharAt) {
                        case '*':
                            if (!z2) {
                                if (i == 0 || isParsedModifier(sParsedPatternScratch[i - 1])) {
                                    throw new IllegalArgumentException("Modifier must follow a token.");
                                }
                                i2 = i + 1;
                                sParsedPatternScratch[i] = -7;
                            }
                            z = false;
                            if (!z2) {
                                if (!z4) {
                                    int i8 = i7 + 2;
                                    if (i8 < length) {
                                        int i9 = i7 + 1;
                                        if (str.charAt(i9) != '-' || str.charAt(i8) == ']') {
                                            int i10 = i2 + 1;
                                            sParsedPatternScratch[i2] = cCharAt;
                                            sParsedPatternScratch[i10] = cCharAt;
                                            i = i10 + 1;
                                        } else {
                                            i = i2 + 1;
                                            sParsedPatternScratch[i2] = cCharAt;
                                            z4 = true;
                                            i7 = i9;
                                        }
                                    }
                                } else {
                                    sParsedPatternScratch[i2] = cCharAt;
                                    i = i2 + 1;
                                    z4 = false;
                                }
                                break;
                            } else if (z3) {
                                int iIndexOf = str.indexOf(125, i7);
                                if (iIndexOf < 0) {
                                    throw new IllegalArgumentException("Range not ended with '}'");
                                }
                                String strSubstring = str.substring(i7, iIndexOf);
                                int iIndexOf2 = strSubstring.indexOf(44);
                                if (iIndexOf2 < 0) {
                                    try {
                                        i3 = Integer.parseInt(strSubstring);
                                        i4 = i3;
                                    } catch (NumberFormatException e) {
                                        throw new IllegalArgumentException("Range number format incorrect", e);
                                    }
                                } else {
                                    i4 = Integer.parseInt(strSubstring.substring(0, iIndexOf2));
                                    i3 = iIndexOf2 == strSubstring.length() - 1 ? Integer.MAX_VALUE : Integer.parseInt(strSubstring.substring(iIndexOf2 + 1));
                                }
                                if (i4 > i3) {
                                    throw new IllegalArgumentException("Range quantifier minimum is greater than maximum");
                                }
                                int i11 = i2 + 1;
                                sParsedPatternScratch[i2] = i4;
                                sParsedPatternScratch[i11] = i3;
                                i7 = iIndexOf;
                                i = i11 + 1;
                                break;
                            } else if (z) {
                                sParsedPatternScratch[i2] = cCharAt;
                                i = i2 + 1;
                            } else {
                                i = i2;
                            }
                            i7++;
                            break;
                        case '+':
                            if (!z2) {
                                if (i == 0 || isParsedModifier(sParsedPatternScratch[i - 1])) {
                                    throw new IllegalArgumentException("Modifier must follow a token.");
                                }
                                i2 = i + 1;
                                sParsedPatternScratch[i] = -8;
                            }
                            z = false;
                            if (!z2) {
                            }
                            i7++;
                            break;
                        default:
                            switch (cCharAt) {
                                case '[':
                                    if (!z2) {
                                        int i12 = i7 + 1;
                                        if (str.charAt(i12) == '^') {
                                            i6 = i + 1;
                                            sParsedPatternScratch[i] = -2;
                                            i7 = i12;
                                        } else {
                                            i6 = i + 1;
                                            sParsedPatternScratch[i] = -1;
                                        }
                                        i = i6;
                                        i7++;
                                        z2 = true;
                                    } else {
                                        i2 = i;
                                        z = true;
                                        if (!z2) {
                                        }
                                        i7++;
                                    }
                                    break;
                                case '\\':
                                    i7++;
                                    if (i7 >= length) {
                                        throw new IllegalArgumentException("Escape found at end of pattern!");
                                    }
                                    cCharAt = str.charAt(i7);
                                    i2 = i;
                                    z = true;
                                    if (!z2) {
                                    }
                                    i7++;
                                    break;
                                    break;
                                case ']':
                                    if (!z2) {
                                        i2 = i;
                                        z = true;
                                        if (!z2) {
                                        }
                                        i7++;
                                    } else {
                                        int i13 = sParsedPatternScratch[i - 1];
                                        if (i13 == -1 || i13 == -2) {
                                            throw new IllegalArgumentException("You must define characters in a set.");
                                        }
                                        int i14 = i + 1;
                                        sParsedPatternScratch[i] = -3;
                                        z = false;
                                        z2 = false;
                                        i2 = i14;
                                        z4 = false;
                                        if (!z2) {
                                        }
                                        i7++;
                                    }
                                    break;
                                default:
                                    i2 = i;
                                    z = true;
                                    if (!z2) {
                                    }
                                    i7++;
                                    break;
                            }
                            break;
                    }
                } else if (z3) {
                    i5 = i + 1;
                    sParsedPatternScratch[i] = -6;
                    z = false;
                    z3 = false;
                    i2 = i5;
                    if (!z2) {
                    }
                    i7++;
                }
                i2 = i;
                z = false;
                if (!z2) {
                }
                i7++;
            } else {
                if (!z2) {
                    if (i == 0 || isParsedModifier(sParsedPatternScratch[i - 1])) {
                        throw new IllegalArgumentException("Modifier must follow a token.");
                    }
                    i5 = i + 1;
                    sParsedPatternScratch[i] = -5;
                    i7++;
                    z = false;
                    z3 = true;
                    i2 = i5;
                    if (!z2) {
                    }
                    i7++;
                }
                i2 = i;
                z = false;
                if (!z2) {
                }
                i7++;
            }
        }
        if (z2) {
            throw new IllegalArgumentException("Set was not terminated!");
        }
        return Arrays.copyOf(sParsedPatternScratch, i);
    }

    private static boolean isParsedModifier(int i) {
        return i == -8 || i == -7 || i == -6 || i == -5;
    }

    static boolean matchAdvancedPattern(int[] iArr, String str) {
        int i;
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        int iMatchChars;
        int length = iArr.length;
        int length2 = str.length();
        int i8 = 0;
        int i9 = 0;
        int i10 = 0;
        int i11 = 0;
        while (i8 < length) {
            int i12 = iArr[i8];
            if (i12 != -4) {
                switch (i12) {
                    case -2:
                    case -1:
                        int i13 = i12 == -1 ? 2 : 3;
                        int i14 = i8 + 1;
                        do {
                            i8++;
                            if (i8 < length) {
                            }
                            int i15 = i8 - 1;
                            i = i8 + 1;
                            i2 = i14;
                            i3 = i15;
                            i4 = i13;
                            break;
                        } while (iArr[i8] != -3);
                        int i152 = i8 - 1;
                        i = i8 + 1;
                        i2 = i14;
                        i3 = i152;
                        i4 = i13;
                        break;
                    default:
                        i2 = i8;
                        i = i8 + 1;
                        i3 = i10;
                        i4 = 0;
                        break;
                }
            } else {
                i = i8 + 1;
                i2 = i9;
                i3 = i10;
                i4 = 1;
            }
            if (i < length) {
                int i16 = iArr[i];
                if (i16 != -5) {
                    switch (i16) {
                        case -8:
                            i5 = i + 1;
                            i7 = Integer.MAX_VALUE;
                            i6 = 1;
                            break;
                        case -7:
                            i5 = i + 1;
                            i7 = Integer.MAX_VALUE;
                            i6 = 0;
                            break;
                        default:
                            i5 = i;
                            i6 = 1;
                            i7 = 1;
                            break;
                    }
                } else {
                    int i17 = i + 1;
                    int i18 = iArr[i17];
                    int i19 = i17 + 1;
                    i5 = i19 + 2;
                    i6 = i18;
                    i7 = iArr[i19];
                }
            }
            if (i6 > i7 || (iMatchChars = matchChars(str, i11, length2, i4, i6, i7, iArr, i2, i3)) == -1) {
                return false;
            }
            i11 += iMatchChars;
            i9 = i2;
            i10 = i3;
            i8 = i5;
        }
        return i8 >= length && i11 >= length2;
    }

    private static int matchChars(String str, int i, int i2, int i3, int i4, int i5, int[] iArr, int i6, int i7) {
        int i8 = 0;
        while (i8 < i5 && matchChar(str, i + i8, i2, i3, iArr, i6, i7)) {
            i8++;
        }
        if (i8 < i4) {
            return -1;
        }
        return i8;
    }

    private static boolean matchChar(String str, int i, int i2, int i3, int[] iArr, int i4, int i5) {
        if (i >= i2) {
            return false;
        }
        switch (i3) {
            case 0:
                if (str.charAt(i) != iArr[i4]) {
                    break;
                }
                break;
            case 2:
                while (i4 < i5) {
                    char cCharAt = str.charAt(i);
                    if (cCharAt < iArr[i4] || cCharAt > iArr[i4 + 1]) {
                        i4 += 2;
                    }
                    break;
                }
                break;
            case 3:
                while (i4 < i5) {
                    char cCharAt2 = str.charAt(i);
                    if (cCharAt2 < iArr[i4] || cCharAt2 > iArr[i4 + 1]) {
                        i4 += 2;
                    }
                    break;
                }
                break;
        }
        return false;
    }
}
