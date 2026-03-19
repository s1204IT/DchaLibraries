package android.icu.impl;

import android.icu.text.SymbolTable;
import android.icu.text.UTF16;
import java.text.ParsePosition;

public class RuleCharacterIterator {
    public static final int DONE = -1;
    public static final int PARSE_ESCAPES = 2;
    public static final int PARSE_VARIABLES = 1;
    public static final int SKIP_WHITESPACE = 4;
    private char[] buf;
    private int bufPos;
    private boolean isEscaped;
    private ParsePosition pos;
    private SymbolTable sym;
    private String text;

    public RuleCharacterIterator(String str, SymbolTable symbolTable, ParsePosition parsePosition) {
        if (str == null || parsePosition.getIndex() > str.length()) {
            throw new IllegalArgumentException();
        }
        this.text = str;
        this.sym = symbolTable;
        this.pos = parsePosition;
        this.buf = null;
    }

    public boolean atEnd() {
        return this.buf == null && this.pos.getIndex() == this.text.length();
    }

    public int next(int i) {
        int i_current;
        this.isEscaped = false;
        while (true) {
            i_current = _current();
            _advance(UTF16.getCharCount(i_current));
            if (i_current == 36 && this.buf == null && (i & 1) != 0 && this.sym != null) {
                String reference = this.sym.parseReference(this.text, this.pos, this.text.length());
                if (reference != null) {
                    this.bufPos = 0;
                    this.buf = this.sym.lookup(reference);
                    if (this.buf == null) {
                        throw new IllegalArgumentException("Undefined variable: " + reference);
                    }
                    if (this.buf.length == 0) {
                        this.buf = null;
                    }
                } else {
                    return i_current;
                }
            } else if ((i & 4) == 0 || !PatternProps.isWhiteSpace(i_current)) {
                break;
            }
        }
        if (i_current == 92 && (i & 2) != 0) {
            int[] iArr = {0};
            int iUnescapeAt = Utility.unescapeAt(lookahead(), iArr);
            jumpahead(iArr[0]);
            this.isEscaped = true;
            if (iUnescapeAt < 0) {
                throw new IllegalArgumentException("Invalid escape");
            }
            return iUnescapeAt;
        }
        return i_current;
    }

    public boolean isEscaped() {
        return this.isEscaped;
    }

    public boolean inVariable() {
        return this.buf != null;
    }

    public Object getPos(Object obj) {
        if (obj == null) {
            return new Object[]{this.buf, new int[]{this.pos.getIndex(), this.bufPos}};
        }
        Object[] objArr = (Object[]) obj;
        objArr[0] = this.buf;
        int[] iArr = (int[]) objArr[1];
        iArr[0] = this.pos.getIndex();
        iArr[1] = this.bufPos;
        return obj;
    }

    public void setPos(Object obj) {
        Object[] objArr = (Object[]) obj;
        this.buf = (char[]) objArr[0];
        int[] iArr = (int[]) objArr[1];
        this.pos.setIndex(iArr[0]);
        this.bufPos = iArr[1];
    }

    public void skipIgnored(int i) {
        if ((i & 4) == 0) {
            return;
        }
        while (true) {
            int i_current = _current();
            if (PatternProps.isWhiteSpace(i_current)) {
                _advance(UTF16.getCharCount(i_current));
            } else {
                return;
            }
        }
    }

    public String lookahead() {
        if (this.buf != null) {
            return new String(this.buf, this.bufPos, this.buf.length - this.bufPos);
        }
        return this.text.substring(this.pos.getIndex());
    }

    public void jumpahead(int i) {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        if (this.buf != null) {
            this.bufPos += i;
            if (this.bufPos > this.buf.length) {
                throw new IllegalArgumentException();
            }
            if (this.bufPos == this.buf.length) {
                this.buf = null;
                return;
            }
            return;
        }
        int index = this.pos.getIndex() + i;
        this.pos.setIndex(index);
        if (index > this.text.length()) {
            throw new IllegalArgumentException();
        }
    }

    public String toString() {
        int index = this.pos.getIndex();
        return this.text.substring(0, index) + '|' + this.text.substring(index);
    }

    private int _current() {
        if (this.buf != null) {
            return UTF16.charAt(this.buf, 0, this.buf.length, this.bufPos);
        }
        int index = this.pos.getIndex();
        if (index < this.text.length()) {
            return UTF16.charAt(this.text, index);
        }
        return -1;
    }

    private void _advance(int i) {
        if (this.buf != null) {
            this.bufPos += i;
            if (this.bufPos == this.buf.length) {
                this.buf = null;
                return;
            }
            return;
        }
        this.pos.setIndex(this.pos.getIndex() + i);
        if (this.pos.getIndex() > this.text.length()) {
            this.pos.setIndex(this.text.length());
        }
    }
}
