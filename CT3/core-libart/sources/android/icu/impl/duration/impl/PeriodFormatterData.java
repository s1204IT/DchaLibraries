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

    public PeriodFormatterData(String localeName, DataRecord dr) {
        this.dr = dr;
        this.localeName = localeName;
        if (localeName == null) {
            throw new NullPointerException("localename is null");
        }
        if (dr != null) {
        } else {
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

    public boolean appendPrefix(int tl, int td, StringBuffer sb) {
        String prefix;
        if (this.dr.scopeData != null) {
            int ix = (tl * 3) + td;
            DataRecord.ScopeData sd = this.dr.scopeData[ix];
            if (sd != null && (prefix = sd.prefix) != null) {
                sb.append(prefix);
                return sd.requiresDigitPrefix;
            }
            return false;
        }
        return false;
    }

    public void appendSuffix(int tl, int td, StringBuffer sb) {
        String suffix;
        if (this.dr.scopeData == null) {
            return;
        }
        int ix = (tl * 3) + td;
        DataRecord.ScopeData sd = this.dr.scopeData[ix];
        if (sd == null || (suffix = sd.suffix) == null) {
            return;
        }
        if (trace) {
            System.out.println("appendSuffix '" + suffix + "'");
        }
        sb.append(suffix);
    }

    public boolean appendUnit(TimeUnit unit, int count, int cv, int uv, boolean useCountSep, boolean useDigitPrefix, boolean multiple, boolean last, boolean wasSkipped, StringBuffer sb) {
        String name;
        boolean omitCount;
        int px = unit.ordinal();
        boolean willRequireSkipMarker = false;
        if (this.dr.requiresSkipMarker != null && this.dr.requiresSkipMarker[px] && this.dr.skippedUnitMarker != null) {
            if (!wasSkipped && last) {
                sb.append(this.dr.skippedUnitMarker);
            }
            willRequireSkipMarker = true;
        }
        if (uv != 0) {
            boolean useMedium = uv == 1;
            String[] names = useMedium ? this.dr.mediumNames : this.dr.shortNames;
            if (names == null || names[px] == null) {
                names = useMedium ? this.dr.shortNames : this.dr.mediumNames;
            }
            if (names != null && names[px] != null) {
                appendCount(unit, false, false, count, cv, useCountSep, names[px], last, sb);
                return false;
            }
        }
        if (cv == 2 && this.dr.halfSupport != null) {
            switch (this.dr.halfSupport[px]) {
                case 2:
                    if (count <= 1000) {
                    }
                case 1:
                    count = (count / BreakIterator.WORD_IDEO_LIMIT) * BreakIterator.WORD_IDEO_LIMIT;
                    cv = 3;
                    break;
            }
        }
        int form = computeForm(unit, count, cv, multiple ? last : false);
        if (form == 4) {
            if (this.dr.singularNames == null) {
                form = 1;
                name = this.dr.pluralNames[px][1];
            } else {
                name = this.dr.singularNames[px];
            }
        } else if (form == 5) {
            name = this.dr.pluralNames[px][1];
        } else if (form == 6) {
            name = this.dr.halfNames[px];
        } else {
            try {
                name = this.dr.pluralNames[px][form];
            } catch (NullPointerException e) {
                System.out.println("Null Pointer in PeriodFormatterData[" + this.localeName + "].au px: " + px + " form: " + form + " pn: " + Arrays.toString(this.dr.pluralNames));
                throw e;
            }
        }
        if (name == null) {
            form = 0;
            name = this.dr.pluralNames[px][0];
        }
        if (form == 4 || form == 6 || (this.dr.omitSingularCount && form == 1)) {
            omitCount = true;
        } else {
            omitCount = this.dr.omitDualCount && form == 2;
        }
        int suffixIndex = appendCount(unit, omitCount, useDigitPrefix, count, cv, useCountSep, name, last, sb);
        if (last && suffixIndex >= 0) {
            String suffix = null;
            if (this.dr.rqdSuffixes != null && suffixIndex < this.dr.rqdSuffixes.length) {
                suffix = this.dr.rqdSuffixes[suffixIndex];
            }
            if (suffix == null && this.dr.optSuffixes != null && suffixIndex < this.dr.optSuffixes.length) {
                suffix = this.dr.optSuffixes[suffixIndex];
            }
            if (suffix != null) {
                sb.append(suffix);
            }
        }
        return willRequireSkipMarker;
    }

    public int appendCount(TimeUnit unit, boolean omitCount, boolean useDigitPrefix, int count, int cv, boolean useSep, String name, boolean last, StringBuffer sb) {
        int hp;
        String measure;
        if (cv == 2 && this.dr.halves == null) {
            cv = 0;
        }
        if (!omitCount && useDigitPrefix && this.dr.digitPrefix != null) {
            sb.append(this.dr.digitPrefix);
        }
        int index = unit.ordinal();
        switch (cv) {
            case 0:
                if (!omitCount) {
                    appendInteger(count / 1000, 1, 10, sb);
                }
                break;
            case 1:
                int val = count / 1000;
                if (unit == TimeUnit.MINUTE && ((this.dr.fiveMinutes != null || this.dr.fifteenMinutes != null) && val != 0 && val % 5 == 0)) {
                    if (this.dr.fifteenMinutes != null && (val == 15 || val == 45)) {
                        int val2 = val == 15 ? 1 : 3;
                        if (!omitCount) {
                            appendInteger(val2, 1, 10, sb);
                        }
                        name = this.dr.fifteenMinutes;
                        index = 8;
                        break;
                    } else if (this.dr.fiveMinutes != null) {
                        int val3 = val / 5;
                        if (!omitCount) {
                            appendInteger(val3, 1, 10, sb);
                        }
                        name = this.dr.fiveMinutes;
                        index = 9;
                        break;
                    }
                } else {
                    if (!omitCount) {
                        appendInteger(val, 1, 10, sb);
                    }
                    break;
                }
                break;
            case 2:
                int v = count / BreakIterator.WORD_IDEO_LIMIT;
                if (v != 1 && !omitCount) {
                    appendCountValue(count, 1, 0, sb);
                }
                if ((v & 1) == 1) {
                    if (v == 1 && this.dr.halfNames != null && this.dr.halfNames[index] != null) {
                        sb.append(name);
                        if (last) {
                            return index;
                        }
                        return -1;
                    }
                    int solox = v == 1 ? 0 : 1;
                    if (this.dr.genders != null && this.dr.halves.length > 2 && this.dr.genders[index] == 1) {
                        solox += 2;
                    }
                    if (this.dr.halfPlacements == null) {
                        hp = 0;
                    } else {
                        hp = this.dr.halfPlacements[solox & 1];
                    }
                    String half = this.dr.halves[solox];
                    String measure2 = this.dr.measures == null ? null : this.dr.measures[index];
                    switch (hp) {
                        case 0:
                            sb.append(half);
                            break;
                        case 1:
                            if (measure2 != null) {
                                sb.append(measure2);
                                sb.append(half);
                                if (useSep && !omitCount) {
                                    sb.append(this.dr.countSep);
                                }
                                sb.append(name);
                                return -1;
                            }
                            sb.append(name);
                            sb.append(half);
                            if (last) {
                                return index;
                            }
                            return -1;
                        case 2:
                            if (measure2 != null) {
                                sb.append(measure2);
                            }
                            if (useSep && !omitCount) {
                                sb.append(this.dr.countSep);
                            }
                            sb.append(name);
                            sb.append(half);
                            if (last) {
                                return index;
                            }
                            return -1;
                    }
                }
                break;
            default:
                int decimals = 1;
                switch (cv) {
                    case 4:
                        decimals = 2;
                        break;
                    case 5:
                        decimals = 3;
                        break;
                }
                if (!omitCount) {
                    appendCountValue(count, 1, decimals, sb);
                }
                break;
        }
        if (!omitCount && useSep) {
            sb.append(this.dr.countSep);
        }
        if (!omitCount && this.dr.measures != null && index < this.dr.measures.length && (measure = this.dr.measures[index]) != null) {
            sb.append(measure);
        }
        sb.append(name);
        if (last) {
            return index;
        }
        return -1;
    }

    public void appendCountValue(int count, int integralDigits, int decimalDigits, StringBuffer sb) {
        int ival = count / 1000;
        if (decimalDigits == 0) {
            appendInteger(ival, integralDigits, 10, sb);
            return;
        }
        if (this.dr.requiresDigitSeparator && sb.length() > 0) {
            sb.append(' ');
        }
        appendDigits(ival, integralDigits, 10, sb);
        int dval = count % 1000;
        if (decimalDigits == 1) {
            dval /= 100;
        } else if (decimalDigits == 2) {
            dval /= 10;
        }
        sb.append(this.dr.decimalSep);
        appendDigits(dval, decimalDigits, decimalDigits, sb);
        if (!this.dr.requiresDigitSeparator) {
            return;
        }
        sb.append(' ');
    }

    public void appendInteger(int num, int mindigits, int maxdigits, StringBuffer sb) {
        String name;
        if (this.dr.numberNames != null && num < this.dr.numberNames.length && (name = this.dr.numberNames[num]) != null) {
            sb.append(name);
            return;
        }
        if (this.dr.requiresDigitSeparator && sb.length() > 0) {
            sb.append(' ');
        }
        switch (this.dr.numberSystem) {
            case 0:
                appendDigits(num, mindigits, maxdigits, sb);
                break;
            case 1:
                sb.append(Utils.chineseNumber(num, Utils.ChineseDigits.TRADITIONAL));
                break;
            case 2:
                sb.append(Utils.chineseNumber(num, Utils.ChineseDigits.SIMPLIFIED));
                break;
            case 3:
                sb.append(Utils.chineseNumber(num, Utils.ChineseDigits.KOREAN));
                break;
        }
        if (!this.dr.requiresDigitSeparator) {
            return;
        }
        sb.append(' ');
    }

    public void appendDigits(long num, int mindigits, int maxdigits, StringBuffer sb) {
        char[] buf = new char[maxdigits];
        int ix = maxdigits;
        while (ix > 0 && num > 0) {
            ix--;
            buf[ix] = (char) (((long) this.dr.zero) + (num % 10));
            num /= 10;
        }
        int e = maxdigits - mindigits;
        while (ix > e) {
            ix--;
            buf[ix] = this.dr.zero;
        }
        sb.append(buf, ix, maxdigits - ix);
    }

    public void appendSkippedUnit(StringBuffer sb) {
        if (this.dr.skippedUnitMarker == null) {
            return;
        }
        sb.append(this.dr.skippedUnitMarker);
    }

    public boolean appendUnitSeparator(TimeUnit unit, boolean longSep, boolean afterFirst, boolean beforeLast, StringBuffer sb) {
        if ((longSep && this.dr.unitSep != null) || this.dr.shortUnitSep != null) {
            if (longSep && this.dr.unitSep != null) {
                int ix = (afterFirst ? 2 : 0) + (beforeLast ? 1 : 0);
                sb.append(this.dr.unitSep[ix]);
                if (this.dr.unitSepRequiresDP != null) {
                    return this.dr.unitSepRequiresDP[ix];
                }
                return false;
            }
            sb.append(this.dr.shortUnitSep);
        }
        return false;
    }

    private int computeForm(TimeUnit unit, int count, int cv, boolean lastOfMultiple) {
        if (trace) {
            System.err.println("pfd.cf unit: " + unit + " count: " + count + " cv: " + cv + " dr.pl: " + ((int) this.dr.pl));
            Thread.dumpStack();
        }
        if (this.dr.pl == 0) {
            return 0;
        }
        int val = count / 1000;
        switch (cv) {
            case 0:
            case 1:
                break;
            case 2:
                switch (this.dr.fractionHandling) {
                    case 0:
                        return 0;
                    case 1:
                    case 2:
                        int v = count / BreakIterator.WORD_IDEO_LIMIT;
                        if (v == 1) {
                            return (this.dr.halfNames == null || this.dr.halfNames[unit.ordinal()] == null) ? 5 : 6;
                        }
                        if ((v & 1) == 1) {
                            if (this.dr.pl == 5 && v > 21) {
                                return 5;
                            }
                            if (v == 3 && this.dr.pl == 1 && this.dr.fractionHandling != 2) {
                                return 0;
                            }
                        }
                        break;
                    case 3:
                        int v2 = count / BreakIterator.WORD_IDEO_LIMIT;
                        if (v2 == 1 || v2 == 3) {
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
                        if (count < 1000) {
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
        if (trace && count == 0) {
            System.err.println("EZeroHandling = " + ((int) this.dr.zeroHandling));
        }
        if (count == 0 && this.dr.zeroHandling == 1) {
            return 4;
        }
        switch (this.dr.pl) {
            case 0:
                return 0;
            case 1:
                if (val != 1) {
                    return 0;
                }
                return 4;
            case 2:
                if (val == 2) {
                    return 2;
                }
                if (val != 1) {
                    return 0;
                }
                return 1;
            case 3:
                int v3 = val % 100;
                if (v3 > 20) {
                    v3 %= 10;
                }
                if (v3 == 1) {
                    return 1;
                }
                if (v3 <= 1 || v3 >= 5) {
                    return 0;
                }
                return 3;
            case 4:
                if (val == 2) {
                    return 2;
                }
                if (val == 1) {
                    if (lastOfMultiple) {
                        return 4;
                    }
                    return 1;
                }
                if (unit != TimeUnit.YEAR || val <= 11) {
                    return 0;
                }
                return 5;
            case 5:
                if (val == 2) {
                    return 2;
                }
                if (val == 1) {
                    return 1;
                }
                if (val <= 10) {
                    return 0;
                }
                return 5;
            default:
                System.err.println("dr.pl is " + ((int) this.dr.pl));
                throw new IllegalStateException();
        }
    }
}
