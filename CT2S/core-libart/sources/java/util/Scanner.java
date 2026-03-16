package java.util;

import dalvik.bytecode.Opcodes;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libcore.io.IoUtils;

public final class Scanner implements Closeable, Iterator<String> {
    private static final int DEFAULT_RADIX = 10;
    private CharBuffer buffer;
    private int bufferLength;
    private Pattern cachedFloatPattern;
    private Pattern cachedIntegerPattern;
    private int cachedIntegerPatternRadix;
    private int cachedNextIndex;
    private Object cachedNextValue;
    private boolean closed;
    private int currentRadix;
    private DecimalFormat decimalFormat;
    private Pattern delimiter;
    private int findStartIndex;
    private Readable input;
    private boolean inputExhausted;
    private IOException lastIOException;
    private Locale locale;
    private boolean matchSuccessful;
    private Matcher matcher;
    private int preStartIndex;
    private static final Pattern DEFAULT_DELIMITER = Pattern.compile("\\p{javaWhitespace}+");
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("true|false", 2);
    private static final String NL = "\n|\r\n|\r|\u0085|\u2028|\u2029";
    private static final Pattern LINE_TERMINATOR = Pattern.compile(NL);
    private static final Pattern MULTI_LINE_TERMINATOR = Pattern.compile("(\n|\r\n|\r|\u0085|\u2028|\u2029)+");
    private static final Pattern LINE_PATTERN = Pattern.compile(".*(\n|\r\n|\r|\u0085|\u2028|\u2029)|.+$");
    private static final Pattern ANY_PATTERN = Pattern.compile("(?s).*");

    public Scanner(File src) throws FileNotFoundException {
        this(src, Charset.defaultCharset().name());
    }

    public Scanner(File src, String charsetName) throws FileNotFoundException {
        this.buffer = CharBuffer.allocate(1024);
        this.delimiter = DEFAULT_DELIMITER;
        this.currentRadix = 10;
        this.locale = Locale.getDefault();
        this.findStartIndex = 0;
        this.preStartIndex = this.findStartIndex;
        this.bufferLength = 0;
        this.closed = false;
        this.matchSuccessful = false;
        this.inputExhausted = false;
        this.cachedNextValue = null;
        this.cachedNextIndex = -1;
        this.cachedFloatPattern = null;
        this.cachedIntegerPatternRadix = -1;
        this.cachedIntegerPattern = null;
        if (src == null) {
            throw new NullPointerException("src == null");
        }
        FileInputStream fis = new FileInputStream(src);
        if (charsetName == null) {
            throw new IllegalArgumentException("charsetName == null");
        }
        try {
            InputStreamReader streamReader = new InputStreamReader(fis, charsetName);
            initialize(streamReader);
        } catch (UnsupportedEncodingException e) {
            IoUtils.closeQuietly(fis);
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public Scanner(String src) {
        this.buffer = CharBuffer.allocate(1024);
        this.delimiter = DEFAULT_DELIMITER;
        this.currentRadix = 10;
        this.locale = Locale.getDefault();
        this.findStartIndex = 0;
        this.preStartIndex = this.findStartIndex;
        this.bufferLength = 0;
        this.closed = false;
        this.matchSuccessful = false;
        this.inputExhausted = false;
        this.cachedNextValue = null;
        this.cachedNextIndex = -1;
        this.cachedFloatPattern = null;
        this.cachedIntegerPatternRadix = -1;
        this.cachedIntegerPattern = null;
        initialize(new StringReader(src));
    }

    public Scanner(InputStream src) {
        this(src, Charset.defaultCharset().name());
    }

    public Scanner(InputStream src, String charsetName) {
        this.buffer = CharBuffer.allocate(1024);
        this.delimiter = DEFAULT_DELIMITER;
        this.currentRadix = 10;
        this.locale = Locale.getDefault();
        this.findStartIndex = 0;
        this.preStartIndex = this.findStartIndex;
        this.bufferLength = 0;
        this.closed = false;
        this.matchSuccessful = false;
        this.inputExhausted = false;
        this.cachedNextValue = null;
        this.cachedNextIndex = -1;
        this.cachedFloatPattern = null;
        this.cachedIntegerPatternRadix = -1;
        this.cachedIntegerPattern = null;
        if (src == null) {
            throw new NullPointerException("src == null");
        }
        try {
            InputStreamReader streamReader = new InputStreamReader(src, charsetName);
            initialize(streamReader);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public Scanner(Readable src) {
        this.buffer = CharBuffer.allocate(1024);
        this.delimiter = DEFAULT_DELIMITER;
        this.currentRadix = 10;
        this.locale = Locale.getDefault();
        this.findStartIndex = 0;
        this.preStartIndex = this.findStartIndex;
        this.bufferLength = 0;
        this.closed = false;
        this.matchSuccessful = false;
        this.inputExhausted = false;
        this.cachedNextValue = null;
        this.cachedNextIndex = -1;
        this.cachedFloatPattern = null;
        this.cachedIntegerPatternRadix = -1;
        this.cachedIntegerPattern = null;
        if (src == null) {
            throw new NullPointerException("src == null");
        }
        initialize(src);
    }

    public Scanner(ReadableByteChannel src) {
        this(src, Charset.defaultCharset().name());
    }

    public Scanner(ReadableByteChannel src, String charsetName) {
        this.buffer = CharBuffer.allocate(1024);
        this.delimiter = DEFAULT_DELIMITER;
        this.currentRadix = 10;
        this.locale = Locale.getDefault();
        this.findStartIndex = 0;
        this.preStartIndex = this.findStartIndex;
        this.bufferLength = 0;
        this.closed = false;
        this.matchSuccessful = false;
        this.inputExhausted = false;
        this.cachedNextValue = null;
        this.cachedNextIndex = -1;
        this.cachedFloatPattern = null;
        this.cachedIntegerPatternRadix = -1;
        this.cachedIntegerPattern = null;
        if (src == null) {
            throw new NullPointerException("src == null");
        }
        if (charsetName == null) {
            throw new IllegalArgumentException("charsetName == null");
        }
        initialize(Channels.newReader(src, charsetName));
    }

    private void initialize(Readable input) {
        this.input = input;
        this.matcher = this.delimiter.matcher("");
        this.matcher.useTransparentBounds(true);
        this.matcher.useAnchoringBounds(false);
    }

    @Override
    public void close() {
        if (!this.closed) {
            if (this.input instanceof Closeable) {
                try {
                    ((Closeable) this.input).close();
                } catch (IOException e) {
                    this.lastIOException = e;
                }
            }
            this.closed = true;
        }
    }

    public Pattern delimiter() {
        return this.delimiter;
    }

    public String findInLine(Pattern pattern) {
        checkOpen();
        checkNotNull(pattern);
        int horizonLineSeparator = 0;
        this.matcher.usePattern(MULTI_LINE_TERMINATOR);
        this.matcher.region(this.findStartIndex, this.bufferLength);
        boolean findComplete = false;
        int terminatorLength = 0;
        while (!findComplete) {
            if (this.matcher.find()) {
                horizonLineSeparator = this.matcher.start();
                terminatorLength = this.matcher.end() - this.matcher.start();
                findComplete = true;
            } else if (!this.inputExhausted) {
                readMore();
                resetMatcher();
            } else {
                horizonLineSeparator = this.bufferLength;
                findComplete = true;
            }
        }
        this.matcher.usePattern(pattern);
        int oldLimit = this.buffer.limit();
        this.buffer.limit(horizonLineSeparator + terminatorLength);
        this.matcher.region(this.findStartIndex, horizonLineSeparator + terminatorLength);
        if (this.matcher.find()) {
            this.findStartIndex = this.matcher.end();
            if (horizonLineSeparator == this.matcher.end()) {
                this.findStartIndex += terminatorLength;
            }
            if (horizonLineSeparator != this.bufferLength && horizonLineSeparator + terminatorLength == this.matcher.end()) {
                this.buffer.limit(oldLimit);
                this.matchSuccessful = false;
                return null;
            }
            this.matchSuccessful = true;
            this.buffer.limit(oldLimit);
            return this.matcher.group();
        }
        this.buffer.limit(oldLimit);
        this.matchSuccessful = false;
        return null;
    }

    public String findInLine(String pattern) {
        return findInLine(Pattern.compile(pattern));
    }

    public String findWithinHorizon(Pattern pattern, int horizon) {
        checkOpen();
        checkNotNull(pattern);
        if (horizon < 0) {
            throw new IllegalArgumentException("horizon < 0");
        }
        this.matcher.usePattern(pattern);
        String result = null;
        int horizonEndIndex = horizon == 0 ? Integer.MAX_VALUE : this.findStartIndex + horizon;
        while (true) {
            int findEndIndex = Math.min(horizonEndIndex, this.bufferLength);
            boolean isHorizonInBuffer = horizonEndIndex <= this.bufferLength;
            this.matcher.region(this.findStartIndex, findEndIndex);
            if (this.matcher.find()) {
                if ((horizon == 0 && !this.matcher.hitEnd()) || isHorizonInBuffer || this.inputExhausted) {
                    break;
                }
                if (this.inputExhausted) {
                    readMore();
                    resetMatcher();
                }
            } else {
                if (isHorizonInBuffer || this.inputExhausted) {
                    break;
                }
                if (this.inputExhausted) {
                }
            }
        }
        if (result != null) {
            this.findStartIndex = this.matcher.end();
            this.matchSuccessful = true;
        } else {
            this.matchSuccessful = false;
        }
        return result;
    }

    public String findWithinHorizon(String pattern, int horizon) {
        return findWithinHorizon(Pattern.compile(pattern), horizon);
    }

    @Override
    public boolean hasNext() {
        return hasNext(ANY_PATTERN);
    }

    public boolean hasNext(Pattern pattern) {
        boolean hasNext = false;
        checkOpen();
        checkNotNull(pattern);
        this.matchSuccessful = false;
        prepareForScan();
        if (!setTokenRegion()) {
            recoverPreviousStatus();
        } else {
            this.matcher.usePattern(pattern);
            hasNext = false;
            if (this.matcher.matches()) {
                this.cachedNextIndex = this.findStartIndex;
                this.matchSuccessful = true;
                hasNext = true;
            }
            recoverPreviousStatus();
        }
        return hasNext;
    }

    public boolean hasNext(String pattern) {
        return hasNext(Pattern.compile(pattern));
    }

    public boolean hasNextBigDecimal() {
        Pattern floatPattern = getFloatPattern();
        if (!hasNext(floatPattern)) {
            return false;
        }
        String floatString = this.matcher.group();
        try {
            this.cachedNextValue = new BigDecimal(removeLocaleInfoFromFloat(floatString));
            return true;
        } catch (NumberFormatException e) {
            this.matchSuccessful = false;
            return false;
        }
    }

    public boolean hasNextBigInteger() {
        return hasNextBigInteger(this.currentRadix);
    }

    public boolean hasNextBigInteger(int radix) {
        Pattern integerPattern = getIntegerPattern(radix);
        if (!hasNext(integerPattern)) {
            return false;
        }
        String intString = this.matcher.group();
        try {
            this.cachedNextValue = new BigInteger(removeLocaleInfo(intString, Integer.TYPE), radix);
            return true;
        } catch (NumberFormatException e) {
            this.matchSuccessful = false;
            return false;
        }
    }

    public boolean hasNextBoolean() {
        return hasNext(BOOLEAN_PATTERN);
    }

    public boolean hasNextByte() {
        return hasNextByte(this.currentRadix);
    }

    public boolean hasNextByte(int radix) {
        Pattern integerPattern = getIntegerPattern(radix);
        if (!hasNext(integerPattern)) {
            return false;
        }
        String intString = this.matcher.group();
        try {
            this.cachedNextValue = Byte.valueOf(removeLocaleInfo(intString, Integer.TYPE), radix);
            return true;
        } catch (NumberFormatException e) {
            this.matchSuccessful = false;
            return false;
        }
    }

    public boolean hasNextDouble() {
        Pattern floatPattern = getFloatPattern();
        if (!hasNext(floatPattern)) {
            return false;
        }
        String floatString = this.matcher.group();
        try {
            this.cachedNextValue = Double.valueOf(removeLocaleInfoFromFloat(floatString));
            return true;
        } catch (NumberFormatException e) {
            this.matchSuccessful = false;
            return false;
        }
    }

    public boolean hasNextFloat() {
        Pattern floatPattern = getFloatPattern();
        if (!hasNext(floatPattern)) {
            return false;
        }
        String floatString = this.matcher.group();
        try {
            this.cachedNextValue = Float.valueOf(removeLocaleInfoFromFloat(floatString));
            return true;
        } catch (NumberFormatException e) {
            this.matchSuccessful = false;
            return false;
        }
    }

    public boolean hasNextInt() {
        return hasNextInt(this.currentRadix);
    }

    public boolean hasNextInt(int radix) {
        Pattern integerPattern = getIntegerPattern(radix);
        if (!hasNext(integerPattern)) {
            return false;
        }
        String intString = this.matcher.group();
        try {
            this.cachedNextValue = Integer.valueOf(removeLocaleInfo(intString, Integer.TYPE), radix);
            return true;
        } catch (NumberFormatException e) {
            this.matchSuccessful = false;
            return false;
        }
    }

    public boolean hasNextLine() {
        prepareForScan();
        String result = findWithinHorizon(LINE_PATTERN, 0);
        recoverPreviousStatus();
        return result != null;
    }

    public boolean hasNextLong() {
        return hasNextLong(this.currentRadix);
    }

    public boolean hasNextLong(int radix) {
        Pattern integerPattern = getIntegerPattern(radix);
        if (!hasNext(integerPattern)) {
            return false;
        }
        String intString = this.matcher.group();
        try {
            this.cachedNextValue = Long.valueOf(removeLocaleInfo(intString, Integer.TYPE), radix);
            return true;
        } catch (NumberFormatException e) {
            this.matchSuccessful = false;
            return false;
        }
    }

    public boolean hasNextShort() {
        return hasNextShort(this.currentRadix);
    }

    public boolean hasNextShort(int radix) {
        Pattern integerPattern = getIntegerPattern(radix);
        if (!hasNext(integerPattern)) {
            return false;
        }
        String intString = this.matcher.group();
        try {
            this.cachedNextValue = Short.valueOf(removeLocaleInfo(intString, Integer.TYPE), radix);
            return true;
        } catch (NumberFormatException e) {
            this.matchSuccessful = false;
            return false;
        }
    }

    public IOException ioException() {
        return this.lastIOException;
    }

    public Locale locale() {
        return this.locale;
    }

    private void setLocale(Locale locale) {
        this.locale = locale;
        this.decimalFormat = null;
        this.cachedFloatPattern = null;
        this.cachedIntegerPatternRadix = -1;
        this.cachedIntegerPattern = null;
    }

    public MatchResult match() {
        if (!this.matchSuccessful) {
            throw new IllegalStateException();
        }
        return this.matcher.toMatchResult();
    }

    @Override
    public String next() {
        return next(ANY_PATTERN);
    }

    public String next(Pattern pattern) {
        checkOpen();
        checkNotNull(pattern);
        this.matchSuccessful = false;
        prepareForScan();
        if (!setTokenRegion()) {
            recoverPreviousStatus();
            throw new NoSuchElementException();
        }
        this.matcher.usePattern(pattern);
        if (!this.matcher.matches()) {
            recoverPreviousStatus();
            throw new InputMismatchException();
        }
        this.matchSuccessful = true;
        return this.matcher.group();
    }

    public String next(String pattern) {
        return next(Pattern.compile(pattern));
    }

    public BigDecimal nextBigDecimal() {
        checkOpen();
        Object obj = this.cachedNextValue;
        this.cachedNextValue = null;
        if (obj instanceof BigDecimal) {
            this.findStartIndex = this.cachedNextIndex;
            return (BigDecimal) obj;
        }
        Pattern floatPattern = getFloatPattern();
        String floatString = next(floatPattern);
        try {
            BigDecimal bigDecimalValue = new BigDecimal(removeLocaleInfoFromFloat(floatString));
            return bigDecimalValue;
        } catch (NumberFormatException e) {
            this.matchSuccessful = false;
            recoverPreviousStatus();
            throw new InputMismatchException();
        }
    }

    public BigInteger nextBigInteger() {
        return nextBigInteger(this.currentRadix);
    }

    public BigInteger nextBigInteger(int radix) {
        checkOpen();
        Object obj = this.cachedNextValue;
        this.cachedNextValue = null;
        if (obj instanceof BigInteger) {
            this.findStartIndex = this.cachedNextIndex;
            return (BigInteger) obj;
        }
        Pattern integerPattern = getIntegerPattern(radix);
        String intString = next(integerPattern);
        try {
            BigInteger bigIntegerValue = new BigInteger(removeLocaleInfo(intString, Integer.TYPE), radix);
            return bigIntegerValue;
        } catch (NumberFormatException e) {
            this.matchSuccessful = false;
            recoverPreviousStatus();
            throw new InputMismatchException();
        }
    }

    public boolean nextBoolean() {
        return Boolean.parseBoolean(next(BOOLEAN_PATTERN));
    }

    public byte nextByte() {
        return nextByte(this.currentRadix);
    }

    public byte nextByte(int radix) {
        checkOpen();
        Object obj = this.cachedNextValue;
        this.cachedNextValue = null;
        if (obj instanceof Byte) {
            this.findStartIndex = this.cachedNextIndex;
            return ((Byte) obj).byteValue();
        }
        Pattern integerPattern = getIntegerPattern(radix);
        String intString = next(integerPattern);
        try {
            byte byteValue = Byte.parseByte(removeLocaleInfo(intString, Integer.TYPE), radix);
            return byteValue;
        } catch (NumberFormatException e) {
            this.matchSuccessful = false;
            recoverPreviousStatus();
            throw new InputMismatchException();
        }
    }

    public double nextDouble() {
        checkOpen();
        Object obj = this.cachedNextValue;
        this.cachedNextValue = null;
        if (obj instanceof Double) {
            this.findStartIndex = this.cachedNextIndex;
            return ((Double) obj).doubleValue();
        }
        Pattern floatPattern = getFloatPattern();
        String floatString = next(floatPattern);
        try {
            return Double.parseDouble(removeLocaleInfoFromFloat(floatString));
        } catch (NumberFormatException e) {
            this.matchSuccessful = false;
            recoverPreviousStatus();
            throw new InputMismatchException();
        }
    }

    public float nextFloat() {
        checkOpen();
        Object obj = this.cachedNextValue;
        this.cachedNextValue = null;
        if (obj instanceof Float) {
            this.findStartIndex = this.cachedNextIndex;
            return ((Float) obj).floatValue();
        }
        Pattern floatPattern = getFloatPattern();
        String floatString = next(floatPattern);
        try {
            return Float.parseFloat(removeLocaleInfoFromFloat(floatString));
        } catch (NumberFormatException e) {
            this.matchSuccessful = false;
            recoverPreviousStatus();
            throw new InputMismatchException();
        }
    }

    public int nextInt() {
        return nextInt(this.currentRadix);
    }

    public int nextInt(int radix) {
        checkOpen();
        Object obj = this.cachedNextValue;
        this.cachedNextValue = null;
        if (obj instanceof Integer) {
            this.findStartIndex = this.cachedNextIndex;
            return ((Integer) obj).intValue();
        }
        Pattern integerPattern = getIntegerPattern(radix);
        String intString = next(integerPattern);
        try {
            return Integer.parseInt(removeLocaleInfo(intString, Integer.TYPE), radix);
        } catch (NumberFormatException e) {
            this.matchSuccessful = false;
            recoverPreviousStatus();
            throw new InputMismatchException();
        }
    }

    public String nextLine() {
        checkOpen();
        this.matcher.usePattern(LINE_PATTERN);
        this.matcher.region(this.findStartIndex, this.bufferLength);
        while (true) {
            if (this.matcher.find()) {
                if (this.inputExhausted || this.matcher.end() != this.bufferLength || this.bufferLength < this.buffer.capacity()) {
                    break;
                }
            } else if (this.inputExhausted) {
                this.matchSuccessful = false;
                throw new NoSuchElementException();
            }
            if (!this.inputExhausted) {
                readMore();
                resetMatcher();
            }
        }
    }

    public long nextLong() {
        return nextLong(this.currentRadix);
    }

    public long nextLong(int radix) {
        checkOpen();
        Object obj = this.cachedNextValue;
        this.cachedNextValue = null;
        if (obj instanceof Long) {
            this.findStartIndex = this.cachedNextIndex;
            return ((Long) obj).longValue();
        }
        Pattern integerPattern = getIntegerPattern(radix);
        String intString = next(integerPattern);
        try {
            return Long.parseLong(removeLocaleInfo(intString, Integer.TYPE), radix);
        } catch (NumberFormatException e) {
            this.matchSuccessful = false;
            recoverPreviousStatus();
            throw new InputMismatchException();
        }
    }

    public short nextShort() {
        return nextShort(this.currentRadix);
    }

    public short nextShort(int radix) {
        checkOpen();
        Object obj = this.cachedNextValue;
        this.cachedNextValue = null;
        if (obj instanceof Short) {
            this.findStartIndex = this.cachedNextIndex;
            return ((Short) obj).shortValue();
        }
        Pattern integerPattern = getIntegerPattern(radix);
        String intString = next(integerPattern);
        try {
            return Short.parseShort(removeLocaleInfo(intString, Integer.TYPE), radix);
        } catch (NumberFormatException e) {
            this.matchSuccessful = false;
            recoverPreviousStatus();
            throw new InputMismatchException();
        }
    }

    public int radix() {
        return this.currentRadix;
    }

    public Scanner skip(Pattern pattern) {
        checkOpen();
        checkNotNull(pattern);
        this.matcher.usePattern(pattern);
        this.matcher.region(this.findStartIndex, this.bufferLength);
        while (true) {
            if (this.matcher.lookingAt()) {
                boolean matchInBuffer = this.matcher.end() < this.bufferLength || (this.matcher.end() == this.bufferLength && this.inputExhausted);
                if (matchInBuffer) {
                    this.matchSuccessful = true;
                    this.findStartIndex = this.matcher.end();
                    return this;
                }
            } else if (this.inputExhausted) {
                this.matchSuccessful = false;
                throw new NoSuchElementException();
            }
            if (!this.inputExhausted) {
                readMore();
                resetMatcher();
            }
        }
    }

    public Scanner skip(String pattern) {
        return skip(Pattern.compile(pattern));
    }

    public String toString() {
        return getClass().getName() + "[delimiter=" + this.delimiter + ",findStartIndex=" + this.findStartIndex + ",matchSuccessful=" + this.matchSuccessful + ",closed=" + this.closed + "]";
    }

    public Scanner useDelimiter(Pattern pattern) {
        this.delimiter = pattern;
        return this;
    }

    public Scanner useDelimiter(String pattern) {
        return useDelimiter(Pattern.compile(pattern));
    }

    public Scanner useLocale(Locale l) {
        if (l == null) {
            throw new NullPointerException("l == null");
        }
        setLocale(l);
        return this;
    }

    public Scanner useRadix(int radix) {
        checkRadix(radix);
        this.currentRadix = radix;
        return this;
    }

    private void checkRadix(int radix) {
        if (radix < 2 || radix > 36) {
            throw new IllegalArgumentException("Invalid radix: " + radix);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private void checkOpen() {
        if (this.closed) {
            throw new IllegalStateException();
        }
    }

    private void checkNotNull(Pattern pattern) {
        if (pattern == null) {
            throw new NullPointerException("pattern == null");
        }
    }

    private void resetMatcher() {
        this.matcher.reset(this.buffer);
        this.matcher.region(this.findStartIndex, this.bufferLength);
    }

    private void prepareForScan() {
        if (this.findStartIndex >= this.buffer.capacity() / 2) {
            int oldPosition = this.buffer.position();
            this.buffer.position(this.findStartIndex);
            this.buffer.compact();
            this.buffer.position(oldPosition);
            this.bufferLength -= this.findStartIndex;
            this.findStartIndex = 0;
            this.preStartIndex = -1;
            resetMatcher();
        }
        this.preStartIndex = this.findStartIndex;
    }

    private void recoverPreviousStatus() {
        this.findStartIndex = this.preStartIndex;
    }

    private Pattern getIntegerPattern(int radix) {
        checkRadix(radix);
        if (this.decimalFormat == null) {
            this.decimalFormat = (DecimalFormat) NumberFormat.getInstance(this.locale);
        }
        if (this.cachedIntegerPatternRadix == radix) {
            return this.cachedIntegerPattern;
        }
        String ASCIIDigit = "0123456789abcdefghijklmnopqrstuvwxyz".substring(0, radix);
        String nonZeroASCIIDigit = "0123456789abcdefghijklmnopqrstuvwxyz".substring(1, radix);
        String digit = "((?i)[" + ASCIIDigit + "]|\\p{javaDigit})";
        String nonZeroDigit = "((?i)[" + nonZeroASCIIDigit + "]|([\\p{javaDigit}&&[^0]]))";
        String numeral = getNumeral(digit, nonZeroDigit);
        String regex = "(([-+]?(" + numeral + ")))|(" + addPositiveSign(numeral) + ")|(" + addNegativeSign(numeral) + ")";
        this.cachedIntegerPatternRadix = radix;
        this.cachedIntegerPattern = Pattern.compile(regex);
        return this.cachedIntegerPattern;
    }

    private Pattern getFloatPattern() {
        if (this.decimalFormat == null) {
            this.decimalFormat = (DecimalFormat) NumberFormat.getInstance(this.locale);
        }
        if (this.cachedFloatPattern != null) {
            return this.cachedFloatPattern;
        }
        DecimalFormatSymbols dfs = this.decimalFormat.getDecimalFormatSymbols();
        String numeral = getNumeral("([0-9]|(\\p{javaDigit}))", "[\\p{javaDigit}&&[^0]]");
        String decimalSeparator = "\\" + dfs.getDecimalSeparator();
        String decimalNumeral = "(" + numeral + "|" + numeral + decimalSeparator + "([0-9]|(\\p{javaDigit}))*+|" + decimalSeparator + "([0-9]|(\\p{javaDigit}))++)";
        String exponent = "([eE][+-]?([0-9]|(\\p{javaDigit}))+)?";
        String decimal = "(([-+]?" + decimalNumeral + "(" + exponent + "?))|(" + addPositiveSign(decimalNumeral) + "(" + exponent + "?))|(" + addNegativeSign(decimalNumeral) + "(" + exponent + "?)))";
        String localNaN = dfs.getNaN();
        String localeInfinity = dfs.getInfinity();
        String nonNumber = "(NaN|\\Q" + localNaN + "\\E|Infinity|\\Q" + localeInfinity + "\\E)";
        String signedNonNumber = "((([-+]?(" + nonNumber + ")))|(" + addPositiveSign(nonNumber) + ")|(" + addNegativeSign(nonNumber) + "))";
        this.cachedFloatPattern = Pattern.compile(decimal + "|([-+]?0[xX][0-9a-fA-F]*\\.[0-9a-fA-F]+([pP][-+]?[0-9]+)?)|" + signedNonNumber);
        return this.cachedFloatPattern;
    }

    private String getNumeral(String digit, String nonZeroDigit) {
        String groupSeparator = "\\" + this.decimalFormat.getDecimalFormatSymbols().getGroupingSeparator();
        String groupedNumeral = "(" + nonZeroDigit + digit + "?" + digit + "?(" + groupSeparator + digit + digit + digit + ")+)";
        return "((" + digit + "++)|" + groupedNumeral + ")";
    }

    private String addPositiveSign(String unsignedNumeral) {
        String positivePrefix = "";
        String positiveSuffix = "";
        if (!this.decimalFormat.getPositivePrefix().isEmpty()) {
            positivePrefix = "\\Q" + this.decimalFormat.getPositivePrefix() + "\\E";
        }
        if (!this.decimalFormat.getPositiveSuffix().isEmpty()) {
            positiveSuffix = "\\Q" + this.decimalFormat.getPositiveSuffix() + "\\E";
        }
        return positivePrefix + unsignedNumeral + positiveSuffix;
    }

    private String addNegativeSign(String unsignedNumeral) {
        String negativePrefix = "";
        String negativeSuffix = "";
        if (!this.decimalFormat.getNegativePrefix().isEmpty()) {
            negativePrefix = "\\Q" + this.decimalFormat.getNegativePrefix() + "\\E";
        }
        if (!this.decimalFormat.getNegativeSuffix().isEmpty()) {
            negativeSuffix = "\\Q" + this.decimalFormat.getNegativeSuffix() + "\\E";
        }
        return negativePrefix + unsignedNumeral + negativeSuffix;
    }

    private String removeLocaleInfoFromFloat(String floatString) {
        if (floatString.indexOf(Opcodes.OP_INVOKE_INTERFACE_RANGE) == -1 && floatString.indexOf(88) == -1) {
            int exponentIndex = floatString.indexOf(Opcodes.OP_SGET_CHAR);
            if (exponentIndex != -1 || (exponentIndex = floatString.indexOf(69)) != -1) {
                String decimalNumeralString = floatString.substring(0, exponentIndex);
                String exponentString = floatString.substring(exponentIndex + 1, floatString.length());
                return removeLocaleInfo(decimalNumeralString, Float.TYPE) + "e" + exponentString;
            }
            return removeLocaleInfo(floatString, Float.TYPE);
        }
        return floatString;
    }

    private String removeLocaleInfo(String token, Class<?> type) {
        DecimalFormatSymbols dfs = this.decimalFormat.getDecimalFormatSymbols();
        StringBuilder tokenBuilder = new StringBuilder(token);
        boolean negative = removeLocaleSign(tokenBuilder);
        String groupSeparator = String.valueOf(dfs.getGroupingSeparator());
        while (true) {
            int separatorIndex = tokenBuilder.indexOf(groupSeparator);
            if (separatorIndex == -1) {
                break;
            }
            tokenBuilder.delete(separatorIndex, separatorIndex + 1);
        }
        String decimalSeparator = String.valueOf(dfs.getDecimalSeparator());
        int separatorIndex2 = tokenBuilder.indexOf(decimalSeparator);
        StringBuilder result = new StringBuilder("");
        if (type == Integer.TYPE) {
            for (int i = 0; i < tokenBuilder.length(); i++) {
                if (Character.digit(tokenBuilder.charAt(i), 36) != -1) {
                    result.append(tokenBuilder.charAt(i));
                }
            }
        } else if (type == Float.TYPE) {
            if (tokenBuilder.toString().equals(dfs.getNaN())) {
                result.append("NaN");
            } else if (tokenBuilder.toString().equals(dfs.getInfinity())) {
                result.append("Infinity");
            } else {
                for (int i2 = 0; i2 < tokenBuilder.length(); i2++) {
                    if (Character.digit(tokenBuilder.charAt(i2), 10) != -1) {
                        result.append(Character.digit(tokenBuilder.charAt(i2), 10));
                    }
                }
            }
        } else {
            throw new AssertionError("Unsupported type: " + type);
        }
        if (result.length() == 0) {
            result = tokenBuilder;
        }
        if (separatorIndex2 != -1) {
            result.insert(separatorIndex2, ".");
        }
        if (negative) {
            result.insert(0, '-');
        }
        return result.toString();
    }

    private boolean removeLocaleSign(StringBuilder tokenBuilder) {
        String positivePrefix = this.decimalFormat.getPositivePrefix();
        String positiveSuffix = this.decimalFormat.getPositiveSuffix();
        String negativePrefix = this.decimalFormat.getNegativePrefix();
        String negativeSuffix = this.decimalFormat.getNegativeSuffix();
        if (tokenBuilder.indexOf("+") == 0) {
            tokenBuilder.delete(0, 1);
        }
        if (!positivePrefix.isEmpty() && tokenBuilder.indexOf(positivePrefix) == 0) {
            tokenBuilder.delete(0, positivePrefix.length());
        }
        if (!positiveSuffix.isEmpty() && tokenBuilder.indexOf(positiveSuffix) != -1) {
            tokenBuilder.delete(tokenBuilder.length() - positiveSuffix.length(), tokenBuilder.length());
        }
        boolean negative = false;
        if (tokenBuilder.indexOf("-") == 0) {
            tokenBuilder.delete(0, 1);
            negative = true;
        }
        if (!negativePrefix.isEmpty() && tokenBuilder.indexOf(negativePrefix) == 0) {
            tokenBuilder.delete(0, negativePrefix.length());
            negative = true;
        }
        if (!negativeSuffix.isEmpty() && tokenBuilder.indexOf(negativeSuffix) != -1) {
            tokenBuilder.delete(tokenBuilder.length() - negativeSuffix.length(), tokenBuilder.length());
            return true;
        }
        return negative;
    }

    private boolean setTokenRegion() {
        this.matcher.usePattern(this.delimiter);
        this.matcher.region(this.findStartIndex, this.bufferLength);
        int tokenStartIndex = findPreDelimiter();
        if (setHeadTokenRegion(tokenStartIndex)) {
            return true;
        }
        int tokenEndIndex = findDelimiterAfter();
        if (tokenEndIndex == -1) {
            if (this.findStartIndex == this.bufferLength) {
                return false;
            }
            tokenEndIndex = this.bufferLength;
            this.findStartIndex = this.bufferLength;
        }
        this.matcher.region(tokenStartIndex, tokenEndIndex);
        return true;
    }

    private int findPreDelimiter() {
        boolean findComplete = false;
        while (!findComplete) {
            if (this.matcher.find()) {
                findComplete = true;
                if (this.matcher.start() == this.findStartIndex && this.matcher.end() == this.bufferLength && !this.inputExhausted) {
                    readMore();
                    resetMatcher();
                    findComplete = false;
                }
            } else if (!this.inputExhausted) {
                readMore();
                resetMatcher();
            } else {
                return -1;
            }
        }
        int tokenStartIndex = this.matcher.end();
        this.findStartIndex = tokenStartIndex;
        return tokenStartIndex;
    }

    private boolean setHeadTokenRegion(int findIndex) {
        boolean setSuccess = false;
        if (findIndex == -1 && this.preStartIndex != this.bufferLength) {
            int tokenStartIndex = this.preStartIndex;
            int tokenEndIndex = this.bufferLength;
            this.findStartIndex = this.bufferLength;
            this.matcher.region(tokenStartIndex, tokenEndIndex);
            setSuccess = true;
        }
        if (findIndex != -1 && this.preStartIndex != this.matcher.start()) {
            int tokenStartIndex2 = this.preStartIndex;
            int tokenEndIndex2 = this.matcher.start();
            this.findStartIndex = this.matcher.start();
            this.matcher.region(tokenStartIndex2, tokenEndIndex2);
            return true;
        }
        return setSuccess;
    }

    private int findDelimiterAfter() {
        boolean findComplete = false;
        while (!findComplete) {
            if (this.matcher.find()) {
                findComplete = true;
                if (this.matcher.start() == this.findStartIndex && this.matcher.start() == this.matcher.end()) {
                    findComplete = false;
                }
            } else if (!this.inputExhausted) {
                readMore();
                resetMatcher();
            } else {
                return -1;
            }
        }
        int tokenEndIndex = this.matcher.start();
        this.findStartIndex = tokenEndIndex;
        return tokenEndIndex;
    }

    private void readMore() {
        int readCount;
        int oldPosition = this.buffer.position();
        int oldBufferLength = this.bufferLength;
        if (this.bufferLength >= this.buffer.capacity()) {
            expandBuffer();
        }
        try {
            this.buffer.limit(this.buffer.capacity());
            this.buffer.position(oldBufferLength);
            do {
                readCount = this.input.read(this.buffer);
            } while (readCount == 0);
        } catch (IOException e) {
            this.bufferLength = this.buffer.position();
            readCount = -1;
            this.lastIOException = e;
        }
        this.buffer.flip();
        this.buffer.position(oldPosition);
        if (readCount == -1) {
            this.inputExhausted = true;
        } else {
            this.bufferLength += readCount;
        }
    }

    private void expandBuffer() {
        int oldPosition = this.buffer.position();
        int oldCapacity = this.buffer.capacity();
        int oldLimit = this.buffer.limit();
        int newCapacity = oldCapacity * 2;
        char[] newBuffer = new char[newCapacity];
        System.arraycopy(this.buffer.array(), 0, newBuffer, 0, oldLimit);
        this.buffer = CharBuffer.wrap(newBuffer, 0, newCapacity);
        this.buffer.position(oldPosition);
        this.buffer.limit(oldLimit);
    }

    public Scanner reset() {
        this.delimiter = DEFAULT_DELIMITER;
        setLocale(Locale.getDefault());
        this.currentRadix = 10;
        return this;
    }
}
