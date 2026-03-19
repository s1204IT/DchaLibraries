package com.android.server.wifi;

import android.util.Log;
import com.android.internal.annotations.Immutable;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WifiLog;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
@Immutable
class LogcatLog implements WifiLog {
    private final String mTag;
    private static volatile boolean sVerboseLogging = false;
    private static final DummyLogMessage sDummyLogMessage = new DummyLogMessage();
    private static final String[] TRACE_FRAMES_TO_IGNORE = {"getNameOfCallingMethod()", "trace()"};

    LogcatLog(String str) {
        this.mTag = str;
    }

    public static void enableVerboseLogging(int i) {
        if (i > 0) {
            sVerboseLogging = true;
        } else {
            sVerboseLogging = false;
        }
    }

    @Override
    public WifiLog.LogMessage err(String str) {
        return new RealLogMessage(6, this.mTag, str);
    }

    @Override
    public WifiLog.LogMessage warn(String str) {
        return new RealLogMessage(5, this.mTag, str);
    }

    @Override
    public WifiLog.LogMessage info(String str) {
        return new RealLogMessage(4, this.mTag, str);
    }

    @Override
    public WifiLog.LogMessage trace(String str) {
        if (sVerboseLogging) {
            return new RealLogMessage(3, this.mTag, str, getNameOfCallingMethod(0));
        }
        return sDummyLogMessage;
    }

    @Override
    public WifiLog.LogMessage trace(String str, int i) {
        if (sVerboseLogging) {
            return new RealLogMessage(3, this.mTag, str, getNameOfCallingMethod(i));
        }
        return sDummyLogMessage;
    }

    @Override
    public WifiLog.LogMessage dump(String str) {
        if (sVerboseLogging) {
            return new RealLogMessage(2, this.mTag, str);
        }
        return sDummyLogMessage;
    }

    @Override
    public void eC(String str) {
        Log.e(this.mTag, str);
    }

    @Override
    public void wC(String str) {
        Log.w(this.mTag, str);
    }

    @Override
    public void iC(String str) {
        Log.i(this.mTag, str);
    }

    @Override
    public void tC(String str) {
        Log.d(this.mTag, str);
    }

    @Override
    public void e(String str) {
        Log.e(this.mTag, str);
    }

    @Override
    public void w(String str) {
        Log.w(this.mTag, str);
    }

    @Override
    public void i(String str) {
        Log.i(this.mTag, str);
    }

    @Override
    public void d(String str) {
        Log.d(this.mTag, str);
    }

    @Override
    public void v(String str) {
        Log.v(this.mTag, str);
    }

    private static class RealLogMessage implements WifiLog.LogMessage {
        private final String mFormat;
        private final int mLogLevel;
        private int mNextFormatCharPos;
        private final StringBuilder mStringBuilder;
        private final String mTag;

        RealLogMessage(int i, String str, String str2) {
            this(i, str, str2, null);
        }

        RealLogMessage(int i, String str, String str2, String str3) {
            this.mLogLevel = i;
            this.mTag = str;
            this.mFormat = str2;
            this.mStringBuilder = new StringBuilder();
            this.mNextFormatCharPos = 0;
            if (str3 != null) {
                StringBuilder sb = this.mStringBuilder;
                sb.append(str3);
                sb.append(" ");
            }
        }

        @Override
        public WifiLog.LogMessage r(String str) {
            return c(str);
        }

        @Override
        public WifiLog.LogMessage c(String str) {
            copyUntilPlaceholder();
            if (this.mNextFormatCharPos < this.mFormat.length()) {
                this.mStringBuilder.append(str);
                this.mNextFormatCharPos++;
            }
            return this;
        }

        @Override
        public WifiLog.LogMessage c(long j) {
            copyUntilPlaceholder();
            if (this.mNextFormatCharPos < this.mFormat.length()) {
                this.mStringBuilder.append(j);
                this.mNextFormatCharPos++;
            }
            return this;
        }

        @Override
        public WifiLog.LogMessage c(char c) {
            copyUntilPlaceholder();
            if (this.mNextFormatCharPos < this.mFormat.length()) {
                this.mStringBuilder.append(c);
                this.mNextFormatCharPos++;
            }
            return this;
        }

        @Override
        public WifiLog.LogMessage c(boolean z) {
            copyUntilPlaceholder();
            if (this.mNextFormatCharPos < this.mFormat.length()) {
                this.mStringBuilder.append(z);
                this.mNextFormatCharPos++;
            }
            return this;
        }

        @Override
        public void flush() {
            if (this.mNextFormatCharPos < this.mFormat.length()) {
                this.mStringBuilder.append((CharSequence) this.mFormat, this.mNextFormatCharPos, this.mFormat.length());
            }
            Log.println(this.mLogLevel, this.mTag, this.mStringBuilder.toString());
        }

        @VisibleForTesting
        public String toString() {
            return this.mStringBuilder.toString();
        }

        private void copyUntilPlaceholder() {
            if (this.mNextFormatCharPos >= this.mFormat.length()) {
                return;
            }
            int iIndexOf = this.mFormat.indexOf(37, this.mNextFormatCharPos);
            if (iIndexOf == -1) {
                iIndexOf = this.mFormat.length();
            }
            this.mStringBuilder.append((CharSequence) this.mFormat, this.mNextFormatCharPos, iIndexOf);
            this.mNextFormatCharPos = iIndexOf;
        }
    }

    private String getNameOfCallingMethod(int i) {
        try {
            return new Throwable().getStackTrace()[i + TRACE_FRAMES_TO_IGNORE.length].getMethodName();
        } catch (ArrayIndexOutOfBoundsException e) {
            return "<unknown>";
        }
    }
}
