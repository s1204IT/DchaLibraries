package android.icu.impl.duration.impl;

import android.icu.impl.duration.TimeUnit;
import android.icu.impl.duration.impl.DataRecord;
import android.icu.impl.duration.impl.Utils;
import android.icu.text.BreakIterator;
import java.util.Arrays;

public class PeriodFormatterData {
    private static final int FORM_DUAL = 2;
    private static final int FORM_HALF_SPELLED = 6;
    private static final int FORM_PAUCAL = 3;
    private static final int FORM_PLURAL = 0;
    private static final int FORM_SINGULAR = 1;
    private static final int FORM_SINGULAR_NO_OMIT = 5;
    private static final int FORM_SINGULAR_SPELLED = 4;
    public static boolean trace = false;
    final DataRecord dr;
    String localeName;

    public PeriodFormatterData(String str, DataRecord dataRecord) {
        this.dr = dataRecord;
        this.localeName = str;
        if (str == null) {
            throw new NullPointerException("localename is null");
        }
        if (dataRecord == null) {
            throw new NullPointerException("data record is null");
        }
    }

    public int pluralization() {
        return this.dr.pl;
    }

    public boolean allowZero() {
        return this.dr.allowZero;
    }

    public boolean weeksAloneOnly() {
        return this.dr.weeksAloneOnly;
    }

    public int useMilliseconds() {
        return this.dr.useMilliseconds;
    }

    public boolean appendPrefix(int i, int i2, StringBuffer stringBuffer) {
        String str;
        if (this.dr.scopeData != null) {
            DataRecord.ScopeData scopeData = this.dr.scopeData[(i * 3) + i2];
            if (scopeData != null && (str = scopeData.prefix) != null) {
                stringBuffer.append(str);
                return scopeData.requiresDigitPrefix;
            }
            return false;
        }
        return false;
    }

    public void appendSuffix(int i, int i2, StringBuffer stringBuffer) {
        String str;
        if (this.dr.scopeData != null) {
            DataRecord.ScopeData scopeData = this.dr.scopeData[(i * 3) + i2];
            if (scopeData != null && (str = scopeData.suffix) != null) {
                if (trace) {
                    System.out.println("appendSuffix '" + str + "'");
                }
                stringBuffer.append(str);
            }
        }
    }

    public boolean appendUnit(TimeUnit timeUnit, int i, int i2, int i3, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, StringBuffer stringBuffer) {
        boolean z6;
        String str;
        int i4 = i;
        int iOrdinal = timeUnit.ordinal();
        boolean z7 = true;
        if (this.dr.requiresSkipMarker == null || !this.dr.requiresSkipMarker[iOrdinal] || this.dr.skippedUnitMarker == null) {
            z6 = false;
        } else {
            if (!z5 && z4) {
                stringBuffer.append(this.dr.skippedUnitMarker);
            }
            z6 = true;
        }
        if (i3 != 0) {
            boolean z8 = i3 == 1;
            String[] strArr = z8 ? this.dr.mediumNames : this.dr.shortNames;
            if (strArr == null || strArr[iOrdinal] == null) {
                strArr = z8 ? this.dr.shortNames : this.dr.mediumNames;
            }
            if (strArr != null && strArr[iOrdinal] != null) {
                appendCount(timeUnit, false, false, i4, i2, z, strArr[iOrdinal], z4, stringBuffer);
                return false;
            }
        }
        int i5 = i2;
        if (i5 == 2 && this.dr.halfSupport != null) {
            switch (this.dr.halfSupport[iOrdinal]) {
                case 2:
                    if (i4 <= 1000) {
                    }
                case 1:
                    i4 = (i4 / BreakIterator.WORD_IDEO_LIMIT) * BreakIterator.WORD_IDEO_LIMIT;
                    i5 = 3;
                    break;
            }
        }
        int i6 = i4;
        int i7 = i5;
        int iComputeForm = computeForm(timeUnit, i6, i7, z3 && z4);
        if (iComputeForm == 4) {
            if (this.dr.singularNames == null) {
                str = this.dr.pluralNames[iOrdinal][1];
                iComputeForm = 1;
            } else {
                str = this.dr.singularNames[iOrdinal];
            }
        } else if (iComputeForm == 5) {
            str = this.dr.pluralNames[iOrdinal][1];
        } else if (iComputeForm == 6) {
            str = this.dr.halfNames[iOrdinal];
        } else {
            try {
                str = this.dr.pluralNames[iOrdinal][iComputeForm];
            } catch (NullPointerException e) {
                System.out.println("Null Pointer in PeriodFormatterData[" + this.localeName + "].au px: " + iOrdinal + " form: " + iComputeForm + " pn: " + Arrays.toString(this.dr.pluralNames));
                throw e;
            }
        }
        if (str == null) {
            str = this.dr.pluralNames[iOrdinal][0];
            iComputeForm = 0;
        }
        if (iComputeForm != 4 && iComputeForm != 6 && ((!this.dr.omitSingularCount || iComputeForm != 1) && (!this.dr.omitDualCount || iComputeForm != 2))) {
            z7 = false;
        }
        int iAppendCount = appendCount(timeUnit, z7, z2, i6, i7, z, str, z4, stringBuffer);
        if (z4 && iAppendCount >= 0) {
            String str2 = null;
            if (this.dr.rqdSuffixes != null && iAppendCount < this.dr.rqdSuffixes.length) {
                str2 = this.dr.rqdSuffixes[iAppendCount];
            }
            if (str2 == null && this.dr.optSuffixes != null && iAppendCount < this.dr.optSuffixes.length) {
                str2 = this.dr.optSuffixes[iAppendCount];
            }
            if (str2 != null) {
                stringBuffer.append(str2);
            }
        }
        return z6;
    }

    public int appendCount(TimeUnit timeUnit, boolean z, boolean z2, int i, int i2, boolean z3, String str, boolean z4, StringBuffer stringBuffer) {
        String str2;
        byte b = 0;
        int i3 = 2;
        if (i2 == 2 && this.dr.halves == null) {
            i2 = 0;
        }
        if (!z && z2 && this.dr.digitPrefix != null) {
            stringBuffer.append(this.dr.digitPrefix);
        }
        int iOrdinal = timeUnit.ordinal();
        int i4 = 3;
        switch (i2) {
            case 0:
                if (!z) {
                    appendInteger(i / 1000, 1, 10, stringBuffer);
                }
                break;
            case 1:
                int i5 = i / 1000;
                if (timeUnit == TimeUnit.MINUTE && ((this.dr.fiveMinutes != null || this.dr.fifteenMinutes != null) && i5 != 0 && i5 % 5 == 0)) {
                    if (this.dr.fifteenMinutes != null && (i5 == 15 || i5 == 45)) {
                        if (i5 == 15) {
                            i4 = 1;
                        }
                        if (!z) {
                            appendInteger(i4, 1, 10, stringBuffer);
                        }
                        str = this.dr.fifteenMinutes;
                        iOrdinal = 8;
                        break;
                    } else if (this.dr.fiveMinutes != null) {
                        int i6 = i5 / 5;
                        if (!z) {
                            appendInteger(i6, 1, 10, stringBuffer);
                        }
                        str = this.dr.fiveMinutes;
                        iOrdinal = 9;
                        break;
                    }
                } else {
                    if (!z) {
                        appendInteger(i5, 1, 10, stringBuffer);
                    }
                    break;
                }
                break;
            case 2:
                int i7 = i / BreakIterator.WORD_IDEO_LIMIT;
                if (i7 != 1 && !z) {
                    appendCountValue(i, 1, 0, stringBuffer);
                }
                if ((i7 & 1) == 1) {
                    if (i7 == 1 && this.dr.halfNames != null && this.dr.halfNames[iOrdinal] != null) {
                        stringBuffer.append(str);
                        if (z4) {
                            return iOrdinal;
                        }
                        return -1;
                    }
                    int i8 = i7 == 1 ? 0 : 1;
                    if (this.dr.genders != null && this.dr.halves.length > 2 && this.dr.genders[iOrdinal] == 1) {
                        i8 += 2;
                    }
                    if (this.dr.halfPlacements != null) {
                        b = this.dr.halfPlacements[i8 & 1];
                    }
                    String str3 = this.dr.halves[i8];
                    String str4 = this.dr.measures == null ? null : this.dr.measures[iOrdinal];
                    switch (b) {
                        case 0:
                            stringBuffer.append(str3);
                            break;
                        case 1:
                            if (str4 != null) {
                                stringBuffer.append(str4);
                                stringBuffer.append(str3);
                                if (z3 && !z) {
                                    stringBuffer.append(this.dr.countSep);
                                }
                                stringBuffer.append(str);
                                return -1;
                            }
                            stringBuffer.append(str);
                            stringBuffer.append(str3);
                            if (z4) {
                                return iOrdinal;
                            }
                            return -1;
                        case 2:
                            if (str4 != null) {
                                stringBuffer.append(str4);
                            }
                            if (z3 && !z) {
                                stringBuffer.append(this.dr.countSep);
                            }
                            stringBuffer.append(str);
                            stringBuffer.append(str3);
                            if (z4) {
                                return iOrdinal;
                            }
                            return -1;
                    }
                }
                break;
            default:
                switch (i2) {
                    case 4:
                        break;
                    case 5:
                        i3 = 3;
                        break;
                    default:
                        i3 = 1;
                        break;
                }
                if (!z) {
                    appendCountValue(i, 1, i3, stringBuffer);
                }
                break;
        }
        if (!z && z3) {
            stringBuffer.append(this.dr.countSep);
        }
        if (!z && this.dr.measures != null && iOrdinal < this.dr.measures.length && (str2 = this.dr.measures[iOrdinal]) != null) {
            stringBuffer.append(str2);
        }
        stringBuffer.append(str);
        if (z4) {
            return iOrdinal;
        }
        return -1;
    }

    public void appendCountValue(int i, int i2, int i3, StringBuffer stringBuffer) {
        int i4 = i / 1000;
        if (i3 == 0) {
            appendInteger(i4, i2, 10, stringBuffer);
            return;
        }
        if (this.dr.requiresDigitSeparator && stringBuffer.length() > 0) {
            stringBuffer.append(' ');
        }
        appendDigits(i4, i2, 10, stringBuffer);
        int i5 = i % 1000;
        if (i3 == 1) {
            i5 /= 100;
        } else if (i3 == 2) {
            i5 /= 10;
        }
        stringBuffer.append(this.dr.decimalSep);
        appendDigits(i5, i3, i3, stringBuffer);
        if (this.dr.requiresDigitSeparator) {
            stringBuffer.append(' ');
        }
    }

    public void appendInteger(int i, int i2, int i3, StringBuffer stringBuffer) {
        String str;
        if (this.dr.numberNames != null && i < this.dr.numberNames.length && (str = this.dr.numberNames[i]) != null) {
            stringBuffer.append(str);
            return;
        }
        if (this.dr.requiresDigitSeparator && stringBuffer.length() > 0) {
            stringBuffer.append(' ');
        }
        switch (this.dr.numberSystem) {
            case 0:
                appendDigits(i, i2, i3, stringBuffer);
                break;
            case 1:
                stringBuffer.append(Utils.chineseNumber(i, Utils.ChineseDigits.TRADITIONAL));
                break;
            case 2:
                stringBuffer.append(Utils.chineseNumber(i, Utils.ChineseDigits.SIMPLIFIED));
                break;
            case 3:
                stringBuffer.append(Utils.chineseNumber(i, Utils.ChineseDigits.KOREAN));
                break;
        }
        if (this.dr.requiresDigitSeparator) {
            stringBuffer.append(' ');
        }
    }

    public void appendDigits(long j, int i, int i2, StringBuffer stringBuffer) {
        char[] cArr = new char[i2];
        int i3 = i2;
        for (long j2 = j; i3 > 0 && j2 > 0; j2 /= 10) {
            i3--;
            cArr[i3] = (char) (((long) this.dr.zero) + (j2 % 10));
        }
        int i4 = i2 - i;
        while (i3 > i4) {
            i3--;
            cArr[i3] = this.dr.zero;
        }
        stringBuffer.append(cArr, i3, i2 - i3);
    }

    public void appendSkippedUnit(StringBuffer stringBuffer) {
        if (this.dr.skippedUnitMarker != null) {
            stringBuffer.append(this.dr.skippedUnitMarker);
        }
    }

    public boolean appendUnitSeparator(TimeUnit timeUnit, boolean z, boolean z2, boolean z3, StringBuffer stringBuffer) {
        if ((z && this.dr.unitSep != null) || this.dr.shortUnitSep != null) {
            if (z && this.dr.unitSep != null) {
                int i = (z2 ? 2 : 0) + (z3 ? 1 : 0);
                stringBuffer.append(this.dr.unitSep[i]);
                return this.dr.unitSepRequiresDP != null && this.dr.unitSepRequiresDP[i];
            }
            stringBuffer.append(this.dr.shortUnitSep);
        }
        return false;
    }

    private int computeForm(TimeUnit timeUnit, int i, int i2, boolean z) {
        if (trace) {
            System.err.println("pfd.cf unit: " + timeUnit + " count: " + i + " cv: " + i2 + " dr.pl: " + ((int) this.dr.pl));
            Thread.dumpStack();
        }
        int i3 = 0;
        if (this.dr.pl == 0) {
            return 0;
        }
        int i4 = i / 1000;
        switch (i2) {
            case 0:
            case 1:
                break;
            case 2:
                switch (this.dr.fractionHandling) {
                    case 0:
                        return 0;
                    case 1:
                    case 2:
                        int i5 = i / BreakIterator.WORD_IDEO_LIMIT;
                        if (i5 == 1) {
                            return (this.dr.halfNames == null || this.dr.halfNames[timeUnit.ordinal()] == null) ? 5 : 6;
                        }
                        if ((i5 & 1) == 1) {
                            if (this.dr.pl == 5 && i5 > 21) {
                                return 5;
                            }
                            if (i5 == 3 && this.dr.pl == 1 && this.dr.fractionHandling != 2) {
                                return 0;
                            }
                        }
                        break;
                    case 3:
                        int i6 = i / BreakIterator.WORD_IDEO_LIMIT;
                        if (i6 == 1 || i6 == 3) {
                            return 3;
                        }
                        break;
                    default:
                        throw new IllegalStateException();
                }
                break;
            default:
                switch (this.dr.decimalHandling) {
                    case 0:
                    default:
                        return 0;
                    case 1:
                        return 5;
                    case 2:
                        if (i < 1000) {
                            return 5;
                        }
                        break;
                    case 3:
                        if (this.dr.pl == 3) {
                            return 3;
                        }
                        break;
                }
                break;
        }
        if (trace && i == 0) {
            System.err.println("EZeroHandling = " + ((int) this.dr.zeroHandling));
        }
        if (i == 0 && this.dr.zeroHandling == 1) {
            return 4;
        }
        switch (this.dr.pl) {
            case 0:
                return i3;
            case 1:
                break;
            case 2:
                if (i4 == 2) {
                    return 2;
                }
                break;
            case 3:
                int i7 = i4 % 100;
                if (i7 > 20) {
                    i7 %= 10;
                }
                if (i7 == 1) {
                    i3 = 1;
                } else if (i7 > 1 && i7 < 5) {
                    i3 = 3;
                }
                return i3;
            case 4:
                if (i4 == 2) {
                    return 2;
                }
                if (i4 != 1) {
                    if (timeUnit == TimeUnit.YEAR) {
                        break;
                    }
                    return i3;
                }
                break;
            case 5:
                if (i4 == 2) {
                    return 2;
                }
                if (i4 != 1) {
                    break;
                }
                return 1;
            default:
                System.err.println("dr.pl is " + ((int) this.dr.pl));
                throw new IllegalStateException();
        }
    }
}
