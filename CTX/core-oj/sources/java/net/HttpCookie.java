package java.net;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import libcore.net.http.HttpDate;

public final class HttpCookie implements Cloneable {
    static final TimeZone GMT;
    private static final long MAX_AGE_UNSPECIFIED = -1;
    private static final Set<String> RESERVED_NAMES = new HashSet();
    private static final String SET_COOKIE = "set-cookie:";
    private static final String SET_COOKIE2 = "set-cookie2:";
    static final Map<String, CookieAttributeAssignor> assignors;
    private static final String tspecials = ",;= \t";
    private String comment;
    private String commentURL;
    private String domain;
    private final String header;
    private boolean httpOnly;
    private long maxAge;
    private final String name;
    private String path;
    private String portlist;
    private boolean secure;
    private boolean toDiscard;
    private String value;
    private int version;
    private final long whenCreated;

    interface CookieAttributeAssignor {
        void assign(HttpCookie httpCookie, String str, String str2);
    }

    static {
        RESERVED_NAMES.add("comment");
        RESERVED_NAMES.add("commenturl");
        RESERVED_NAMES.add("discard");
        RESERVED_NAMES.add("domain");
        RESERVED_NAMES.add("expires");
        RESERVED_NAMES.add("httponly");
        RESERVED_NAMES.add("max-age");
        RESERVED_NAMES.add("path");
        RESERVED_NAMES.add("port");
        RESERVED_NAMES.add("secure");
        RESERVED_NAMES.add("version");
        assignors = new HashMap();
        assignors.put("comment", new CookieAttributeAssignor() {
            @Override
            public void assign(HttpCookie httpCookie, String str, String str2) {
                if (httpCookie.getComment() == null) {
                    httpCookie.setComment(str2);
                }
            }
        });
        assignors.put("commenturl", new CookieAttributeAssignor() {
            @Override
            public void assign(HttpCookie httpCookie, String str, String str2) {
                if (httpCookie.getCommentURL() == null) {
                    httpCookie.setCommentURL(str2);
                }
            }
        });
        assignors.put("discard", new CookieAttributeAssignor() {
            @Override
            public void assign(HttpCookie httpCookie, String str, String str2) {
                httpCookie.setDiscard(true);
            }
        });
        assignors.put("domain", new CookieAttributeAssignor() {
            @Override
            public void assign(HttpCookie httpCookie, String str, String str2) {
                if (httpCookie.getDomain() == null) {
                    httpCookie.setDomain(str2);
                }
            }
        });
        assignors.put("max-age", new CookieAttributeAssignor() {
            @Override
            public void assign(HttpCookie httpCookie, String str, String str2) {
                try {
                    long j = Long.parseLong(str2);
                    if (httpCookie.getMaxAge() == -1) {
                        httpCookie.setMaxAge(j);
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Illegal cookie max-age attribute");
                }
            }
        });
        assignors.put("path", new CookieAttributeAssignor() {
            @Override
            public void assign(HttpCookie httpCookie, String str, String str2) {
                if (httpCookie.getPath() == null) {
                    httpCookie.setPath(str2);
                }
            }
        });
        assignors.put("port", new CookieAttributeAssignor() {
            @Override
            public void assign(HttpCookie httpCookie, String str, String str2) {
                if (httpCookie.getPortlist() == null) {
                    if (str2 == null) {
                        str2 = "";
                    }
                    httpCookie.setPortlist(str2);
                }
            }
        });
        assignors.put("secure", new CookieAttributeAssignor() {
            @Override
            public void assign(HttpCookie httpCookie, String str, String str2) {
                httpCookie.setSecure(true);
            }
        });
        assignors.put("httponly", new CookieAttributeAssignor() {
            @Override
            public void assign(HttpCookie httpCookie, String str, String str2) {
                httpCookie.setHttpOnly(true);
            }
        });
        assignors.put("version", new CookieAttributeAssignor() {
            @Override
            public void assign(HttpCookie httpCookie, String str, String str2) {
                try {
                    httpCookie.setVersion(Integer.parseInt(str2));
                } catch (NumberFormatException e) {
                }
            }
        });
        assignors.put("expires", new CookieAttributeAssignor() {
            @Override
            public void assign(HttpCookie httpCookie, String str, String str2) {
                if (httpCookie.getMaxAge() == -1) {
                    Date date = HttpDate.parse(str2);
                    long j = 0;
                    if (date != null) {
                        long time = (date.getTime() - httpCookie.whenCreated) / 1000;
                        if (time != -1) {
                            j = time;
                        }
                    }
                    httpCookie.setMaxAge(j);
                }
            }
        });
        GMT = TimeZone.getTimeZone("GMT");
    }

    public HttpCookie(String str, String str2) {
        this(str, str2, null);
    }

    private HttpCookie(String str, String str2, String str3) {
        this.maxAge = -1L;
        this.version = 1;
        String strTrim = str.trim();
        if (strTrim.length() == 0 || !isToken(strTrim) || strTrim.charAt(0) == '$') {
            throw new IllegalArgumentException("Illegal cookie name");
        }
        this.name = strTrim;
        this.value = str2;
        this.toDiscard = false;
        this.secure = false;
        this.whenCreated = System.currentTimeMillis();
        this.portlist = null;
        this.header = str3;
    }

    public static List<HttpCookie> parse(String str) {
        return parse(str, false);
    }

    private static List<HttpCookie> parse(String str, boolean z) {
        int iGuessCookieVersion = guessCookieVersion(str);
        if (startsWithIgnoreCase(str, SET_COOKIE2)) {
            str = str.substring(SET_COOKIE2.length());
        } else if (startsWithIgnoreCase(str, SET_COOKIE)) {
            str = str.substring(SET_COOKIE.length());
        }
        ArrayList arrayList = new ArrayList();
        if (iGuessCookieVersion == 0) {
            HttpCookie internal = parseInternal(str, z);
            internal.setVersion(0);
            arrayList.add(internal);
        } else {
            Iterator<String> it = splitMultiCookies(str).iterator();
            while (it.hasNext()) {
                HttpCookie internal2 = parseInternal(it.next(), z);
                internal2.setVersion(1);
                arrayList.add(internal2);
            }
        }
        return arrayList;
    }

    public boolean hasExpired() {
        if (this.maxAge == 0) {
            return true;
        }
        return this.maxAge != -1 && (System.currentTimeMillis() - this.whenCreated) / 1000 > this.maxAge;
    }

    public void setComment(String str) {
        this.comment = str;
    }

    public String getComment() {
        return this.comment;
    }

    public void setCommentURL(String str) {
        this.commentURL = str;
    }

    public String getCommentURL() {
        return this.commentURL;
    }

    public void setDiscard(boolean z) {
        this.toDiscard = z;
    }

    public boolean getDiscard() {
        return this.toDiscard;
    }

    public void setPortlist(String str) {
        this.portlist = str;
    }

    public String getPortlist() {
        return this.portlist;
    }

    public void setDomain(String str) {
        if (str != null) {
            this.domain = str.toLowerCase();
        } else {
            this.domain = str;
        }
    }

    public String getDomain() {
        return this.domain;
    }

    public void setMaxAge(long j) {
        this.maxAge = j;
    }

    public long getMaxAge() {
        return this.maxAge;
    }

    public void setPath(String str) {
        this.path = str;
    }

    public String getPath() {
        return this.path;
    }

    public void setSecure(boolean z) {
        this.secure = z;
    }

    public boolean getSecure() {
        return this.secure;
    }

    public String getName() {
        return this.name;
    }

    public void setValue(String str) {
        this.value = str;
    }

    public String getValue() {
        return this.value;
    }

    public int getVersion() {
        return this.version;
    }

    public void setVersion(int i) {
        if (i != 0 && i != 1) {
            throw new IllegalArgumentException("cookie version should be 0 or 1");
        }
        this.version = i;
    }

    public boolean isHttpOnly() {
        return this.httpOnly;
    }

    public void setHttpOnly(boolean z) {
        this.httpOnly = z;
    }

    public static boolean domainMatches(String str, String str2) {
        if (str == null || str2 == null) {
            return false;
        }
        boolean zEqualsIgnoreCase = ".local".equalsIgnoreCase(str);
        int iIndexOf = str.indexOf(46);
        if (iIndexOf == 0) {
            iIndexOf = str.indexOf(46, 1);
        }
        if (!zEqualsIgnoreCase && (iIndexOf == -1 || iIndexOf == str.length() - 1)) {
            return false;
        }
        if (str2.indexOf(46) == -1) {
            if (!zEqualsIgnoreCase) {
            }
            return true;
        }
        int length = str2.length() - str.length();
        if (length == 0) {
            return str2.equalsIgnoreCase(str);
        }
        if (length > 0) {
            str2.substring(0, length);
            if (!str2.substring(length).equalsIgnoreCase(str)) {
                return false;
            }
            if ((!str.startsWith(".") || !isFullyQualifiedDomainName(str, 1)) && !zEqualsIgnoreCase) {
                return false;
            }
            return true;
        }
        if (length != -1 || str.charAt(0) != '.' || !str2.equalsIgnoreCase(str.substring(1))) {
            return false;
        }
        return true;
    }

    private static boolean isFullyQualifiedDomainName(String str, int i) {
        int iIndexOf = str.indexOf(46, i + 1);
        return iIndexOf != -1 && iIndexOf < str.length() - 1;
    }

    public String toString() {
        if (getVersion() > 0) {
            return toRFC2965HeaderString();
        }
        return toNetscapeHeaderString();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof HttpCookie)) {
            return false;
        }
        HttpCookie httpCookie = (HttpCookie) obj;
        return equalsIgnoreCase(getName(), httpCookie.getName()) && equalsIgnoreCase(getDomain(), httpCookie.getDomain()) && Objects.equals(getPath(), httpCookie.getPath());
    }

    public int hashCode() {
        return this.name.toLowerCase().hashCode() + (this.domain != null ? this.domain.toLowerCase().hashCode() : 0) + (this.path != null ? this.path.hashCode() : 0);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static boolean isToken(String str) {
        if (RESERVED_NAMES.contains(str.toLowerCase(Locale.US))) {
            return false;
        }
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt < ' ' || cCharAt >= 127 || tspecials.indexOf(cCharAt) != -1) {
                return false;
            }
        }
        return true;
    }

    private static HttpCookie parseInternal(String str, boolean z) {
        HttpCookie httpCookie;
        String strTrim;
        String strTrim2;
        StringTokenizer stringTokenizer = new StringTokenizer(str, ";");
        try {
            String strNextToken = stringTokenizer.nextToken();
            int iIndexOf = strNextToken.indexOf(61);
            if (iIndexOf != -1) {
                String strTrim3 = strNextToken.substring(0, iIndexOf).trim();
                String strTrim4 = strNextToken.substring(iIndexOf + 1).trim();
                if (z) {
                    httpCookie = new HttpCookie(strTrim3, stripOffSurroundingQuote(strTrim4), str);
                } else {
                    httpCookie = new HttpCookie(strTrim3, stripOffSurroundingQuote(strTrim4));
                }
                while (stringTokenizer.hasMoreTokens()) {
                    String strNextToken2 = stringTokenizer.nextToken();
                    int iIndexOf2 = strNextToken2.indexOf(61);
                    if (iIndexOf2 != -1) {
                        strTrim = strNextToken2.substring(0, iIndexOf2).trim();
                        strTrim2 = strNextToken2.substring(iIndexOf2 + 1).trim();
                    } else {
                        strTrim = strNextToken2.trim();
                        strTrim2 = null;
                    }
                    assignAttribute(httpCookie, strTrim, strTrim2);
                }
                return httpCookie;
            }
            throw new IllegalArgumentException("Invalid cookie name-value pair");
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("Empty cookie header string");
        }
    }

    private static void assignAttribute(HttpCookie httpCookie, String str, String str2) {
        String strStripOffSurroundingQuote = stripOffSurroundingQuote(str2);
        CookieAttributeAssignor cookieAttributeAssignor = assignors.get(str.toLowerCase());
        if (cookieAttributeAssignor != null) {
            cookieAttributeAssignor.assign(httpCookie, str, strStripOffSurroundingQuote);
        }
    }

    private String header() {
        return this.header;
    }

    private String toNetscapeHeaderString() {
        return getName() + "=" + getValue();
    }

    private String toRFC2965HeaderString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append("=\"");
        sb.append(getValue());
        sb.append('\"');
        if (getPath() != null) {
            sb.append(";$Path=\"");
            sb.append(getPath());
            sb.append('\"');
        }
        if (getDomain() != null) {
            sb.append(";$Domain=\"");
            sb.append(getDomain());
            sb.append('\"');
        }
        if (getPortlist() != null) {
            sb.append(";$Port=\"");
            sb.append(getPortlist());
            sb.append('\"');
        }
        return sb.toString();
    }

    private static int guessCookieVersion(String str) {
        String lowerCase = str.toLowerCase();
        return (lowerCase.indexOf("expires=") == -1 && !(lowerCase.indexOf("version=") == -1 && lowerCase.indexOf("max-age") == -1 && !startsWithIgnoreCase(lowerCase, SET_COOKIE2))) ? 1 : 0;
    }

    private static String stripOffSurroundingQuote(String str) {
        if (str != null && str.length() > 2 && str.charAt(0) == '\"' && str.charAt(str.length() - 1) == '\"') {
            return str.substring(1, str.length() - 1);
        }
        if (str != null && str.length() > 2 && str.charAt(0) == '\'' && str.charAt(str.length() - 1) == '\'') {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    private static boolean equalsIgnoreCase(String str, String str2) {
        if (str == str2) {
            return true;
        }
        if (str != null && str2 != null) {
            return str.equalsIgnoreCase(str2);
        }
        return false;
    }

    private static boolean startsWithIgnoreCase(String str, String str2) {
        if (str == null || str2 == null || str.length() < str2.length() || !str2.equalsIgnoreCase(str.substring(0, str2.length()))) {
            return false;
        }
        return true;
    }

    private static List<String> splitMultiCookies(String str) {
        ArrayList arrayList = new ArrayList();
        int i = 0;
        int i2 = 0;
        for (int i3 = 0; i3 < str.length(); i3++) {
            char cCharAt = str.charAt(i3);
            if (cCharAt == '\"') {
                i2++;
            }
            if (cCharAt == ',' && i2 % 2 == 0) {
                arrayList.add(str.substring(i, i3));
                i = i3 + 1;
            }
        }
        arrayList.add(str.substring(i));
        return arrayList;
    }
}
