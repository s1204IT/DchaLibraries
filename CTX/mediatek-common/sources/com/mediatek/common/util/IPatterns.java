package com.mediatek.common.util;

public interface IPatterns {
    UrlData getWebUrl(String str, int i, int i2);

    public static class UrlData {
        public int end;
        public int start;
        public String urlStr;

        public UrlData(String str, int i, int i2) {
            this.urlStr = str;
            this.start = i;
            this.end = i2;
        }
    }
}
