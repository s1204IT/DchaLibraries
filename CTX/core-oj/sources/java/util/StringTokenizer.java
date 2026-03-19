package java.util;

public class StringTokenizer implements Enumeration<Object> {
    private int currentPosition;
    private int[] delimiterCodePoints;
    private String delimiters;
    private boolean delimsChanged;
    private boolean hasSurrogates;
    private int maxDelimCodePoint;
    private int maxPosition;
    private int newPosition;
    private boolean retDelims;
    private String str;

    private void setMaxDelimCodePoint() {
        int i = 0;
        if (this.delimiters == null) {
            this.maxDelimCodePoint = 0;
            return;
        }
        int iCharCount = 0;
        int i2 = 0;
        int i3 = 0;
        while (iCharCount < this.delimiters.length()) {
            int iCharAt = this.delimiters.charAt(iCharCount);
            if (iCharAt >= 55296 && iCharAt <= 57343) {
                iCharAt = this.delimiters.codePointAt(iCharCount);
                this.hasSurrogates = true;
            }
            if (i2 < iCharAt) {
                i2 = iCharAt;
            }
            i3++;
            iCharCount += Character.charCount(iCharAt);
        }
        this.maxDelimCodePoint = i2;
        if (this.hasSurrogates) {
            this.delimiterCodePoints = new int[i3];
            int iCharCount2 = 0;
            while (i < i3) {
                int iCodePointAt = this.delimiters.codePointAt(iCharCount2);
                this.delimiterCodePoints[i] = iCodePointAt;
                i++;
                iCharCount2 += Character.charCount(iCodePointAt);
            }
        }
    }

    public StringTokenizer(String str, String str2, boolean z) {
        this.hasSurrogates = false;
        this.currentPosition = 0;
        this.newPosition = -1;
        this.delimsChanged = false;
        this.str = str;
        this.maxPosition = str.length();
        this.delimiters = str2;
        this.retDelims = z;
        setMaxDelimCodePoint();
    }

    public StringTokenizer(String str, String str2) {
        this(str, str2, false);
    }

    public StringTokenizer(String str) {
        this(str, " \t\n\r\f", false);
    }

    private int skipDelimiters(int i) {
        if (this.delimiters == null) {
            throw new NullPointerException();
        }
        while (!this.retDelims && i < this.maxPosition) {
            if (!this.hasSurrogates) {
                char cCharAt = this.str.charAt(i);
                if (cCharAt > this.maxDelimCodePoint || this.delimiters.indexOf(cCharAt) < 0) {
                    break;
                }
                i++;
            } else {
                int iCodePointAt = this.str.codePointAt(i);
                if (iCodePointAt > this.maxDelimCodePoint || !isDelimiter(iCodePointAt)) {
                    break;
                }
                i += Character.charCount(iCodePointAt);
            }
        }
        return i;
    }

    private int scanToken(int i) {
        int iCharCount = i;
        while (iCharCount < this.maxPosition) {
            if (!this.hasSurrogates) {
                char cCharAt = this.str.charAt(iCharCount);
                if (cCharAt <= this.maxDelimCodePoint && this.delimiters.indexOf(cCharAt) >= 0) {
                    break;
                }
                iCharCount++;
            } else {
                int iCodePointAt = this.str.codePointAt(iCharCount);
                if (iCodePointAt <= this.maxDelimCodePoint && isDelimiter(iCodePointAt)) {
                    break;
                }
                iCharCount += Character.charCount(iCodePointAt);
            }
        }
        if (this.retDelims && i == iCharCount) {
            if (!this.hasSurrogates) {
                char cCharAt2 = this.str.charAt(iCharCount);
                if (cCharAt2 <= this.maxDelimCodePoint && this.delimiters.indexOf(cCharAt2) >= 0) {
                    return iCharCount + 1;
                }
                return iCharCount;
            }
            int iCodePointAt2 = this.str.codePointAt(iCharCount);
            if (iCodePointAt2 <= this.maxDelimCodePoint && isDelimiter(iCodePointAt2)) {
                return iCharCount + Character.charCount(iCodePointAt2);
            }
            return iCharCount;
        }
        return iCharCount;
    }

    private boolean isDelimiter(int i) {
        for (int i2 = 0; i2 < this.delimiterCodePoints.length; i2++) {
            if (this.delimiterCodePoints[i2] == i) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMoreTokens() {
        this.newPosition = skipDelimiters(this.currentPosition);
        return this.newPosition < this.maxPosition;
    }

    public String nextToken() {
        this.currentPosition = (this.newPosition < 0 || this.delimsChanged) ? skipDelimiters(this.currentPosition) : this.newPosition;
        this.delimsChanged = false;
        this.newPosition = -1;
        if (this.currentPosition >= this.maxPosition) {
            throw new NoSuchElementException();
        }
        int i = this.currentPosition;
        this.currentPosition = scanToken(this.currentPosition);
        return this.str.substring(i, this.currentPosition);
    }

    public String nextToken(String str) {
        this.delimiters = str;
        this.delimsChanged = true;
        setMaxDelimCodePoint();
        return nextToken();
    }

    @Override
    public boolean hasMoreElements() {
        return hasMoreTokens();
    }

    @Override
    public Object nextElement() {
        return nextToken();
    }

    public int countTokens() {
        int iSkipDelimiters;
        int iScanToken = this.currentPosition;
        int i = 0;
        while (iScanToken < this.maxPosition && (iSkipDelimiters = skipDelimiters(iScanToken)) < this.maxPosition) {
            iScanToken = scanToken(iSkipDelimiters);
            i++;
        }
        return i;
    }
}
