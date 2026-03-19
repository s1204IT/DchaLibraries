package java.util;

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
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.sql.Types;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Locale;
import libcore.icu.LocaleData;
import sun.misc.FormattedFloatingDecimal;

public final class Formatter implements Closeable, Flushable {
    private static final int MAX_FD_CHARS = 30;
    private static double scaleUp;
    private Appendable a;
    private final Locale l;
    private IOException lastException;
    private final char zero;

    public enum BigDecimalLayoutForm {
        SCIENTIFIC,
        DECIMAL_FLOAT
    }

    private interface FormatString {
        int index();

        void print(Object obj, Locale locale) throws IOException;

        String toString();
    }

    private static Charset toCharset(String str) throws UnsupportedEncodingException {
        Objects.requireNonNull(str, "charsetName");
        try {
            return Charset.forName(str);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            throw new UnsupportedEncodingException(str);
        }
    }

    private static final Appendable nonNullAppendable(Appendable appendable) {
        if (appendable == null) {
            return new StringBuilder();
        }
        return appendable;
    }

    private Formatter(Locale locale, Appendable appendable) {
        this.a = appendable;
        this.l = locale;
        this.zero = getZero(locale);
    }

    private Formatter(Charset charset, Locale locale, File file) throws FileNotFoundException {
        this(locale, new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset)));
    }

    public Formatter() {
        this(Locale.getDefault(Locale.Category.FORMAT), new StringBuilder());
    }

    public Formatter(Appendable appendable) {
        this(Locale.getDefault(Locale.Category.FORMAT), nonNullAppendable(appendable));
    }

    public Formatter(Locale locale) {
        this(locale, new StringBuilder());
    }

    public Formatter(Appendable appendable, Locale locale) {
        this(locale, nonNullAppendable(appendable));
    }

    public Formatter(String str) throws FileNotFoundException {
        this(Locale.getDefault(Locale.Category.FORMAT), new BufferedWriter(new OutputStreamWriter(new FileOutputStream(str))));
    }

    public Formatter(String str, String str2) throws UnsupportedEncodingException, FileNotFoundException {
        this(str, str2, Locale.getDefault(Locale.Category.FORMAT));
    }

    public Formatter(String str, String str2, Locale locale) throws UnsupportedEncodingException, FileNotFoundException {
        this(toCharset(str2), locale, new File(str));
    }

    public Formatter(File file) throws FileNotFoundException {
        this(Locale.getDefault(Locale.Category.FORMAT), new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))));
    }

    public Formatter(File file, String str) throws UnsupportedEncodingException, FileNotFoundException {
        this(file, str, Locale.getDefault(Locale.Category.FORMAT));
    }

    public Formatter(File file, String str, Locale locale) throws UnsupportedEncodingException, FileNotFoundException {
        this(toCharset(str), locale, file);
    }

    public Formatter(PrintStream printStream) {
        this(Locale.getDefault(Locale.Category.FORMAT), (Appendable) Objects.requireNonNull(printStream));
    }

    public Formatter(OutputStream outputStream) {
        this(Locale.getDefault(Locale.Category.FORMAT), new BufferedWriter(new OutputStreamWriter(outputStream)));
    }

    public Formatter(OutputStream outputStream, String str) throws UnsupportedEncodingException {
        this(outputStream, str, Locale.getDefault(Locale.Category.FORMAT));
    }

    public Formatter(OutputStream outputStream, String str, Locale locale) throws UnsupportedEncodingException {
        this(locale, new BufferedWriter(new OutputStreamWriter(outputStream, str)));
    }

    private static char getZero(Locale locale) {
        if (locale != null && !locale.equals(Locale.US)) {
            return DecimalFormatSymbols.getInstance(locale).getZeroDigit();
        }
        return '0';
    }

    public Locale locale() {
        ensureOpen();
        return this.l;
    }

    public Appendable out() {
        ensureOpen();
        return this.a;
    }

    public String toString() {
        ensureOpen();
        return this.a.toString();
    }

    @Override
    public void flush() {
        ensureOpen();
        if (this.a instanceof Flushable) {
            try {
                ((Flushable) this.a).flush();
            } catch (IOException e) {
                this.lastException = e;
            }
        }
    }

    @Override
    public void close() {
        if (this.a == null) {
            return;
        }
        try {
            try {
                if (this.a instanceof Closeable) {
                    ((Closeable) this.a).close();
                }
            } catch (IOException e) {
                this.lastException = e;
            }
        } finally {
            this.a = null;
        }
    }

    private void ensureOpen() {
        if (this.a == null) {
            throw new FormatterClosedException();
        }
    }

    public IOException ioException() {
        return this.lastException;
    }

    public Formatter format(String str, Object... objArr) {
        return format(this.l, str, objArr);
    }

    public Formatter format(Locale locale, String str, Object... objArr) {
        IOException e;
        ensureOpen();
        int i = -1;
        int i2 = -1;
        for (FormatString formatString : parse(str)) {
            int iIndex = formatString.index();
            Object obj = null;
            switch (iIndex) {
                case -2:
                    formatString.print(null, locale);
                    continue;
                    break;
                case -1:
                    if (i < 0 || (objArr != null && i > objArr.length - 1)) {
                        throw new MissingFormatArgumentException(formatString.toString());
                    }
                    if (objArr != null) {
                        obj = objArr[i];
                    }
                    formatString.print(obj, locale);
                    continue;
                    break;
                    break;
                case 0:
                    i = i2 + 1;
                    if (objArr != null) {
                        try {
                            if (i > objArr.length - 1) {
                                throw new MissingFormatArgumentException(formatString.toString());
                            }
                        } catch (IOException e2) {
                            e = e2;
                            i2 = i;
                            this.lastException = e;
                        }
                    }
                    if (objArr != null) {
                        obj = objArr[i];
                    }
                    formatString.print(obj, locale);
                    i2 = i;
                    continue;
                    break;
                default:
                    i = iIndex - 1;
                    if (objArr != null) {
                        try {
                            if (i > objArr.length - 1) {
                                throw new MissingFormatArgumentException(formatString.toString());
                            }
                        } catch (IOException e3) {
                            e = e3;
                            this.lastException = e;
                        }
                    }
                    if (objArr != null) {
                        obj = objArr[i];
                    }
                    formatString.print(obj, locale);
                    continue;
                    break;
            }
            this.lastException = e;
        }
        return this;
    }

    private FormatString[] parse(String str) {
        ArrayList arrayList = new ArrayList();
        int length = str.length();
        int endIdx = 0;
        while (endIdx < length) {
            int iIndexOf = str.indexOf(37, endIdx);
            if (str.charAt(endIdx) != '%') {
                if (iIndexOf == -1) {
                    iIndexOf = length;
                }
                arrayList.add(new FixedString(str.substring(endIdx, iIndexOf)));
                endIdx = iIndexOf;
            } else {
                FormatSpecifierParser formatSpecifierParser = new FormatSpecifierParser(str, endIdx + 1);
                arrayList.add(formatSpecifierParser.getFormatSpecifier());
                endIdx = formatSpecifierParser.getEndIdx();
            }
        }
        return (FormatString[]) arrayList.toArray(new FormatString[arrayList.size()]);
    }

    private class FormatSpecifierParser {
        private static final String FLAGS = ",-(+# 0<";
        private String conv;
        private int cursor;
        private String flags;
        private final String format;
        private FormatSpecifier fs;
        private String index;
        private String precision;
        private String tT;
        private String width;

        public FormatSpecifierParser(String str, int i) {
            this.format = str;
            this.cursor = i;
            if (nextIsInt()) {
                String strNextInt = nextInt();
                if (peek() == '$') {
                    this.index = strNextInt;
                    advance();
                } else if (strNextInt.charAt(0) == '0') {
                    back(strNextInt.length());
                } else {
                    this.width = strNextInt;
                }
            }
            this.flags = "";
            while (this.width == null && FLAGS.indexOf(peek()) >= 0) {
                this.flags += advance();
            }
            if (this.width == null && nextIsInt()) {
                this.width = nextInt();
            }
            if (peek() == '.') {
                advance();
                if (!nextIsInt()) {
                    throw new IllegalFormatPrecisionException(peek());
                }
                this.precision = nextInt();
            }
            if (peek() == 't' || peek() == 'T') {
                this.tT = String.valueOf(advance());
            }
            this.conv = String.valueOf(advance());
            this.fs = Formatter.this.new FormatSpecifier(this.index, this.flags, this.width, this.precision, this.tT, this.conv);
        }

        private String nextInt() {
            int i = this.cursor;
            while (nextIsInt()) {
                advance();
            }
            return this.format.substring(i, this.cursor);
        }

        private boolean nextIsInt() {
            return !isEnd() && Character.isDigit(peek());
        }

        private char peek() {
            if (isEnd()) {
                throw new UnknownFormatConversionException("End of String");
            }
            return this.format.charAt(this.cursor);
        }

        private char advance() {
            if (isEnd()) {
                throw new UnknownFormatConversionException("End of String");
            }
            String str = this.format;
            int i = this.cursor;
            this.cursor = i + 1;
            return str.charAt(i);
        }

        private void back(int i) {
            this.cursor -= i;
        }

        private boolean isEnd() {
            return this.cursor == this.format.length();
        }

        public FormatSpecifier getFormatSpecifier() {
            return this.fs;
        }

        public int getEndIdx() {
            return this.cursor;
        }
    }

    private class FixedString implements FormatString {
        private String s;

        FixedString(String str) {
            this.s = str;
        }

        @Override
        public int index() {
            return -2;
        }

        @Override
        public void print(Object obj, Locale locale) throws IOException {
            Formatter.this.a.append(this.s);
        }

        @Override
        public String toString() {
            return this.s;
        }
    }

    private class FormatSpecifier implements FormatString {
        static final boolean $assertionsDisabled = false;
        private char c;
        private boolean dt;
        private int precision;
        private int width;
        private int index = -1;
        private Flags f = Flags.NONE;

        private int index(String str) {
            if (str != null) {
                try {
                    this.index = Integer.parseInt(str);
                } catch (NumberFormatException e) {
                }
            } else {
                this.index = 0;
            }
            return this.index;
        }

        @Override
        public int index() {
            return this.index;
        }

        private Flags flags(String str) {
            this.f = Flags.parse(str);
            if (this.f.contains(Flags.PREVIOUS)) {
                this.index = -1;
            }
            return this.f;
        }

        Flags flags() {
            return this.f;
        }

        private int width(String str) {
            this.width = -1;
            if (str != null) {
                try {
                    this.width = Integer.parseInt(str);
                    if (this.width < 0) {
                        throw new IllegalFormatWidthException(this.width);
                    }
                } catch (NumberFormatException e) {
                }
            }
            return this.width;
        }

        int width() {
            return this.width;
        }

        private int precision(String str) {
            this.precision = -1;
            if (str != null) {
                try {
                    this.precision = Integer.parseInt(str);
                    if (this.precision < 0) {
                        throw new IllegalFormatPrecisionException(this.precision);
                    }
                } catch (NumberFormatException e) {
                }
            }
            return this.precision;
        }

        int precision() {
            return this.precision;
        }

        private char conversion(String str) {
            this.c = str.charAt(0);
            if (!this.dt) {
                if (!Conversion.isValid(this.c)) {
                    throw new UnknownFormatConversionException(String.valueOf(this.c));
                }
                if (Character.isUpperCase(this.c)) {
                    this.f.add(Flags.UPPERCASE);
                }
                this.c = Character.toLowerCase(this.c);
                if (Conversion.isText(this.c)) {
                    this.index = -2;
                }
            }
            return this.c;
        }

        private char conversion() {
            return this.c;
        }

        FormatSpecifier(String str, String str2, String str3, String str4, String str5, String str6) {
            this.dt = false;
            index(str);
            flags(str2);
            width(str3);
            precision(str4);
            if (str5 != null) {
                this.dt = true;
                if (str5.equals("T")) {
                    this.f.add(Flags.UPPERCASE);
                }
            }
            conversion(str6);
            if (this.dt) {
                checkDateTime();
                return;
            }
            if (Conversion.isGeneral(this.c)) {
                checkGeneral();
                return;
            }
            if (Conversion.isCharacter(this.c)) {
                checkCharacter();
                return;
            }
            if (Conversion.isInteger(this.c)) {
                checkInteger();
            } else if (Conversion.isFloat(this.c)) {
                checkFloat();
            } else {
                if (Conversion.isText(this.c)) {
                    checkText();
                    return;
                }
                throw new UnknownFormatConversionException(String.valueOf(this.c));
            }
        }

        @Override
        public void print(Object obj, Locale locale) throws IOException {
            if (this.dt) {
                printDateTime(obj, locale);
                return;
            }
            char c = this.c;
            if (c == '%') {
                Formatter.this.a.append('%');
                return;
            }
            if (c != 'C') {
                if (c != 's') {
                    if (c != 'x') {
                        switch (c) {
                            case 'a':
                            case 'e':
                            case 'f':
                            case 'g':
                                printFloat(obj, locale);
                                return;
                            case 'b':
                                printBoolean(obj);
                                return;
                            case 'c':
                                break;
                            case 'd':
                                break;
                            case 'h':
                                printHashCode(obj);
                                return;
                            default:
                                switch (c) {
                                    case 'n':
                                        Formatter.this.a.append(System.lineSeparator());
                                        break;
                                }
                                return;
                        }
                    }
                    printInteger(obj, locale);
                    return;
                }
                printString(obj, locale);
                return;
            }
            printCharacter(obj);
        }

        private void printInteger(Object obj, Locale locale) throws IOException {
            if (obj == null) {
                print("null");
                return;
            }
            if (obj instanceof Byte) {
                print(((Byte) obj).byteValue(), locale);
                return;
            }
            if (obj instanceof Short) {
                print(((Short) obj).shortValue(), locale);
                return;
            }
            if (obj instanceof Integer) {
                print(((Integer) obj).intValue(), locale);
                return;
            }
            if (obj instanceof Long) {
                print(((Long) obj).longValue(), locale);
            } else if (obj instanceof BigInteger) {
                print((BigInteger) obj, locale);
            } else {
                failConversion(this.c, obj);
            }
        }

        private void printFloat(Object obj, Locale locale) throws IOException {
            if (obj == null) {
                print("null");
                return;
            }
            if (obj instanceof Float) {
                print(((Float) obj).floatValue(), locale);
                return;
            }
            if (obj instanceof Double) {
                print(((Double) obj).doubleValue(), locale);
            } else if (obj instanceof BigDecimal) {
                print((BigDecimal) obj, locale);
            } else {
                failConversion(this.c, obj);
            }
        }

        private void printDateTime(Object obj, Locale locale) throws IOException {
            if (obj == null) {
                print("null");
                return;
            }
            Calendar calendar = null;
            if (obj instanceof Long) {
                calendar = Calendar.getInstance(locale == null ? Locale.US : locale);
                calendar.setTimeInMillis(((Long) obj).longValue());
            } else if (obj instanceof Date) {
                calendar = Calendar.getInstance(locale == null ? Locale.US : locale);
                calendar.setTime((Date) obj);
            } else if (obj instanceof Calendar) {
                calendar = (Calendar) ((Calendar) obj).clone();
                calendar.setLenient(true);
            } else {
                if (obj instanceof TemporalAccessor) {
                    print((TemporalAccessor) obj, this.c, locale);
                    return;
                }
                failConversion(this.c, obj);
            }
            print(calendar, this.c, locale);
        }

        private void printCharacter(Object obj) throws IOException {
            if (obj == null) {
                print("null");
                return;
            }
            String str = null;
            if (obj instanceof Character) {
                str = ((Character) obj).toString();
            } else if (obj instanceof Byte) {
                byte bByteValue = ((Byte) obj).byteValue();
                if (Character.isValidCodePoint(bByteValue)) {
                    str = new String(Character.toChars(bByteValue));
                } else {
                    throw new IllegalFormatCodePointException(bByteValue);
                }
            } else if (obj instanceof Short) {
                short sShortValue = ((Short) obj).shortValue();
                if (Character.isValidCodePoint(sShortValue)) {
                    str = new String(Character.toChars(sShortValue));
                } else {
                    throw new IllegalFormatCodePointException(sShortValue);
                }
            } else if (obj instanceof Integer) {
                int iIntValue = ((Integer) obj).intValue();
                if (Character.isValidCodePoint(iIntValue)) {
                    str = new String(Character.toChars(iIntValue));
                } else {
                    throw new IllegalFormatCodePointException(iIntValue);
                }
            } else {
                failConversion(this.c, obj);
            }
            print(str);
        }

        private void printString(Object obj, Locale locale) throws IOException {
            if (obj instanceof Formattable) {
                Formatter formatter = Formatter.this;
                if (formatter.locale() != locale) {
                    formatter = new Formatter(formatter.out(), locale);
                }
                ((Formattable) obj).formatTo(formatter, this.f.valueOf(), this.width, this.precision);
                return;
            }
            if (this.f.contains(Flags.ALTERNATE)) {
                failMismatch(Flags.ALTERNATE, 's');
            }
            if (obj == null) {
                print("null");
            } else {
                print(obj.toString());
            }
        }

        private void printBoolean(Object obj) throws IOException {
            String string;
            if (obj != null) {
                if (obj instanceof Boolean) {
                    string = ((Boolean) obj).toString();
                } else {
                    string = Boolean.toString(true);
                }
            } else {
                string = Boolean.toString(false);
            }
            print(string);
        }

        private void printHashCode(Object obj) throws IOException {
            String hexString;
            if (obj == null) {
                hexString = "null";
            } else {
                hexString = Integer.toHexString(obj.hashCode());
            }
            print(hexString);
        }

        private void print(String str) throws IOException {
            if (this.precision != -1 && this.precision < str.length()) {
                str = str.substring(0, this.precision);
            }
            if (this.f.contains(Flags.UPPERCASE)) {
                str = str.toUpperCase(Formatter.this.l != null ? Formatter.this.l : Locale.getDefault());
            }
            Formatter.this.a.append(justify(str));
        }

        private String justify(String str) {
            if (this.width == -1) {
                return str;
            }
            StringBuilder sb = new StringBuilder();
            boolean zContains = this.f.contains(Flags.LEFT_JUSTIFY);
            int length = this.width - str.length();
            if (!zContains) {
                for (int i = 0; i < length; i++) {
                    sb.append(' ');
                }
            }
            sb.append(str);
            if (zContains) {
                for (int i2 = 0; i2 < length; i2++) {
                    sb.append(' ');
                }
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("%");
            sb.append(this.f.dup().remove(Flags.UPPERCASE).toString());
            if (this.index > 0) {
                sb.append(this.index);
                sb.append('$');
            }
            if (this.width != -1) {
                sb.append(this.width);
            }
            if (this.precision != -1) {
                sb.append('.');
                sb.append(this.precision);
            }
            if (this.dt) {
                sb.append(this.f.contains(Flags.UPPERCASE) ? 'T' : 't');
            }
            sb.append(this.f.contains(Flags.UPPERCASE) ? Character.toUpperCase(this.c) : this.c);
            return sb.toString();
        }

        private void checkGeneral() {
            if ((this.c == 'b' || this.c == 'h') && this.f.contains(Flags.ALTERNATE)) {
                failMismatch(Flags.ALTERNATE, this.c);
            }
            if (this.width == -1 && this.f.contains(Flags.LEFT_JUSTIFY)) {
                throw new MissingFormatWidthException(toString());
            }
            checkBadFlags(Flags.PLUS, Flags.LEADING_SPACE, Flags.ZERO_PAD, Flags.GROUP, Flags.PARENTHESES);
        }

        private void checkDateTime() {
            if (this.precision != -1) {
                throw new IllegalFormatPrecisionException(this.precision);
            }
            if (!DateTime.isValid(this.c)) {
                throw new UnknownFormatConversionException("t" + this.c);
            }
            checkBadFlags(Flags.ALTERNATE, Flags.PLUS, Flags.LEADING_SPACE, Flags.ZERO_PAD, Flags.GROUP, Flags.PARENTHESES);
            if (this.width == -1 && this.f.contains(Flags.LEFT_JUSTIFY)) {
                throw new MissingFormatWidthException(toString());
            }
        }

        private void checkCharacter() {
            if (this.precision != -1) {
                throw new IllegalFormatPrecisionException(this.precision);
            }
            checkBadFlags(Flags.ALTERNATE, Flags.PLUS, Flags.LEADING_SPACE, Flags.ZERO_PAD, Flags.GROUP, Flags.PARENTHESES);
            if (this.width == -1 && this.f.contains(Flags.LEFT_JUSTIFY)) {
                throw new MissingFormatWidthException(toString());
            }
        }

        private void checkInteger() {
            checkNumeric();
            if (this.precision != -1) {
                throw new IllegalFormatPrecisionException(this.precision);
            }
            if (this.c == 'd') {
                checkBadFlags(Flags.ALTERNATE);
            } else if (this.c == 'o') {
                checkBadFlags(Flags.GROUP);
            } else {
                checkBadFlags(Flags.GROUP);
            }
        }

        private void checkBadFlags(Flags... flagsArr) {
            for (int i = 0; i < flagsArr.length; i++) {
                if (this.f.contains(flagsArr[i])) {
                    failMismatch(flagsArr[i], this.c);
                }
            }
        }

        private void checkFloat() {
            checkNumeric();
            if (this.c != 'f') {
                if (this.c == 'a') {
                    checkBadFlags(Flags.PARENTHESES, Flags.GROUP);
                } else if (this.c == 'e') {
                    checkBadFlags(Flags.GROUP);
                } else if (this.c == 'g') {
                    checkBadFlags(Flags.ALTERNATE);
                }
            }
        }

        private void checkNumeric() {
            if (this.width != -1 && this.width < 0) {
                throw new IllegalFormatWidthException(this.width);
            }
            if (this.precision != -1 && this.precision < 0) {
                throw new IllegalFormatPrecisionException(this.precision);
            }
            if (this.width == -1 && (this.f.contains(Flags.LEFT_JUSTIFY) || this.f.contains(Flags.ZERO_PAD))) {
                throw new MissingFormatWidthException(toString());
            }
            if ((this.f.contains(Flags.PLUS) && this.f.contains(Flags.LEADING_SPACE)) || (this.f.contains(Flags.LEFT_JUSTIFY) && this.f.contains(Flags.ZERO_PAD))) {
                throw new IllegalFormatFlagsException(this.f.toString());
            }
        }

        private void checkText() {
            if (this.precision != -1) {
                throw new IllegalFormatPrecisionException(this.precision);
            }
            char c = this.c;
            if (c != '%') {
                if (c == 'n') {
                    if (this.width != -1) {
                        throw new IllegalFormatWidthException(this.width);
                    }
                    if (this.f.valueOf() != Flags.NONE.valueOf()) {
                        throw new IllegalFormatFlagsException(this.f.toString());
                    }
                    return;
                }
                return;
            }
            if (this.f.valueOf() != Flags.LEFT_JUSTIFY.valueOf() && this.f.valueOf() != Flags.NONE.valueOf()) {
                throw new IllegalFormatFlagsException(this.f.toString());
            }
            if (this.width == -1 && this.f.contains(Flags.LEFT_JUSTIFY)) {
                throw new MissingFormatWidthException(toString());
            }
        }

        private void print(byte b, Locale locale) throws IOException {
            long j = b;
            if (b < 0 && (this.c == 'o' || this.c == 'x')) {
                j += 256;
            }
            print(j, locale);
        }

        private void print(short s, Locale locale) throws IOException {
            long j = s;
            if (s < 0 && (this.c == 'o' || this.c == 'x')) {
                j += 65536;
            }
            print(j, locale);
        }

        private void print(int i, Locale locale) throws IOException {
            long j = i;
            if (i < 0 && (this.c == 'o' || this.c == 'x')) {
                j += 4294967296L;
            }
            print(j, locale);
        }

        private void print(long j, Locale locale) throws IOException {
            int length;
            int length2;
            char[] charArray;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            if (this.c == 'd') {
                boolean z = j < 0;
                if (j < 0) {
                    charArray = Long.toString(j, 10).substring(1).toCharArray();
                } else {
                    charArray = Long.toString(j, 10).toCharArray();
                }
                char[] cArr = charArray;
                leadingSign(sb, z);
                localizedMagnitude(sb, cArr, this.f, adjustWidth(this.width, this.f, z), locale);
                trailingSign(sb, z);
            } else if (this.c == 'o') {
                checkBadFlags(Flags.PARENTHESES, Flags.LEADING_SPACE, Flags.PLUS);
                String octalString = Long.toOctalString(j);
                if (this.f.contains(Flags.ALTERNATE)) {
                    length2 = octalString.length() + 1;
                } else {
                    length2 = octalString.length();
                }
                if (this.f.contains(Flags.ALTERNATE)) {
                    sb.append('0');
                }
                if (this.f.contains(Flags.ZERO_PAD)) {
                    while (i < this.width - length2) {
                        sb.append('0');
                        i++;
                    }
                }
                sb.append(octalString);
            } else if (this.c == 'x') {
                checkBadFlags(Flags.PARENTHESES, Flags.LEADING_SPACE, Flags.PLUS);
                String hexString = Long.toHexString(j);
                if (this.f.contains(Flags.ALTERNATE)) {
                    length = hexString.length() + 2;
                } else {
                    length = hexString.length();
                }
                if (this.f.contains(Flags.ALTERNATE)) {
                    sb.append(this.f.contains(Flags.UPPERCASE) ? "0X" : "0x");
                }
                if (this.f.contains(Flags.ZERO_PAD)) {
                    while (i < this.width - length) {
                        sb.append('0');
                        i++;
                    }
                }
                if (this.f.contains(Flags.UPPERCASE)) {
                    hexString = hexString.toUpperCase();
                }
                sb.append(hexString);
            }
            Formatter.this.a.append(justify(sb.toString()));
        }

        private StringBuilder leadingSign(StringBuilder sb, boolean z) {
            if (!z) {
                if (this.f.contains(Flags.PLUS)) {
                    sb.append('+');
                } else if (this.f.contains(Flags.LEADING_SPACE)) {
                    sb.append(' ');
                }
            } else if (this.f.contains(Flags.PARENTHESES)) {
                sb.append('(');
            } else {
                sb.append('-');
            }
            return sb;
        }

        private StringBuilder trailingSign(StringBuilder sb, boolean z) {
            if (z && this.f.contains(Flags.PARENTHESES)) {
                sb.append(')');
            }
            return sb;
        }

        private void print(BigInteger bigInteger, Locale locale) throws IOException {
            StringBuilder sb = new StringBuilder();
            boolean z = bigInteger.signum() == -1;
            BigInteger bigIntegerAbs = bigInteger.abs();
            leadingSign(sb, z);
            if (this.c == 'd') {
                localizedMagnitude(sb, bigIntegerAbs.toString().toCharArray(), this.f, adjustWidth(this.width, this.f, z), locale);
            } else if (this.c == 'o') {
                String string = bigIntegerAbs.toString(8);
                int length = string.length() + sb.length();
                if (z && this.f.contains(Flags.PARENTHESES)) {
                    length++;
                }
                if (this.f.contains(Flags.ALTERNATE)) {
                    length++;
                    sb.append('0');
                }
                if (this.f.contains(Flags.ZERO_PAD)) {
                    for (int i = 0; i < this.width - length; i++) {
                        sb.append('0');
                    }
                }
                sb.append(string);
            } else if (this.c == 'x') {
                String string2 = bigIntegerAbs.toString(16);
                int length2 = string2.length() + sb.length();
                if (z && this.f.contains(Flags.PARENTHESES)) {
                    length2++;
                }
                if (this.f.contains(Flags.ALTERNATE)) {
                    length2 += 2;
                    sb.append(this.f.contains(Flags.UPPERCASE) ? "0X" : "0x");
                }
                if (this.f.contains(Flags.ZERO_PAD)) {
                    for (int i2 = 0; i2 < this.width - length2; i2++) {
                        sb.append('0');
                    }
                }
                if (this.f.contains(Flags.UPPERCASE)) {
                    string2 = string2.toUpperCase();
                }
                sb.append(string2);
            }
            trailingSign(sb, bigInteger.signum() == -1);
            Formatter.this.a.append(justify(sb.toString()));
        }

        private void print(float f, Locale locale) throws IOException {
            print(f, locale);
        }

        private void print(double d, Locale locale) throws IOException {
            StringBuilder sb = new StringBuilder();
            boolean z = Double.compare(d, 0.0d) == -1;
            if (!Double.isNaN(d)) {
                double dAbs = Math.abs(d);
                leadingSign(sb, z);
                if (!Double.isInfinite(dAbs)) {
                    print(sb, dAbs, locale, this.f, this.c, this.precision, z);
                } else {
                    sb.append(this.f.contains(Flags.UPPERCASE) ? "INFINITY" : "Infinity");
                }
                trailingSign(sb, z);
            } else {
                sb.append(this.f.contains(Flags.UPPERCASE) ? "NAN" : "NaN");
            }
            Formatter.this.a.append(justify(sb.toString()));
        }

        private void print(StringBuilder sb, double d, Locale locale, Flags flags, char c, int i, boolean z) throws IOException {
            int exponentRounded;
            char[] cArr;
            char[] cArr2;
            Locale locale2;
            String lowerCase;
            int i2 = i;
            if (c == 'e') {
                int i3 = i2 == -1 ? 6 : i2;
                FormattedFloatingDecimal formattedFloatingDecimalValueOf = FormattedFloatingDecimal.valueOf(d, i3, FormattedFloatingDecimal.Form.SCIENTIFIC);
                char[] cArrAddZeros = addZeros(formattedFloatingDecimalValueOf.getMantissa(), i3);
                if (flags.contains(Flags.ALTERNATE) && i3 == 0) {
                    cArrAddZeros = addDot(cArrAddZeros);
                }
                char[] exponent = d == 0.0d ? new char[]{'+', '0', '0'} : formattedFloatingDecimalValueOf.getExponent();
                int iAdjustWidth = this.width;
                if (this.width != -1) {
                    iAdjustWidth = adjustWidth((this.width - exponent.length) - 1, flags, z);
                }
                localizedMagnitude(sb, cArrAddZeros, flags, iAdjustWidth, locale);
                if (locale == null) {
                    locale2 = Locale.getDefault();
                } else {
                    locale2 = locale;
                }
                LocaleData localeData = LocaleData.get(locale2);
                if (flags.contains(Flags.UPPERCASE)) {
                    lowerCase = localeData.exponentSeparator.toUpperCase(locale2);
                } else {
                    lowerCase = localeData.exponentSeparator.toLowerCase(locale2);
                }
                sb.append(lowerCase);
                Flags flagsRemove = flags.dup().remove(Flags.GROUP);
                sb.append(exponent[0]);
                char[] cArr3 = new char[exponent.length - 1];
                System.arraycopy((Object) exponent, 1, (Object) cArr3, 0, exponent.length - 1);
                sb.append((CharSequence) localizedMagnitude((StringBuilder) null, cArr3, flagsRemove, -1, locale));
                return;
            }
            if (c == 'f') {
                if (i2 == -1) {
                    i2 = 6;
                }
                char[] cArrAddZeros2 = addZeros(FormattedFloatingDecimal.valueOf(d, i2, FormattedFloatingDecimal.Form.DECIMAL_FLOAT).getMantissa(), i2);
                if (flags.contains(Flags.ALTERNATE) && i2 == 0) {
                    cArrAddZeros2 = addDot(cArrAddZeros2);
                }
                char[] cArr4 = cArrAddZeros2;
                int iAdjustWidth2 = this.width;
                if (this.width != -1) {
                    iAdjustWidth2 = adjustWidth(this.width, flags, z);
                }
                localizedMagnitude(sb, cArr4, flags, iAdjustWidth2, locale);
                return;
            }
            if (c != 'g') {
                if (c == 'a') {
                    if (i2 != -1) {
                        if (i2 == 0) {
                            i2 = 1;
                        }
                    } else {
                        i2 = 0;
                    }
                    String strHexDouble = hexDouble(d, i2);
                    boolean zContains = flags.contains(Flags.UPPERCASE);
                    sb.append(zContains ? "0X" : "0x");
                    if (flags.contains(Flags.ZERO_PAD)) {
                        for (int i4 = 0; i4 < (this.width - strHexDouble.length()) - 2; i4++) {
                            sb.append('0');
                        }
                    }
                    int iIndexOf = strHexDouble.indexOf(112);
                    char[] charArray = strHexDouble.substring(0, iIndexOf).toCharArray();
                    if (zContains) {
                        charArray = new String(charArray).toUpperCase(Locale.US).toCharArray();
                    }
                    if (i2 != 0) {
                        charArray = addZeros(charArray, i2);
                    }
                    sb.append(charArray);
                    sb.append(zContains ? 'P' : 'p');
                    sb.append(strHexDouble.substring(iIndexOf + 1));
                    return;
                }
                return;
            }
            if (i2 != -1) {
                if (i2 == 0) {
                    i2 = 1;
                }
            } else {
                i2 = 6;
            }
            if (d == 0.0d) {
                cArr2 = new char[]{'0'};
                cArr = null;
                exponentRounded = 0;
            } else {
                FormattedFloatingDecimal formattedFloatingDecimalValueOf2 = FormattedFloatingDecimal.valueOf(d, i2, FormattedFloatingDecimal.Form.GENERAL);
                char[] exponent2 = formattedFloatingDecimalValueOf2.getExponent();
                char[] mantissa = formattedFloatingDecimalValueOf2.getMantissa();
                exponentRounded = formattedFloatingDecimalValueOf2.getExponentRounded();
                cArr = exponent2;
                cArr2 = mantissa;
            }
            int i5 = cArr != null ? i2 - 1 : i2 - (exponentRounded + 1);
            char[] cArrAddZeros3 = addZeros(cArr2, i5);
            if (flags.contains(Flags.ALTERNATE) && i5 == 0) {
                cArrAddZeros3 = addDot(cArrAddZeros3);
            }
            char[] cArr5 = cArrAddZeros3;
            int iAdjustWidth3 = this.width;
            if (this.width != -1) {
                iAdjustWidth3 = cArr != null ? adjustWidth((this.width - cArr.length) - 1, flags, z) : adjustWidth(this.width, flags, z);
            }
            localizedMagnitude(sb, cArr5, flags, iAdjustWidth3, locale);
            if (cArr != null) {
                sb.append(flags.contains(Flags.UPPERCASE) ? 'E' : 'e');
                Flags flagsRemove2 = flags.dup().remove(Flags.GROUP);
                sb.append(cArr[0]);
                char[] cArr6 = new char[cArr.length - 1];
                System.arraycopy((Object) cArr, 1, (Object) cArr6, 0, cArr.length - 1);
                sb.append((CharSequence) localizedMagnitude((StringBuilder) null, cArr6, flagsRemove2, -1, locale));
            }
        }

        private char[] addZeros(char[] cArr, int i) {
            int i2;
            int i3 = 0;
            while (i3 < cArr.length && cArr[i3] != '.') {
                i3++;
            }
            if (i3 != cArr.length) {
                i2 = 0;
            } else {
                i2 = 1;
            }
            int length = (cArr.length - i3) - (i2 ^ 1);
            if (length == i) {
                return cArr;
            }
            char[] cArr2 = new char[((cArr.length + i) - length) + i2];
            System.arraycopy((Object) cArr, 0, (Object) cArr2, 0, cArr.length);
            int length2 = cArr.length;
            if (i2 != 0) {
                cArr2[cArr.length] = '.';
                length2++;
            }
            while (length2 < cArr2.length) {
                cArr2[length2] = '0';
                length2++;
            }
            return cArr2;
        }

        private String hexDouble(double d, int i) {
            double d2;
            if (!Double.isFinite(d) || d == 0.0d || i == 0 || i >= 13) {
                return Double.toHexString(d).substring(2);
            }
            boolean z = Math.getExponent(d) == -1023;
            if (z) {
                double unused = Formatter.scaleUp = Math.scalb(1.0d, 54);
                d2 = d * Formatter.scaleUp;
                Math.getExponent(d2);
            } else {
                d2 = d;
            }
            int i2 = 53 - ((i * 4) + 1);
            long jDoubleToLongBits = Double.doubleToLongBits(d2);
            long j = (Long.MAX_VALUE & jDoubleToLongBits) >> i2;
            long j2 = (~((-1) << i2)) & jDoubleToLongBits;
            boolean z2 = (j & 1) == 0;
            long j3 = 1 << (i2 - 1);
            boolean z3 = (j3 & j2) != 0;
            boolean z4 = i2 > 1 && ((~j3) & j2) != 0;
            if ((z2 && z3 && z4) || (!z2 && z3)) {
                j++;
            }
            double dLongBitsToDouble = Double.longBitsToDouble((jDoubleToLongBits & Long.MIN_VALUE) | (j << i2));
            if (Double.isInfinite(dLongBitsToDouble)) {
                return "1.0p1024";
            }
            String strSubstring = Double.toHexString(dLongBitsToDouble).substring(2);
            if (!z) {
                return strSubstring;
            }
            int iIndexOf = strSubstring.indexOf(112);
            if (iIndexOf == -1) {
                return null;
            }
            return strSubstring.substring(0, iIndexOf) + "p" + Integer.toString(Integer.parseInt(strSubstring.substring(iIndexOf + 1)) - 54);
        }

        private void print(BigDecimal bigDecimal, Locale locale) throws IOException {
            if (this.c == 'a') {
                failConversion(this.c, bigDecimal);
            }
            StringBuilder sb = new StringBuilder();
            boolean z = bigDecimal.signum() == -1;
            BigDecimal bigDecimalAbs = bigDecimal.abs();
            leadingSign(sb, z);
            print(sb, bigDecimalAbs, locale, this.f, this.c, this.precision, z);
            trailingSign(sb, z);
            Formatter.this.a.append(justify(sb.toString()));
        }

        private void print(StringBuilder sb, BigDecimal bigDecimal, Locale locale, Flags flags, char c, int i, boolean z) throws IOException {
            BigDecimal bigDecimal2;
            int i2;
            int i3;
            int i4 = i;
            if (c == 'e') {
                if (i4 == -1) {
                    i4 = 6;
                }
                int iScale = bigDecimal.scale();
                int iPrecision = bigDecimal.precision();
                int i5 = iPrecision - 1;
                if (i4 > i5) {
                    i3 = i4 - i5;
                    i2 = iPrecision;
                } else {
                    i2 = i4 + 1;
                    i3 = 0;
                }
                BigDecimal bigDecimal3 = new BigDecimal(bigDecimal.unscaledValue(), iScale, new MathContext(i2));
                BigDecimalLayout bigDecimalLayout = new BigDecimalLayout(bigDecimal3.unscaledValue(), bigDecimal3.scale(), BigDecimalLayoutForm.SCIENTIFIC);
                char[] cArrMantissa = bigDecimalLayout.mantissa();
                if ((iPrecision == 1 || !bigDecimalLayout.hasDot()) && (i3 > 0 || flags.contains(Flags.ALTERNATE))) {
                    cArrMantissa = addDot(cArrMantissa);
                }
                char[] cArrTrailingZeros = trailingZeros(cArrMantissa, i3);
                char[] cArrExponent = bigDecimalLayout.exponent();
                int iAdjustWidth = this.width;
                if (this.width != -1) {
                    iAdjustWidth = adjustWidth((this.width - cArrExponent.length) - 1, flags, z);
                }
                localizedMagnitude(sb, cArrTrailingZeros, flags, iAdjustWidth, locale);
                sb.append(flags.contains(Flags.UPPERCASE) ? 'E' : 'e');
                Flags flagsRemove = flags.dup().remove(Flags.GROUP);
                char c2 = cArrExponent[0];
                sb.append(cArrExponent[0]);
                char[] cArr = new char[cArrExponent.length - 1];
                System.arraycopy((Object) cArrExponent, 1, (Object) cArr, 0, cArrExponent.length - 1);
                sb.append((CharSequence) localizedMagnitude((StringBuilder) null, cArr, flagsRemove, -1, locale));
                return;
            }
            if (c != 'f') {
                if (c == 'g') {
                    if (i4 != -1) {
                        if (i4 == 0) {
                            i4 = 1;
                        }
                    } else {
                        i4 = 6;
                    }
                    BigDecimal bigDecimalValueOf = BigDecimal.valueOf(1L, 4);
                    BigDecimal bigDecimalValueOf2 = BigDecimal.valueOf(1L, -i4);
                    if (bigDecimal.equals(BigDecimal.ZERO) || (bigDecimal.compareTo(bigDecimalValueOf) != -1 && bigDecimal.compareTo(bigDecimalValueOf2) == -1)) {
                        print(sb, bigDecimal, locale, flags, 'f', (i4 - ((-bigDecimal.scale()) + (bigDecimal.unscaledValue().toString().length() - 1))) - 1, z);
                        return;
                    } else {
                        print(sb, bigDecimal, locale, flags, 'e', i4 - 1, z);
                        return;
                    }
                }
                if (c == 'a') {
                }
                return;
            }
            if (i4 == -1) {
                i4 = 6;
            }
            int iScale2 = bigDecimal.scale();
            if (iScale2 > i4) {
                int iPrecision2 = bigDecimal.precision();
                if (iPrecision2 <= iScale2) {
                    bigDecimal2 = bigDecimal.setScale(i4, RoundingMode.HALF_UP);
                } else {
                    bigDecimal2 = new BigDecimal(bigDecimal.unscaledValue(), iScale2, new MathContext(iPrecision2 - (iScale2 - i4)));
                }
            } else {
                bigDecimal2 = bigDecimal;
            }
            BigDecimalLayout bigDecimalLayout2 = new BigDecimalLayout(bigDecimal2.unscaledValue(), bigDecimal2.scale(), BigDecimalLayoutForm.DECIMAL_FLOAT);
            char[] cArrMantissa2 = bigDecimalLayout2.mantissa();
            int iScale3 = bigDecimalLayout2.scale() < i4 ? i4 - bigDecimalLayout2.scale() : 0;
            if (bigDecimalLayout2.scale() == 0 && (flags.contains(Flags.ALTERNATE) || iScale3 > 0)) {
                cArrMantissa2 = addDot(bigDecimalLayout2.mantissa());
            }
            localizedMagnitude(sb, trailingZeros(cArrMantissa2, iScale3), flags, adjustWidth(this.width, flags, z), locale);
        }

        private class BigDecimalLayout {
            private boolean dot = false;
            private StringBuilder exp;
            private StringBuilder mant;
            private int scale;

            public BigDecimalLayout(BigInteger bigInteger, int i, BigDecimalLayoutForm bigDecimalLayoutForm) {
                layout(bigInteger, i, bigDecimalLayoutForm);
            }

            public boolean hasDot() {
                return this.dot;
            }

            public int scale() {
                return this.scale;
            }

            public char[] layoutChars() {
                StringBuilder sb = new StringBuilder(this.mant);
                if (this.exp != null) {
                    sb.append('E');
                    sb.append((CharSequence) this.exp);
                }
                return toCharArray(sb);
            }

            public char[] mantissa() {
                return toCharArray(this.mant);
            }

            public char[] exponent() {
                return toCharArray(this.exp);
            }

            private char[] toCharArray(StringBuilder sb) {
                if (sb == null) {
                    return null;
                }
                char[] cArr = new char[sb.length()];
                sb.getChars(0, cArr.length, cArr, 0);
                return cArr;
            }

            private void layout(BigInteger bigInteger, int i, BigDecimalLayoutForm bigDecimalLayoutForm) {
                char[] charArray = bigInteger.toString().toCharArray();
                this.scale = i;
                this.mant = new StringBuilder(charArray.length + 14);
                if (i == 0) {
                    int length = charArray.length;
                    if (length > 1) {
                        this.mant.append(charArray[0]);
                        if (bigDecimalLayoutForm == BigDecimalLayoutForm.SCIENTIFIC) {
                            this.mant.append('.');
                            this.dot = true;
                            int i2 = length - 1;
                            this.mant.append(charArray, 1, i2);
                            this.exp = new StringBuilder("+");
                            if (length < 10) {
                                StringBuilder sb = this.exp;
                                sb.append("0");
                                sb.append(i2);
                                return;
                            }
                            this.exp.append(i2);
                            return;
                        }
                        this.mant.append(charArray, 1, length - 1);
                        return;
                    }
                    this.mant.append(charArray);
                    if (bigDecimalLayoutForm == BigDecimalLayoutForm.SCIENTIFIC) {
                        this.exp = new StringBuilder("+00");
                        return;
                    }
                    return;
                }
                long length2 = (-i) + ((long) (charArray.length - 1));
                if (bigDecimalLayoutForm == BigDecimalLayoutForm.DECIMAL_FLOAT) {
                    int length3 = i - charArray.length;
                    if (length3 >= 0) {
                        this.mant.append("0.");
                        this.dot = true;
                        while (length3 > 0) {
                            this.mant.append('0');
                            length3--;
                        }
                        this.mant.append(charArray);
                        return;
                    }
                    int i3 = -length3;
                    if (i3 < charArray.length) {
                        this.mant.append(charArray, 0, i3);
                        this.mant.append('.');
                        this.dot = true;
                        this.mant.append(charArray, i3, i);
                        return;
                    }
                    this.mant.append(charArray, 0, charArray.length);
                    for (int i4 = 0; i4 < (-i); i4++) {
                        this.mant.append('0');
                    }
                    this.scale = 0;
                    return;
                }
                this.mant.append(charArray[0]);
                if (charArray.length > 1) {
                    this.mant.append('.');
                    this.dot = true;
                    this.mant.append(charArray, 1, charArray.length - 1);
                }
                this.exp = new StringBuilder();
                if (length2 != 0) {
                    long jAbs = Math.abs(length2);
                    this.exp.append(length2 < 0 ? '-' : '+');
                    if (jAbs < 10) {
                        this.exp.append('0');
                    }
                    this.exp.append(jAbs);
                    return;
                }
                this.exp.append("+00");
            }
        }

        private int adjustWidth(int i, Flags flags, boolean z) {
            if (i != -1 && z && flags.contains(Flags.PARENTHESES)) {
                return i - 1;
            }
            return i;
        }

        private char[] addDot(char[] cArr) {
            char[] cArr2 = new char[cArr.length + 1];
            System.arraycopy((Object) cArr, 0, (Object) cArr2, 0, cArr.length);
            cArr2[cArr2.length - 1] = '.';
            return cArr2;
        }

        private char[] trailingZeros(char[] cArr, int i) {
            if (i <= 0) {
                return cArr;
            }
            char[] cArr2 = new char[cArr.length + i];
            System.arraycopy((Object) cArr, 0, (Object) cArr2, 0, cArr.length);
            for (int length = cArr.length; length < cArr2.length; length++) {
                cArr2[length] = '0';
            }
            return cArr2;
        }

        private void print(Calendar calendar, char c, Locale locale) throws IOException {
            StringBuilder sb = new StringBuilder();
            print(sb, calendar, c, locale);
            String strJustify = justify(sb.toString());
            if (this.f.contains(Flags.UPPERCASE)) {
                strJustify = strJustify.toUpperCase();
            }
            Formatter.this.a.append(strJustify);
        }

        private Appendable print(StringBuilder sb, Calendar calendar, char c, Locale locale) throws IOException {
            int i;
            int i2;
            Flags flags;
            Flags flags2;
            StringBuilder sb2 = sb == null ? new StringBuilder() : sb;
            switch (c) {
                case 'A':
                case 'a':
                    int i3 = calendar.get(7);
                    DateFormatSymbols dateFormatSymbols = DateFormatSymbols.getInstance(locale == null ? Locale.US : locale);
                    if (c == 'A') {
                        sb2.append(dateFormatSymbols.getWeekdays()[i3]);
                    } else {
                        sb2.append(dateFormatSymbols.getShortWeekdays()[i3]);
                    }
                    return sb2;
                case 'B':
                case 'b':
                case 'h':
                    int i4 = calendar.get(2);
                    DateFormatSymbols dateFormatSymbols2 = DateFormatSymbols.getInstance(locale == null ? Locale.US : locale);
                    if (c == 'B') {
                        sb2.append(dateFormatSymbols2.getMonths()[i4]);
                    } else {
                        sb2.append(dateFormatSymbols2.getShortMonths()[i4]);
                    }
                    return sb2;
                case 'C':
                case 'Y':
                case 'y':
                    int i5 = calendar.get(1);
                    if (c == 'C') {
                        i5 /= 100;
                    } else {
                        if (c == 'Y') {
                            i = 4;
                            sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, i5, Flags.ZERO_PAD, i, locale));
                            return sb2;
                        }
                        if (c == 'y') {
                            i5 %= 100;
                        }
                    }
                    i = 2;
                    sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, i5, Flags.ZERO_PAD, i, locale));
                    return sb2;
                case 'D':
                    print(sb2, calendar, 'm', locale).append('/');
                    print(sb2, calendar, 'd', locale).append('/');
                    print(sb2, calendar, 'y', locale);
                    return sb2;
                case 'E':
                case 'G':
                case 'J':
                case 'K':
                case 'O':
                case 'P':
                case 'U':
                case 'V':
                case 'W':
                case 'X':
                case Types.DATE:
                case Types.TIME:
                case Types.TIMESTAMP:
                case '^':
                case '_':
                case '`':
                case 'f':
                case 'g':
                case 'i':
                case 'n':
                case 'o':
                case 'q':
                case 't':
                case 'u':
                case 'v':
                case 'w':
                case 'x':
                default:
                    return sb2;
                case Types.DATALINK:
                    print(sb2, calendar, 'Y', locale).append('-');
                    print(sb2, calendar, 'm', locale).append('-');
                    print(sb2, calendar, 'd', locale);
                    return sb2;
                case 'H':
                case 'I':
                case 'k':
                case 'l':
                    int i6 = calendar.get(11);
                    if (c == 'I' || c == 'l') {
                        if (i6 != 0) {
                            i2 = 12;
                            if (i6 != 12) {
                                i6 %= 12;
                            }
                        } else {
                            i2 = 12;
                        }
                        i6 = i2;
                    }
                    if (c == 'H' || c == 'I') {
                        flags = Flags.ZERO_PAD;
                    } else {
                        flags = Flags.NONE;
                    }
                    sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, i6, flags, 2, locale));
                    return sb2;
                case 'L':
                    sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, calendar.get(14), Flags.ZERO_PAD, 3, locale));
                    return sb2;
                case 'M':
                    sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, calendar.get(12), Flags.ZERO_PAD, 2, locale));
                    return sb2;
                case 'N':
                    sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, calendar.get(14) * 1000000, Flags.ZERO_PAD, 9, locale));
                    return sb2;
                case 'Q':
                    sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, calendar.getTimeInMillis(), Flags.NONE, this.width, locale));
                    return sb2;
                case 'R':
                case 'T':
                    print(sb2, calendar, 'H', locale).append(':');
                    print(sb2, calendar, 'M', locale);
                    if (c == 'T') {
                        sb2.append(':');
                        print(sb2, calendar, 'S', locale);
                    }
                    return sb2;
                case 'S':
                    sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, calendar.get(13), Flags.ZERO_PAD, 2, locale));
                    return sb2;
                case 'Z':
                    sb2.append(calendar.getTimeZone().getDisplayName(calendar.get(16) != 0, 0, locale == null ? Locale.US : locale));
                    return sb2;
                case 'c':
                    print(sb2, calendar, 'a', locale).append(' ');
                    print(sb2, calendar, 'b', locale).append(' ');
                    print(sb2, calendar, 'd', locale).append(' ');
                    print(sb2, calendar, 'T', locale).append(' ');
                    print(sb2, calendar, 'Z', locale).append(' ');
                    print(sb2, calendar, 'Y', locale);
                    return sb2;
                case 'd':
                case 'e':
                    int i7 = calendar.get(5);
                    if (c == 'd') {
                        flags2 = Flags.ZERO_PAD;
                    } else {
                        flags2 = Flags.NONE;
                    }
                    sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, i7, flags2, 2, locale));
                    return sb2;
                case 'j':
                    sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, calendar.get(6), Flags.ZERO_PAD, 3, locale));
                    return sb2;
                case 'm':
                    sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, calendar.get(2) + 1, Flags.ZERO_PAD, 2, locale));
                    return sb2;
                case 'p':
                    String[] amPmStrings = {"AM", "PM"};
                    if (locale != null && locale != Locale.US) {
                        amPmStrings = DateFormatSymbols.getInstance(locale).getAmPmStrings();
                    }
                    sb2.append(amPmStrings[calendar.get(9)].toLowerCase(locale != null ? locale : Locale.US));
                    return sb2;
                case 'r':
                    print(sb2, calendar, 'I', locale).append(':');
                    print(sb2, calendar, 'M', locale).append(':');
                    print(sb2, calendar, 'S', locale).append(' ');
                    StringBuilder sb3 = new StringBuilder();
                    print(sb3, calendar, 'p', locale);
                    sb2.append(sb3.toString().toUpperCase(locale != null ? locale : Locale.US));
                    return sb2;
                case 's':
                    sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, calendar.getTimeInMillis() / 1000, Flags.NONE, this.width, locale));
                    return sb2;
                case 'z':
                    int i8 = calendar.get(15) + calendar.get(16);
                    boolean z = i8 < 0;
                    sb2.append(z ? '-' : '+');
                    if (z) {
                        i8 = -i8;
                    }
                    int i9 = i8 / 60000;
                    sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, ((i9 / 60) * 100) + (i9 % 60), Flags.ZERO_PAD, 4, locale));
                    return sb2;
            }
        }

        private void print(TemporalAccessor temporalAccessor, char c, Locale locale) throws IOException {
            StringBuilder sb = new StringBuilder();
            print(sb, temporalAccessor, c, locale);
            String strJustify = justify(sb.toString());
            if (this.f.contains(Flags.UPPERCASE)) {
                strJustify = strJustify.toUpperCase();
            }
            Formatter.this.a.append(strJustify);
        }

        private Appendable print(StringBuilder sb, TemporalAccessor temporalAccessor, char c, Locale locale) throws IOException {
            int i;
            Flags flags;
            StringBuilder sb2 = sb == null ? new StringBuilder() : sb;
            char c2 = '-';
            try {
                switch (c) {
                    case 'A':
                    case 'a':
                        int i2 = (temporalAccessor.get(ChronoField.DAY_OF_WEEK) % 7) + 1;
                        DateFormatSymbols dateFormatSymbols = DateFormatSymbols.getInstance(locale == null ? Locale.US : locale);
                        if (c == 'A') {
                            sb2.append(dateFormatSymbols.getWeekdays()[i2]);
                        } else {
                            sb2.append(dateFormatSymbols.getShortWeekdays()[i2]);
                        }
                        return sb2;
                    case 'B':
                    case 'b':
                    case 'h':
                        int i3 = temporalAccessor.get(ChronoField.MONTH_OF_YEAR) - 1;
                        DateFormatSymbols dateFormatSymbols2 = DateFormatSymbols.getInstance(locale == null ? Locale.US : locale);
                        if (c == 'B') {
                            sb2.append(dateFormatSymbols2.getMonths()[i3]);
                        } else {
                            sb2.append(dateFormatSymbols2.getShortMonths()[i3]);
                        }
                        return sb2;
                    case 'C':
                    case 'Y':
                    case 'y':
                        int i4 = temporalAccessor.get(ChronoField.YEAR_OF_ERA);
                        if (c == 'C') {
                            i4 /= 100;
                        } else {
                            if (c == 'Y') {
                                i = 4;
                                sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, i4, Flags.ZERO_PAD, i, locale));
                                return sb2;
                            }
                            if (c == 'y') {
                                i4 %= 100;
                            }
                        }
                        i = 2;
                        sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, i4, Flags.ZERO_PAD, i, locale));
                        return sb2;
                    case 'D':
                        print(sb2, temporalAccessor, 'm', locale).append('/');
                        print(sb2, temporalAccessor, 'd', locale).append('/');
                        print(sb2, temporalAccessor, 'y', locale);
                        return sb2;
                    case 'E':
                    case 'G':
                    case 'J':
                    case 'K':
                    case 'O':
                    case 'P':
                    case 'U':
                    case 'V':
                    case 'W':
                    case 'X':
                    case Types.DATE:
                    case Types.TIME:
                    case Types.TIMESTAMP:
                    case '^':
                    case '_':
                    case '`':
                    case 'f':
                    case 'g':
                    case 'i':
                    case 'n':
                    case 'o':
                    case 'q':
                    case 't':
                    case 'u':
                    case 'v':
                    case 'w':
                    case 'x':
                    default:
                        return sb2;
                    case Types.DATALINK:
                        print(sb2, temporalAccessor, 'Y', locale).append('-');
                        print(sb2, temporalAccessor, 'm', locale).append('-');
                        print(sb2, temporalAccessor, 'd', locale);
                        return sb2;
                    case 'H':
                        sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, temporalAccessor.get(ChronoField.HOUR_OF_DAY), Flags.ZERO_PAD, 2, locale));
                        return sb2;
                    case 'I':
                        sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, temporalAccessor.get(ChronoField.CLOCK_HOUR_OF_AMPM), Flags.ZERO_PAD, 2, locale));
                        return sb2;
                    case 'L':
                        sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, temporalAccessor.get(ChronoField.MILLI_OF_SECOND), Flags.ZERO_PAD, 3, locale));
                        return sb2;
                    case 'M':
                        sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, temporalAccessor.get(ChronoField.MINUTE_OF_HOUR), Flags.ZERO_PAD, 2, locale));
                        return sb2;
                    case 'N':
                        sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, temporalAccessor.get(ChronoField.MILLI_OF_SECOND) * 1000000, Flags.ZERO_PAD, 9, locale));
                        return sb2;
                    case 'Q':
                        sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, temporalAccessor.getLong(ChronoField.MILLI_OF_SECOND) + (temporalAccessor.getLong(ChronoField.INSTANT_SECONDS) * 1000), Flags.NONE, this.width, locale));
                        return sb2;
                    case 'R':
                    case 'T':
                        print(sb2, temporalAccessor, 'H', locale).append(':');
                        print(sb2, temporalAccessor, 'M', locale);
                        if (c == 'T') {
                            sb2.append(':');
                            print(sb2, temporalAccessor, 'S', locale);
                        }
                        return sb2;
                    case 'S':
                        sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, temporalAccessor.get(ChronoField.SECOND_OF_MINUTE), Flags.ZERO_PAD, 2, locale));
                        return sb2;
                    case 'Z':
                        ZoneId zoneId = (ZoneId) temporalAccessor.query(TemporalQueries.zone());
                        if (zoneId == null) {
                            throw new IllegalFormatConversionException(c, temporalAccessor.getClass());
                        }
                        if (!(zoneId instanceof ZoneOffset) && temporalAccessor.isSupported(ChronoField.INSTANT_SECONDS)) {
                            sb2.append(TimeZone.getTimeZone(zoneId.getId()).getDisplayName(zoneId.getRules().isDaylightSavings(Instant.from(temporalAccessor)), 0, locale == null ? Locale.US : locale));
                        } else {
                            sb2.append(zoneId.getId());
                        }
                        return sb2;
                    case 'c':
                        print(sb2, temporalAccessor, 'a', locale).append(' ');
                        print(sb2, temporalAccessor, 'b', locale).append(' ');
                        print(sb2, temporalAccessor, 'd', locale).append(' ');
                        print(sb2, temporalAccessor, 'T', locale).append(' ');
                        print(sb2, temporalAccessor, 'Z', locale).append(' ');
                        print(sb2, temporalAccessor, 'Y', locale);
                        return sb2;
                    case 'd':
                    case 'e':
                        int i5 = temporalAccessor.get(ChronoField.DAY_OF_MONTH);
                        if (c == 'd') {
                            flags = Flags.ZERO_PAD;
                        } else {
                            flags = Flags.NONE;
                        }
                        sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, i5, flags, 2, locale));
                        return sb2;
                    case 'j':
                        sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, temporalAccessor.get(ChronoField.DAY_OF_YEAR), Flags.ZERO_PAD, 3, locale));
                        return sb2;
                    case 'k':
                        sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, temporalAccessor.get(ChronoField.HOUR_OF_DAY), Flags.NONE, 2, locale));
                        return sb2;
                    case 'l':
                        sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, temporalAccessor.get(ChronoField.CLOCK_HOUR_OF_AMPM), Flags.NONE, 2, locale));
                        return sb2;
                    case 'm':
                        sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, temporalAccessor.get(ChronoField.MONTH_OF_YEAR), Flags.ZERO_PAD, 2, locale));
                        return sb2;
                    case 'p':
                        String[] amPmStrings = {"AM", "PM"};
                        if (locale != null && locale != Locale.US) {
                            amPmStrings = DateFormatSymbols.getInstance(locale).getAmPmStrings();
                        }
                        sb2.append(amPmStrings[temporalAccessor.get(ChronoField.AMPM_OF_DAY)].toLowerCase(locale != null ? locale : Locale.US));
                        return sb2;
                    case 'r':
                        print(sb2, temporalAccessor, 'I', locale).append(':');
                        print(sb2, temporalAccessor, 'M', locale).append(':');
                        print(sb2, temporalAccessor, 'S', locale).append(' ');
                        StringBuilder sb3 = new StringBuilder();
                        print(sb3, temporalAccessor, 'p', locale);
                        sb2.append(sb3.toString().toUpperCase(locale != null ? locale : Locale.US));
                        return sb2;
                    case 's':
                        sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, temporalAccessor.getLong(ChronoField.INSTANT_SECONDS), Flags.NONE, this.width, locale));
                        return sb2;
                    case 'z':
                        int i6 = temporalAccessor.get(ChronoField.OFFSET_SECONDS);
                        boolean z = i6 < 0;
                        if (!z) {
                            c2 = '+';
                        }
                        sb2.append(c2);
                        if (z) {
                            i6 = -i6;
                        }
                        int i7 = i6 / 60;
                        sb2.append((CharSequence) localizedMagnitude((StringBuilder) null, ((i7 / 60) * 100) + (i7 % 60), Flags.ZERO_PAD, 4, locale));
                        return sb2;
                }
            } catch (DateTimeException e) {
                throw new IllegalFormatConversionException(c, temporalAccessor.getClass());
            }
        }

        private void failMismatch(Flags flags, char c) {
            throw new FormatFlagsConversionMismatchException(flags.toString(), c);
        }

        private void failConversion(char c, Object obj) {
            throw new IllegalFormatConversionException(c, obj.getClass());
        }

        private char getZero(Locale locale) {
            if (locale == null || locale.equals(Formatter.this.locale())) {
                return Formatter.this.zero;
            }
            return DecimalFormatSymbols.getInstance(locale).getZeroDigit();
        }

        private StringBuilder localizedMagnitude(StringBuilder sb, long j, Flags flags, int i, Locale locale) {
            return localizedMagnitude(sb, Long.toString(j, 10).toCharArray(), flags, i, locale);
        }

        private StringBuilder localizedMagnitude(StringBuilder sb, char[] cArr, Flags flags, int i, Locale locale) {
            char decimalSeparator;
            char groupingSeparator;
            int groupingSize;
            StringBuilder sb2 = sb == null ? new StringBuilder() : sb;
            int length = sb2.length();
            char zero = getZero(locale);
            int length2 = cArr.length;
            int i2 = 0;
            while (true) {
                decimalSeparator = '.';
                if (i2 >= length2) {
                    i2 = length2;
                    break;
                }
                if (cArr[i2] == '.') {
                    break;
                }
                i2++;
            }
            if (i2 >= length2) {
                decimalSeparator = 0;
            } else if (locale != null && !locale.equals(Locale.US)) {
                decimalSeparator = DecimalFormatSymbols.getInstance(locale).getDecimalSeparator();
            }
            if (!flags.contains(Flags.GROUP)) {
                groupingSeparator = 0;
                groupingSize = -1;
            } else if (locale == null || locale.equals(Locale.US)) {
                groupingSize = 3;
                groupingSeparator = ',';
            } else {
                groupingSeparator = DecimalFormatSymbols.getInstance(locale).getGroupingSeparator();
                DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getIntegerInstance(locale);
                groupingSize = decimalFormat.getGroupingSize();
                if (!decimalFormat.isGroupingUsed() || decimalFormat.getGroupingSize() == 0) {
                    groupingSeparator = 0;
                }
            }
            for (int i3 = 0; i3 < length2; i3++) {
                if (i3 == i2) {
                    sb2.append(decimalSeparator);
                    groupingSeparator = 0;
                } else {
                    sb2.append((char) ((cArr[i3] - '0') + zero));
                    if (groupingSeparator != 0 && i3 != i2 - 1 && (i2 - i3) % groupingSize == 1) {
                        sb2.append(groupingSeparator);
                    }
                }
            }
            int length3 = sb2.length();
            if (i != -1 && flags.contains(Flags.ZERO_PAD)) {
                for (int i4 = 0; i4 < i - length3; i4++) {
                    sb2.insert(length, zero);
                }
            }
            return sb2;
        }
    }

    private static class Flags {
        private int flags;
        static final Flags NONE = new Flags(0);
        static final Flags LEFT_JUSTIFY = new Flags(1);
        static final Flags UPPERCASE = new Flags(2);
        static final Flags ALTERNATE = new Flags(4);
        static final Flags PLUS = new Flags(8);
        static final Flags LEADING_SPACE = new Flags(16);
        static final Flags ZERO_PAD = new Flags(32);
        static final Flags GROUP = new Flags(64);
        static final Flags PARENTHESES = new Flags(128);
        static final Flags PREVIOUS = new Flags(256);

        private Flags(int i) {
            this.flags = i;
        }

        public int valueOf() {
            return this.flags;
        }

        public boolean contains(Flags flags) {
            return (this.flags & flags.valueOf()) == flags.valueOf();
        }

        public Flags dup() {
            return new Flags(this.flags);
        }

        private Flags add(Flags flags) {
            this.flags = flags.valueOf() | this.flags;
            return this;
        }

        public Flags remove(Flags flags) {
            this.flags = (~flags.valueOf()) & this.flags;
            return this;
        }

        public static Flags parse(String str) {
            char[] charArray = str.toCharArray();
            Flags flags = new Flags(0);
            for (char c : charArray) {
                Flags flags2 = parse(c);
                if (flags.contains(flags2)) {
                    throw new DuplicateFormatFlagsException(flags2.toString());
                }
                flags.add(flags2);
            }
            return flags;
        }

        private static Flags parse(char c) {
            if (c == ' ') {
                return LEADING_SPACE;
            }
            if (c == '#') {
                return ALTERNATE;
            }
            if (c == '(') {
                return PARENTHESES;
            }
            if (c == '0') {
                return ZERO_PAD;
            }
            if (c != '<') {
                switch (c) {
                    case '+':
                        return PLUS;
                    case ',':
                        return GROUP;
                    case '-':
                        return LEFT_JUSTIFY;
                    default:
                        throw new UnknownFormatFlagsException(String.valueOf(c));
                }
            }
            return PREVIOUS;
        }

        public static String toString(Flags flags) {
            return flags.toString();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (contains(LEFT_JUSTIFY)) {
                sb.append('-');
            }
            if (contains(UPPERCASE)) {
                sb.append('^');
            }
            if (contains(ALTERNATE)) {
                sb.append('#');
            }
            if (contains(PLUS)) {
                sb.append('+');
            }
            if (contains(LEADING_SPACE)) {
                sb.append(' ');
            }
            if (contains(ZERO_PAD)) {
                sb.append('0');
            }
            if (contains(GROUP)) {
                sb.append(',');
            }
            if (contains(PARENTHESES)) {
                sb.append('(');
            }
            if (contains(PREVIOUS)) {
                sb.append('<');
            }
            return sb.toString();
        }
    }

    private static class Conversion {
        static final char BOOLEAN = 'b';
        static final char BOOLEAN_UPPER = 'B';
        static final char CHARACTER = 'c';
        static final char CHARACTER_UPPER = 'C';
        static final char DATE_TIME = 't';
        static final char DATE_TIME_UPPER = 'T';
        static final char DECIMAL_FLOAT = 'f';
        static final char DECIMAL_INTEGER = 'd';
        static final char GENERAL = 'g';
        static final char GENERAL_UPPER = 'G';
        static final char HASHCODE = 'h';
        static final char HASHCODE_UPPER = 'H';
        static final char HEXADECIMAL_FLOAT = 'a';
        static final char HEXADECIMAL_FLOAT_UPPER = 'A';
        static final char HEXADECIMAL_INTEGER = 'x';
        static final char HEXADECIMAL_INTEGER_UPPER = 'X';
        static final char LINE_SEPARATOR = 'n';
        static final char OCTAL_INTEGER = 'o';
        static final char PERCENT_SIGN = '%';
        static final char SCIENTIFIC = 'e';
        static final char SCIENTIFIC_UPPER = 'E';
        static final char STRING = 's';
        static final char STRING_UPPER = 'S';

        private Conversion() {
        }

        static boolean isValid(char c) {
            return isGeneral(c) || isInteger(c) || isFloat(c) || isText(c) || c == 't' || isCharacter(c);
        }

        static boolean isGeneral(char c) {
            if (c == 'B' || c == 'H' || c == 'S' || c == 'b' || c == 'h' || c == 's') {
                return true;
            }
            return false;
        }

        static boolean isCharacter(char c) {
            if (c == 'C' || c == 'c') {
                return true;
            }
            return false;
        }

        static boolean isInteger(char c) {
            if (c == 'X' || c == 'd' || c == 'o' || c == 'x') {
                return true;
            }
            return false;
        }

        static boolean isFloat(char c) {
            if (c == 'A' || c == 'E' || c == 'G' || c == 'a') {
                return true;
            }
            switch (c) {
                case 'e':
                case 'f':
                case 'g':
                    return true;
                default:
                    return false;
            }
        }

        static boolean isText(char c) {
            if (c == '%' || c == 'n') {
                return true;
            }
            return false;
        }
    }

    private static class DateTime {
        static final char AM_PM = 'p';
        static final char CENTURY = 'C';
        static final char DATE = 'D';
        static final char DATE_TIME = 'c';
        static final char DAY_OF_MONTH = 'e';
        static final char DAY_OF_MONTH_0 = 'd';
        static final char DAY_OF_YEAR = 'j';
        static final char HOUR = 'l';
        static final char HOUR_0 = 'I';
        static final char HOUR_OF_DAY = 'k';
        static final char HOUR_OF_DAY_0 = 'H';
        static final char ISO_STANDARD_DATE = 'F';
        static final char MILLISECOND = 'L';
        static final char MILLISECOND_SINCE_EPOCH = 'Q';
        static final char MINUTE = 'M';
        static final char MONTH = 'm';
        static final char NAME_OF_DAY = 'A';
        static final char NAME_OF_DAY_ABBREV = 'a';
        static final char NAME_OF_MONTH = 'B';
        static final char NAME_OF_MONTH_ABBREV = 'b';
        static final char NAME_OF_MONTH_ABBREV_X = 'h';
        static final char NANOSECOND = 'N';
        static final char SECOND = 'S';
        static final char SECONDS_SINCE_EPOCH = 's';
        static final char TIME = 'T';
        static final char TIME_12_HOUR = 'r';
        static final char TIME_24_HOUR = 'R';
        static final char YEAR_2 = 'y';
        static final char YEAR_4 = 'Y';
        static final char ZONE = 'Z';
        static final char ZONE_NUMERIC = 'z';

        private DateTime() {
        }

        static boolean isValid(char c) {
            switch (c) {
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case Types.DATALINK:
                case 'H':
                case 'I':
                case 'L':
                case 'M':
                case 'N':
                case 'Q':
                case 'R':
                case 'S':
                case 'T':
                case 'Y':
                case 'Z':
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'h':
                case 'j':
                case 'k':
                case 'l':
                case 'm':
                case 'p':
                case 'r':
                case 's':
                case 'y':
                case 'z':
                    return true;
                case 'E':
                case 'G':
                case 'J':
                case 'K':
                case 'O':
                case 'P':
                case 'U':
                case 'V':
                case 'W':
                case 'X':
                case Types.DATE:
                case Types.TIME:
                case Types.TIMESTAMP:
                case '^':
                case '_':
                case '`':
                case 'f':
                case 'g':
                case 'i':
                case 'n':
                case 'o':
                case 'q':
                case 't':
                case 'u':
                case 'v':
                case 'w':
                case 'x':
                default:
                    return false;
            }
        }
    }
}
