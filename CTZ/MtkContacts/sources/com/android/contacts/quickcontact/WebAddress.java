package com.android.contacts.quickcontact;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebAddress {
    static Pattern sAddressPattern = Pattern.compile("(?:(http|https|file)\\:\\/\\/)?(?:([-A-Za-z0-9$_.+!*'(),;?&=]+(?:\\:[-A-Za-z0-9$_.+!*'(),;?&=]+)?)@)?([a-zA-Z0-9 -\ud7ff豈-﷏ﷰ-\uffef%_-][a-zA-Z0-9 -\ud7ff豈-﷏ﷰ-\uffef%_\\.-]*|\\[[0-9a-fA-F:\\.]+\\])?(?:\\:([0-9]*))?(\\/?[^#]*)?.*", 2);
    private String mAuthInfo;
    private String mHost;
    private String mPath;
    private int mPort;
    private String mScheme;

    public WebAddress(String str) throws ParseException {
        if (str == null) {
            throw new NullPointerException();
        }
        this.mScheme = "";
        this.mHost = "";
        this.mPort = -1;
        this.mPath = "/";
        this.mAuthInfo = "";
        Matcher matcher = sAddressPattern.matcher(str);
        if (matcher.matches()) {
            String strGroup = matcher.group(1);
            if (strGroup != null) {
                this.mScheme = strGroup.toLowerCase(Locale.ROOT);
            }
            String strGroup2 = matcher.group(2);
            if (strGroup2 != null) {
                this.mAuthInfo = strGroup2;
            }
            String strGroup3 = matcher.group(3);
            if (strGroup3 != null) {
                this.mHost = strGroup3;
            }
            String strGroup4 = matcher.group(4);
            if (strGroup4 != null && strGroup4.length() > 0) {
                try {
                    this.mPort = Integer.parseInt(strGroup4);
                } catch (NumberFormatException e) {
                    throw new ParseException("Bad port");
                }
            }
            String strGroup5 = matcher.group(5);
            if (strGroup5 != null && strGroup5.length() > 0) {
                if (strGroup5.charAt(0) == '/') {
                    this.mPath = strGroup5;
                } else {
                    this.mPath = "/" + strGroup5;
                }
            }
            if (this.mPort != 443 || !this.mScheme.equals("")) {
                if (this.mPort == -1) {
                    if (this.mScheme.equals("https")) {
                        this.mPort = 443;
                    } else {
                        this.mPort = 80;
                    }
                }
            } else {
                this.mScheme = "https";
            }
            if (this.mScheme.equals("")) {
                this.mScheme = "http";
                return;
            }
            return;
        }
        throw new ParseException("Bad address");
    }

    public String toString() {
        String str = "";
        if ((this.mPort != 443 && this.mScheme.equals("https")) || (this.mPort != 80 && this.mScheme.equals("http"))) {
            str = ":" + Integer.toString(this.mPort);
        }
        String str2 = "";
        if (this.mAuthInfo.length() > 0) {
            str2 = this.mAuthInfo + "@";
        }
        return this.mScheme + "://" + str2 + this.mHost + str + this.mPath;
    }

    public class ParseException extends Exception {
        public String response;

        ParseException(String str) {
            this.response = str;
        }
    }
}
