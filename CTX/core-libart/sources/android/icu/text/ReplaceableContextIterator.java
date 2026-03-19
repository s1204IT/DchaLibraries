package android.icu.text;

import android.icu.impl.UCaseProps;

class ReplaceableContextIterator implements UCaseProps.ContextIterator {
    protected Replaceable rep = null;
    protected int contextLimit = 0;
    protected int contextStart = 0;
    protected int index = 0;
    protected int cpLimit = 0;
    protected int cpStart = 0;
    protected int limit = 0;
    protected int dir = 0;
    protected boolean reachedLimit = false;

    ReplaceableContextIterator() {
    }

    public void setText(Replaceable replaceable) {
        this.rep = replaceable;
        int length = replaceable.length();
        this.contextLimit = length;
        this.limit = length;
        this.contextStart = 0;
        this.index = 0;
        this.cpLimit = 0;
        this.cpStart = 0;
        this.dir = 0;
        this.reachedLimit = false;
    }

    public void setIndex(int i) {
        this.cpLimit = i;
        this.cpStart = i;
        this.index = 0;
        this.dir = 0;
        this.reachedLimit = false;
    }

    public int getCaseMapCPStart() {
        return this.cpStart;
    }

    public void setLimit(int i) {
        if (i >= 0 && i <= this.rep.length()) {
            this.limit = i;
        } else {
            this.limit = this.rep.length();
        }
        this.reachedLimit = false;
    }

    public void setContextLimits(int i, int i2) {
        if (i < 0) {
            this.contextStart = 0;
        } else if (i <= this.rep.length()) {
            this.contextStart = i;
        } else {
            this.contextStart = this.rep.length();
        }
        if (i2 < this.contextStart) {
            this.contextLimit = this.contextStart;
        } else if (i2 <= this.rep.length()) {
            this.contextLimit = i2;
        } else {
            this.contextLimit = this.rep.length();
        }
        this.reachedLimit = false;
    }

    public int nextCaseMapCP() {
        if (this.cpLimit < this.limit) {
            this.cpStart = this.cpLimit;
            int iChar32At = this.rep.char32At(this.cpLimit);
            this.cpLimit += UTF16.getCharCount(iChar32At);
            return iChar32At;
        }
        return -1;
    }

    public int replace(String str) {
        int length = str.length() - (this.cpLimit - this.cpStart);
        this.rep.replace(this.cpStart, this.cpLimit, str);
        this.cpLimit += length;
        this.limit += length;
        this.contextLimit += length;
        return length;
    }

    public boolean didReachLimit() {
        return this.reachedLimit;
    }

    @Override
    public void reset(int i) {
        if (i > 0) {
            this.dir = 1;
            this.index = this.cpLimit;
        } else if (i < 0) {
            this.dir = -1;
            this.index = this.cpStart;
        } else {
            this.dir = 0;
            this.index = 0;
        }
        this.reachedLimit = false;
    }

    @Override
    public int next() {
        if (this.dir > 0) {
            if (this.index < this.contextLimit) {
                int iChar32At = this.rep.char32At(this.index);
                this.index += UTF16.getCharCount(iChar32At);
                return iChar32At;
            }
            this.reachedLimit = true;
            return -1;
        }
        if (this.dir < 0 && this.index > this.contextStart) {
            int iChar32At2 = this.rep.char32At(this.index - 1);
            this.index -= UTF16.getCharCount(iChar32At2);
            return iChar32At2;
        }
        return -1;
    }
}
