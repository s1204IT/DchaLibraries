package java.util;

import dalvik.bytecode.Opcodes;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.charset.Charset;
import libcore.icu.LocaleData;
import libcore.icu.NativeDecimalFormat;
import libcore.io.IoUtils;

public final class Formatter implements Closeable, Flushable {
    private static final char[] ZEROS = {'0', '0', '0', '0', '0', '0', '0', '0', '0'};
    private static final ThreadLocal<CachedDecimalFormat> cachedDecimalFormat = new ThreadLocal<CachedDecimalFormat>() {
        @Override
        protected CachedDecimalFormat initialValue() {
            return new CachedDecimalFormat();
        }
    };
    private Object arg;
    private boolean closed;
    private FormatToken formatToken;
    private IOException lastIOException;
    private Locale locale;
    private LocaleData localeData;
    private Appendable out;

    public enum BigDecimalLayoutForm {
        SCIENTIFIC,
        DECIMAL_FLOAT
    }

    private static class CachedDecimalFormat {
        public LocaleData currentLocaleData;
        public String currentPattern;
        public NativeDecimalFormat decimalFormat;

        public NativeDecimalFormat update(LocaleData localeData, String pattern) {
            if (this.decimalFormat == null) {
                this.currentPattern = pattern;
                this.currentLocaleData = localeData;
                this.decimalFormat = new NativeDecimalFormat(this.currentPattern, this.currentLocaleData);
            }
            if (!pattern.equals(this.currentPattern)) {
                this.decimalFormat.applyPattern(pattern);
                this.currentPattern = pattern;
            }
            if (localeData != this.currentLocaleData) {
                this.decimalFormat.setDecimalFormatSymbols(localeData);
                this.currentLocaleData = localeData;
            }
            return this.decimalFormat;
        }
    }

    private NativeDecimalFormat getDecimalFormat(String pattern) {
        return cachedDecimalFormat.get().update(this.localeData, pattern);
    }

    public Formatter() {
        this(new StringBuilder(), Locale.getDefault());
    }

    public Formatter(Appendable a) {
        this(a, Locale.getDefault());
    }

    public Formatter(Locale l) {
        this(new StringBuilder(), l);
    }

    public Formatter(Appendable a, Locale l) {
        this.closed = false;
        if (a == null) {
            this.out = new StringBuilder();
        } else {
            this.out = a;
        }
        this.locale = l;
    }

    public Formatter(String fileName) throws FileNotFoundException {
        this(new File(fileName));
    }

    public Formatter(String fileName, String csn) throws UnsupportedEncodingException, FileNotFoundException {
        this(new File(fileName), csn);
    }

    public Formatter(String fileName, String csn, Locale l) throws UnsupportedEncodingException, FileNotFoundException {
        this(new File(fileName), csn, l);
    }

    public Formatter(File file) throws FileNotFoundException {
        this(new FileOutputStream(file));
    }

    public Formatter(File file, String csn) throws UnsupportedEncodingException, FileNotFoundException {
        this(file, csn, Locale.getDefault());
    }

    public Formatter(File file, String csn, Locale l) throws UnsupportedEncodingException, FileNotFoundException {
        this.closed = false;
        FileOutputStream fout = null;
        try {
            FileOutputStream fout2 = new FileOutputStream(file);
            try {
                this.out = new BufferedWriter(new OutputStreamWriter(fout2, csn));
                this.locale = l;
            } catch (UnsupportedEncodingException e) {
                e = e;
                fout = fout2;
                IoUtils.closeQuietly(fout);
                throw e;
            } catch (RuntimeException e2) {
                e = e2;
                fout = fout2;
                IoUtils.closeQuietly(fout);
                throw e;
            }
        } catch (UnsupportedEncodingException e3) {
            e = e3;
        } catch (RuntimeException e4) {
            e = e4;
        }
    }

    public Formatter(OutputStream os) {
        this.closed = false;
        this.out = new BufferedWriter(new OutputStreamWriter(os, Charset.defaultCharset()));
        this.locale = Locale.getDefault();
    }

    public Formatter(OutputStream os, String csn) throws UnsupportedEncodingException {
        this(os, csn, Locale.getDefault());
    }

    public Formatter(OutputStream os, String csn, Locale l) throws UnsupportedEncodingException {
        this.closed = false;
        this.out = new BufferedWriter(new OutputStreamWriter(os, csn));
        this.locale = l;
    }

    public Formatter(PrintStream ps) {
        this.closed = false;
        if (ps == null) {
            throw new NullPointerException("ps == null");
        }
        this.out = ps;
        this.locale = Locale.getDefault();
    }

    private void checkNotClosed() {
        if (this.closed) {
            throw new FormatterClosedException();
        }
    }

    public Locale locale() {
        checkNotClosed();
        return this.locale;
    }

    public Appendable out() {
        checkNotClosed();
        return this.out;
    }

    public String toString() {
        checkNotClosed();
        return this.out.toString();
    }

    @Override
    public void flush() {
        checkNotClosed();
        if (this.out instanceof Flushable) {
            try {
                ((Flushable) this.out).flush();
            } catch (IOException e) {
                this.lastIOException = e;
            }
        }
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            try {
                if (this.out instanceof Closeable) {
                    ((Closeable) this.out).close();
                }
            } catch (IOException e) {
                this.lastIOException = e;
            }
        }
    }

    public IOException ioException() {
        return this.lastIOException;
    }

    public Formatter format(String format, Object... args) {
        return format(this.locale, format, args);
    }

    public Formatter format(Locale l, String format, Object... args) {
        Locale originalLocale = this.locale;
        if (l == null) {
            try {
                l = Locale.US;
            } finally {
                this.locale = originalLocale;
            }
        }
        this.locale = l;
        this.localeData = LocaleData.get(this.locale);
        doFormat(format, args);
        return this;
    }

    private void doFormat(String format, Object... args) {
        Object lastArgument;
        int currentObjectIndex;
        int index;
        checkNotClosed();
        FormatSpecifierParser fsp = new FormatSpecifierParser(format);
        boolean hasLastArgumentSet = false;
        int length = format.length();
        int i = 0;
        Object obj = null;
        int currentObjectIndex2 = 0;
        while (i < length) {
            int plainTextStart = i;
            int nextPercent = format.indexOf(37, i);
            int plainTextEnd = nextPercent == -1 ? length : nextPercent;
            if (plainTextEnd > plainTextStart) {
                outputCharSequence(format, plainTextStart, plainTextEnd);
            }
            i = plainTextEnd;
            if (i < length) {
                FormatToken token = fsp.parseFormatToken(i + 1);
                Object argument = null;
                if (token.requireArgument()) {
                    if (token.getArgIndex() == -1) {
                        currentObjectIndex = currentObjectIndex2 + 1;
                        index = currentObjectIndex2;
                    } else {
                        index = token.getArgIndex();
                        currentObjectIndex = currentObjectIndex2;
                    }
                    argument = getArgument(args, index, fsp, obj, hasLastArgumentSet);
                    lastArgument = argument;
                    hasLastArgumentSet = true;
                } else {
                    lastArgument = obj;
                    currentObjectIndex = currentObjectIndex2;
                }
                CharSequence substitution = transform(token, argument);
                if (substitution != null) {
                    outputCharSequence(substitution, 0, substitution.length());
                }
                i = fsp.i;
            } else {
                lastArgument = obj;
                currentObjectIndex = currentObjectIndex2;
            }
            obj = lastArgument;
            currentObjectIndex2 = currentObjectIndex;
        }
    }

    private void outputCharSequence(CharSequence cs, int start, int end) {
        try {
            this.out.append(cs, start, end);
        } catch (IOException e) {
            this.lastIOException = e;
        }
    }

    private Object getArgument(Object[] args, int index, FormatSpecifierParser fsp, Object lastArgument, boolean hasLastArgumentSet) {
        if (index == -2 && !hasLastArgumentSet) {
            throw new MissingFormatArgumentException("<");
        }
        if (args == null) {
            return null;
        }
        if (index >= args.length) {
            throw new MissingFormatArgumentException(fsp.getFormatSpecifierText());
        }
        if (index != -2) {
            Object lastArgument2 = args[index];
            return lastArgument2;
        }
        return lastArgument;
    }

    private static class FormatToken {
        static final int DEFAULT_PRECISION = 6;
        static final int FLAGS_UNSET = 0;
        static final int FLAG_ZERO = 16;
        static final int LAST_ARGUMENT_INDEX = -2;
        static final int UNSET = -1;
        private int argIndex;
        private char conversionType;
        private char dateSuffix;
        boolean flagComma;
        boolean flagMinus;
        boolean flagParenthesis;
        boolean flagPlus;
        boolean flagSharp;
        boolean flagSpace;
        boolean flagZero;
        private int precision;
        private StringBuilder strFlags;
        private int width;

        private FormatToken() {
            this.argIndex = -1;
            this.conversionType = (char) 65535;
            this.precision = -1;
            this.width = -1;
        }

        boolean isDefault() {
            return (this.flagComma || this.flagMinus || this.flagParenthesis || this.flagPlus || this.flagSharp || this.flagSpace || this.flagZero || this.width != -1 || this.precision != -1) ? false : true;
        }

        boolean isPrecisionSet() {
            return this.precision != -1;
        }

        int getArgIndex() {
            return this.argIndex;
        }

        void setArgIndex(int index) {
            this.argIndex = index;
        }

        int getWidth() {
            return this.width;
        }

        void setWidth(int width) {
            this.width = width;
        }

        int getPrecision() {
            return this.precision;
        }

        void setPrecision(int precise) {
            this.precision = precise;
        }

        String getStrFlags() {
            return this.strFlags != null ? this.strFlags.toString() : "";
        }

        boolean setFlag(int ch) {
            boolean dupe;
            switch (ch) {
                case 32:
                    dupe = this.flagSpace;
                    this.flagSpace = true;
                    break;
                case 35:
                    dupe = this.flagSharp;
                    this.flagSharp = true;
                    break;
                case Opcodes.OP_GOTO:
                    dupe = this.flagParenthesis;
                    this.flagParenthesis = true;
                    break;
                case Opcodes.OP_PACKED_SWITCH:
                    dupe = this.flagPlus;
                    this.flagPlus = true;
                    break;
                case 44:
                    dupe = this.flagComma;
                    this.flagComma = true;
                    break;
                case Opcodes.OP_CMPL_FLOAT:
                    dupe = this.flagMinus;
                    this.flagMinus = true;
                    break;
                case 48:
                    dupe = this.flagZero;
                    this.flagZero = true;
                    break;
                default:
                    return false;
            }
            if (dupe) {
                throw new DuplicateFormatFlagsException(String.valueOf(ch));
            }
            if (this.strFlags == null) {
                this.strFlags = new StringBuilder(7);
            }
            this.strFlags.append((char) ch);
            return true;
        }

        char getConversionType() {
            return this.conversionType;
        }

        void setConversionType(char c) {
            this.conversionType = c;
        }

        char getDateSuffix() {
            return this.dateSuffix;
        }

        void setDateSuffix(char c) {
            this.dateSuffix = c;
        }

        boolean requireArgument() {
            return (this.conversionType == '%' || this.conversionType == 'n') ? false : true;
        }

        void checkFlags(Object arg) {
            boolean allowComma = false;
            boolean allowMinus = true;
            boolean allowParenthesis = false;
            int allowPlus = 0;
            boolean allowSharp = false;
            boolean allowSpace = false;
            boolean allowZero = false;
            boolean allowPrecision = true;
            boolean allowWidth = true;
            boolean allowArgument = true;
            switch (this.conversionType) {
                case Opcodes.OP_FILLED_NEW_ARRAY_RANGE:
                    allowArgument = false;
                    allowPrecision = false;
                    break;
                case 'A':
                case Opcodes.OP_SGET_WIDE:
                    allowZero = true;
                    allowSpace = true;
                    allowSharp = true;
                    allowPlus = 1;
                    break;
                case 'B':
                case Opcodes.OP_AGET_BYTE:
                case Opcodes.OP_SGET_OBJECT:
                case Opcodes.OP_SPUT_WIDE:
                    break;
                case 'C':
                case Opcodes.OP_IGET_OBJECT:
                case Opcodes.OP_SGET_BOOLEAN:
                case Opcodes.OP_INVOKE_VIRTUAL_RANGE:
                    allowPrecision = false;
                    break;
                case Opcodes.OP_AGET_WIDE:
                case Opcodes.OP_SGET_CHAR:
                    allowZero = true;
                    allowSpace = true;
                    allowSharp = true;
                    allowPlus = 1;
                    allowParenthesis = true;
                    break;
                case Opcodes.OP_AGET_BOOLEAN:
                case Opcodes.OP_SPUT:
                    allowZero = true;
                    allowSpace = true;
                    allowPlus = 1;
                    allowParenthesis = true;
                    allowComma = true;
                    break;
                case Opcodes.OP_IGET_WIDE:
                case 's':
                    if (arg instanceof Formattable) {
                        allowSharp = true;
                    }
                    break;
                case Opcodes.OP_IGET_SHORT:
                case Opcodes.OP_INVOKE_SUPER:
                case Opcodes.OP_INVOKE_INTERFACE_RANGE:
                    allowZero = true;
                    allowSharp = true;
                    if (arg == null || (arg instanceof BigInteger)) {
                        allowSpace = true;
                        allowPlus = 1;
                        allowParenthesis = true;
                    }
                    allowPrecision = false;
                    break;
                case Opcodes.OP_SGET_BYTE:
                    allowZero = true;
                    allowSpace = true;
                    allowPlus = 1;
                    allowParenthesis = true;
                    allowComma = true;
                    allowPrecision = false;
                    break;
                case Opcodes.OP_SGET_SHORT:
                    allowZero = true;
                    allowSpace = true;
                    allowSharp = true;
                    allowPlus = 1;
                    allowParenthesis = true;
                    allowComma = true;
                    break;
                case Opcodes.OP_INVOKE_VIRTUAL:
                    allowMinus = false;
                    allowWidth = false;
                    allowPrecision = false;
                    allowArgument = false;
                    break;
                default:
                    throw unknownFormatConversionException();
            }
            String mismatch = null;
            if (!allowComma && this.flagComma) {
                mismatch = ",";
            } else if (!allowMinus && this.flagMinus) {
                mismatch = "-";
            } else if (!allowParenthesis && this.flagParenthesis) {
                mismatch = "(";
            } else if (allowPlus == 0 && this.flagPlus) {
                mismatch = "+";
            } else if (!allowSharp && this.flagSharp) {
                mismatch = "#";
            } else if (!allowSpace && this.flagSpace) {
                mismatch = " ";
            } else if (!allowZero && this.flagZero) {
                mismatch = "0";
            }
            if (mismatch != null) {
                if (this.conversionType == 'n') {
                    throw new IllegalFormatFlagsException(mismatch);
                }
                throw new FormatFlagsConversionMismatchException(mismatch, this.conversionType);
            }
            if ((this.flagMinus || this.flagZero) && this.width == -1) {
                throw new MissingFormatWidthException("-" + this.conversionType);
            }
            if (!allowArgument && this.argIndex != -1) {
                throw new IllegalFormatFlagsException("%" + this.conversionType + " doesn't take an argument");
            }
            if (!allowPrecision && this.precision != -1) {
                throw new IllegalFormatPrecisionException(this.precision);
            }
            if (!allowWidth && this.width != -1) {
                throw new IllegalFormatWidthException(this.width);
            }
            if (this.flagPlus && this.flagSpace) {
                throw new IllegalFormatFlagsException("the '+' and ' ' flags are incompatible");
            }
            if (this.flagMinus && this.flagZero) {
                throw new IllegalFormatFlagsException("the '-' and '0' flags are incompatible");
            }
        }

        public UnknownFormatConversionException unknownFormatConversionException() {
            if (this.conversionType == 't' || this.conversionType == 'T') {
                throw new UnknownFormatConversionException(String.format("%c%c", Character.valueOf(this.conversionType), Character.valueOf(this.dateSuffix)));
            }
            throw new UnknownFormatConversionException(String.valueOf(this.conversionType));
        }
    }

    private CharSequence transform(FormatToken token, Object argument) {
        CharSequence result;
        this.formatToken = token;
        this.arg = argument;
        if (token.isDefault()) {
            switch (token.getConversionType()) {
                case Opcodes.OP_SGET_BYTE:
                    boolean needLocalizedDigits = this.localeData.zeroDigit != '0';
                    if ((this.out instanceof StringBuilder) && !needLocalizedDigits) {
                        if ((this.arg instanceof Integer) || (this.arg instanceof Short) || (this.arg instanceof Byte)) {
                            IntegralToString.appendInt((StringBuilder) this.out, ((Number) this.arg).intValue());
                            return null;
                        }
                        if (this.arg instanceof Long) {
                            IntegralToString.appendLong((StringBuilder) this.out, ((Long) this.arg).longValue());
                            return null;
                        }
                    }
                    if ((this.arg instanceof Integer) || (this.arg instanceof Long) || (this.arg instanceof Short) || (this.arg instanceof Byte)) {
                        String result2 = this.arg.toString();
                        return needLocalizedDigits ? localizeDigits(result2) : result2;
                    }
                    break;
                case 's':
                    if (this.arg == null) {
                        return "null";
                    }
                    if (!(this.arg instanceof Formattable)) {
                        return this.arg.toString();
                    }
                    break;
            }
        }
        this.formatToken.checkFlags(this.arg);
        switch (token.getConversionType()) {
            case Opcodes.OP_FILLED_NEW_ARRAY_RANGE:
                result = transformFromPercent();
                break;
            case 'A':
            case Opcodes.OP_AGET_WIDE:
            case Opcodes.OP_AGET_BOOLEAN:
            case Opcodes.OP_SGET_WIDE:
            case Opcodes.OP_SGET_CHAR:
            case Opcodes.OP_SGET_SHORT:
            case Opcodes.OP_SPUT:
                result = transformFromFloat();
                break;
            case 'B':
            case Opcodes.OP_SGET_OBJECT:
                result = transformFromBoolean();
                break;
            case 'C':
            case Opcodes.OP_SGET_BOOLEAN:
                result = transformFromCharacter();
                break;
            case Opcodes.OP_AGET_BYTE:
            case Opcodes.OP_SPUT_WIDE:
                result = transformFromHashCode();
                break;
            case Opcodes.OP_IGET_WIDE:
            case 's':
                result = transformFromString();
                break;
            case Opcodes.OP_IGET_OBJECT:
            case Opcodes.OP_INVOKE_VIRTUAL_RANGE:
                result = transformFromDateTime();
                break;
            case Opcodes.OP_IGET_SHORT:
            case Opcodes.OP_SGET_BYTE:
            case Opcodes.OP_INVOKE_SUPER:
            case Opcodes.OP_INVOKE_INTERFACE_RANGE:
                if (this.arg == null || (this.arg instanceof BigInteger)) {
                    result = transformFromBigInteger();
                } else {
                    result = transformFromInteger();
                }
                break;
            case Opcodes.OP_INVOKE_VIRTUAL:
                result = System.lineSeparator();
                break;
            default:
                throw token.unknownFormatConversionException();
        }
        if (Character.isUpperCase(token.getConversionType()) && result != null) {
            return result.toString().toUpperCase(this.locale);
        }
        return result;
    }

    private IllegalFormatConversionException badArgumentType() {
        throw new IllegalFormatConversionException(this.formatToken.getConversionType(), this.arg.getClass());
    }

    private CharSequence localizeDigits(CharSequence s) {
        int length = s.length();
        int offsetToLocalizedDigits = this.localeData.zeroDigit - '0';
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9') {
                ch = (char) (ch + offsetToLocalizedDigits);
            }
            result.append(ch);
        }
        return result;
    }

    private CharSequence insertGrouping(CharSequence s) {
        StringBuilder result = new StringBuilder(s.length() + (s.length() / 3));
        int digitsLength = s.length();
        int i = 0;
        if (s.charAt(0) == '-') {
            digitsLength--;
            i = 0 + 1;
            result.append('-');
        }
        int headLength = digitsLength % 3;
        if (headLength == 0) {
            headLength = 3;
        }
        result.append(s, i, i + headLength);
        for (int i2 = i + headLength; i2 < s.length(); i2 += 3) {
            result.append(this.localeData.groupingSeparator);
            result.append(s, i2, i2 + 3);
        }
        return result;
    }

    private CharSequence transformFromBoolean() {
        CharSequence result;
        if (this.arg instanceof Boolean) {
            result = this.arg.toString();
        } else if (this.arg == null) {
            result = "false";
        } else {
            result = "true";
        }
        return padding(result, 0);
    }

    private CharSequence transformFromHashCode() {
        CharSequence result;
        if (this.arg == null) {
            result = "null";
        } else {
            result = Integer.toHexString(this.arg.hashCode());
        }
        return padding(result, 0);
    }

    private CharSequence transformFromString() {
        if (this.arg instanceof Formattable) {
            int flags = 0;
            if (this.formatToken.flagMinus) {
                flags = 0 | 1;
            }
            if (this.formatToken.flagSharp) {
                flags |= 4;
            }
            if (Character.isUpperCase(this.formatToken.getConversionType())) {
                flags |= 2;
            }
            ((Formattable) this.arg).formatTo(this, flags, this.formatToken.getWidth(), this.formatToken.getPrecision());
            return null;
        }
        String result = this.arg != null ? this.arg.toString() : "null";
        return padding(result, 0);
    }

    private CharSequence transformFromCharacter() {
        if (this.arg == null) {
            return padding("null", 0);
        }
        if (this.arg instanceof Character) {
            return padding(String.valueOf(this.arg), 0);
        }
        if ((this.arg instanceof Byte) || (this.arg instanceof Short) || (this.arg instanceof Integer)) {
            int codePoint = ((Number) this.arg).intValue();
            if (!Character.isValidCodePoint(codePoint)) {
                throw new IllegalFormatCodePointException(codePoint);
            }
            CharSequence result = codePoint < 65536 ? String.valueOf((char) codePoint) : String.valueOf(Character.toChars(codePoint));
            return padding(result, 0);
        }
        throw badArgumentType();
    }

    private CharSequence transformFromPercent() {
        return padding("%", 0);
    }

    private CharSequence padding(CharSequence source, int startIndex) {
        int start = startIndex;
        int width = this.formatToken.getWidth();
        int precision = this.formatToken.getPrecision();
        int length = source.length();
        if (precision >= 0) {
            length = Math.min(length, precision);
            if (source instanceof StringBuilder) {
                ((StringBuilder) source).setLength(length);
            } else {
                source = source.subSequence(0, length);
            }
        }
        if (width > 0) {
            width = Math.max(source.length(), width);
        }
        if (length < width) {
            char paddingChar = ' ';
            if (this.formatToken.flagZero) {
                if (this.formatToken.getConversionType() == 'd') {
                    paddingChar = this.localeData.zeroDigit;
                } else {
                    paddingChar = '0';
                }
            } else {
                start = 0;
            }
            char[] paddingChars = new char[width - length];
            Arrays.fill(paddingChars, paddingChar);
            boolean paddingRight = this.formatToken.flagMinus;
            StringBuilder result = toStringBuilder(source);
            if (paddingRight) {
                result.append(paddingChars);
            } else {
                result.insert(start, paddingChars);
            }
            return result;
        }
        return source;
    }

    private StringBuilder toStringBuilder(CharSequence cs) {
        return cs instanceof StringBuilder ? (StringBuilder) cs : new StringBuilder(cs);
    }

    private StringBuilder wrapParentheses(StringBuilder result) {
        result.setCharAt(0, '(');
        if (this.formatToken.flagZero) {
            this.formatToken.setWidth(this.formatToken.getWidth() - 1);
            StringBuilder result2 = (StringBuilder) padding(result, 1);
            result2.append(')');
            return result2;
        }
        result.append(')');
        return (StringBuilder) padding(result, 0);
    }

    private CharSequence transformFromInteger() {
        long value;
        int startIndex = 0;
        StringBuilder result = new StringBuilder();
        char currentConversionType = this.formatToken.getConversionType();
        if (this.arg instanceof Long) {
            value = ((Long) this.arg).longValue();
        } else if (this.arg instanceof Integer) {
            value = ((Integer) this.arg).longValue();
        } else if (this.arg instanceof Short) {
            value = ((Short) this.arg).longValue();
        } else if (this.arg instanceof Byte) {
            value = ((Byte) this.arg).longValue();
        } else {
            throw badArgumentType();
        }
        if (this.formatToken.flagSharp) {
            if (currentConversionType == 'o') {
                result.append("0");
                startIndex = 0 + 1;
            } else {
                result.append("0x");
                startIndex = 0 + 2;
            }
        }
        if (currentConversionType == 'd') {
            CharSequence digits = Long.toString(value);
            if (this.formatToken.flagComma) {
                digits = insertGrouping(digits);
            }
            if (this.localeData.zeroDigit != '0') {
                digits = localizeDigits(digits);
            }
            result.append(digits);
            if (value < 0) {
                if (this.formatToken.flagParenthesis) {
                    return wrapParentheses(result);
                }
                if (this.formatToken.flagZero) {
                    startIndex++;
                }
            } else if (this.formatToken.flagPlus) {
                result.insert(0, '+');
                startIndex++;
            } else if (this.formatToken.flagSpace) {
                result.insert(0, ' ');
                startIndex++;
            }
        } else {
            if (this.arg instanceof Byte) {
                value &= 255;
            } else if (this.arg instanceof Short) {
                value &= 65535;
            } else if (this.arg instanceof Integer) {
                value &= 4294967295L;
            }
            if (currentConversionType == 'o') {
                result.append(Long.toOctalString(value));
            } else {
                result.append(Long.toHexString(value));
            }
        }
        return padding(result, startIndex);
    }

    private CharSequence transformFromNull() {
        this.formatToken.flagZero = false;
        return padding("null", 0);
    }

    private CharSequence transformFromBigInteger() {
        int startIndex = 0;
        StringBuilder result = new StringBuilder();
        BigInteger bigInt = (BigInteger) this.arg;
        char currentConversionType = this.formatToken.getConversionType();
        if (bigInt == null) {
            return transformFromNull();
        }
        boolean isNegative = bigInt.compareTo(BigInteger.ZERO) < 0;
        if (currentConversionType == 'd') {
            CharSequence digits = bigInt.toString(10);
            if (this.formatToken.flagComma) {
                digits = insertGrouping(digits);
            }
            result.append(digits);
        } else if (currentConversionType == 'o') {
            result.append(bigInt.toString(8));
        } else {
            result.append(bigInt.toString(16));
        }
        if (this.formatToken.flagSharp) {
            startIndex = isNegative ? 1 : 0;
            if (currentConversionType == 'o') {
                result.insert(startIndex, "0");
                startIndex++;
            } else if (currentConversionType == 'x' || currentConversionType == 'X') {
                result.insert(startIndex, "0x");
                startIndex += 2;
            }
        }
        if (!isNegative) {
            if (this.formatToken.flagPlus) {
                result.insert(0, '+');
                startIndex++;
            }
            if (this.formatToken.flagSpace) {
                result.insert(0, ' ');
                startIndex++;
            }
        }
        if (isNegative && this.formatToken.flagParenthesis) {
            return wrapParentheses(result);
        }
        if (isNegative && this.formatToken.flagZero) {
            startIndex++;
        }
        return padding(result, startIndex);
    }

    private CharSequence transformFromDateTime() {
        Date date;
        Calendar calendar;
        if (this.arg == null) {
            return transformFromNull();
        }
        if (this.arg instanceof Calendar) {
            calendar = (Calendar) this.arg;
        } else {
            if (this.arg instanceof Long) {
                date = new Date(((Long) this.arg).longValue());
            } else if (this.arg instanceof Date) {
                date = (Date) this.arg;
            } else {
                throw badArgumentType();
            }
            calendar = Calendar.getInstance(this.locale);
            calendar.setTime(date);
        }
        StringBuilder result = new StringBuilder();
        if (!appendT(result, this.formatToken.getDateSuffix(), calendar)) {
            throw this.formatToken.unknownFormatConversionException();
        }
        return padding(result, 0);
    }

    private boolean appendT(StringBuilder result, char conversion, Calendar calendar) {
        switch (conversion) {
            case 'A':
                result.append(this.localeData.longWeekdayNames[calendar.get(7)]);
                return true;
            case 'B':
                result.append(this.localeData.longMonthNames[calendar.get(2)]);
                return true;
            case 'C':
                appendLocalized(result, calendar.get(1) / 100, 2);
                return true;
            case Opcodes.OP_AGET:
                appendT(result, 'm', calendar);
                result.append('/');
                appendT(result, 'd', calendar);
                result.append('/');
                appendT(result, 'y', calendar);
                return true;
            case Opcodes.OP_AGET_WIDE:
            case Opcodes.OP_AGET_BOOLEAN:
            case Opcodes.OP_AGET_SHORT:
            case Opcodes.OP_APUT:
            case Opcodes.OP_APUT_BYTE:
            case 'P':
            case Opcodes.OP_IGET_BOOLEAN:
            case Opcodes.OP_IGET_BYTE:
            case Opcodes.OP_IGET_CHAR:
            case Opcodes.OP_IGET_SHORT:
            case '[':
            case '\\':
            case ']':
            case Opcodes.OP_IPUT_CHAR:
            case Opcodes.OP_IPUT_SHORT:
            case Opcodes.OP_SGET:
            case Opcodes.OP_SGET_SHORT:
            case Opcodes.OP_SPUT:
            case Opcodes.OP_SPUT_OBJECT:
            case Opcodes.OP_INVOKE_VIRTUAL:
            case Opcodes.OP_INVOKE_SUPER:
            case Opcodes.OP_INVOKE_STATIC:
            case Opcodes.OP_INVOKE_VIRTUAL_RANGE:
            case Opcodes.OP_INVOKE_SUPER_RANGE:
            case Opcodes.OP_INVOKE_DIRECT_RANGE:
            case Opcodes.OP_INVOKE_STATIC_RANGE:
            case Opcodes.OP_INVOKE_INTERFACE_RANGE:
            default:
                return false;
            case 'F':
                appendT(result, 'Y', calendar);
                result.append('-');
                appendT(result, 'm', calendar);
                result.append('-');
                appendT(result, 'd', calendar);
                return true;
            case Opcodes.OP_AGET_BYTE:
                appendLocalized(result, calendar.get(11), 2);
                return true;
            case Opcodes.OP_AGET_CHAR:
                appendLocalized(result, to12Hour(calendar.get(10)), 2);
                return true;
            case Opcodes.OP_APUT_WIDE:
                appendLocalized(result, calendar.get(14), 3);
                return true;
            case Opcodes.OP_APUT_OBJECT:
                appendLocalized(result, calendar.get(12), 2);
                return true;
            case Opcodes.OP_APUT_BOOLEAN:
                appendLocalized(result, ((long) calendar.get(14)) * 1000000, 9);
                return true;
            case 'Q':
                appendLocalized(result, calendar.getTimeInMillis(), 0);
                return true;
            case 'R':
                appendT(result, 'H', calendar);
                result.append(':');
                appendT(result, 'M', calendar);
                return true;
            case Opcodes.OP_IGET_WIDE:
                appendLocalized(result, calendar.get(13), 2);
                return true;
            case Opcodes.OP_IGET_OBJECT:
                appendT(result, 'H', calendar);
                result.append(':');
                appendT(result, 'M', calendar);
                result.append(':');
                appendT(result, 'S', calendar);
                return true;
            case Opcodes.OP_IPUT:
                appendLocalized(result, calendar.get(1), 4);
                return true;
            case Opcodes.OP_IPUT_WIDE:
                TimeZone timeZone = calendar.getTimeZone();
                result.append(timeZone.getDisplayName(timeZone.inDaylightTime(calendar.getTime()), 0, this.locale));
                return true;
            case Opcodes.OP_SGET_WIDE:
                result.append(this.localeData.shortWeekdayNames[calendar.get(7)]);
                return true;
            case Opcodes.OP_SGET_OBJECT:
            case Opcodes.OP_SPUT_WIDE:
                result.append(this.localeData.shortMonthNames[calendar.get(2)]);
                return true;
            case Opcodes.OP_SGET_BOOLEAN:
                appendT(result, 'a', calendar);
                result.append(' ');
                appendT(result, 'b', calendar);
                result.append(' ');
                appendT(result, 'd', calendar);
                result.append(' ');
                appendT(result, 'T', calendar);
                result.append(' ');
                appendT(result, 'Z', calendar);
                result.append(' ');
                appendT(result, 'Y', calendar);
                return true;
            case Opcodes.OP_SGET_BYTE:
                appendLocalized(result, calendar.get(5), 2);
                return true;
            case Opcodes.OP_SGET_CHAR:
                appendLocalized(result, calendar.get(5), 0);
                return true;
            case Opcodes.OP_SPUT_BOOLEAN:
                appendLocalized(result, calendar.get(6), 3);
                return true;
            case Opcodes.OP_SPUT_BYTE:
                appendLocalized(result, calendar.get(11), 0);
                return true;
            case Opcodes.OP_SPUT_CHAR:
                appendLocalized(result, to12Hour(calendar.get(10)), 0);
                return true;
            case Opcodes.OP_SPUT_SHORT:
                appendLocalized(result, calendar.get(2) + 1, 2);
                return true;
            case 'p':
                result.append(this.localeData.amPm[calendar.get(9)].toLowerCase(this.locale));
                return true;
            case Opcodes.OP_INVOKE_INTERFACE:
                appendT(result, 'I', calendar);
                result.append(':');
                appendT(result, 'M', calendar);
                result.append(':');
                appendT(result, 'S', calendar);
                result.append(' ');
                result.append(this.localeData.amPm[calendar.get(9)]);
                return true;
            case 's':
                appendLocalized(result, calendar.getTimeInMillis() / 1000, 0);
                return true;
            case 'y':
                appendLocalized(result, calendar.get(1) % 100, 2);
                return true;
            case 'z':
                long offset = calendar.get(15) + calendar.get(16);
                char sign = '+';
                if (offset < 0) {
                    sign = '-';
                    offset = -offset;
                }
                result.append(sign);
                appendLocalized(result, offset / 3600000, 2);
                appendLocalized(result, (offset % 3600000) / 60000, 2);
                return true;
        }
    }

    private int to12Hour(int hour) {
        if (hour == 0) {
            return 12;
        }
        return hour;
    }

    private void appendLocalized(StringBuilder result, long value, int width) {
        int paddingIndex = result.length();
        char zeroDigit = this.localeData.zeroDigit;
        if (zeroDigit == '0') {
            result.append(value);
        } else {
            result.append(localizeDigits(Long.toString(value)));
        }
        int zeroCount = width - (result.length() - paddingIndex);
        if (zeroCount > 0) {
            if (zeroDigit == '0') {
                result.insert(paddingIndex, ZEROS, 0, zeroCount);
                return;
            }
            for (int i = 0; i < zeroCount; i++) {
                result.insert(paddingIndex, zeroDigit);
            }
        }
    }

    private CharSequence transformFromSpecialNumber(double d) {
        String source;
        if (Double.isNaN(d)) {
            source = "NaN";
        } else if (d == Double.POSITIVE_INFINITY) {
            if (this.formatToken.flagPlus) {
                source = "+Infinity";
            } else if (this.formatToken.flagSpace) {
                source = " Infinity";
            } else {
                source = "Infinity";
            }
        } else if (d == Double.NEGATIVE_INFINITY) {
            if (this.formatToken.flagParenthesis) {
                source = "(Infinity)";
            } else {
                source = "-Infinity";
            }
        } else {
            return null;
        }
        this.formatToken.setPrecision(-1);
        this.formatToken.flagZero = false;
        return padding(source, 0);
    }

    private CharSequence transformFromFloat() {
        if (this.arg == null) {
            return transformFromNull();
        }
        if ((this.arg instanceof Float) || (this.arg instanceof Double)) {
            Number number = (Number) this.arg;
            double d = number.doubleValue();
            if (d != d || d == Double.POSITIVE_INFINITY || d == Double.NEGATIVE_INFINITY) {
                return transformFromSpecialNumber(d);
            }
        } else if (!(this.arg instanceof BigDecimal)) {
            throw badArgumentType();
        }
        char conversionType = this.formatToken.getConversionType();
        if (conversionType != 'a' && conversionType != 'A' && !this.formatToken.isPrecisionSet()) {
            this.formatToken.setPrecision(6);
        }
        StringBuilder result = new StringBuilder();
        switch (conversionType) {
            case 'A':
            case Opcodes.OP_SGET_WIDE:
                transformA(result);
                break;
            case Opcodes.OP_AGET_WIDE:
            case Opcodes.OP_SGET_CHAR:
                transformE(result);
                break;
            case Opcodes.OP_AGET_BOOLEAN:
            case Opcodes.OP_SPUT:
                transformG(result);
                break;
            case Opcodes.OP_SGET_SHORT:
                transformF(result);
                break;
            default:
                throw this.formatToken.unknownFormatConversionException();
        }
        this.formatToken.setPrecision(-1);
        int startIndex = 0;
        if (startsWithMinusSign(result, this.localeData.minusSign)) {
            if (this.formatToken.flagParenthesis) {
                return wrapParentheses(result);
            }
        } else {
            if (this.formatToken.flagSpace) {
                result.insert(0, ' ');
                startIndex = 0 + 1;
            }
            if (this.formatToken.flagPlus) {
                result.insert(0, '+');
                startIndex++;
            }
        }
        char firstChar = result.charAt(0);
        if (this.formatToken.flagZero && (firstChar == '+' || startsWithMinusSign(result, this.localeData.minusSign))) {
            startIndex = this.localeData.minusSign.length();
        }
        if (conversionType == 'a' || conversionType == 'A') {
            startIndex += 2;
        }
        return padding(result, startIndex);
    }

    private static boolean startsWithMinusSign(CharSequence cs, String minusSign) {
        if (cs.length() < minusSign.length()) {
            return false;
        }
        for (int i = 0; i < minusSign.length(); i++) {
            if (minusSign.charAt(i) != cs.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private void transformE(StringBuilder result) {
        char[] chars;
        int precision = this.formatToken.getPrecision();
        String pattern = "0E+00";
        if (precision > 0) {
            StringBuilder sb = new StringBuilder("0.");
            char[] zeros = new char[precision];
            Arrays.fill(zeros, '0');
            sb.append(zeros);
            sb.append("E+00");
            pattern = sb.toString();
        }
        NativeDecimalFormat nf = getDecimalFormat(pattern);
        if (this.arg instanceof BigDecimal) {
            chars = nf.formatBigDecimal((BigDecimal) this.arg, null);
        } else {
            chars = nf.formatDouble(((Number) this.arg).doubleValue(), null);
        }
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == 'E') {
                chars[i] = 'e';
            }
        }
        result.append(chars);
        if (this.formatToken.flagSharp && precision == 0) {
            int indexOfE = result.indexOf("e");
            result.insert(indexOfE, this.localeData.decimalSeparator);
        }
    }

    private void transformG(StringBuilder result) {
        int precision = this.formatToken.getPrecision();
        if (precision == 0) {
            precision = 1;
        }
        this.formatToken.setPrecision(precision);
        double d = ((Number) this.arg).doubleValue();
        if (d == 0.0d) {
            this.formatToken.setPrecision(precision - 1);
            transformF(result);
            return;
        }
        boolean requireScientificRepresentation = true;
        double d2 = Math.abs(d);
        if (Double.isInfinite(d2)) {
            this.formatToken.setPrecision(this.formatToken.getPrecision() - 1);
            transformE(result);
            return;
        }
        BigDecimal b = new BigDecimal(d2, new MathContext(precision));
        double d3 = b.doubleValue();
        long l = b.longValue();
        if (d3 < 1.0d || d3 >= Math.pow(10.0d, precision)) {
            long l2 = b.movePointRight(4).longValue();
            if (d3 >= Math.pow(10.0d, -4.0d) && d3 < 1.0d) {
                requireScientificRepresentation = false;
                int precision2 = precision + (4 - String.valueOf(l2).length());
                if (String.valueOf(b.movePointRight(precision2 + 1).longValue()).length() <= this.formatToken.getPrecision()) {
                    precision2++;
                }
                if (b.movePointRight(precision2).longValue() >= Math.pow(10.0d, precision2 - 4)) {
                    this.formatToken.setPrecision(precision2);
                }
            }
        } else if (l < Math.pow(10.0d, precision)) {
            requireScientificRepresentation = false;
            int precision3 = precision - String.valueOf(l).length();
            if (precision3 < 0) {
                precision3 = 0;
            }
            if (String.valueOf(Math.round(Math.pow(10.0d, precision3 + 1) * d3)).length() <= this.formatToken.getPrecision()) {
                precision3++;
            }
            this.formatToken.setPrecision(precision3);
        }
        if (requireScientificRepresentation) {
            this.formatToken.setPrecision(this.formatToken.getPrecision() - 1);
            transformE(result);
            return;
        }
        transformF(result);
    }

    private void transformF(StringBuilder result) {
        String pattern = "0.000000";
        int precision = this.formatToken.getPrecision();
        if (this.formatToken.flagComma || precision != 6) {
            StringBuilder patternBuilder = new StringBuilder();
            if (this.formatToken.flagComma) {
                patternBuilder.append(',');
                char[] sharps = new char[2];
                Arrays.fill(sharps, '#');
                patternBuilder.append(sharps);
            }
            patternBuilder.append('0');
            if (precision > 0) {
                patternBuilder.append('.');
                for (int i = 0; i < precision; i++) {
                    patternBuilder.append('0');
                }
            }
            pattern = patternBuilder.toString();
        }
        NativeDecimalFormat nf = getDecimalFormat(pattern);
        if (this.arg instanceof BigDecimal) {
            result.append(nf.formatBigDecimal((BigDecimal) this.arg, null));
        } else {
            result.append(nf.formatDouble(((Number) this.arg).doubleValue(), null));
        }
        if (this.formatToken.flagSharp && precision == 0) {
            result.append(this.localeData.decimalSeparator);
        }
    }

    private void transformA(StringBuilder result) {
        if (this.arg instanceof Float) {
            result.append(Float.toHexString(((Float) this.arg).floatValue()));
        } else if (this.arg instanceof Double) {
            result.append(Double.toHexString(((Double) this.arg).doubleValue()));
        } else {
            throw badArgumentType();
        }
        if (this.formatToken.isPrecisionSet()) {
            int precision = this.formatToken.getPrecision();
            if (precision == 0) {
                precision = 1;
            }
            int indexOfFirstFractionalDigit = result.indexOf(".") + 1;
            int indexOfP = result.indexOf("p");
            int fractionalLength = indexOfP - indexOfFirstFractionalDigit;
            if (fractionalLength != precision) {
                if (fractionalLength < precision) {
                    char[] zeros = new char[precision - fractionalLength];
                    Arrays.fill(zeros, '0');
                    result.insert(indexOfP, zeros);
                    return;
                }
                result.delete(indexOfFirstFractionalDigit + precision, indexOfP);
            }
        }
    }

    private static class FormatSpecifierParser {
        private String format;
        private int i;
        private int length;
        private int startIndex;

        FormatSpecifierParser(String format) {
            this.format = format;
            this.length = format.length();
        }

        FormatToken parseFormatToken(int offset) {
            this.startIndex = offset;
            this.i = offset;
            return parseArgumentIndexAndFlags(new FormatToken());
        }

        String getFormatSpecifierText() {
            return this.format.substring(this.startIndex, this.i);
        }

        private int peek() {
            if (this.i < this.length) {
                return this.format.charAt(this.i);
            }
            return -1;
        }

        private char advance() {
            if (this.i >= this.length) {
                throw unknownFormatConversionException();
            }
            String str = this.format;
            int i = this.i;
            this.i = i + 1;
            return str.charAt(i);
        }

        private UnknownFormatConversionException unknownFormatConversionException() {
            throw new UnknownFormatConversionException(getFormatSpecifierText());
        }

        private FormatToken parseArgumentIndexAndFlags(FormatToken token) {
            int position = this.i;
            int ch = peek();
            if (Character.isDigit(ch)) {
                int number = nextInt();
                if (peek() == 36) {
                    advance();
                    if (number == -1) {
                        throw new MissingFormatArgumentException(getFormatSpecifierText());
                    }
                    token.setArgIndex(Math.max(0, number - 1));
                } else if (ch == 48) {
                    this.i = position;
                } else {
                    return parseWidth(token, number);
                }
            } else if (ch == 60) {
                token.setArgIndex(-2);
                advance();
            }
            while (token.setFlag(peek())) {
                advance();
            }
            int ch2 = peek();
            if (Character.isDigit(ch2)) {
                return parseWidth(token, nextInt());
            }
            if (ch2 == 46) {
                return parsePrecision(token);
            }
            return parseConversionType(token);
        }

        private FormatToken parseWidth(FormatToken token, int width) {
            token.setWidth(width);
            int ch = peek();
            return ch == 46 ? parsePrecision(token) : parseConversionType(token);
        }

        private FormatToken parsePrecision(FormatToken token) {
            advance();
            int ch = peek();
            if (Character.isDigit(ch)) {
                token.setPrecision(nextInt());
                return parseConversionType(token);
            }
            throw unknownFormatConversionException();
        }

        private FormatToken parseConversionType(FormatToken token) {
            char conversionType = advance();
            token.setConversionType(conversionType);
            if (conversionType == 't' || conversionType == 'T') {
                char dateSuffix = advance();
                token.setDateSuffix(dateSuffix);
            }
            return token;
        }

        private int nextInt() {
            long value = 0;
            while (this.i < this.length && Character.isDigit(this.format.charAt(this.i))) {
                String str = this.format;
                this.i = this.i + 1;
                value = (10 * value) + ((long) (str.charAt(r5) - '0'));
                if (value > 2147483647L) {
                    return failNextInt();
                }
            }
            return (int) value;
        }

        private int failNextInt() {
            while (Character.isDigit(peek())) {
                advance();
            }
            return -1;
        }
    }
}
