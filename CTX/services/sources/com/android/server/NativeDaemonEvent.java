package com.android.server;

import com.google.android.collect.Lists;
import java.io.FileDescriptor;
import java.util.ArrayList;

public class NativeDaemonEvent {
    public static final String SENSITIVE_MARKER = "{{sensitive}}";
    private final int mCmdNumber;
    private final int mCode;
    private FileDescriptor[] mFdList;
    private final String mLogMessage;
    private final String mMessage;
    private String[] mParsed = null;
    private final String mRawEvent;

    private NativeDaemonEvent(int i, int i2, String str, String str2, String str3, FileDescriptor[] fileDescriptorArr) {
        this.mCmdNumber = i;
        this.mCode = i2;
        this.mMessage = str;
        this.mRawEvent = str2;
        this.mLogMessage = str3;
        this.mFdList = fileDescriptorArr;
    }

    public int getCmdNumber() {
        return this.mCmdNumber;
    }

    public int getCode() {
        return this.mCode;
    }

    public String getMessage() {
        return this.mMessage;
    }

    public FileDescriptor[] getFileDescriptors() {
        return this.mFdList;
    }

    @Deprecated
    public String getRawEvent() {
        return this.mRawEvent;
    }

    public String toString() {
        return this.mLogMessage;
    }

    public boolean isClassContinue() {
        return this.mCode >= 100 && this.mCode < 200;
    }

    public boolean isClassOk() {
        return this.mCode >= 200 && this.mCode < 300;
    }

    public boolean isClassServerError() {
        return this.mCode >= 400 && this.mCode < 500;
    }

    public boolean isClassClientError() {
        return this.mCode >= 500 && this.mCode < 600;
    }

    public boolean isClassUnsolicited() {
        return isClassUnsolicited(this.mCode);
    }

    private static boolean isClassUnsolicited(int i) {
        return i >= 600 && i < 700;
    }

    public void checkCode(int i) {
        if (this.mCode != i) {
            throw new IllegalStateException("Expected " + i + " but was: " + this);
        }
    }

    public static NativeDaemonEvent parseRawEvent(String str, FileDescriptor[] fileDescriptorArr) {
        String str2;
        String[] strArrSplit = str.split(" ");
        if (strArrSplit.length < 2) {
            throw new IllegalArgumentException("Insufficient arguments");
        }
        try {
            int i = Integer.parseInt(strArrSplit[0]);
            int length = strArrSplit[0].length() + 1;
            int i2 = -1;
            if (!isClassUnsolicited(i)) {
                if (strArrSplit.length < 3) {
                    throw new IllegalArgumentException("Insufficient arguemnts");
                }
                try {
                    i2 = Integer.parseInt(strArrSplit[1]);
                    length += strArrSplit[1].length() + 1;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("problem parsing cmdNumber", e);
                }
            }
            if (strArrSplit.length <= 2 || !strArrSplit[2].equals(SENSITIVE_MARKER)) {
                str2 = str;
            } else {
                length += strArrSplit[2].length() + 1;
                str2 = strArrSplit[0] + " " + strArrSplit[1] + " {}";
            }
            return new NativeDaemonEvent(i2, i, str.substring(length), str, str2, fileDescriptorArr);
        } catch (NumberFormatException e2) {
            throw new IllegalArgumentException("problem parsing code", e2);
        }
    }

    public static String[] filterMessageList(NativeDaemonEvent[] nativeDaemonEventArr, int i) {
        ArrayList arrayListNewArrayList = Lists.newArrayList();
        for (NativeDaemonEvent nativeDaemonEvent : nativeDaemonEventArr) {
            if (nativeDaemonEvent.getCode() == i) {
                arrayListNewArrayList.add(nativeDaemonEvent.getMessage());
            }
        }
        return (String[]) arrayListNewArrayList.toArray(new String[arrayListNewArrayList.size()]);
    }

    public String getField(int i) {
        if (this.mParsed == null) {
            this.mParsed = unescapeArgs(this.mRawEvent);
        }
        int i2 = i + 2;
        if (i2 > this.mParsed.length) {
            return null;
        }
        return this.mParsed[i2];
    }

    public static String[] unescapeArgs(String str) {
        int length;
        ArrayList arrayList = new ArrayList();
        int length2 = str.length();
        if (str.charAt(0) != '\"') {
            length = 0;
        } else {
            length = 1;
        }
        int i = length;
        while (length < length2) {
            char c = i != 0 ? '\"' : ' ';
            int i2 = length;
            while (i2 < length2 && str.charAt(i2) != c) {
                if (str.charAt(i2) == '\\') {
                    i2++;
                }
                i2++;
            }
            if (i2 > length2) {
                i2 = length2;
            }
            String strSubstring = str.substring(length, i2);
            length += strSubstring.length();
            if (i == 0) {
                strSubstring = strSubstring.trim();
            } else {
                length++;
            }
            arrayList.add(strSubstring.replace("\\\\", "\\").replace("\\\"", "\""));
            int iIndexOf = str.indexOf(32, length);
            int iIndexOf2 = str.indexOf(" \"", length);
            if (iIndexOf2 <= -1 || iIndexOf2 > iIndexOf) {
                if (iIndexOf > -1) {
                    length = iIndexOf + 1;
                }
                i = 0;
            } else {
                i = 1;
                length = iIndexOf2 + 2;
            }
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }
}
