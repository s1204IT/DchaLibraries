package mf.org.apache.xerces.impl.xpath.regex;

import com.mediatek.plugin.preload.SoOperater;
import java.io.Serializable;
import mf.org.apache.xerces.impl.xpath.XPath;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

final class RangeToken extends Token implements Serializable {
    private static final int MAPSIZE = 256;
    private static final long serialVersionUID = -553983121197679934L;
    boolean compacted;
    RangeToken icaseCache;
    int[] map;
    int nonMapIndex;
    int[] ranges;
    boolean sorted;

    RangeToken(int type) {
        super(type);
        this.icaseCache = null;
        this.map = null;
        setSorted(false);
    }

    @Override
    protected void addRange(int start, int end) {
        int r1;
        int r2;
        this.icaseCache = null;
        if (start <= end) {
            r1 = start;
            r2 = end;
        } else {
            r1 = end;
            r2 = start;
        }
        if (this.ranges == null) {
            this.ranges = new int[2];
            this.ranges[0] = r1;
            this.ranges[1] = r2;
            setSorted(true);
            return;
        }
        int pos = this.ranges.length;
        if (this.ranges[pos - 1] + 1 == r1) {
            this.ranges[pos - 1] = r2;
            return;
        }
        int[] temp = new int[pos + 2];
        System.arraycopy(this.ranges, 0, temp, 0, pos);
        this.ranges = temp;
        if (this.ranges[pos - 1] >= r1) {
            setSorted(false);
        }
        this.ranges[pos] = r1;
        this.ranges[pos + 1] = r2;
        if (!this.sorted) {
            sortRanges();
        }
    }

    private final boolean isSorted() {
        return this.sorted;
    }

    private final void setSorted(boolean sort) {
        this.sorted = sort;
        if (!sort) {
            this.compacted = false;
        }
    }

    private final boolean isCompacted() {
        return this.compacted;
    }

    private final void setCompacted() {
        this.compacted = true;
    }

    @Override
    protected void sortRanges() {
        if (isSorted() || this.ranges == null) {
            return;
        }
        for (int i = this.ranges.length - 4; i >= 0; i -= 2) {
            for (int j = 0; j <= i; j += 2) {
                if (this.ranges[j] > this.ranges[j + 2] || (this.ranges[j] == this.ranges[j + 2] && this.ranges[j + 1] > this.ranges[j + 3])) {
                    int tmp = this.ranges[j + 2];
                    this.ranges[j + 2] = this.ranges[j];
                    this.ranges[j] = tmp;
                    int tmp2 = this.ranges[j + 3];
                    this.ranges[j + 3] = this.ranges[j + 1];
                    this.ranges[j + 1] = tmp2;
                }
            }
        }
        setSorted(true);
    }

    @Override
    protected void compactRanges() {
        int target;
        if (this.ranges == null || this.ranges.length <= 2 || isCompacted()) {
            return;
        }
        int base = 0;
        int baseend = 0;
        while (baseend < this.ranges.length) {
            if (base != baseend) {
                int[] iArr = this.ranges;
                int target2 = baseend + 1;
                int target3 = this.ranges[baseend];
                iArr[base] = target3;
                target = target2 + 1;
                this.ranges[base + 1] = this.ranges[target2];
            } else {
                target = baseend + 2;
            }
            int baseend2 = this.ranges[base + 1];
            int baseend3 = baseend2;
            baseend = target;
            while (baseend < this.ranges.length && baseend3 + 1 >= this.ranges[baseend]) {
                if (baseend3 + 1 == this.ranges[baseend]) {
                    if (0 != 0) {
                        System.err.println("Token#compactRanges(): Compaction: [" + this.ranges[base] + ", " + this.ranges[base + 1] + "], [" + this.ranges[baseend] + ", " + this.ranges[baseend + 1] + "] -> [" + this.ranges[base] + ", " + this.ranges[baseend + 1] + "]");
                    }
                    this.ranges[base + 1] = this.ranges[baseend + 1];
                    baseend3 = this.ranges[base + 1];
                    baseend += 2;
                } else if (baseend3 >= this.ranges[baseend + 1]) {
                    if (0 != 0) {
                        System.err.println("Token#compactRanges(): Compaction: [" + this.ranges[base] + ", " + this.ranges[base + 1] + "], [" + this.ranges[baseend] + ", " + this.ranges[baseend + 1] + "] -> [" + this.ranges[base] + ", " + this.ranges[base + 1] + "]");
                    }
                    baseend += 2;
                } else if (baseend3 < this.ranges[baseend + 1]) {
                    if (0 != 0) {
                        System.err.println("Token#compactRanges(): Compaction: [" + this.ranges[base] + ", " + this.ranges[base + 1] + "], [" + this.ranges[baseend] + ", " + this.ranges[baseend + 1] + "] -> [" + this.ranges[base] + ", " + this.ranges[baseend + 1] + "]");
                    }
                    this.ranges[base + 1] = this.ranges[baseend + 1];
                    baseend3 = this.ranges[base + 1];
                    baseend += 2;
                } else {
                    throw new RuntimeException("Token#compactRanges(): Internel Error: [" + this.ranges[base] + "," + this.ranges[base + 1] + "] [" + this.ranges[baseend] + "," + this.ranges[baseend + 1] + "]");
                }
            }
            base += 2;
        }
        if (base != this.ranges.length) {
            int[] result = new int[base];
            System.arraycopy(this.ranges, 0, result, 0, base);
            this.ranges = result;
        }
        setCompacted();
    }

    @Override
    protected void mergeRanges(Token token) {
        int j;
        int i;
        RangeToken tok = (RangeToken) token;
        sortRanges();
        tok.sortRanges();
        if (tok.ranges == null) {
            return;
        }
        this.icaseCache = null;
        setSorted(true);
        if (this.ranges == null) {
            this.ranges = new int[tok.ranges.length];
            System.arraycopy(tok.ranges, 0, this.ranges, 0, tok.ranges.length);
            return;
        }
        int[] result = new int[this.ranges.length + tok.ranges.length];
        int i2 = 0;
        int j2 = 0;
        int k = 0;
        while (true) {
            if (i2 < this.ranges.length || j2 < tok.ranges.length) {
                if (i2 >= this.ranges.length) {
                    int k2 = k + 1;
                    int j3 = j2 + 1;
                    result[k] = tok.ranges[j2];
                    k = k2 + 1;
                    j = j3 + 1;
                    result[k2] = tok.ranges[j3];
                } else {
                    if (j2 >= tok.ranges.length) {
                        int k3 = k + 1;
                        int i3 = i2 + 1;
                        result[k] = this.ranges[i2];
                        k = k3 + 1;
                        i = i3 + 1;
                        result[k3] = this.ranges[i3];
                    } else if (tok.ranges[j2] < this.ranges[i2] || (tok.ranges[j2] == this.ranges[i2] && tok.ranges[j2 + 1] < this.ranges[i2 + 1])) {
                        int k4 = k + 1;
                        int j4 = j2 + 1;
                        result[k] = tok.ranges[j2];
                        k = k4 + 1;
                        j = j4 + 1;
                        result[k4] = tok.ranges[j4];
                    } else {
                        int k5 = k + 1;
                        int i4 = i2 + 1;
                        result[k] = this.ranges[i2];
                        k = k5 + 1;
                        i = i4 + 1;
                        result[k5] = this.ranges[i4];
                    }
                    i2 = i;
                }
                j2 = j;
            } else {
                this.ranges = result;
                return;
            }
        }
    }

    @Override
    protected void subtractRanges(Token token) {
        if (token.type == 5) {
            intersectRanges(token);
            return;
        }
        RangeToken tok = (RangeToken) token;
        if (tok.ranges == null || this.ranges == null) {
            return;
        }
        this.icaseCache = null;
        sortRanges();
        compactRanges();
        tok.sortRanges();
        tok.compactRanges();
        int[] result = new int[this.ranges.length + tok.ranges.length];
        int wp = 0;
        int src = 0;
        int sub = 0;
        while (src < this.ranges.length && sub < tok.ranges.length) {
            int srcbegin = this.ranges[src];
            int srcend = this.ranges[src + 1];
            int subbegin = tok.ranges[sub];
            int subend = tok.ranges[sub + 1];
            if (srcend < subbegin) {
                int wp2 = wp + 1;
                int src2 = src + 1;
                result[wp] = this.ranges[src];
                wp = wp2 + 1;
                result[wp2] = this.ranges[src2];
                src = src2 + 1;
            } else if (srcend < subbegin || srcbegin > subend) {
                if (subend < srcbegin) {
                    sub += 2;
                } else {
                    throw new RuntimeException("Token#subtractRanges(): Internal Error: [" + this.ranges[src] + "," + this.ranges[src + 1] + "] - [" + tok.ranges[sub] + "," + tok.ranges[sub + 1] + "]");
                }
            } else if (subbegin <= srcbegin && srcend <= subend) {
                src += 2;
            } else if (subbegin <= srcbegin) {
                this.ranges[src] = subend + 1;
                sub += 2;
            } else if (srcend <= subend) {
                int wp3 = wp + 1;
                result[wp] = srcbegin;
                wp = wp3 + 1;
                result[wp3] = subbegin - 1;
                src += 2;
            } else {
                int wp4 = wp + 1;
                result[wp] = srcbegin;
                wp = wp4 + 1;
                result[wp4] = subbegin - 1;
                this.ranges[src] = subend + 1;
                sub += 2;
            }
        }
        while (src < this.ranges.length) {
            int wp5 = wp + 1;
            int src3 = src + 1;
            result[wp] = this.ranges[src];
            wp = wp5 + 1;
            result[wp5] = this.ranges[src3];
            src = src3 + 1;
        }
        this.ranges = new int[wp];
        System.arraycopy(result, 0, this.ranges, 0, wp);
    }

    @Override
    protected void intersectRanges(Token token) {
        RangeToken tok = (RangeToken) token;
        if (tok.ranges == null || this.ranges == null) {
            return;
        }
        this.icaseCache = null;
        sortRanges();
        compactRanges();
        tok.sortRanges();
        tok.compactRanges();
        int[] result = new int[this.ranges.length + tok.ranges.length];
        int wp = 0;
        int src1 = 0;
        int src2 = 0;
        while (src1 < this.ranges.length && src2 < tok.ranges.length) {
            int src1begin = this.ranges[src1];
            int src1end = this.ranges[src1 + 1];
            int src2begin = tok.ranges[src2];
            int src2end = tok.ranges[src2 + 1];
            if (src1end < src2begin) {
                src1 += 2;
            } else if (src1end < src2begin || src1begin > src2end) {
                if (src2end < src1begin) {
                    src2 += 2;
                } else {
                    throw new RuntimeException("Token#intersectRanges(): Internal Error: [" + this.ranges[src1] + "," + this.ranges[src1 + 1] + "] & [" + tok.ranges[src2] + "," + tok.ranges[src2 + 1] + "]");
                }
            } else if (src2begin <= src1begin && src1end <= src2end) {
                int wp2 = wp + 1;
                result[wp] = src1begin;
                wp = wp2 + 1;
                result[wp2] = src1end;
                src1 += 2;
            } else if (src2begin <= src1begin) {
                int wp3 = wp + 1;
                result[wp] = src1begin;
                wp = wp3 + 1;
                result[wp3] = src2end;
                this.ranges[src1] = src2end + 1;
                src2 += 2;
            } else if (src1end <= src2end) {
                int wp4 = wp + 1;
                result[wp] = src2begin;
                wp = wp4 + 1;
                result[wp4] = src1end;
                src1 += 2;
            } else {
                int wp5 = wp + 1;
                result[wp] = src2begin;
                wp = wp5 + 1;
                result[wp5] = src2end;
                this.ranges[src1] = src2end + 1;
            }
        }
        while (src1 < this.ranges.length) {
            int wp6 = wp + 1;
            int src12 = src1 + 1;
            result[wp] = this.ranges[src1];
            wp = wp6 + 1;
            result[wp6] = this.ranges[src12];
            src1 = src12 + 1;
        }
        this.ranges = new int[wp];
        System.arraycopy(result, 0, this.ranges, 0, wp);
    }

    static Token complementRanges(Token token) {
        if (token.type != 4 && token.type != 5) {
            throw new IllegalArgumentException("Token#complementRanges(): must be RANGE: " + token.type);
        }
        RangeToken tok = (RangeToken) token;
        tok.sortRanges();
        tok.compactRanges();
        int len = tok.ranges.length + 2;
        if (tok.ranges[0] == 0) {
            len -= 2;
        }
        int last = tok.ranges[tok.ranges.length - 1];
        if (last == 1114111) {
            len -= 2;
        }
        RangeToken ret = Token.createRange();
        ret.ranges = new int[len];
        int wp = 0;
        if (tok.ranges[0] > 0) {
            int wp2 = 0 + 1;
            ret.ranges[0] = 0;
            ret.ranges[wp2] = tok.ranges[0] - 1;
            wp = wp2 + 1;
        }
        int i = 1;
        while (i < tok.ranges.length - 2) {
            int wp3 = wp + 1;
            ret.ranges[wp] = tok.ranges[i] + 1;
            ret.ranges[wp3] = tok.ranges[i + 1] - 1;
            i += 2;
            wp = wp3 + 1;
        }
        if (last != 1114111) {
            ret.ranges[wp] = last + 1;
            ret.ranges[wp + 1] = 1114111;
        }
        ret.setCompacted();
        return ret;
    }

    synchronized RangeToken getCaseInsensitiveToken() {
        if (this.icaseCache != null) {
            return this.icaseCache;
        }
        RangeToken uppers = this.type == 4 ? Token.createRange() : Token.createNRange();
        for (int i = 0; i < this.ranges.length; i += 2) {
            for (int ch = this.ranges[i]; ch <= this.ranges[i + 1]; ch++) {
                if (ch > 65535) {
                    uppers.addRange(ch, ch);
                } else {
                    char uch = Character.toUpperCase((char) ch);
                    uppers.addRange(uch, uch);
                }
            }
        }
        int i2 = this.type;
        RangeToken lowers = i2 == 4 ? Token.createRange() : Token.createNRange();
        for (int i3 = 0; i3 < uppers.ranges.length; i3 += 2) {
            for (int ch2 = uppers.ranges[i3]; ch2 <= uppers.ranges[i3 + 1]; ch2++) {
                if (ch2 > 65535) {
                    lowers.addRange(ch2, ch2);
                } else {
                    char uch2 = Character.toLowerCase((char) ch2);
                    lowers.addRange(uch2, uch2);
                }
            }
        }
        lowers.mergeRanges(uppers);
        lowers.mergeRanges(this);
        lowers.compactRanges();
        this.icaseCache = lowers;
        return lowers;
    }

    void dumpRanges() {
        System.err.print("RANGE: ");
        if (this.ranges == null) {
            System.err.println(" NULL");
            return;
        }
        for (int i = 0; i < this.ranges.length; i += 2) {
            System.err.print("[" + this.ranges[i] + "," + this.ranges[i + 1] + "] ");
        }
        System.err.println("");
    }

    @Override
    boolean match(int ch) {
        boolean ret;
        if (this.map == null) {
            createMap();
        }
        if (this.type == 4) {
            if (ch < 256) {
                return (this.map[ch / 32] & (1 << (ch & 31))) != 0;
            }
            ret = false;
            for (int i = this.nonMapIndex; i < this.ranges.length; i += 2) {
                if (this.ranges[i] <= ch && ch <= this.ranges[i + 1]) {
                    return true;
                }
            }
        } else {
            if (ch < 256) {
                return (this.map[ch / 32] & (1 << (ch & 31))) == 0;
            }
            ret = true;
            for (int i2 = this.nonMapIndex; i2 < this.ranges.length; i2 += 2) {
                if (this.ranges[i2] <= ch && ch <= this.ranges[i2 + 1]) {
                    return false;
                }
            }
        }
        return ret;
    }

    private void createMap() {
        int[] map = new int[8];
        int nonMapIndex = this.ranges.length;
        for (int i = 0; i < 8; i++) {
            map[i] = 0;
        }
        int i2 = 0;
        while (true) {
            if (i2 >= this.ranges.length) {
                break;
            }
            int s = this.ranges[i2];
            int e = this.ranges[i2 + 1];
            if (s < 256) {
                for (int j = s; j <= e && j < 256; j++) {
                    int i3 = j / 32;
                    map[i3] = map[i3] | (1 << (j & 31));
                }
                if (e < 256) {
                    i2 += 2;
                } else {
                    nonMapIndex = i2;
                    break;
                }
            } else {
                nonMapIndex = i2;
                break;
            }
        }
        this.map = map;
        this.nonMapIndex = nonMapIndex;
    }

    @Override
    public String toString(int options) {
        if (this.type == 4) {
            if (this == Token.token_dot) {
                return ".";
            }
            if (this == Token.token_0to9) {
                return "\\d";
            }
            if (this == Token.token_wordchars) {
                return "\\w";
            }
            if (this == Token.token_spaces) {
                return "\\s";
            }
            StringBuffer sb = new StringBuffer();
            sb.append('[');
            for (int i = 0; i < this.ranges.length; i += 2) {
                if ((options & SoOperater.STEP) != 0 && i > 0) {
                    sb.append(',');
                }
                if (this.ranges[i] == this.ranges[i + 1]) {
                    sb.append(escapeCharInCharClass(this.ranges[i]));
                } else {
                    sb.append(escapeCharInCharClass(this.ranges[i]));
                    sb.append('-');
                    sb.append(escapeCharInCharClass(this.ranges[i + 1]));
                }
            }
            sb.append(']');
            String ret = sb.toString();
            return ret;
        }
        if (this == Token.token_not_0to9) {
            return "\\D";
        }
        if (this == Token.token_not_wordchars) {
            return "\\W";
        }
        if (this == Token.token_not_spaces) {
            return "\\S";
        }
        StringBuffer sb2 = new StringBuffer();
        sb2.append("[^");
        for (int i2 = 0; i2 < this.ranges.length; i2 += 2) {
            if ((options & SoOperater.STEP) != 0 && i2 > 0) {
                sb2.append(',');
            }
            if (this.ranges[i2] == this.ranges[i2 + 1]) {
                sb2.append(escapeCharInCharClass(this.ranges[i2]));
            } else {
                sb2.append(escapeCharInCharClass(this.ranges[i2]));
                sb2.append('-');
                sb2.append(escapeCharInCharClass(this.ranges[i2 + 1]));
            }
        }
        sb2.append(']');
        String ret2 = sb2.toString();
        return ret2;
    }

    private static String escapeCharInCharClass(int ch) {
        switch (ch) {
            case 9:
                return "\\t";
            case 10:
                return "\\n";
            case 12:
                return "\\f";
            case 13:
                return "\\r";
            case XPath.Tokens.EXPRTOKEN_OPERATOR_NOT_EQUAL:
                return "\\e";
            case XPath.Tokens.EXPRTOKEN_AXISNAME_PRECEDING_SIBLING:
            case XPath.Tokens.EXPRTOKEN_AXISNAME_SELF:
            case 91:
            case 92:
            case 93:
            case 94:
                String ret = "\\" + ((char) ch);
                return ret;
            default:
                if (ch < 32) {
                    String pre = SchemaSymbols.ATTVAL_FALSE_0 + Integer.toHexString(ch);
                    return "\\x" + pre.substring(pre.length() - 2, pre.length());
                }
                if (ch >= 65536) {
                    String pre2 = SchemaSymbols.ATTVAL_FALSE_0 + Integer.toHexString(ch);
                    return "\\v" + pre2.substring(pre2.length() - 6, pre2.length());
                }
                StringBuilder sb = new StringBuilder();
                sb.append((char) ch);
                String ret2 = sb.toString();
                return ret2;
        }
    }
}
