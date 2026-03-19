package com.android.internal.util;

import android.net.wifi.WifiEnterpriseConfig;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;

public class IndentingPrintWriter extends PrintWriter {
    private char[] mCurrentIndent;
    private int mCurrentLength;
    private boolean mEmptyLine;
    private StringBuilder mIndentBuilder;
    private char[] mSingleChar;
    private final String mSingleIndent;
    private final int mWrapLength;

    public IndentingPrintWriter(Writer writer, String str) {
        this(writer, str, -1);
    }

    public IndentingPrintWriter(Writer writer, String str, int i) {
        super(writer);
        this.mIndentBuilder = new StringBuilder();
        this.mEmptyLine = true;
        this.mSingleChar = new char[1];
        this.mSingleIndent = str;
        this.mWrapLength = i;
    }

    public IndentingPrintWriter setIndent(String str) {
        this.mIndentBuilder.setLength(0);
        this.mIndentBuilder.append(str);
        this.mCurrentIndent = null;
        return this;
    }

    public IndentingPrintWriter setIndent(int i) {
        this.mIndentBuilder.setLength(0);
        for (int i2 = 0; i2 < i; i2++) {
            increaseIndent();
        }
        return this;
    }

    public IndentingPrintWriter increaseIndent() {
        this.mIndentBuilder.append(this.mSingleIndent);
        this.mCurrentIndent = null;
        return this;
    }

    public IndentingPrintWriter decreaseIndent() {
        this.mIndentBuilder.delete(0, this.mSingleIndent.length());
        this.mCurrentIndent = null;
        return this;
    }

    public IndentingPrintWriter printPair(String str, Object obj) {
        print(str + "=" + String.valueOf(obj) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        return this;
    }

    public IndentingPrintWriter printPair(String str, Object[] objArr) {
        print(str + "=" + Arrays.toString(objArr) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        return this;
    }

    public IndentingPrintWriter printHexPair(String str, int i) {
        print(str + "=0x" + Integer.toHexString(i) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        return this;
    }

    @Override
    public void println() {
        write(10);
    }

    @Override
    public void write(int i) {
        this.mSingleChar[0] = (char) i;
        write(this.mSingleChar, 0, 1);
    }

    @Override
    public void write(String str, int i, int i2) {
        char[] cArr = new char[i2];
        str.getChars(i, i2 - i, cArr, 0);
        write(cArr, 0, i2);
    }

    @Override
    public void write(char[] cArr, int i, int i2) {
        int length = this.mIndentBuilder.length();
        int i3 = i2 + i;
        int i4 = i;
        while (i < i3) {
            int i5 = i + 1;
            char c = cArr[i];
            this.mCurrentLength++;
            if (c == '\n') {
                maybeWriteIndent();
                super.write(cArr, i4, i5 - i4);
                this.mEmptyLine = true;
                this.mCurrentLength = 0;
                i4 = i5;
            }
            if (this.mWrapLength > 0 && this.mCurrentLength >= this.mWrapLength - length) {
                if (!this.mEmptyLine) {
                    super.write(10);
                    this.mEmptyLine = true;
                    this.mCurrentLength = i5 - i4;
                } else {
                    maybeWriteIndent();
                    super.write(cArr, i4, i5 - i4);
                    super.write(10);
                    this.mEmptyLine = true;
                    this.mCurrentLength = 0;
                    i4 = i5;
                }
            }
            i = i5;
        }
        if (i4 != i) {
            maybeWriteIndent();
            super.write(cArr, i4, i - i4);
        }
    }

    private void maybeWriteIndent() {
        if (this.mEmptyLine) {
            this.mEmptyLine = false;
            if (this.mIndentBuilder.length() != 0) {
                if (this.mCurrentIndent == null) {
                    this.mCurrentIndent = this.mIndentBuilder.toString().toCharArray();
                }
                super.write(this.mCurrentIndent, 0, this.mCurrentIndent.length);
            }
        }
    }
}
