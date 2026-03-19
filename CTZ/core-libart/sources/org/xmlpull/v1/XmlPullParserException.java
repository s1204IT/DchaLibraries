package org.xmlpull.v1;

import android.icu.impl.number.Padder;

public class XmlPullParserException extends Exception {
    protected int column;
    protected Throwable detail;
    protected int row;

    public XmlPullParserException(String str) {
        super(str);
        this.row = -1;
        this.column = -1;
    }

    public XmlPullParserException(String str, XmlPullParser xmlPullParser, Throwable th) {
        String str2;
        String str3;
        String str4;
        StringBuilder sb = new StringBuilder();
        if (str == null) {
            str2 = "";
        } else {
            str2 = str + Padder.FALLBACK_PADDING_STRING;
        }
        sb.append(str2);
        if (xmlPullParser == null) {
            str3 = "";
        } else {
            str3 = "(position:" + xmlPullParser.getPositionDescription() + ") ";
        }
        sb.append(str3);
        if (th == null) {
            str4 = "";
        } else {
            str4 = "caused by: " + th;
        }
        sb.append(str4);
        super(sb.toString());
        this.row = -1;
        this.column = -1;
        if (xmlPullParser != null) {
            this.row = xmlPullParser.getLineNumber();
            this.column = xmlPullParser.getColumnNumber();
        }
        this.detail = th;
    }

    public Throwable getDetail() {
        return this.detail;
    }

    public int getLineNumber() {
        return this.row;
    }

    public int getColumnNumber() {
        return this.column;
    }

    @Override
    public void printStackTrace() {
        if (this.detail == null) {
            super.printStackTrace();
            return;
        }
        synchronized (System.err) {
            System.err.println(super.getMessage() + "; nested exception is:");
            this.detail.printStackTrace();
        }
    }
}
