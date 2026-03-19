package android.icu.text;

import android.icu.impl.PluralRulesLoader;
import android.icu.impl.number.Padder;
import android.icu.util.Output;
import android.icu.util.ULocale;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class PluralRules implements Serializable {

    @Deprecated
    public static final String CATEGORY_SEPARATOR = ";  ";
    public static final String KEYWORD_FEW = "few";
    public static final String KEYWORD_MANY = "many";
    public static final String KEYWORD_ONE = "one";

    @Deprecated
    public static final String KEYWORD_RULE_SEPARATOR = ": ";
    public static final String KEYWORD_TWO = "two";
    public static final String KEYWORD_ZERO = "zero";
    public static final double NO_UNIQUE_VALUE = -0.00123456777d;
    private static final long serialVersionUID = 1;
    private final transient Set<String> keywords;
    private final RuleList rules;
    static final UnicodeSet ALLOWED_ID = new UnicodeSet("[a-z]").freeze();
    private static final Constraint NO_CONSTRAINT = new Constraint() {
        private static final long serialVersionUID = 9163464945387899416L;

        @Override
        public boolean isFulfilled(IFixedDecimal iFixedDecimal) {
            return true;
        }

        @Override
        public boolean isLimited(SampleType sampleType) {
            return false;
        }

        public String toString() {
            return "";
        }
    };
    public static final String KEYWORD_OTHER = "other";
    private static final Rule DEFAULT_RULE = new Rule(KEYWORD_OTHER, NO_CONSTRAINT, null, null);
    public static final PluralRules DEFAULT = new PluralRules(new RuleList().addRule(DEFAULT_RULE));
    static final Pattern AT_SEPARATED = Pattern.compile("\\s*\\Q\\E@\\s*");
    static final Pattern OR_SEPARATED = Pattern.compile("\\s*or\\s*");
    static final Pattern AND_SEPARATED = Pattern.compile("\\s*and\\s*");
    static final Pattern COMMA_SEPARATED = Pattern.compile("\\s*,\\s*");
    static final Pattern DOTDOT_SEPARATED = Pattern.compile("\\s*\\Q..\\E\\s*");
    static final Pattern TILDE_SEPARATED = Pattern.compile("\\s*~\\s*");
    static final Pattern SEMI_SEPARATED = Pattern.compile("\\s*;\\s*");

    private interface Constraint extends Serializable {
        boolean isFulfilled(IFixedDecimal iFixedDecimal);

        boolean isLimited(SampleType sampleType);
    }

    @Deprecated
    public interface IFixedDecimal {
        @Deprecated
        double getPluralOperand(Operand operand);

        @Deprecated
        boolean isInfinite();

        @Deprecated
        boolean isNaN();
    }

    public enum KeywordStatus {
        INVALID,
        SUPPRESSED,
        UNIQUE,
        BOUNDED,
        UNBOUNDED
    }

    @Deprecated
    public enum Operand {
        n,
        i,
        f,
        t,
        v,
        w,
        j
    }

    public enum PluralType {
        CARDINAL,
        ORDINAL
    }

    @Deprecated
    public enum SampleType {
        INTEGER,
        DECIMAL
    }

    @Deprecated
    public static abstract class Factory {
        @Deprecated
        public abstract PluralRules forLocale(ULocale uLocale, PluralType pluralType);

        @Deprecated
        public abstract ULocale[] getAvailableULocales();

        @Deprecated
        public abstract ULocale getFunctionalEquivalent(ULocale uLocale, boolean[] zArr);

        @Deprecated
        public abstract boolean hasOverride(ULocale uLocale);

        @Deprecated
        protected Factory() {
        }

        @Deprecated
        public final PluralRules forLocale(ULocale uLocale) {
            return forLocale(uLocale, PluralType.CARDINAL);
        }

        @Deprecated
        public static PluralRulesLoader getDefaultFactory() {
            return PluralRulesLoader.loader;
        }
    }

    public static PluralRules parseDescription(String str) throws ParseException {
        String strTrim = str.trim();
        return strTrim.length() == 0 ? DEFAULT : new PluralRules(parseRuleChain(strTrim));
    }

    public static PluralRules createRules(String str) {
        try {
            return parseDescription(str);
        } catch (Exception e) {
            return null;
        }
    }

    @Deprecated
    public static class FixedDecimal extends Number implements Comparable<FixedDecimal>, IFixedDecimal {
        static final long MAX = 1000000000000000000L;
        private static final long MAX_INTEGER_PART = 1000000000;
        private static final long serialVersionUID = -4756200506571685661L;
        private final int baseFactor;
        final long decimalDigits;
        final long decimalDigitsWithoutTrailingZeros;
        final boolean hasIntegerValue;
        final long integerValue;
        final boolean isNegative;
        final double source;
        final int visibleDecimalDigitCount;
        final int visibleDecimalDigitCountWithoutTrailingZeros;

        @Deprecated
        public double getSource() {
            return this.source;
        }

        @Deprecated
        public int getVisibleDecimalDigitCount() {
            return this.visibleDecimalDigitCount;
        }

        @Deprecated
        public int getVisibleDecimalDigitCountWithoutTrailingZeros() {
            return this.visibleDecimalDigitCountWithoutTrailingZeros;
        }

        @Deprecated
        public long getDecimalDigits() {
            return this.decimalDigits;
        }

        @Deprecated
        public long getDecimalDigitsWithoutTrailingZeros() {
            return this.decimalDigitsWithoutTrailingZeros;
        }

        @Deprecated
        public long getIntegerValue() {
            return this.integerValue;
        }

        @Deprecated
        public boolean isHasIntegerValue() {
            return this.hasIntegerValue;
        }

        @Deprecated
        public boolean isNegative() {
            return this.isNegative;
        }

        @Deprecated
        public int getBaseFactor() {
            return this.baseFactor;
        }

        @Deprecated
        public FixedDecimal(double d, int i, long j) {
            long j2;
            this.isNegative = d < 0.0d;
            this.source = this.isNegative ? -d : d;
            this.visibleDecimalDigitCount = i;
            this.decimalDigits = j;
            if (d > 1.0E18d) {
                j2 = MAX;
            } else {
                j2 = (long) d;
            }
            this.integerValue = j2;
            this.hasIntegerValue = this.source == ((double) this.integerValue);
            if (j == 0) {
                this.decimalDigitsWithoutTrailingZeros = 0L;
                this.visibleDecimalDigitCountWithoutTrailingZeros = 0;
            } else {
                int i2 = i;
                while (j % 10 == 0) {
                    j /= 10;
                    i2--;
                }
                this.decimalDigitsWithoutTrailingZeros = j;
                this.visibleDecimalDigitCountWithoutTrailingZeros = i2;
            }
            this.baseFactor = (int) Math.pow(10.0d, i);
        }

        @Deprecated
        public FixedDecimal(double d, int i) {
            this(d, i, getFractionalDigits(d, i));
        }

        private static int getFractionalDigits(double d, int i) {
            if (i == 0) {
                return 0;
            }
            if (d < 0.0d) {
                d = -d;
            }
            int iPow = (int) Math.pow(10.0d, i);
            return (int) (Math.round(d * ((double) iPow)) % ((long) iPow));
        }

        @Deprecated
        public FixedDecimal(double d) {
            this(d, decimals(d));
        }

        @Deprecated
        public FixedDecimal(long j) {
            this(j, 0);
        }

        @Deprecated
        public static int decimals(double d) {
            if (Double.isInfinite(d) || Double.isNaN(d)) {
                return 0;
            }
            if (d < 0.0d) {
                d = -d;
            }
            if (d == Math.floor(d)) {
                return 0;
            }
            if (d < 1.0E9d) {
                long j = ((long) (d * 1000000.0d)) % 1000000;
                int i = 10;
                for (int i2 = 6; i2 > 0; i2--) {
                    if (j % ((long) i) == 0) {
                        i *= 10;
                    } else {
                        return i2;
                    }
                }
                return 0;
            }
            String str = String.format(Locale.ENGLISH, "%1.15e", Double.valueOf(d));
            int iLastIndexOf = str.lastIndexOf(101);
            int i3 = iLastIndexOf + 1;
            if (str.charAt(i3) == '+') {
                i3++;
            }
            int i4 = (iLastIndexOf - 2) - Integer.parseInt(str.substring(i3));
            if (i4 < 0) {
                return 0;
            }
            for (int i5 = iLastIndexOf - 1; i4 > 0 && str.charAt(i5) == '0'; i5--) {
                i4--;
            }
            return i4;
        }

        @Deprecated
        public FixedDecimal(String str) {
            this(Double.parseDouble(str), getVisibleFractionCount(str));
        }

        private static int getVisibleFractionCount(String str) {
            String strTrim = str.trim();
            int iIndexOf = strTrim.indexOf(46) + 1;
            if (iIndexOf == 0) {
                return 0;
            }
            return strTrim.length() - iIndexOf;
        }

        @Override
        @Deprecated
        public double getPluralOperand(Operand operand) {
            switch (operand) {
                case n:
                    return this.source;
                case i:
                    return this.integerValue;
                case f:
                    return this.decimalDigits;
                case t:
                    return this.decimalDigitsWithoutTrailingZeros;
                case v:
                    return this.visibleDecimalDigitCount;
                case w:
                    return this.visibleDecimalDigitCountWithoutTrailingZeros;
                default:
                    return this.source;
            }
        }

        @Deprecated
        public static Operand getOperand(String str) {
            return Operand.valueOf(str);
        }

        @Override
        @Deprecated
        public int compareTo(FixedDecimal fixedDecimal) {
            if (this.integerValue != fixedDecimal.integerValue) {
                return this.integerValue < fixedDecimal.integerValue ? -1 : 1;
            }
            if (this.source != fixedDecimal.source) {
                return this.source < fixedDecimal.source ? -1 : 1;
            }
            if (this.visibleDecimalDigitCount != fixedDecimal.visibleDecimalDigitCount) {
                return this.visibleDecimalDigitCount < fixedDecimal.visibleDecimalDigitCount ? -1 : 1;
            }
            long j = this.decimalDigits - fixedDecimal.decimalDigits;
            if (j != 0) {
                return j < 0 ? -1 : 1;
            }
            return 0;
        }

        @Deprecated
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof FixedDecimal)) {
                return false;
            }
            FixedDecimal fixedDecimal = (FixedDecimal) obj;
            if (this.source != fixedDecimal.source || this.visibleDecimalDigitCount != fixedDecimal.visibleDecimalDigitCount || this.decimalDigits != fixedDecimal.decimalDigits) {
                return false;
            }
            return true;
        }

        @Deprecated
        public int hashCode() {
            return (int) (this.decimalDigits + ((long) (37 * (this.visibleDecimalDigitCount + ((int) (37.0d * this.source))))));
        }

        @Deprecated
        public String toString() {
            return String.format("%." + this.visibleDecimalDigitCount + "f", Double.valueOf(this.source));
        }

        @Deprecated
        public boolean hasIntegerValue() {
            return this.hasIntegerValue;
        }

        @Override
        @Deprecated
        public int intValue() {
            return (int) this.integerValue;
        }

        @Override
        @Deprecated
        public long longValue() {
            return this.integerValue;
        }

        @Override
        @Deprecated
        public float floatValue() {
            return (float) this.source;
        }

        @Override
        @Deprecated
        public double doubleValue() {
            return this.isNegative ? -this.source : this.source;
        }

        @Deprecated
        public long getShiftedValue() {
            return (this.integerValue * ((long) this.baseFactor)) + this.decimalDigits;
        }

        private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
            throw new NotSerializableException();
        }

        private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
            throw new NotSerializableException();
        }

        @Override
        @Deprecated
        public boolean isNaN() {
            return Double.isNaN(this.source);
        }

        @Override
        @Deprecated
        public boolean isInfinite() {
            return Double.isInfinite(this.source);
        }
    }

    @Deprecated
    public static class FixedDecimalRange {

        @Deprecated
        public final FixedDecimal end;

        @Deprecated
        public final FixedDecimal start;

        @Deprecated
        public FixedDecimalRange(FixedDecimal fixedDecimal, FixedDecimal fixedDecimal2) {
            if (fixedDecimal.visibleDecimalDigitCount != fixedDecimal2.visibleDecimalDigitCount) {
                throw new IllegalArgumentException("Ranges must have the same number of visible decimals: " + fixedDecimal + "~" + fixedDecimal2);
            }
            this.start = fixedDecimal;
            this.end = fixedDecimal2;
        }

        @Deprecated
        public String toString() {
            String str;
            StringBuilder sb = new StringBuilder();
            sb.append(this.start);
            if (this.end == this.start) {
                str = "";
            } else {
                str = "~" + this.end;
            }
            sb.append(str);
            return sb.toString();
        }
    }

    @Deprecated
    public static class FixedDecimalSamples {

        @Deprecated
        public final boolean bounded;

        @Deprecated
        public final SampleType sampleType;

        @Deprecated
        public final Set<FixedDecimalRange> samples;

        private FixedDecimalSamples(SampleType sampleType, Set<FixedDecimalRange> set, boolean z) {
            this.sampleType = sampleType;
            this.samples = set;
            this.bounded = z;
        }

        static FixedDecimalSamples parse(String str) {
            SampleType sampleType;
            LinkedHashSet linkedHashSet = new LinkedHashSet();
            if (str.startsWith("integer")) {
                sampleType = SampleType.INTEGER;
            } else if (str.startsWith("decimal")) {
                sampleType = SampleType.DECIMAL;
            } else {
                throw new IllegalArgumentException("Samples must start with 'integer' or 'decimal'");
            }
            boolean z = true;
            boolean z2 = false;
            for (String str2 : PluralRules.COMMA_SEPARATED.split(str.substring(7).trim())) {
                if (str2.equals("…") || str2.equals("...")) {
                    z2 = true;
                    z = false;
                } else {
                    if (z2) {
                        throw new IllegalArgumentException("Can only have … at the end of samples: " + str2);
                    }
                    String[] strArrSplit = PluralRules.TILDE_SEPARATED.split(str2);
                    switch (strArrSplit.length) {
                        case 1:
                            FixedDecimal fixedDecimal = new FixedDecimal(strArrSplit[0]);
                            checkDecimal(sampleType, fixedDecimal);
                            linkedHashSet.add(new FixedDecimalRange(fixedDecimal, fixedDecimal));
                            break;
                        case 2:
                            FixedDecimal fixedDecimal2 = new FixedDecimal(strArrSplit[0]);
                            FixedDecimal fixedDecimal3 = new FixedDecimal(strArrSplit[1]);
                            checkDecimal(sampleType, fixedDecimal2);
                            checkDecimal(sampleType, fixedDecimal3);
                            linkedHashSet.add(new FixedDecimalRange(fixedDecimal2, fixedDecimal3));
                            break;
                        default:
                            throw new IllegalArgumentException("Ill-formed number range: " + str2);
                    }
                }
            }
            return new FixedDecimalSamples(sampleType, Collections.unmodifiableSet(linkedHashSet), z);
        }

        private static void checkDecimal(SampleType sampleType, FixedDecimal fixedDecimal) {
            if ((sampleType == SampleType.INTEGER) != (fixedDecimal.getVisibleDecimalDigitCount() == 0)) {
                throw new IllegalArgumentException("Ill-formed number range: " + fixedDecimal);
            }
        }

        @Deprecated
        public Set<Double> addSamples(Set<Double> set) {
            for (FixedDecimalRange fixedDecimalRange : this.samples) {
                long shiftedValue = fixedDecimalRange.end.getShiftedValue();
                for (long shiftedValue2 = fixedDecimalRange.start.getShiftedValue(); shiftedValue2 <= shiftedValue; shiftedValue2 += PluralRules.serialVersionUID) {
                    set.add(Double.valueOf(shiftedValue2 / ((double) fixedDecimalRange.start.baseFactor)));
                }
            }
            return set;
        }

        @Deprecated
        public String toString() {
            StringBuilder sb = new StringBuilder("@");
            sb.append(this.sampleType.toString().toLowerCase(Locale.ENGLISH));
            boolean z = true;
            for (FixedDecimalRange fixedDecimalRange : this.samples) {
                if (z) {
                    z = false;
                } else {
                    sb.append(",");
                }
                sb.append(' ');
                sb.append(fixedDecimalRange);
            }
            if (!this.bounded) {
                sb.append(", …");
            }
            return sb.toString();
        }

        @Deprecated
        public Set<FixedDecimalRange> getSamples() {
            return this.samples;
        }

        @Deprecated
        public void getStartEndSamples(Set<FixedDecimal> set) {
            for (FixedDecimalRange fixedDecimalRange : this.samples) {
                set.add(fixedDecimalRange.start);
                set.add(fixedDecimalRange.end);
            }
        }
    }

    static class SimpleTokenizer {
        static final UnicodeSet BREAK_AND_IGNORE = new UnicodeSet(9, 10, 12, 13, 32, 32).freeze();
        static final UnicodeSet BREAK_AND_KEEP = new UnicodeSet(33, 33, 37, 37, 44, 44, 46, 46, 61, 61).freeze();

        SimpleTokenizer() {
        }

        static String[] split(String str) {
            ArrayList arrayList = new ArrayList();
            int i = -1;
            for (int i2 = 0; i2 < str.length(); i2++) {
                char cCharAt = str.charAt(i2);
                if (BREAK_AND_IGNORE.contains(cCharAt)) {
                    if (i >= 0) {
                        arrayList.add(str.substring(i, i2));
                        i = -1;
                    }
                } else if (BREAK_AND_KEEP.contains(cCharAt)) {
                    if (i >= 0) {
                        arrayList.add(str.substring(i, i2));
                    }
                    arrayList.add(str.substring(i2, i2 + 1));
                    i = -1;
                } else if (i < 0) {
                    i = i2;
                }
            }
            if (i >= 0) {
                arrayList.add(str.substring(i));
            }
            return (String[]) arrayList.toArray(new String[arrayList.size()]);
        }
    }

    private static Constraint parseConstraint(String str) throws ParseException {
        String[] strArr;
        int i;
        Constraint constraint;
        Constraint rangeConstraint;
        int i2;
        String strNextToken;
        boolean z;
        int i3;
        boolean zEquals;
        int i4;
        String strNextToken2;
        boolean z2;
        boolean z3;
        int i5;
        long j;
        int i6;
        long j2;
        int i7;
        long[] jArr;
        String[] strArrSplit = OR_SEPARATED.split(str);
        int i8 = 0;
        int i9 = 0;
        Constraint orConstraint = null;
        while (i9 < strArrSplit.length) {
            String[] strArrSplit2 = AND_SEPARATED.split(strArrSplit[i9]);
            int i10 = i8;
            Constraint andConstraint = null;
            while (i10 < strArrSplit2.length) {
                Constraint constraint2 = NO_CONSTRAINT;
                String strTrim = strArrSplit2[i10].trim();
                String[] strArrSplit3 = SimpleTokenizer.split(strTrim);
                String str2 = strArrSplit3[i8];
                try {
                    Operand operand = FixedDecimal.getOperand(str2);
                    if (1 < strArrSplit3.length) {
                        String strNextToken3 = strArrSplit3[1];
                        if ("mod".equals(strNextToken3) || "%".equals(strNextToken3)) {
                            int i11 = Integer.parseInt(strArrSplit3[2]);
                            strNextToken3 = nextToken(strArrSplit3, 3, strTrim);
                            i8 = i11;
                            i2 = 4;
                        } else {
                            i2 = 2;
                        }
                        if ("not".equals(strNextToken3)) {
                            i3 = i2 + 1;
                            strNextToken = nextToken(strArrSplit3, i2, strTrim);
                            if ("=".equals(strNextToken)) {
                                throw unexpected(strNextToken, strTrim);
                            }
                        } else if ("!".equals(strNextToken3)) {
                            i3 = i2 + 1;
                            strNextToken = nextToken(strArrSplit3, i2, strTrim);
                            if (!"=".equals(strNextToken)) {
                                throw unexpected(strNextToken, strTrim);
                            }
                        } else {
                            strNextToken = strNextToken3;
                            z = true;
                            if (!"is".equals(strNextToken) || "in".equals(strNextToken) || "=".equals(strNextToken)) {
                                zEquals = "is".equals(strNextToken);
                                if (!zEquals && !z) {
                                    throw unexpected(strNextToken, strTrim);
                                }
                                i4 = i2 + 1;
                                strNextToken2 = nextToken(strArrSplit3, i2, strTrim);
                                z2 = true;
                            } else if ("within".equals(strNextToken)) {
                                i4 = i2 + 1;
                                strNextToken2 = nextToken(strArrSplit3, i2, strTrim);
                                zEquals = false;
                                z2 = false;
                            } else {
                                throw unexpected(strNextToken, strTrim);
                            }
                            if ("not".equals(strNextToken2)) {
                                z3 = z;
                                i5 = i4;
                            } else {
                                if (!zEquals && !z) {
                                    throw unexpected(strNextToken2, strTrim);
                                }
                                i5 = i4 + 1;
                                strNextToken2 = nextToken(strArrSplit3, i4, strTrim);
                                z3 = !z;
                            }
                            ArrayList arrayList = new ArrayList();
                            double dMin = 9.223372036854776E18d;
                            double dMax = -9.223372036854776E18d;
                            while (true) {
                                int i12 = i8;
                                j = Long.parseLong(strNextToken2);
                                if (i5 >= strArrSplit3.length) {
                                    i6 = i5 + 1;
                                    strNextToken2 = nextToken(strArrSplit3, i5, strTrim);
                                    if (strNextToken2.equals(".")) {
                                        int i13 = i6 + 1;
                                        String strNextToken4 = nextToken(strArrSplit3, i6, strTrim);
                                        if (!strNextToken4.equals(".")) {
                                            throw unexpected(strNextToken4, strTrim);
                                        }
                                        i6 = i13 + 1;
                                        strNextToken2 = nextToken(strArrSplit3, i13, strTrim);
                                        j2 = Long.parseLong(strNextToken2);
                                        strArr = strArrSplit;
                                        if (i6 < strArrSplit3.length) {
                                            int i14 = i6 + 1;
                                            strNextToken2 = nextToken(strArrSplit3, i6, strTrim);
                                            if (!strNextToken2.equals(",")) {
                                                throw unexpected(strNextToken2, strTrim);
                                            }
                                            i6 = i14;
                                        }
                                        if (j > j2) {
                                            throw unexpected(j + "~" + j2, strTrim);
                                        }
                                        if (i12 != 0) {
                                            i = i9;
                                            constraint = orConstraint;
                                            i7 = i12;
                                            if (j2 >= i7) {
                                                throw unexpected(j2 + ">mod=" + i7, strTrim);
                                            }
                                        } else {
                                            i = i9;
                                            constraint = orConstraint;
                                            i7 = i12;
                                        }
                                        arrayList.add(Long.valueOf(j));
                                        arrayList.add(Long.valueOf(j2));
                                        dMin = Math.min(dMin, j);
                                        dMax = Math.max(dMax, j2);
                                        if (i6 < strArrSplit3.length) {
                                            i5 = i6 + 1;
                                            strNextToken2 = nextToken(strArrSplit3, i6, strTrim);
                                            i8 = i7;
                                            strArrSplit = strArr;
                                            i9 = i;
                                            orConstraint = constraint;
                                        } else {
                                            if (strNextToken2.equals(",")) {
                                                throw unexpected(strNextToken2, strTrim);
                                            }
                                            if (arrayList.size() != 2) {
                                                long[] jArr2 = new long[arrayList.size()];
                                                for (int i15 = 0; i15 < jArr2.length; i15++) {
                                                    jArr2[i15] = ((Long) arrayList.get(i15)).longValue();
                                                }
                                                jArr = jArr2;
                                            } else {
                                                jArr = null;
                                            }
                                            if (dMin != dMax && zEquals && !z3) {
                                                throw unexpected("is not <range>", strTrim);
                                            }
                                            rangeConstraint = new RangeConstraint(i7, z3, operand, z2, dMin, dMax, jArr);
                                        }
                                    } else {
                                        strArr = strArrSplit;
                                        if (!strNextToken2.equals(",")) {
                                            throw unexpected(strNextToken2, strTrim);
                                        }
                                    }
                                } else {
                                    strArr = strArrSplit;
                                    i6 = i5;
                                }
                                j2 = j;
                                if (j > j2) {
                                }
                            }
                        }
                        i2 = i3;
                        z = false;
                        if (!"is".equals(strNextToken)) {
                            zEquals = "is".equals(strNextToken);
                            if (!zEquals) {
                            }
                            i4 = i2 + 1;
                            strNextToken2 = nextToken(strArrSplit3, i2, strTrim);
                            z2 = true;
                            if ("not".equals(strNextToken2)) {
                            }
                            ArrayList arrayList2 = new ArrayList();
                            double dMin2 = 9.223372036854776E18d;
                            double dMax2 = -9.223372036854776E18d;
                            while (true) {
                                int i122 = i8;
                                j = Long.parseLong(strNextToken2);
                                if (i5 >= strArrSplit3.length) {
                                }
                                j2 = j;
                                if (j > j2) {
                                }
                                i5 = i6 + 1;
                                strNextToken2 = nextToken(strArrSplit3, i6, strTrim);
                                i8 = i7;
                                strArrSplit = strArr;
                                i9 = i;
                                orConstraint = constraint;
                            }
                        }
                    } else {
                        strArr = strArrSplit;
                        i = i9;
                        constraint = orConstraint;
                        rangeConstraint = constraint2;
                    }
                    if (andConstraint == null) {
                        andConstraint = rangeConstraint;
                    } else {
                        andConstraint = new AndConstraint(andConstraint, rangeConstraint);
                    }
                    i10++;
                    strArrSplit = strArr;
                    i9 = i;
                    orConstraint = constraint;
                    i8 = 0;
                } catch (Exception e) {
                    throw unexpected(str2, strTrim);
                }
            }
            String[] strArr2 = strArrSplit;
            int i16 = i9;
            Constraint constraint3 = orConstraint;
            if (constraint3 == null) {
                orConstraint = andConstraint;
            } else {
                orConstraint = new OrConstraint(constraint3, andConstraint);
            }
            i9 = i16 + 1;
            strArrSplit = strArr2;
            i8 = 0;
        }
        return orConstraint;
    }

    private static ParseException unexpected(String str, String str2) {
        return new ParseException("unexpected token '" + str + "' in '" + str2 + "'", -1);
    }

    private static String nextToken(String[] strArr, int i, String str) throws ParseException {
        if (i < strArr.length) {
            return strArr[i];
        }
        throw new ParseException("missing token at end of '" + str + "'", -1);
    }

    private static Rule parseRule(String str) throws ParseException {
        FixedDecimalSamples fixedDecimalSamples;
        Constraint constraint;
        if (str.length() == 0) {
            return DEFAULT_RULE;
        }
        String lowerCase = str.toLowerCase(Locale.ENGLISH);
        int iIndexOf = lowerCase.indexOf(58);
        if (iIndexOf == -1) {
            throw new ParseException("missing ':' in rule description '" + lowerCase + "'", 0);
        }
        String strTrim = lowerCase.substring(0, iIndexOf).trim();
        if (!isValidKeyword(strTrim)) {
            throw new ParseException("keyword '" + strTrim + " is not valid", 0);
        }
        String strTrim2 = lowerCase.substring(iIndexOf + 1).trim();
        String[] strArrSplit = AT_SEPARATED.split(strTrim2);
        FixedDecimalSamples fixedDecimalSamples2 = null;
        switch (strArrSplit.length) {
            case 1:
                fixedDecimalSamples = null;
                break;
            case 2:
                fixedDecimalSamples = FixedDecimalSamples.parse(strArrSplit[1]);
                if (fixedDecimalSamples.sampleType != SampleType.DECIMAL) {
                    fixedDecimalSamples2 = fixedDecimalSamples;
                    fixedDecimalSamples = null;
                }
                break;
            case 3:
                fixedDecimalSamples2 = FixedDecimalSamples.parse(strArrSplit[1]);
                FixedDecimalSamples fixedDecimalSamples3 = FixedDecimalSamples.parse(strArrSplit[2]);
                if (fixedDecimalSamples2.sampleType != SampleType.INTEGER || fixedDecimalSamples3.sampleType != SampleType.DECIMAL) {
                    throw new IllegalArgumentException("Must have @integer then @decimal in " + strTrim2);
                }
                fixedDecimalSamples = fixedDecimalSamples3;
                break;
                break;
            default:
                throw new IllegalArgumentException("Too many samples in " + strTrim2);
        }
        boolean zEquals = strTrim.equals(KEYWORD_OTHER);
        if (zEquals != (strArrSplit[0].length() == 0)) {
            throw new IllegalArgumentException("The keyword 'other' must have no constraints, just samples.");
        }
        if (zEquals) {
            constraint = NO_CONSTRAINT;
        } else {
            constraint = parseConstraint(strArrSplit[0]);
        }
        return new Rule(strTrim, constraint, fixedDecimalSamples2, fixedDecimalSamples);
    }

    private static RuleList parseRuleChain(String str) throws ParseException {
        RuleList ruleList = new RuleList();
        if (str.endsWith(";")) {
            str = str.substring(0, str.length() - 1);
        }
        for (String str2 : SEMI_SEPARATED.split(str)) {
            Rule rule = parseRule(str2.trim());
            RuleList.access$276(ruleList, (rule.integerSamples == null && rule.decimalSamples == null) ? 0 : 1);
            ruleList.addRule(rule);
        }
        return ruleList.finish();
    }

    private static class RangeConstraint implements Constraint, Serializable {
        private static final long serialVersionUID = 1;
        private final boolean inRange;
        private final boolean integersOnly;
        private final double lowerBound;
        private final int mod;
        private final Operand operand;
        private final long[] range_list;
        private final double upperBound;

        RangeConstraint(int i, boolean z, Operand operand, boolean z2, double d, double d2, long[] jArr) {
            this.mod = i;
            this.inRange = z;
            this.integersOnly = z2;
            this.lowerBound = d;
            this.upperBound = d2;
            this.range_list = jArr;
            this.operand = operand;
        }

        @Override
        public boolean isFulfilled(IFixedDecimal iFixedDecimal) {
            double pluralOperand = iFixedDecimal.getPluralOperand(this.operand);
            if ((this.integersOnly && pluralOperand - ((long) pluralOperand) != 0.0d) || (this.operand == Operand.j && iFixedDecimal.getPluralOperand(Operand.v) != 0.0d)) {
                return !this.inRange;
            }
            if (this.mod != 0) {
                pluralOperand %= (double) this.mod;
            }
            boolean z = pluralOperand >= this.lowerBound && pluralOperand <= this.upperBound;
            if (z && this.range_list != null) {
                z = false;
                for (int i = 0; !z && i < this.range_list.length; i += 2) {
                    z = pluralOperand >= ((double) this.range_list[i]) && pluralOperand <= ((double) this.range_list[i + 1]);
                }
            }
            return this.inRange == z;
        }

        @Override
        public boolean isLimited(SampleType sampleType) {
            boolean z = (this.operand == Operand.v || this.operand == Operand.w || this.operand == Operand.f || this.operand == Operand.t) && this.inRange != ((this.lowerBound > this.upperBound ? 1 : (this.lowerBound == this.upperBound ? 0 : -1)) == 0 && (this.lowerBound > 0.0d ? 1 : (this.lowerBound == 0.0d ? 0 : -1)) == 0);
            switch (sampleType) {
                case INTEGER:
                    if (!z) {
                        if ((this.operand != Operand.n && this.operand != Operand.i && this.operand != Operand.j) || this.mod != 0 || !this.inRange) {
                            break;
                        }
                    }
                    break;
                case DECIMAL:
                    if ((z && this.operand != Operand.n && this.operand != Operand.j) || ((!this.integersOnly && this.lowerBound != this.upperBound) || this.mod != 0 || !this.inRange)) {
                        break;
                    }
                    break;
            }
            return false;
        }

        public String toString() {
            String str;
            StringBuilder sb = new StringBuilder();
            sb.append(this.operand);
            if (this.mod != 0) {
                sb.append(" % ");
                sb.append(this.mod);
            }
            if (!(this.lowerBound != this.upperBound)) {
                str = this.inRange ? " = " : " != ";
            } else if (this.integersOnly) {
                str = this.inRange ? " = " : " != ";
            } else {
                str = this.inRange ? " within " : " not within ";
            }
            sb.append(str);
            if (this.range_list == null) {
                PluralRules.addRange(sb, this.lowerBound, this.upperBound, false);
            } else {
                int i = 0;
                while (i < this.range_list.length) {
                    PluralRules.addRange(sb, this.range_list[i], this.range_list[i + 1], i != 0);
                    i += 2;
                }
            }
            return sb.toString();
        }
    }

    private static void addRange(StringBuilder sb, double d, double d2, boolean z) {
        if (z) {
            sb.append(",");
        }
        if (d == d2) {
            sb.append(format(d));
            return;
        }
        sb.append(format(d) + ".." + format(d2));
    }

    private static String format(double d) {
        long j = (long) d;
        return d == ((double) j) ? String.valueOf(j) : String.valueOf(d);
    }

    private static abstract class BinaryConstraint implements Constraint, Serializable {
        private static final long serialVersionUID = 1;
        protected final Constraint a;
        protected final Constraint b;

        protected BinaryConstraint(Constraint constraint, Constraint constraint2) {
            this.a = constraint;
            this.b = constraint2;
        }
    }

    private static class AndConstraint extends BinaryConstraint {
        private static final long serialVersionUID = 7766999779862263523L;

        AndConstraint(Constraint constraint, Constraint constraint2) {
            super(constraint, constraint2);
        }

        @Override
        public boolean isFulfilled(IFixedDecimal iFixedDecimal) {
            return this.a.isFulfilled(iFixedDecimal) && this.b.isFulfilled(iFixedDecimal);
        }

        @Override
        public boolean isLimited(SampleType sampleType) {
            return this.a.isLimited(sampleType) || this.b.isLimited(sampleType);
        }

        public String toString() {
            return this.a.toString() + " and " + this.b.toString();
        }
    }

    private static class OrConstraint extends BinaryConstraint {
        private static final long serialVersionUID = 1405488568664762222L;

        OrConstraint(Constraint constraint, Constraint constraint2) {
            super(constraint, constraint2);
        }

        @Override
        public boolean isFulfilled(IFixedDecimal iFixedDecimal) {
            return this.a.isFulfilled(iFixedDecimal) || this.b.isFulfilled(iFixedDecimal);
        }

        @Override
        public boolean isLimited(SampleType sampleType) {
            return this.a.isLimited(sampleType) && this.b.isLimited(sampleType);
        }

        public String toString() {
            return this.a.toString() + " or " + this.b.toString();
        }
    }

    private static class Rule implements Serializable {
        private static final long serialVersionUID = 1;
        private final Constraint constraint;
        private final FixedDecimalSamples decimalSamples;
        private final FixedDecimalSamples integerSamples;
        private final String keyword;

        public Rule(String str, Constraint constraint, FixedDecimalSamples fixedDecimalSamples, FixedDecimalSamples fixedDecimalSamples2) {
            this.keyword = str;
            this.constraint = constraint;
            this.integerSamples = fixedDecimalSamples;
            this.decimalSamples = fixedDecimalSamples2;
        }

        public Rule and(Constraint constraint) {
            return new Rule(this.keyword, new AndConstraint(this.constraint, constraint), this.integerSamples, this.decimalSamples);
        }

        public Rule or(Constraint constraint) {
            return new Rule(this.keyword, new OrConstraint(this.constraint, constraint), this.integerSamples, this.decimalSamples);
        }

        public String getKeyword() {
            return this.keyword;
        }

        public boolean appliesTo(IFixedDecimal iFixedDecimal) {
            return this.constraint.isFulfilled(iFixedDecimal);
        }

        public boolean isLimited(SampleType sampleType) {
            return this.constraint.isLimited(sampleType);
        }

        public String toString() {
            String str;
            String str2;
            StringBuilder sb = new StringBuilder();
            sb.append(this.keyword);
            sb.append(PluralRules.KEYWORD_RULE_SEPARATOR);
            sb.append(this.constraint.toString());
            if (this.integerSamples == null) {
                str = "";
            } else {
                str = Padder.FALLBACK_PADDING_STRING + this.integerSamples.toString();
            }
            sb.append(str);
            if (this.decimalSamples == null) {
                str2 = "";
            } else {
                str2 = Padder.FALLBACK_PADDING_STRING + this.decimalSamples.toString();
            }
            sb.append(str2);
            return sb.toString();
        }

        @Deprecated
        public int hashCode() {
            return this.keyword.hashCode() ^ this.constraint.hashCode();
        }

        public String getConstraint() {
            return this.constraint.toString();
        }
    }

    private static class RuleList implements Serializable {
        private static final long serialVersionUID = 1;
        private boolean hasExplicitBoundingInfo;
        private final List<Rule> rules;

        private RuleList() {
            this.hasExplicitBoundingInfo = false;
            this.rules = new ArrayList();
        }

        static boolean access$276(RuleList ruleList, int i) {
            ?? r2 = (byte) (i | (ruleList.hasExplicitBoundingInfo ? 1 : 0));
            ruleList.hasExplicitBoundingInfo = r2;
            return r2;
        }

        public RuleList addRule(Rule rule) {
            String keyword = rule.getKeyword();
            Iterator<Rule> it = this.rules.iterator();
            while (it.hasNext()) {
                if (keyword.equals(it.next().getKeyword())) {
                    throw new IllegalArgumentException("Duplicate keyword: " + keyword);
                }
            }
            this.rules.add(rule);
            return this;
        }

        public RuleList finish() throws ParseException {
            Iterator<Rule> it = this.rules.iterator();
            Rule rule = null;
            while (it.hasNext()) {
                Rule next = it.next();
                if (PluralRules.KEYWORD_OTHER.equals(next.getKeyword())) {
                    it.remove();
                    rule = next;
                }
            }
            if (rule == null) {
                rule = PluralRules.parseRule("other:");
            }
            this.rules.add(rule);
            return this;
        }

        private Rule selectRule(IFixedDecimal iFixedDecimal) {
            for (Rule rule : this.rules) {
                if (rule.appliesTo(iFixedDecimal)) {
                    return rule;
                }
            }
            return null;
        }

        public String select(IFixedDecimal iFixedDecimal) {
            if (iFixedDecimal.isInfinite() || iFixedDecimal.isNaN()) {
                return PluralRules.KEYWORD_OTHER;
            }
            return selectRule(iFixedDecimal).getKeyword();
        }

        public Set<String> getKeywords() {
            LinkedHashSet linkedHashSet = new LinkedHashSet();
            Iterator<Rule> it = this.rules.iterator();
            while (it.hasNext()) {
                linkedHashSet.add(it.next().getKeyword());
            }
            return linkedHashSet;
        }

        public boolean isLimited(String str, SampleType sampleType) {
            if (this.hasExplicitBoundingInfo) {
                FixedDecimalSamples decimalSamples = getDecimalSamples(str, sampleType);
                if (decimalSamples == null) {
                    return true;
                }
                return decimalSamples.bounded;
            }
            return computeLimited(str, sampleType);
        }

        public boolean computeLimited(String str, SampleType sampleType) {
            boolean z = false;
            for (Rule rule : this.rules) {
                if (str.equals(rule.getKeyword())) {
                    if (!rule.isLimited(sampleType)) {
                        return false;
                    }
                    z = true;
                }
            }
            return z;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Rule rule : this.rules) {
                if (sb.length() != 0) {
                    sb.append(PluralRules.CATEGORY_SEPARATOR);
                }
                sb.append(rule);
            }
            return sb.toString();
        }

        public String getRules(String str) {
            for (Rule rule : this.rules) {
                if (rule.getKeyword().equals(str)) {
                    return rule.getConstraint();
                }
            }
            return null;
        }

        public boolean select(IFixedDecimal iFixedDecimal, String str) {
            for (Rule rule : this.rules) {
                if (rule.getKeyword().equals(str) && rule.appliesTo(iFixedDecimal)) {
                    return true;
                }
            }
            return false;
        }

        public FixedDecimalSamples getDecimalSamples(String str, SampleType sampleType) {
            for (Rule rule : this.rules) {
                if (rule.getKeyword().equals(str)) {
                    return sampleType == SampleType.INTEGER ? rule.integerSamples : rule.decimalSamples;
                }
            }
            return null;
        }
    }

    private boolean addConditional(Set<IFixedDecimal> set, Set<IFixedDecimal> set2, double d) {
        FixedDecimal fixedDecimal = new FixedDecimal(d);
        if (!set.contains(fixedDecimal) && !set2.contains(fixedDecimal)) {
            set2.add(fixedDecimal);
            return true;
        }
        return false;
    }

    public static PluralRules forLocale(ULocale uLocale) {
        return Factory.getDefaultFactory().forLocale(uLocale, PluralType.CARDINAL);
    }

    public static PluralRules forLocale(Locale locale) {
        return forLocale(ULocale.forLocale(locale));
    }

    public static PluralRules forLocale(ULocale uLocale, PluralType pluralType) {
        return Factory.getDefaultFactory().forLocale(uLocale, pluralType);
    }

    public static PluralRules forLocale(Locale locale, PluralType pluralType) {
        return forLocale(ULocale.forLocale(locale), pluralType);
    }

    private static boolean isValidKeyword(String str) {
        return ALLOWED_ID.containsAll(str);
    }

    private PluralRules(RuleList ruleList) {
        this.rules = ruleList;
        this.keywords = Collections.unmodifiableSet(ruleList.getKeywords());
    }

    @Deprecated
    public int hashCode() {
        return this.rules.hashCode();
    }

    public String select(double d) {
        return this.rules.select(new FixedDecimal(d));
    }

    @Deprecated
    public String select(double d, int i, long j) {
        return this.rules.select(new FixedDecimal(d, i, j));
    }

    @Deprecated
    public String select(IFixedDecimal iFixedDecimal) {
        return this.rules.select(iFixedDecimal);
    }

    @Deprecated
    public boolean matches(FixedDecimal fixedDecimal, String str) {
        return this.rules.select(fixedDecimal, str);
    }

    public Set<String> getKeywords() {
        return this.keywords;
    }

    public double getUniqueKeywordValue(String str) {
        Collection<Double> allKeywordValues = getAllKeywordValues(str);
        if (allKeywordValues != null && allKeywordValues.size() == 1) {
            return allKeywordValues.iterator().next().doubleValue();
        }
        return -0.00123456777d;
    }

    public Collection<Double> getAllKeywordValues(String str) {
        return getAllKeywordValues(str, SampleType.INTEGER);
    }

    @Deprecated
    public Collection<Double> getAllKeywordValues(String str, SampleType sampleType) {
        Collection<Double> samples;
        if (isLimited(str, sampleType) && (samples = getSamples(str, sampleType)) != null) {
            return Collections.unmodifiableCollection(samples);
        }
        return null;
    }

    public Collection<Double> getSamples(String str) {
        return getSamples(str, SampleType.INTEGER);
    }

    @Deprecated
    public Collection<Double> getSamples(String str, SampleType sampleType) {
        if (!this.keywords.contains(str)) {
            return null;
        }
        TreeSet treeSet = new TreeSet();
        if (this.rules.hasExplicitBoundingInfo) {
            FixedDecimalSamples decimalSamples = this.rules.getDecimalSamples(str, sampleType);
            return decimalSamples == null ? Collections.unmodifiableSet(treeSet) : Collections.unmodifiableSet(decimalSamples.addSamples(treeSet));
        }
        int i = isLimited(str, sampleType) ? Integer.MAX_VALUE : 20;
        int i2 = 0;
        switch (sampleType) {
            case INTEGER:
                while (i2 < 200 && addSample(str, Integer.valueOf(i2), i, treeSet)) {
                    i2++;
                }
                addSample(str, 1000000, i, treeSet);
                break;
            case DECIMAL:
                while (i2 < 2000 && addSample(str, new FixedDecimal(((double) i2) / 10.0d, 1), i, treeSet)) {
                    i2++;
                }
                addSample(str, new FixedDecimal(1000000.0d, 1), i, treeSet);
                break;
        }
        if (treeSet.size() == 0) {
            return null;
        }
        return Collections.unmodifiableSet(treeSet);
    }

    @Deprecated
    public boolean addSample(String str, Number number, int i, Set<Double> set) {
        if ((number instanceof FixedDecimal ? select((FixedDecimal) number) : select(number.doubleValue())).equals(str)) {
            set.add(Double.valueOf(number.doubleValue()));
            if (i - 1 < 0) {
                return false;
            }
            return true;
        }
        return true;
    }

    @Deprecated
    public FixedDecimalSamples getDecimalSamples(String str, SampleType sampleType) {
        return this.rules.getDecimalSamples(str, sampleType);
    }

    public static ULocale[] getAvailableULocales() {
        return Factory.getDefaultFactory().getAvailableULocales();
    }

    public static ULocale getFunctionalEquivalent(ULocale uLocale, boolean[] zArr) {
        return Factory.getDefaultFactory().getFunctionalEquivalent(uLocale, zArr);
    }

    public String toString() {
        return this.rules.toString();
    }

    public boolean equals(Object obj) {
        return (obj instanceof PluralRules) && equals((PluralRules) obj);
    }

    public boolean equals(PluralRules pluralRules) {
        return pluralRules != null && toString().equals(pluralRules.toString());
    }

    public KeywordStatus getKeywordStatus(String str, int i, Set<Double> set, Output<Double> output) {
        return getKeywordStatus(str, i, set, output, SampleType.INTEGER);
    }

    @Deprecated
    public KeywordStatus getKeywordStatus(String str, int i, Set<Double> set, Output<Double> output, SampleType sampleType) {
        if (output != null) {
            output.value = null;
        }
        if (!this.keywords.contains(str)) {
            return KeywordStatus.INVALID;
        }
        if (!isLimited(str, sampleType)) {
            return KeywordStatus.UNBOUNDED;
        }
        Collection<Double> samples = getSamples(str, sampleType);
        int size = samples.size();
        if (set == null) {
            set = Collections.emptySet();
        }
        if (size > set.size()) {
            if (size == 1) {
                if (output != null) {
                    output.value = samples.iterator().next();
                }
                return KeywordStatus.UNIQUE;
            }
            return KeywordStatus.BOUNDED;
        }
        HashSet hashSet = new HashSet(samples);
        Iterator<Double> it = set.iterator();
        while (it.hasNext()) {
            hashSet.remove(Double.valueOf(it.next().doubleValue() - ((double) i)));
        }
        if (hashSet.size() == 0) {
            return KeywordStatus.SUPPRESSED;
        }
        if (output != null && hashSet.size() == 1) {
            output.value = hashSet.iterator().next();
        }
        return size == 1 ? KeywordStatus.UNIQUE : KeywordStatus.BOUNDED;
    }

    @Deprecated
    public String getRules(String str) {
        return this.rules.getRules(str);
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        throw new NotSerializableException();
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        throw new NotSerializableException();
    }

    private Object writeReplace() throws ObjectStreamException {
        return new PluralRulesSerialProxy(toString());
    }

    @Deprecated
    public int compareTo(PluralRules pluralRules) {
        return toString().compareTo(pluralRules.toString());
    }

    @Deprecated
    public Boolean isLimited(String str) {
        return Boolean.valueOf(this.rules.isLimited(str, SampleType.INTEGER));
    }

    @Deprecated
    public boolean isLimited(String str, SampleType sampleType) {
        return this.rules.isLimited(str, sampleType);
    }

    @Deprecated
    public boolean computeLimited(String str, SampleType sampleType) {
        return this.rules.computeLimited(str, sampleType);
    }
}
