package android.icu.util;

import android.icu.text.UTF16;
import android.icu.text.UnicodeSet;
import java.util.Enumeration;
import java.util.NoSuchElementException;

public final class StringTokenizer implements Enumeration<Object> {
    private static final UnicodeSet DEFAULT_DELIMITERS_ = new UnicodeSet(9, 10, 12, 13, 32, 32);
    private static final UnicodeSet EMPTY_DELIMITER_ = UnicodeSet.EMPTY;
    private static final int TOKEN_SIZE_ = 100;
    private boolean[] delims;
    private boolean m_coalesceDelimiters_;
    private UnicodeSet m_delimiters_;
    private int m_length_;
    private int m_nextOffset_;
    private boolean m_returnDelimiters_;
    private String m_source_;
    private int[] m_tokenLimit_;
    private int m_tokenOffset_;
    private int m_tokenSize_;
    private int[] m_tokenStart_;

    public StringTokenizer(String str, UnicodeSet unicodeSet, boolean z) {
        this(str, unicodeSet, z, false);
    }

    @Deprecated
    public StringTokenizer(String str, UnicodeSet unicodeSet, boolean z, boolean z2) {
        this.m_source_ = str;
        this.m_length_ = str.length();
        if (unicodeSet == null) {
            this.m_delimiters_ = EMPTY_DELIMITER_;
        } else {
            this.m_delimiters_ = unicodeSet;
        }
        this.m_returnDelimiters_ = z;
        this.m_coalesceDelimiters_ = z2;
        this.m_tokenOffset_ = -1;
        this.m_tokenSize_ = -1;
        if (this.m_length_ == 0) {
            this.m_nextOffset_ = -1;
            return;
        }
        this.m_nextOffset_ = 0;
        if (!z) {
            this.m_nextOffset_ = getNextNonDelimiter(0);
        }
    }

    public StringTokenizer(String str, UnicodeSet unicodeSet) {
        this(str, unicodeSet, false, false);
    }

    public StringTokenizer(String str, String str2, boolean z) {
        this(str, str2, z, false);
    }

    @Deprecated
    public StringTokenizer(String str, String str2, boolean z, boolean z2) {
        this.m_delimiters_ = EMPTY_DELIMITER_;
        if (str2 != null && str2.length() > 0) {
            this.m_delimiters_ = new UnicodeSet();
            this.m_delimiters_.addAll(str2);
            checkDelimiters();
        }
        this.m_coalesceDelimiters_ = z2;
        this.m_source_ = str;
        this.m_length_ = str.length();
        this.m_returnDelimiters_ = z;
        this.m_tokenOffset_ = -1;
        this.m_tokenSize_ = -1;
        if (this.m_length_ == 0) {
            this.m_nextOffset_ = -1;
            return;
        }
        this.m_nextOffset_ = 0;
        if (!z) {
            this.m_nextOffset_ = getNextNonDelimiter(0);
        }
    }

    public StringTokenizer(String str, String str2) {
        this(str, str2, false, false);
    }

    public StringTokenizer(String str) {
        this(str, DEFAULT_DELIMITERS_, false, false);
    }

    public boolean hasMoreTokens() {
        return this.m_nextOffset_ >= 0;
    }

    public String nextToken() {
        String strSubstring;
        String strSubstring2;
        int nextDelimiter = -1;
        boolean zContains = true;
        if (this.m_tokenOffset_ < 0) {
            if (this.m_nextOffset_ < 0) {
                throw new NoSuchElementException("No more tokens in String");
            }
            if (this.m_returnDelimiters_) {
                int iCharAt = UTF16.charAt(this.m_source_, this.m_nextOffset_);
                if (this.delims == null) {
                    zContains = this.m_delimiters_.contains(iCharAt);
                } else if (iCharAt >= this.delims.length || !this.delims[iCharAt]) {
                    zContains = false;
                }
                if (zContains) {
                    if (this.m_coalesceDelimiters_) {
                        nextDelimiter = getNextNonDelimiter(this.m_nextOffset_);
                    } else {
                        int charCount = UTF16.getCharCount(iCharAt) + this.m_nextOffset_;
                        if (charCount != this.m_length_) {
                            nextDelimiter = charCount;
                        }
                    }
                } else {
                    nextDelimiter = getNextDelimiter(this.m_nextOffset_);
                }
                if (nextDelimiter < 0) {
                    strSubstring2 = this.m_source_.substring(this.m_nextOffset_);
                } else {
                    strSubstring2 = this.m_source_.substring(this.m_nextOffset_, nextDelimiter);
                }
                this.m_nextOffset_ = nextDelimiter;
                return strSubstring2;
            }
            int nextDelimiter2 = getNextDelimiter(this.m_nextOffset_);
            if (nextDelimiter2 < 0) {
                String strSubstring3 = this.m_source_.substring(this.m_nextOffset_);
                this.m_nextOffset_ = nextDelimiter2;
                return strSubstring3;
            }
            String strSubstring4 = this.m_source_.substring(this.m_nextOffset_, nextDelimiter2);
            this.m_nextOffset_ = getNextNonDelimiter(nextDelimiter2);
            return strSubstring4;
        }
        if (this.m_tokenOffset_ >= this.m_tokenSize_) {
            throw new NoSuchElementException("No more tokens in String");
        }
        if (this.m_tokenLimit_[this.m_tokenOffset_] >= 0) {
            strSubstring = this.m_source_.substring(this.m_tokenStart_[this.m_tokenOffset_], this.m_tokenLimit_[this.m_tokenOffset_]);
        } else {
            strSubstring = this.m_source_.substring(this.m_tokenStart_[this.m_tokenOffset_]);
        }
        this.m_tokenOffset_++;
        this.m_nextOffset_ = -1;
        if (this.m_tokenOffset_ < this.m_tokenSize_) {
            this.m_nextOffset_ = this.m_tokenStart_[this.m_tokenOffset_];
        }
        return strSubstring;
    }

    public String nextToken(String str) {
        this.m_delimiters_ = EMPTY_DELIMITER_;
        if (str != null && str.length() > 0) {
            this.m_delimiters_ = new UnicodeSet();
            this.m_delimiters_.addAll(str);
        }
        return nextToken(this.m_delimiters_);
    }

    public String nextToken(UnicodeSet unicodeSet) {
        this.m_delimiters_ = unicodeSet;
        checkDelimiters();
        this.m_tokenOffset_ = -1;
        this.m_tokenSize_ = -1;
        if (!this.m_returnDelimiters_) {
            this.m_nextOffset_ = getNextNonDelimiter(this.m_nextOffset_);
        }
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
        boolean zContains;
        if (!hasMoreTokens()) {
            return 0;
        }
        if (this.m_tokenOffset_ >= 0) {
            return this.m_tokenSize_ - this.m_tokenOffset_;
        }
        if (this.m_tokenStart_ == null) {
            this.m_tokenStart_ = new int[100];
            this.m_tokenLimit_ = new int[100];
        }
        int i = 0;
        do {
            if (this.m_tokenStart_.length == i) {
                int[] iArr = this.m_tokenStart_;
                int[] iArr2 = this.m_tokenLimit_;
                int length = iArr.length;
                int i2 = length + 100;
                this.m_tokenStart_ = new int[i2];
                this.m_tokenLimit_ = new int[i2];
                System.arraycopy(iArr, 0, this.m_tokenStart_, 0, length);
                System.arraycopy(iArr2, 0, this.m_tokenLimit_, 0, length);
            }
            this.m_tokenStart_[i] = this.m_nextOffset_;
            if (this.m_returnDelimiters_) {
                int iCharAt = UTF16.charAt(this.m_source_, this.m_nextOffset_);
                if (this.delims == null) {
                    zContains = this.m_delimiters_.contains(iCharAt);
                } else {
                    zContains = iCharAt < this.delims.length && this.delims[iCharAt];
                }
                if (zContains) {
                    if (this.m_coalesceDelimiters_) {
                        this.m_tokenLimit_[i] = getNextNonDelimiter(this.m_nextOffset_);
                    } else {
                        int i3 = this.m_nextOffset_ + 1;
                        if (i3 == this.m_length_) {
                            i3 = -1;
                        }
                        this.m_tokenLimit_[i] = i3;
                    }
                } else {
                    this.m_tokenLimit_[i] = getNextDelimiter(this.m_nextOffset_);
                }
                this.m_nextOffset_ = this.m_tokenLimit_[i];
            } else {
                this.m_tokenLimit_[i] = getNextDelimiter(this.m_nextOffset_);
                this.m_nextOffset_ = getNextNonDelimiter(this.m_tokenLimit_[i]);
            }
            i++;
        } while (this.m_nextOffset_ >= 0);
        this.m_tokenOffset_ = 0;
        this.m_tokenSize_ = i;
        this.m_nextOffset_ = this.m_tokenStart_[0];
        return i;
    }

    private int getNextDelimiter(int i) {
        if (i >= 0) {
            if (this.delims == null) {
                while (!this.m_delimiters_.contains(UTF16.charAt(this.m_source_, i)) && (i = i + 1) < this.m_length_) {
                }
            } else {
                do {
                    int iCharAt = UTF16.charAt(this.m_source_, i);
                    if (iCharAt < this.delims.length && this.delims[iCharAt]) {
                        break;
                    }
                    i++;
                } while (i < this.m_length_);
            }
            if (i < this.m_length_) {
                return i;
            }
        }
        return (-1) - this.m_length_;
    }

    private int getNextNonDelimiter(int i) {
        if (i >= 0) {
            if (this.delims == null) {
                while (this.m_delimiters_.contains(UTF16.charAt(this.m_source_, i)) && (i = i + 1) < this.m_length_) {
                }
            } else {
                do {
                    int iCharAt = UTF16.charAt(this.m_source_, i);
                    if (iCharAt >= this.delims.length || !this.delims[iCharAt]) {
                        break;
                    }
                    i++;
                } while (i < this.m_length_);
            }
            if (i < this.m_length_) {
                return i;
            }
        }
        return (-1) - this.m_length_;
    }

    void checkDelimiters() {
        int i = 0;
        if (this.m_delimiters_ == null || this.m_delimiters_.size() == 0) {
            this.delims = new boolean[0];
            return;
        }
        int rangeEnd = this.m_delimiters_.getRangeEnd(this.m_delimiters_.getRangeCount() - 1);
        if (rangeEnd < 127) {
            this.delims = new boolean[rangeEnd + 1];
            while (true) {
                int iCharAt = this.m_delimiters_.charAt(i);
                if (-1 != iCharAt) {
                    this.delims[iCharAt] = true;
                    i++;
                } else {
                    return;
                }
            }
        } else {
            this.delims = null;
        }
    }
}
