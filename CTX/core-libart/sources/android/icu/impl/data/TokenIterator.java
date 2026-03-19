package android.icu.impl.data;

import android.icu.impl.PatternProps;
import android.icu.impl.Utility;
import android.icu.text.UTF16;
import java.io.IOException;

public class TokenIterator {
    private ResourceReader reader;
    private String line = null;
    private boolean done = false;
    private StringBuffer buf = new StringBuffer();
    private int lastpos = -1;
    private int pos = -1;

    public TokenIterator(ResourceReader resourceReader) {
        this.reader = resourceReader;
    }

    public String next() throws IOException {
        if (this.done) {
            return null;
        }
        while (true) {
            if (this.line == null) {
                this.line = this.reader.readLineSkippingComments();
                if (this.line == null) {
                    this.done = true;
                    return null;
                }
                this.pos = 0;
            }
            this.buf.setLength(0);
            this.lastpos = this.pos;
            this.pos = nextToken(this.pos);
            if (this.pos < 0) {
                this.line = null;
            } else {
                return this.buf.toString();
            }
        }
    }

    public int getLineNumber() {
        return this.reader.getLineNumber();
    }

    public String describePosition() {
        return this.reader.describePosition() + ':' + (this.lastpos + 1);
    }

    private int nextToken(int i) {
        int iSkipWhiteSpace = PatternProps.skipWhiteSpace(this.line, i);
        if (iSkipWhiteSpace == this.line.length()) {
            return -1;
        }
        int i2 = iSkipWhiteSpace + 1;
        char cCharAt = this.line.charAt(iSkipWhiteSpace);
        if (cCharAt != '\'') {
            switch (cCharAt) {
                case '\"':
                    break;
                case '#':
                    return -1;
                default:
                    this.buf.append(cCharAt);
                    cCharAt = 0;
                    break;
            }
        }
        int[] iArr = null;
        while (i2 < this.line.length()) {
            char cCharAt2 = this.line.charAt(i2);
            if (cCharAt2 == '\\') {
                if (iArr == null) {
                    iArr = new int[1];
                }
                iArr[0] = i2 + 1;
                int iUnescapeAt = Utility.unescapeAt(this.line, iArr);
                if (iUnescapeAt < 0) {
                    throw new RuntimeException("Invalid escape at " + this.reader.describePosition() + ':' + i2);
                }
                UTF16.append(this.buf, iUnescapeAt);
                i2 = iArr[0];
            } else {
                if ((cCharAt != 0 && cCharAt2 == cCharAt) || (cCharAt == 0 && PatternProps.isWhiteSpace(cCharAt2))) {
                    return i2 + 1;
                }
                if (cCharAt == 0 && cCharAt2 == '#') {
                    return i2;
                }
                this.buf.append(cCharAt2);
                i2++;
            }
        }
        if (cCharAt != 0) {
            throw new RuntimeException("Unterminated quote at " + this.reader.describePosition() + ':' + iSkipWhiteSpace);
        }
        return i2;
    }
}
