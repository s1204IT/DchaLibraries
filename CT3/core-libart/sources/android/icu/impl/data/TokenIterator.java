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

    public TokenIterator(ResourceReader r) {
        this.reader = r;
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

    private int nextToken(int position) {
        int position2 = PatternProps.skipWhiteSpace(this.line, position);
        if (position2 == this.line.length()) {
            return -1;
        }
        int position3 = position2 + 1;
        char c = this.line.charAt(position2);
        char quote = 0;
        switch (c) {
            case '\"':
            case '\'':
                quote = c;
                break;
            case '#':
                return -1;
            case '$':
            case '%':
            case '&':
            default:
                this.buf.append(c);
                break;
        }
        int[] iArr = null;
        while (position3 < this.line.length()) {
            char c2 = this.line.charAt(position3);
            if (c2 == '\\') {
                if (iArr == null) {
                    iArr = new int[1];
                }
                iArr[0] = position3 + 1;
                int c32 = Utility.unescapeAt(this.line, iArr);
                if (c32 < 0) {
                    throw new RuntimeException("Invalid escape at " + this.reader.describePosition() + ':' + position3);
                }
                UTF16.append(this.buf, c32);
                position3 = iArr[0];
            } else {
                if ((quote != 0 && c2 == quote) || (quote == 0 && PatternProps.isWhiteSpace(c2))) {
                    return position3 + 1;
                }
                if (quote == 0 && c2 == '#') {
                    return position3;
                }
                this.buf.append(c2);
                position3++;
            }
        }
        if (quote != 0) {
            throw new RuntimeException("Unterminated quote at " + this.reader.describePosition() + ':' + position2);
        }
        return position3;
    }
}
