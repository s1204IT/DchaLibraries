package com.mediatek.camera.common.debug.profiler;

public class LogFormatter {
    private final String mName;

    public LogFormatter(String str) {
        this.mName = str;
    }

    protected final String format(double d, String str) {
        return String.format("[%7sms]%7s %s", String.format("%.3f", Double.valueOf(d)), str, this.mName);
    }

    protected final String format(double d, String str, double d2, String str2) {
        return String.format("[%7sms]%7s %s [%6sms] - %s", String.format("%.3f", Double.valueOf(d)), str, this.mName, String.format("%.3f", Double.valueOf(d2)), str2);
    }
}
