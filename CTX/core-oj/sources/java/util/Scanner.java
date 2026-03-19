package java.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sun.misc.LRUCache;
import sun.util.locale.LanguageTag;

public final class Scanner implements Iterator<String>, Closeable {
    static final boolean $assertionsDisabled = false;
    private static final String BOOLEAN_PATTERN = "true|false";
    private static final int BUFFER_SIZE = 1024;
    private static final String LINE_PATTERN = ".*(\r\n|[\n\r\u2028\u2029\u0085])|.+$";
    private static final String LINE_SEPARATOR_PATTERN = "\r\n|[\n\r\u2028\u2029\u0085]";
    private static volatile Pattern boolPattern;
    private static volatile Pattern linePattern;
    private static volatile Pattern separatorPattern;
    private int SIMPLE_GROUP_INDEX;
    private CharBuffer buf;
    private boolean closed;
    private Pattern decimalPattern;
    private String decimalSeparator;
    private int defaultRadix;
    private Pattern delimPattern;
    private String digits;
    private Pattern floatPattern;
    private String groupSeparator;
    private Pattern hasNextPattern;
    private int hasNextPosition;
    private String hasNextResult;
    private String infinityString;
    private Pattern integerPattern;
    private IOException lastException;
    private Locale locale;
    private boolean matchValid;
    private Matcher matcher;
    private String nanString;
    private boolean needInput;
    private String negativePrefix;
    private String negativeSuffix;
    private String non0Digit;
    private LRUCache<String, Pattern> patternCache;
    private int position;
    private String positivePrefix;
    private String positiveSuffix;
    private int radix;
    private int savedScannerPosition;
    private boolean skipped;
    private Readable source;
    private boolean sourceClosed;
    private Object typeCache;
    private static Pattern WHITESPACE_PATTERN = Pattern.compile("\\p{javaWhitespace}+");
    private static Pattern FIND_ANY_PATTERN = Pattern.compile("(?s).*");
    private static Pattern NON_ASCII_DIGIT = Pattern.compile("[\\p{javaDigit}&&[^0-9]]");

    private static Pattern boolPattern() {
        Pattern pattern = boolPattern;
        if (pattern == null) {
            Pattern patternCompile = Pattern.compile(BOOLEAN_PATTERN, 2);
            boolPattern = patternCompile;
            return patternCompile;
        }
        return pattern;
    }

    private String buildIntegerPatternString() {
        String strSubstring = this.digits.substring(0, this.radix);
        String str = "((?i)[" + strSubstring + "]|\\p{javaDigit})";
        String str2 = "((" + str + "++)|" + ("(" + ("((?i)[" + this.digits.substring(1, this.radix) + "]|(" + this.non0Digit + "))") + str + "?" + str + "?(" + this.groupSeparator + str + str + str + ")+)") + ")";
        String str3 = "([-+]?(" + str2 + "))";
        String str4 = this.negativePrefix + str2 + this.negativeSuffix;
        return "(" + str3 + ")|(" + (this.positivePrefix + str2 + this.positiveSuffix) + ")|(" + str4 + ")";
    }

    private Pattern integerPattern() {
        if (this.integerPattern == null) {
            this.integerPattern = this.patternCache.forName(buildIntegerPatternString());
        }
        return this.integerPattern;
    }

    private static Pattern separatorPattern() {
        Pattern pattern = separatorPattern;
        if (pattern == null) {
            Pattern patternCompile = Pattern.compile(LINE_SEPARATOR_PATTERN);
            separatorPattern = patternCompile;
            return patternCompile;
        }
        return pattern;
    }

    private static Pattern linePattern() {
        Pattern pattern = linePattern;
        if (pattern == null) {
            Pattern patternCompile = Pattern.compile(LINE_PATTERN);
            linePattern = patternCompile;
            return patternCompile;
        }
        return pattern;
    }

    private void buildFloatAndDecimalPattern() {
        String str = "([eE][+-]?([0-9]|(\\p{javaDigit}))+)?";
        String str2 = "((([0-9]|(\\p{javaDigit}))++)|" + ("(" + this.non0Digit + "([0-9]|(\\p{javaDigit}))?([0-9]|(\\p{javaDigit}))?(" + this.groupSeparator + "([0-9]|(\\p{javaDigit}))([0-9]|(\\p{javaDigit}))([0-9]|(\\p{javaDigit})))+)") + ")";
        String str3 = "(" + str2 + "|" + str2 + this.decimalSeparator + "([0-9]|(\\p{javaDigit}))*+|" + this.decimalSeparator + "([0-9]|(\\p{javaDigit}))++)";
        String str4 = "(NaN|" + this.nanString + "|Infinity|" + this.infinityString + ")";
        String str5 = "(([-+]?" + str3 + str + ")|" + ("(" + this.positivePrefix + str3 + this.positiveSuffix + str + ")") + "|" + ("(" + this.negativePrefix + str3 + this.negativeSuffix + str + ")") + ")";
        this.floatPattern = Pattern.compile(str5 + "|[-+]?0[xX][0-9a-fA-F]*\\.[0-9a-fA-F]+([pP][-+]?[0-9]+)?|" + ("(([-+]?" + str4 + ")|" + ("(" + this.positivePrefix + str4 + this.positiveSuffix + ")") + "|" + ("(" + this.negativePrefix + str4 + this.negativeSuffix + ")") + ")"));
        this.decimalPattern = Pattern.compile(str5);
    }

    private Pattern floatPattern() {
        if (this.floatPattern == null) {
            buildFloatAndDecimalPattern();
        }
        return this.floatPattern;
    }

    private Pattern decimalPattern() {
        if (this.decimalPattern == null) {
            buildFloatAndDecimalPattern();
        }
        return this.decimalPattern;
    }

    private Scanner(Readable readable, Pattern pattern) {
        this.sourceClosed = $assertionsDisabled;
        this.needInput = $assertionsDisabled;
        this.skipped = $assertionsDisabled;
        this.savedScannerPosition = -1;
        this.typeCache = null;
        this.matchValid = $assertionsDisabled;
        this.closed = $assertionsDisabled;
        this.radix = 10;
        this.defaultRadix = 10;
        this.locale = null;
        this.patternCache = new LRUCache<String, Pattern>(7) {
            @Override
            protected Pattern create(String str) {
                return Pattern.compile(str);
            }

            @Override
            protected boolean hasName(Pattern pattern2, String str) {
                return pattern2.pattern().equals(str);
            }
        };
        this.groupSeparator = "\\,";
        this.decimalSeparator = "\\.";
        this.nanString = "NaN";
        this.infinityString = "Infinity";
        this.positivePrefix = "";
        this.negativePrefix = "\\-";
        this.positiveSuffix = "";
        this.negativeSuffix = "";
        this.digits = "0123456789abcdefghijklmnopqrstuvwxyz";
        this.non0Digit = "[\\p{javaDigit}&&[^0]]";
        this.SIMPLE_GROUP_INDEX = 5;
        this.source = readable;
        this.delimPattern = pattern;
        this.buf = CharBuffer.allocate(1024);
        this.buf.limit(0);
        this.matcher = this.delimPattern.matcher(this.buf);
        this.matcher.useTransparentBounds(true);
        this.matcher.useAnchoringBounds($assertionsDisabled);
        useLocale(Locale.getDefault(Locale.Category.FORMAT));
    }

    public Scanner(Readable readable) {
        this((Readable) Objects.requireNonNull(readable, "source"), WHITESPACE_PATTERN);
    }

    public Scanner(InputStream inputStream) {
        this(new InputStreamReader(inputStream), WHITESPACE_PATTERN);
    }

    public Scanner(InputStream inputStream, String str) {
        this(makeReadable((InputStream) Objects.requireNonNull(inputStream, "source"), toCharset(str)), WHITESPACE_PATTERN);
    }

    private static Charset toCharset(String str) {
        Objects.requireNonNull(str, "charsetName");
        try {
            return Charset.forName(str);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Readable makeReadable(InputStream inputStream, Charset charset) {
        return new InputStreamReader(inputStream, charset);
    }

    public Scanner(File file) throws FileNotFoundException {
        this(new FileInputStream(file).getChannel());
    }

    public Scanner(File file, String str) throws FileNotFoundException {
        this((File) Objects.requireNonNull(file), toDecoder(str));
    }

    private Scanner(File file, CharsetDecoder charsetDecoder) throws FileNotFoundException {
        this(makeReadable(new FileInputStream(file).getChannel(), charsetDecoder));
    }

    private static CharsetDecoder toDecoder(String str) {
        if (str == null) {
            throw new IllegalArgumentException("charsetName == null");
        }
        try {
            return Charset.forName(str).newDecoder();
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            throw new IllegalArgumentException(str);
        }
    }

    private static Readable makeReadable(ReadableByteChannel readableByteChannel, CharsetDecoder charsetDecoder) {
        return Channels.newReader(readableByteChannel, charsetDecoder, -1);
    }

    public Scanner(Path path) throws IOException {
        this(Files.newInputStream(path, new OpenOption[0]));
    }

    public Scanner(Path path, String str) throws IOException {
        this((Path) Objects.requireNonNull(path), toCharset(str));
    }

    private Scanner(Path path, Charset charset) throws IOException {
        this(makeReadable(Files.newInputStream(path, new OpenOption[0]), charset));
    }

    public Scanner(String str) {
        this(new StringReader(str), WHITESPACE_PATTERN);
    }

    public Scanner(ReadableByteChannel readableByteChannel) {
        this(makeReadable((ReadableByteChannel) Objects.requireNonNull(readableByteChannel, "source")), WHITESPACE_PATTERN);
    }

    private static Readable makeReadable(ReadableByteChannel readableByteChannel) {
        return makeReadable(readableByteChannel, Charset.defaultCharset().newDecoder());
    }

    public Scanner(ReadableByteChannel readableByteChannel, String str) {
        this(makeReadable((ReadableByteChannel) Objects.requireNonNull(readableByteChannel, "source"), toDecoder(str)), WHITESPACE_PATTERN);
    }

    private void saveState() {
        this.savedScannerPosition = this.position;
    }

    private void revertState() {
        this.position = this.savedScannerPosition;
        this.savedScannerPosition = -1;
        this.skipped = $assertionsDisabled;
    }

    private boolean revertState(boolean z) {
        this.position = this.savedScannerPosition;
        this.savedScannerPosition = -1;
        this.skipped = $assertionsDisabled;
        return z;
    }

    private void cacheResult() {
        this.hasNextResult = this.matcher.group();
        this.hasNextPosition = this.matcher.end();
        this.hasNextPattern = this.matcher.pattern();
    }

    private void cacheResult(String str) {
        this.hasNextResult = str;
        this.hasNextPosition = this.matcher.end();
        this.hasNextPattern = this.matcher.pattern();
    }

    private void clearCaches() {
        this.hasNextPattern = null;
        this.typeCache = null;
    }

    private String getCachedResult() {
        this.position = this.hasNextPosition;
        this.hasNextPattern = null;
        this.typeCache = null;
        return this.hasNextResult;
    }

    private void useTypeCache() {
        if (this.closed) {
            throw new IllegalStateException("Scanner closed");
        }
        this.position = this.hasNextPosition;
        this.hasNextPattern = null;
        this.typeCache = null;
    }

    private void readInput() {
        int i;
        if (this.buf.limit() == this.buf.capacity()) {
            makeSpace();
        }
        int iPosition = this.buf.position();
        this.buf.position(this.buf.limit());
        this.buf.limit(this.buf.capacity());
        try {
            i = this.source.read(this.buf);
        } catch (IOException e) {
            this.lastException = e;
            i = -1;
        }
        if (i == -1) {
            this.sourceClosed = true;
            this.needInput = $assertionsDisabled;
        }
        if (i > 0) {
            this.needInput = $assertionsDisabled;
        }
        this.buf.limit(this.buf.position());
        this.buf.position(iPosition);
        this.matcher.reset(this.buf);
    }

    private boolean makeSpace() {
        clearCaches();
        int i = this.savedScannerPosition == -1 ? this.position : this.savedScannerPosition;
        this.buf.position(i);
        if (i > 0) {
            this.buf.compact();
            translateSavedIndexes(i);
            this.position -= i;
            this.buf.flip();
            return true;
        }
        CharBuffer charBufferAllocate = CharBuffer.allocate(this.buf.capacity() * 2);
        charBufferAllocate.put(this.buf);
        charBufferAllocate.flip();
        translateSavedIndexes(i);
        this.position -= i;
        this.buf = charBufferAllocate;
        this.matcher.reset(this.buf);
        return true;
    }

    private void translateSavedIndexes(int i) {
        if (this.savedScannerPosition != -1) {
            this.savedScannerPosition -= i;
        }
    }

    private void throwFor() {
        this.skipped = $assertionsDisabled;
        if (this.sourceClosed && this.position == this.buf.limit()) {
            throw new NoSuchElementException();
        }
        throw new InputMismatchException();
    }

    private boolean hasTokenInBuffer() {
        this.matchValid = $assertionsDisabled;
        this.matcher.usePattern(this.delimPattern);
        this.matcher.region(this.position, this.buf.limit());
        if (this.matcher.lookingAt()) {
            this.position = this.matcher.end();
        }
        if (this.position == this.buf.limit()) {
            return $assertionsDisabled;
        }
        return true;
    }

    private String getCompleteTokenInBuffer(Pattern pattern) {
        this.matchValid = $assertionsDisabled;
        this.matcher.usePattern(this.delimPattern);
        if (!this.skipped) {
            this.matcher.region(this.position, this.buf.limit());
            if (this.matcher.lookingAt()) {
                if (this.matcher.hitEnd() && !this.sourceClosed) {
                    this.needInput = true;
                    return null;
                }
                this.skipped = true;
                this.position = this.matcher.end();
            }
        }
        if (this.position == this.buf.limit()) {
            if (this.sourceClosed) {
                return null;
            }
            this.needInput = true;
            return null;
        }
        this.matcher.region(this.position, this.buf.limit());
        boolean zFind = this.matcher.find();
        if (zFind && this.matcher.end() == this.position) {
            zFind = this.matcher.find();
        }
        if (zFind) {
            if (this.matcher.requireEnd() && !this.sourceClosed) {
                this.needInput = true;
                return null;
            }
            int iStart = this.matcher.start();
            if (pattern == null) {
                pattern = FIND_ANY_PATTERN;
            }
            this.matcher.usePattern(pattern);
            this.matcher.region(this.position, iStart);
            if (!this.matcher.matches()) {
                return null;
            }
            String strGroup = this.matcher.group();
            this.position = this.matcher.end();
            return strGroup;
        }
        if (this.sourceClosed) {
            if (pattern == null) {
                pattern = FIND_ANY_PATTERN;
            }
            this.matcher.usePattern(pattern);
            this.matcher.region(this.position, this.buf.limit());
            if (!this.matcher.matches()) {
                return null;
            }
            String strGroup2 = this.matcher.group();
            this.position = this.matcher.end();
            return strGroup2;
        }
        this.needInput = true;
        return null;
    }

    private String findPatternInBuffer(Pattern pattern, int i) {
        int i2;
        this.matchValid = $assertionsDisabled;
        this.matcher.usePattern(pattern);
        int iLimit = this.buf.limit();
        if (i > 0) {
            i2 = this.position + i;
            if (i2 < iLimit) {
                iLimit = i2;
            }
        } else {
            i2 = -1;
        }
        this.matcher.region(this.position, iLimit);
        if (this.matcher.find()) {
            if (this.matcher.hitEnd() && !this.sourceClosed) {
                if (iLimit != i2) {
                    this.needInput = true;
                    return null;
                }
                if (iLimit == i2 && this.matcher.requireEnd()) {
                    this.needInput = true;
                    return null;
                }
            }
            this.position = this.matcher.end();
            return this.matcher.group();
        }
        if (this.sourceClosed) {
            return null;
        }
        if (i == 0 || iLimit != i2) {
            this.needInput = true;
        }
        return null;
    }

    private String matchPatternInBuffer(Pattern pattern) {
        this.matchValid = $assertionsDisabled;
        this.matcher.usePattern(pattern);
        this.matcher.region(this.position, this.buf.limit());
        if (this.matcher.lookingAt()) {
            if (this.matcher.hitEnd() && !this.sourceClosed) {
                this.needInput = true;
                return null;
            }
            this.position = this.matcher.end();
            return this.matcher.group();
        }
        if (this.sourceClosed) {
            return null;
        }
        this.needInput = true;
        return null;
    }

    private void ensureOpen() {
        if (this.closed) {
            throw new IllegalStateException("Scanner closed");
        }
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        if (this.source instanceof Closeable) {
            try {
                ((Closeable) this.source).close();
            } catch (IOException e) {
                this.lastException = e;
            }
        }
        this.sourceClosed = true;
        this.source = null;
        this.closed = true;
    }

    public IOException ioException() {
        return this.lastException;
    }

    public Pattern delimiter() {
        return this.delimPattern;
    }

    public Scanner useDelimiter(Pattern pattern) {
        this.delimPattern = pattern;
        return this;
    }

    public Scanner useDelimiter(String str) {
        this.delimPattern = this.patternCache.forName(str);
        return this;
    }

    public Locale locale() {
        return this.locale;
    }

    public Scanner useLocale(Locale locale) {
        if (locale.equals(this.locale)) {
            return this;
        }
        this.locale = locale;
        DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getNumberInstance(locale);
        DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance(locale);
        this.groupSeparator = "\\" + decimalFormatSymbols.getGroupingSeparator();
        this.decimalSeparator = "\\" + decimalFormatSymbols.getDecimalSeparator();
        this.nanString = "\\Q" + decimalFormatSymbols.getNaN() + "\\E";
        this.infinityString = "\\Q" + decimalFormatSymbols.getInfinity() + "\\E";
        this.positivePrefix = decimalFormat.getPositivePrefix();
        if (this.positivePrefix.length() > 0) {
            this.positivePrefix = "\\Q" + this.positivePrefix + "\\E";
        }
        this.negativePrefix = decimalFormat.getNegativePrefix();
        if (this.negativePrefix.length() > 0) {
            this.negativePrefix = "\\Q" + this.negativePrefix + "\\E";
        }
        this.positiveSuffix = decimalFormat.getPositiveSuffix();
        if (this.positiveSuffix.length() > 0) {
            this.positiveSuffix = "\\Q" + this.positiveSuffix + "\\E";
        }
        this.negativeSuffix = decimalFormat.getNegativeSuffix();
        if (this.negativeSuffix.length() > 0) {
            this.negativeSuffix = "\\Q" + this.negativeSuffix + "\\E";
        }
        this.integerPattern = null;
        this.floatPattern = null;
        return this;
    }

    public int radix() {
        return this.defaultRadix;
    }

    public Scanner useRadix(int i) {
        if (i < 2 || i > 36) {
            throw new IllegalArgumentException("radix:" + i);
        }
        if (this.defaultRadix == i) {
            return this;
        }
        this.defaultRadix = i;
        this.integerPattern = null;
        return this;
    }

    private void setRadix(int i) {
        if (i > 36) {
            throw new IllegalArgumentException("radix == " + i);
        }
        if (this.radix != i) {
            this.integerPattern = null;
            this.radix = i;
        }
    }

    public MatchResult match() {
        if (!this.matchValid) {
            throw new IllegalStateException("No match result available");
        }
        return this.matcher.toMatchResult();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("java.util.Scanner");
        sb.append("[delimiters=" + ((Object) this.delimPattern) + "]");
        sb.append("[position=" + this.position + "]");
        sb.append("[match valid=" + this.matchValid + "]");
        sb.append("[need input=" + this.needInput + "]");
        sb.append("[source closed=" + this.sourceClosed + "]");
        sb.append("[skipped=" + this.skipped + "]");
        sb.append("[group separator=" + this.groupSeparator + "]");
        sb.append("[decimal separator=" + this.decimalSeparator + "]");
        sb.append("[positive prefix=" + this.positivePrefix + "]");
        sb.append("[negative prefix=" + this.negativePrefix + "]");
        sb.append("[positive suffix=" + this.positiveSuffix + "]");
        sb.append("[negative suffix=" + this.negativeSuffix + "]");
        sb.append("[NaN string=" + this.nanString + "]");
        sb.append("[infinity string=" + this.infinityString + "]");
        return sb.toString();
    }

    @Override
    public boolean hasNext() {
        ensureOpen();
        saveState();
        while (!this.sourceClosed) {
            if (hasTokenInBuffer()) {
                return revertState(true);
            }
            readInput();
        }
        return revertState(hasTokenInBuffer());
    }

    @Override
    public String next() {
        ensureOpen();
        clearCaches();
        while (true) {
            String completeTokenInBuffer = getCompleteTokenInBuffer(null);
            if (completeTokenInBuffer != null) {
                this.matchValid = true;
                this.skipped = $assertionsDisabled;
                return completeTokenInBuffer;
            }
            if (this.needInput) {
                readInput();
            } else {
                throwFor();
            }
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public boolean hasNext(String str) {
        return hasNext(this.patternCache.forName(str));
    }

    public String next(String str) {
        return next(this.patternCache.forName(str));
    }

    public boolean hasNext(Pattern pattern) {
        ensureOpen();
        if (pattern == null) {
            throw new NullPointerException();
        }
        this.hasNextPattern = null;
        saveState();
        while (getCompleteTokenInBuffer(pattern) == null) {
            if (this.needInput) {
                readInput();
            } else {
                return revertState($assertionsDisabled);
            }
        }
        this.matchValid = true;
        cacheResult();
        return revertState(true);
    }

    public String next(Pattern pattern) {
        ensureOpen();
        if (pattern == null) {
            throw new NullPointerException();
        }
        if (this.hasNextPattern == pattern) {
            return getCachedResult();
        }
        clearCaches();
        while (true) {
            String completeTokenInBuffer = getCompleteTokenInBuffer(pattern);
            if (completeTokenInBuffer != null) {
                this.matchValid = true;
                this.skipped = $assertionsDisabled;
                return completeTokenInBuffer;
            }
            if (this.needInput) {
                readInput();
            } else {
                throwFor();
            }
        }
    }

    public boolean hasNextLine() {
        saveState();
        String strFindWithinHorizon = findWithinHorizon(linePattern(), 0);
        if (strFindWithinHorizon != null) {
            String strGroup = match().group(1);
            if (strGroup != null) {
                strFindWithinHorizon = strFindWithinHorizon.substring(0, strFindWithinHorizon.length() - strGroup.length());
                cacheResult(strFindWithinHorizon);
            } else {
                cacheResult();
            }
        }
        revertState();
        if (strFindWithinHorizon != null) {
            return true;
        }
        return $assertionsDisabled;
    }

    public String nextLine() {
        if (this.hasNextPattern == linePattern()) {
            return getCachedResult();
        }
        clearCaches();
        String strFindWithinHorizon = findWithinHorizon(linePattern, 0);
        if (strFindWithinHorizon == null) {
            throw new NoSuchElementException("No line found");
        }
        String strGroup = match().group(1);
        if (strGroup != null) {
            strFindWithinHorizon = strFindWithinHorizon.substring(0, strFindWithinHorizon.length() - strGroup.length());
        }
        if (strFindWithinHorizon == null) {
            throw new NoSuchElementException();
        }
        return strFindWithinHorizon;
    }

    public String findInLine(String str) {
        return findInLine(this.patternCache.forName(str));
    }

    public String findInLine(Pattern pattern) {
        int iStart;
        ensureOpen();
        if (pattern == null) {
            throw new NullPointerException();
        }
        clearCaches();
        saveState();
        while (true) {
            if (findPatternInBuffer(separatorPattern(), 0) != null) {
                iStart = this.matcher.start();
                break;
            }
            if (this.needInput) {
                readInput();
            } else {
                iStart = this.buf.limit();
                break;
            }
        }
        revertState();
        int i = iStart - this.position;
        if (i == 0) {
            return null;
        }
        return findWithinHorizon(pattern, i);
    }

    public String findWithinHorizon(String str, int i) {
        return findWithinHorizon(this.patternCache.forName(str), i);
    }

    public String findWithinHorizon(Pattern pattern, int i) {
        ensureOpen();
        if (pattern == null) {
            throw new NullPointerException();
        }
        if (i < 0) {
            throw new IllegalArgumentException("horizon < 0");
        }
        clearCaches();
        while (true) {
            String strFindPatternInBuffer = findPatternInBuffer(pattern, i);
            if (strFindPatternInBuffer != null) {
                this.matchValid = true;
                return strFindPatternInBuffer;
            }
            if (this.needInput) {
                readInput();
            } else {
                return null;
            }
        }
    }

    public Scanner skip(Pattern pattern) {
        ensureOpen();
        if (pattern == null) {
            throw new NullPointerException();
        }
        clearCaches();
        while (matchPatternInBuffer(pattern) == null) {
            if (this.needInput) {
                readInput();
            } else {
                throw new NoSuchElementException();
            }
        }
        this.matchValid = true;
        this.position = this.matcher.end();
        return this;
    }

    public Scanner skip(String str) {
        return skip(this.patternCache.forName(str));
    }

    public boolean hasNextBoolean() {
        return hasNext(boolPattern());
    }

    public boolean nextBoolean() {
        clearCaches();
        return Boolean.parseBoolean(next(boolPattern()));
    }

    public boolean hasNextByte() {
        return hasNextByte(this.defaultRadix);
    }

    public boolean hasNextByte(int i) {
        String strProcessIntegerToken;
        setRadix(i);
        boolean zHasNext = hasNext(integerPattern());
        if (zHasNext) {
            try {
                if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                    strProcessIntegerToken = processIntegerToken(this.hasNextResult);
                } else {
                    strProcessIntegerToken = this.hasNextResult;
                }
                this.typeCache = Byte.valueOf(Byte.parseByte(strProcessIntegerToken, i));
                return zHasNext;
            } catch (NumberFormatException e) {
                return $assertionsDisabled;
            }
        }
        return zHasNext;
    }

    public byte nextByte() {
        return nextByte(this.defaultRadix);
    }

    public byte nextByte(int i) {
        if (this.typeCache != null && (this.typeCache instanceof Byte) && this.radix == i) {
            byte bByteValue = ((Byte) this.typeCache).byteValue();
            useTypeCache();
            return bByteValue;
        }
        setRadix(i);
        clearCaches();
        try {
            String next = next(integerPattern());
            if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                next = processIntegerToken(next);
            }
            return Byte.parseByte(next, i);
        } catch (NumberFormatException e) {
            this.position = this.matcher.start();
            throw new InputMismatchException(e.getMessage());
        }
    }

    public boolean hasNextShort() {
        return hasNextShort(this.defaultRadix);
    }

    public boolean hasNextShort(int i) {
        String strProcessIntegerToken;
        setRadix(i);
        boolean zHasNext = hasNext(integerPattern());
        if (zHasNext) {
            try {
                if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                    strProcessIntegerToken = processIntegerToken(this.hasNextResult);
                } else {
                    strProcessIntegerToken = this.hasNextResult;
                }
                this.typeCache = Short.valueOf(Short.parseShort(strProcessIntegerToken, i));
                return zHasNext;
            } catch (NumberFormatException e) {
                return $assertionsDisabled;
            }
        }
        return zHasNext;
    }

    public short nextShort() {
        return nextShort(this.defaultRadix);
    }

    public short nextShort(int i) {
        if (this.typeCache != null && (this.typeCache instanceof Short) && this.radix == i) {
            short sShortValue = ((Short) this.typeCache).shortValue();
            useTypeCache();
            return sShortValue;
        }
        setRadix(i);
        clearCaches();
        try {
            String next = next(integerPattern());
            if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                next = processIntegerToken(next);
            }
            return Short.parseShort(next, i);
        } catch (NumberFormatException e) {
            this.position = this.matcher.start();
            throw new InputMismatchException(e.getMessage());
        }
    }

    public boolean hasNextInt() {
        return hasNextInt(this.defaultRadix);
    }

    public boolean hasNextInt(int i) {
        String strProcessIntegerToken;
        setRadix(i);
        boolean zHasNext = hasNext(integerPattern());
        if (zHasNext) {
            try {
                if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                    strProcessIntegerToken = processIntegerToken(this.hasNextResult);
                } else {
                    strProcessIntegerToken = this.hasNextResult;
                }
                this.typeCache = Integer.valueOf(Integer.parseInt(strProcessIntegerToken, i));
                return zHasNext;
            } catch (NumberFormatException e) {
                return $assertionsDisabled;
            }
        }
        return zHasNext;
    }

    private String processIntegerToken(String str) {
        boolean z;
        String strReplaceAll = str.replaceAll("" + this.groupSeparator, "");
        int length = this.negativePrefix.length();
        if (length <= 0 || !strReplaceAll.startsWith(this.negativePrefix)) {
            z = $assertionsDisabled;
        } else {
            strReplaceAll = strReplaceAll.substring(length);
            z = true;
        }
        int length2 = this.negativeSuffix.length();
        if (length2 > 0 && strReplaceAll.endsWith(this.negativeSuffix)) {
            strReplaceAll = strReplaceAll.substring(strReplaceAll.length() - length2, strReplaceAll.length());
            z = true;
        }
        if (z) {
            return LanguageTag.SEP + strReplaceAll;
        }
        return strReplaceAll;
    }

    public int nextInt() {
        return nextInt(this.defaultRadix);
    }

    public int nextInt(int i) {
        if (this.typeCache != null && (this.typeCache instanceof Integer) && this.radix == i) {
            int iIntValue = ((Integer) this.typeCache).intValue();
            useTypeCache();
            return iIntValue;
        }
        setRadix(i);
        clearCaches();
        try {
            String next = next(integerPattern());
            if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                next = processIntegerToken(next);
            }
            return Integer.parseInt(next, i);
        } catch (NumberFormatException e) {
            this.position = this.matcher.start();
            throw new InputMismatchException(e.getMessage());
        }
    }

    public boolean hasNextLong() {
        return hasNextLong(this.defaultRadix);
    }

    public boolean hasNextLong(int i) {
        String strProcessIntegerToken;
        setRadix(i);
        boolean zHasNext = hasNext(integerPattern());
        if (zHasNext) {
            try {
                if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                    strProcessIntegerToken = processIntegerToken(this.hasNextResult);
                } else {
                    strProcessIntegerToken = this.hasNextResult;
                }
                this.typeCache = Long.valueOf(Long.parseLong(strProcessIntegerToken, i));
                return zHasNext;
            } catch (NumberFormatException e) {
                return $assertionsDisabled;
            }
        }
        return zHasNext;
    }

    public long nextLong() {
        return nextLong(this.defaultRadix);
    }

    public long nextLong(int i) {
        if (this.typeCache != null && (this.typeCache instanceof Long) && this.radix == i) {
            long jLongValue = ((Long) this.typeCache).longValue();
            useTypeCache();
            return jLongValue;
        }
        setRadix(i);
        clearCaches();
        try {
            String next = next(integerPattern());
            if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                next = processIntegerToken(next);
            }
            return Long.parseLong(next, i);
        } catch (NumberFormatException e) {
            this.position = this.matcher.start();
            throw new InputMismatchException(e.getMessage());
        }
    }

    private String processFloatToken(String str) {
        boolean z;
        String strReplaceAll = str.replaceAll(this.groupSeparator, "");
        if (!this.decimalSeparator.equals("\\.")) {
            strReplaceAll = strReplaceAll.replaceAll(this.decimalSeparator, ".");
        }
        int length = this.negativePrefix.length();
        if (length <= 0 || !strReplaceAll.startsWith(this.negativePrefix)) {
            z = false;
        } else {
            strReplaceAll = strReplaceAll.substring(length);
            z = true;
        }
        int length2 = this.negativeSuffix.length();
        if (length2 > 0 && strReplaceAll.endsWith(this.negativeSuffix)) {
            strReplaceAll = strReplaceAll.substring(strReplaceAll.length() - length2, strReplaceAll.length());
            z = true;
        }
        if (strReplaceAll.equals(this.nanString)) {
            strReplaceAll = "NaN";
        }
        if (strReplaceAll.equals(this.infinityString)) {
            strReplaceAll = "Infinity";
        }
        if (strReplaceAll.equals("∞")) {
            strReplaceAll = "Infinity";
        }
        if (z) {
            strReplaceAll = LanguageTag.SEP + strReplaceAll;
        }
        if (NON_ASCII_DIGIT.matcher(strReplaceAll).find()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < strReplaceAll.length(); i++) {
                char cCharAt = strReplaceAll.charAt(i);
                if (Character.isDigit(cCharAt)) {
                    int iDigit = Character.digit(cCharAt, 10);
                    if (iDigit != -1) {
                        sb.append(iDigit);
                    } else {
                        sb.append(cCharAt);
                    }
                } else {
                    sb.append(cCharAt);
                }
            }
            return sb.toString();
        }
        return strReplaceAll;
    }

    public boolean hasNextFloat() {
        setRadix(10);
        boolean zHasNext = hasNext(floatPattern());
        if (zHasNext) {
            try {
                this.typeCache = Float.valueOf(Float.parseFloat(processFloatToken(this.hasNextResult)));
                return zHasNext;
            } catch (NumberFormatException e) {
                return $assertionsDisabled;
            }
        }
        return zHasNext;
    }

    public float nextFloat() {
        if (this.typeCache != null && (this.typeCache instanceof Float)) {
            float fFloatValue = ((Float) this.typeCache).floatValue();
            useTypeCache();
            return fFloatValue;
        }
        setRadix(10);
        clearCaches();
        try {
            return Float.parseFloat(processFloatToken(next(floatPattern())));
        } catch (NumberFormatException e) {
            this.position = this.matcher.start();
            throw new InputMismatchException(e.getMessage());
        }
    }

    public boolean hasNextDouble() {
        setRadix(10);
        boolean zHasNext = hasNext(floatPattern());
        if (zHasNext) {
            try {
                this.typeCache = Double.valueOf(Double.parseDouble(processFloatToken(this.hasNextResult)));
                return zHasNext;
            } catch (NumberFormatException e) {
                return $assertionsDisabled;
            }
        }
        return zHasNext;
    }

    public double nextDouble() {
        if (this.typeCache != null && (this.typeCache instanceof Double)) {
            double dDoubleValue = ((Double) this.typeCache).doubleValue();
            useTypeCache();
            return dDoubleValue;
        }
        setRadix(10);
        clearCaches();
        try {
            return Double.parseDouble(processFloatToken(next(floatPattern())));
        } catch (NumberFormatException e) {
            this.position = this.matcher.start();
            throw new InputMismatchException(e.getMessage());
        }
    }

    public boolean hasNextBigInteger() {
        return hasNextBigInteger(this.defaultRadix);
    }

    public boolean hasNextBigInteger(int i) {
        String strProcessIntegerToken;
        setRadix(i);
        boolean zHasNext = hasNext(integerPattern());
        if (zHasNext) {
            try {
                if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                    strProcessIntegerToken = processIntegerToken(this.hasNextResult);
                } else {
                    strProcessIntegerToken = this.hasNextResult;
                }
                this.typeCache = new BigInteger(strProcessIntegerToken, i);
                return zHasNext;
            } catch (NumberFormatException e) {
                return $assertionsDisabled;
            }
        }
        return zHasNext;
    }

    public BigInteger nextBigInteger() {
        return nextBigInteger(this.defaultRadix);
    }

    public BigInteger nextBigInteger(int i) {
        if (this.typeCache != null && (this.typeCache instanceof BigInteger) && this.radix == i) {
            BigInteger bigInteger = (BigInteger) this.typeCache;
            useTypeCache();
            return bigInteger;
        }
        setRadix(i);
        clearCaches();
        try {
            String next = next(integerPattern());
            if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                next = processIntegerToken(next);
            }
            return new BigInteger(next, i);
        } catch (NumberFormatException e) {
            this.position = this.matcher.start();
            throw new InputMismatchException(e.getMessage());
        }
    }

    public boolean hasNextBigDecimal() {
        setRadix(10);
        boolean zHasNext = hasNext(decimalPattern());
        if (zHasNext) {
            try {
                this.typeCache = new BigDecimal(processFloatToken(this.hasNextResult));
                return zHasNext;
            } catch (NumberFormatException e) {
                return $assertionsDisabled;
            }
        }
        return zHasNext;
    }

    public BigDecimal nextBigDecimal() {
        if (this.typeCache != null && (this.typeCache instanceof BigDecimal)) {
            BigDecimal bigDecimal = (BigDecimal) this.typeCache;
            useTypeCache();
            return bigDecimal;
        }
        setRadix(10);
        clearCaches();
        try {
            return new BigDecimal(processFloatToken(next(decimalPattern())));
        } catch (NumberFormatException e) {
            this.position = this.matcher.start();
            throw new InputMismatchException(e.getMessage());
        }
    }

    public Scanner reset() {
        this.delimPattern = WHITESPACE_PATTERN;
        useLocale(Locale.getDefault(Locale.Category.FORMAT));
        useRadix(10);
        clearCaches();
        return this;
    }
}
