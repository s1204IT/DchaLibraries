package org.apache.http.message;

import java.util.NoSuchElementException;
import org.apache.http.HeaderIterator;
import org.apache.http.ParseException;
import org.apache.http.TokenIterator;

@Deprecated
public class BasicTokenIterator implements TokenIterator {
    public static final String HTTP_SEPARATORS = " ,;=()<>@:\\\"/[]?{}\t";
    protected String currentHeader;
    protected String currentToken;
    protected final HeaderIterator headerIt;
    protected int searchPos;

    public BasicTokenIterator(HeaderIterator headerIterator) {
        if (headerIterator == null) {
            throw new IllegalArgumentException("Header iterator must not be null.");
        }
        this.headerIt = headerIterator;
        this.searchPos = findNext(-1);
    }

    @Override
    public boolean hasNext() {
        return this.currentToken != null;
    }

    @Override
    public String nextToken() throws ParseException, NoSuchElementException {
        if (this.currentToken == null) {
            throw new NoSuchElementException("Iteration already finished.");
        }
        String str = this.currentToken;
        this.searchPos = findNext(this.searchPos);
        return str;
    }

    @Override
    public final Object next() throws ParseException, NoSuchElementException {
        return nextToken();
    }

    @Override
    public final void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Removing tokens is not supported.");
    }

    protected int findNext(int i) throws ParseException {
        int iFindTokenSeparator;
        if (i < 0) {
            if (!this.headerIt.hasNext()) {
                return -1;
            }
            this.currentHeader = this.headerIt.nextHeader().getValue();
            iFindTokenSeparator = 0;
        } else {
            iFindTokenSeparator = findTokenSeparator(i);
        }
        int iFindTokenStart = findTokenStart(iFindTokenSeparator);
        if (iFindTokenStart < 0) {
            this.currentToken = null;
            return -1;
        }
        int iFindTokenEnd = findTokenEnd(iFindTokenStart);
        this.currentToken = createToken(this.currentHeader, iFindTokenStart, iFindTokenEnd);
        return iFindTokenEnd;
    }

    protected String createToken(String str, int i, int i2) {
        return str.substring(i, i2);
    }

    protected int findTokenStart(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Search position must not be negative: " + i);
        }
        int i2 = i;
        boolean z = false;
        while (!z && this.currentHeader != null) {
            int length = this.currentHeader.length();
            while (!z && i2 < length) {
                char cCharAt = this.currentHeader.charAt(i2);
                if (isTokenSeparator(cCharAt) || isWhitespace(cCharAt)) {
                    i2++;
                } else {
                    if (!isTokenChar(this.currentHeader.charAt(i2))) {
                        throw new ParseException("Invalid character before token (pos " + i2 + "): " + this.currentHeader);
                    }
                    z = true;
                }
            }
            if (!z) {
                if (this.headerIt.hasNext()) {
                    this.currentHeader = this.headerIt.nextHeader().getValue();
                    i2 = 0;
                } else {
                    this.currentHeader = null;
                }
            }
        }
        if (z) {
            return i2;
        }
        return -1;
    }

    protected int findTokenSeparator(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Search position must not be negative: " + i);
        }
        boolean z = false;
        int length = this.currentHeader.length();
        while (!z && i < length) {
            char cCharAt = this.currentHeader.charAt(i);
            if (!isTokenSeparator(cCharAt)) {
                if (isWhitespace(cCharAt)) {
                    i++;
                } else {
                    if (isTokenChar(cCharAt)) {
                        throw new ParseException("Tokens without separator (pos " + i + "): " + this.currentHeader);
                    }
                    throw new ParseException("Invalid character after token (pos " + i + "): " + this.currentHeader);
                }
            } else {
                z = true;
            }
        }
        return i;
    }

    protected int findTokenEnd(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Token start position must not be negative: " + i);
        }
        int length = this.currentHeader.length();
        int i2 = i + 1;
        while (i2 < length && isTokenChar(this.currentHeader.charAt(i2))) {
            i2++;
        }
        return i2;
    }

    protected boolean isTokenSeparator(char c) {
        return c == ',';
    }

    protected boolean isWhitespace(char c) {
        return c == '\t' || Character.isSpaceChar(c);
    }

    protected boolean isTokenChar(char c) {
        if (Character.isLetterOrDigit(c)) {
            return true;
        }
        return (Character.isISOControl(c) || isHttpSeparator(c)) ? false : true;
    }

    protected boolean isHttpSeparator(char c) {
        return HTTP_SEPARATORS.indexOf(c) >= 0;
    }
}
