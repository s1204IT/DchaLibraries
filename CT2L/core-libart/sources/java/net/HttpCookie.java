package java.net;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.xml.transform.OutputKeys;
import libcore.net.http.HttpDate;
import libcore.util.Objects;

public final class HttpCookie implements Cloneable {
    private static final Set<String> RESERVED_NAMES = new HashSet();
    private String comment;
    private String commentURL;
    private boolean discard;
    private String domain;
    private boolean httpOnly;
    private final String name;
    private String path;
    private String portList;
    private boolean secure;
    private String value;
    private long maxAge = -1;
    private int version = 1;

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
        RESERVED_NAMES.add(OutputKeys.VERSION);
    }

    public static boolean domainMatches(String domainPattern, String host) {
        if (domainPattern == null || host == null) {
            return false;
        }
        String a = host.toLowerCase(Locale.US);
        String b = domainPattern.toLowerCase(Locale.US);
        if (a.equals(b) && (isFullyQualifiedDomainName(a, 0) || InetAddress.isNumeric(a))) {
            return true;
        }
        if (!isFullyQualifiedDomainName(a, 0)) {
            return b.equals(".local");
        }
        if (b.length() == a.length() + 1 && b.startsWith(".") && b.endsWith(a) && isFullyQualifiedDomainName(b, 1)) {
            return true;
        }
        return a.length() > b.length() && a.endsWith(b) && ((b.startsWith(".") && isFullyQualifiedDomainName(b, 1)) || b.equals(".local"));
    }

    static boolean pathMatches(HttpCookie cookie, URI uri) {
        String uriPath = matchablePath(uri.getPath());
        String cookiePath = matchablePath(cookie.getPath());
        return uriPath.startsWith(cookiePath);
    }

    static boolean secureMatches(HttpCookie cookie, URI uri) {
        return !cookie.getSecure() || "https".equalsIgnoreCase(uri.getScheme());
    }

    static boolean portMatches(HttpCookie cookie, URI uri) {
        if (cookie.getPortlist() == null) {
            return true;
        }
        return Arrays.asList(cookie.getPortlist().split(",")).contains(Integer.toString(uri.getEffectivePort()));
    }

    private static String matchablePath(String path) {
        if (path == null) {
            return "/";
        }
        return !path.endsWith("/") ? path + "/" : path;
    }

    private static boolean isFullyQualifiedDomainName(String s, int firstCharacter) {
        int dotPosition = s.indexOf(46, firstCharacter + 1);
        return dotPosition != -1 && dotPosition < s.length() + (-1);
    }

    public static List<HttpCookie> parse(String header) {
        return new CookieParser(header).parse();
    }

    static class CookieParser {
        private static final String ATTRIBUTE_NAME_TERMINATORS = ",;= \t";
        private static final String WHITESPACE = " \t";
        private final String input;
        private final String inputLowerCase;
        private int pos = 0;
        boolean hasExpires = false;
        boolean hasMaxAge = false;
        boolean hasVersion = false;

        CookieParser(String input) {
            this.input = input;
            this.inputLowerCase = input.toLowerCase(Locale.US);
        }

        public List<HttpCookie> parse() {
            List<HttpCookie> cookies = new ArrayList<>(2);
            boolean pre2965 = true;
            if (this.inputLowerCase.startsWith("set-cookie2:")) {
                this.pos += "set-cookie2:".length();
                pre2965 = false;
                this.hasVersion = true;
            } else if (this.inputLowerCase.startsWith("set-cookie:")) {
                this.pos += "set-cookie:".length();
            }
            while (true) {
                String name = readAttributeName(false);
                if (name == null) {
                    if (cookies.isEmpty()) {
                        throw new IllegalArgumentException("No cookies in " + this.input);
                    }
                    return cookies;
                }
                if (!readEqualsSign()) {
                    throw new IllegalArgumentException("Expected '=' after " + name + " in " + this.input);
                }
                String value = readAttributeValue(pre2965 ? ";" : ",;");
                HttpCookie cookie = new HttpCookie(name, value);
                cookie.version = pre2965 ? 0 : 1;
                cookies.add(cookie);
                while (true) {
                    skipWhitespace();
                    if (this.pos == this.input.length()) {
                        break;
                    }
                    if (this.input.charAt(this.pos) == ',') {
                        this.pos++;
                        break;
                    }
                    if (this.input.charAt(this.pos) == ';') {
                        this.pos++;
                    }
                    String attributeName = readAttributeName(true);
                    if (attributeName != null) {
                        String terminators = (pre2965 || "expires".equals(attributeName) || "port".equals(attributeName)) ? ";" : ";,";
                        String attributeValue = null;
                        if (readEqualsSign()) {
                            attributeValue = readAttributeValue(terminators);
                        }
                        setAttribute(cookie, attributeName, attributeValue);
                    }
                }
                if (this.hasExpires) {
                    cookie.version = 0;
                } else if (this.hasMaxAge) {
                    cookie.version = 1;
                }
            }
        }

        private void setAttribute(HttpCookie cookie, String name, String value) {
            if (name.equals("comment") && cookie.comment == null) {
                cookie.comment = value;
                return;
            }
            if (name.equals("commenturl") && cookie.commentURL == null) {
                cookie.commentURL = value;
                return;
            }
            if (name.equals("discard")) {
                cookie.discard = true;
                return;
            }
            if (name.equals("domain") && cookie.domain == null) {
                cookie.domain = value;
                return;
            }
            if (name.equals("expires")) {
                this.hasExpires = true;
                if (cookie.maxAge == -1) {
                    Date date = HttpDate.parse(value);
                    if (date != null) {
                        cookie.setExpires(date);
                        return;
                    } else {
                        cookie.maxAge = 0L;
                        return;
                    }
                }
                return;
            }
            if (name.equals("max-age") && cookie.maxAge == -1) {
                try {
                    long maxAge = Long.parseLong(value);
                    this.hasMaxAge = true;
                    cookie.maxAge = maxAge;
                    return;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid max-age: " + value);
                }
            }
            if (name.equals("path") && cookie.path == null) {
                cookie.path = value;
                return;
            }
            if (name.equals("port") && cookie.portList == null) {
                if (value == null) {
                    value = "";
                }
                cookie.portList = value;
            } else {
                if (name.equals("secure")) {
                    cookie.secure = true;
                    return;
                }
                if (name.equals("httponly")) {
                    cookie.httpOnly = true;
                } else if (name.equals(OutputKeys.VERSION) && !this.hasVersion) {
                    cookie.version = Integer.parseInt(value);
                }
            }
        }

        private String readAttributeName(boolean returnLowerCase) {
            skipWhitespace();
            int c = find(ATTRIBUTE_NAME_TERMINATORS);
            String forSubstring = returnLowerCase ? this.inputLowerCase : this.input;
            String result = this.pos < c ? forSubstring.substring(this.pos, c) : null;
            this.pos = c;
            return result;
        }

        private boolean readEqualsSign() {
            skipWhitespace();
            if (this.pos >= this.input.length() || this.input.charAt(this.pos) != '=') {
                return false;
            }
            this.pos++;
            return true;
        }

        private String readAttributeValue(String terminators) {
            skipWhitespace();
            if (this.pos < this.input.length() && (this.input.charAt(this.pos) == '\"' || this.input.charAt(this.pos) == '\'')) {
                String str = this.input;
                int i = this.pos;
                this.pos = i + 1;
                char quoteCharacter = str.charAt(i);
                int closeQuote = this.input.indexOf(quoteCharacter, this.pos);
                if (closeQuote == -1) {
                    throw new IllegalArgumentException("Unterminated string literal in " + this.input);
                }
                String result = this.input.substring(this.pos, closeQuote);
                this.pos = closeQuote + 1;
                return result;
            }
            int c = find(terminators);
            String result2 = this.input.substring(this.pos, c);
            this.pos = c;
            return result2;
        }

        private int find(String chars) {
            for (int c = this.pos; c < this.input.length(); c++) {
                if (chars.indexOf(this.input.charAt(c)) != -1) {
                    return c;
                }
            }
            int c2 = this.input.length();
            return c2;
        }

        private void skipWhitespace() {
            while (this.pos < this.input.length() && WHITESPACE.indexOf(this.input.charAt(this.pos)) != -1) {
                this.pos++;
            }
        }
    }

    public HttpCookie(String name, String value) {
        String ntrim = name.trim();
        if (!isValidName(ntrim)) {
            throw new IllegalArgumentException("Invalid name: " + name);
        }
        this.name = ntrim;
        this.value = value;
    }

    private boolean isValidName(String n) {
        boolean isValid = (n.length() == 0 || n.startsWith("$") || RESERVED_NAMES.contains(n.toLowerCase(Locale.US))) ? false : true;
        if (isValid) {
            for (int i = 0; i < n.length(); i++) {
                char nameChar = n.charAt(i);
                if (nameChar < 0 || nameChar >= 127 || nameChar == ';' || nameChar == ',' || (Character.isWhitespace(nameChar) && nameChar != ' ')) {
                    return false;
                }
            }
            return isValid;
        }
        return isValid;
    }

    public String getComment() {
        return this.comment;
    }

    public String getCommentURL() {
        return this.commentURL;
    }

    public boolean getDiscard() {
        return this.discard;
    }

    public String getDomain() {
        return this.domain;
    }

    public long getMaxAge() {
        return this.maxAge;
    }

    public String getName() {
        return this.name;
    }

    public String getPath() {
        return this.path;
    }

    public String getPortlist() {
        return this.portList;
    }

    public boolean getSecure() {
        return this.secure;
    }

    public String getValue() {
        return this.value;
    }

    public int getVersion() {
        return this.version;
    }

    public boolean hasExpired() {
        if (this.maxAge == -1 || this.maxAge > 0) {
            return false;
        }
        return true;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setCommentURL(String commentURL) {
        this.commentURL = commentURL;
    }

    public void setDiscard(boolean discard) {
        this.discard = discard;
    }

    public void setDomain(String pattern) {
        this.domain = pattern == null ? null : pattern.toLowerCase(Locale.US);
    }

    public void setMaxAge(long deltaSeconds) {
        this.maxAge = deltaSeconds;
    }

    private void setExpires(Date expires) {
        this.maxAge = (expires.getTime() - System.currentTimeMillis()) / 1000;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setPortlist(String portList) {
        this.portList = portList;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setVersion(int newVersion) {
        if (newVersion != 0 && newVersion != 1) {
            throw new IllegalArgumentException("Bad version: " + newVersion);
        }
        this.version = newVersion;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof HttpCookie)) {
            return false;
        }
        HttpCookie that = (HttpCookie) object;
        return this.name.equalsIgnoreCase(that.getName()) && (this.domain == null ? that.domain == null : this.domain.equalsIgnoreCase(that.domain)) && Objects.equal(this.path, that.path);
    }

    public int hashCode() {
        return (this.domain == null ? 0 : this.domain.toLowerCase(Locale.US).hashCode()) + this.name.toLowerCase(Locale.US).hashCode() + (this.path != null ? this.path.hashCode() : 0);
    }

    public String toString() {
        if (this.version == 0) {
            return this.name + "=" + this.value;
        }
        StringBuilder result = new StringBuilder().append(this.name).append("=").append("\"").append(this.value).append("\"");
        appendAttribute(result, "Path", this.path);
        appendAttribute(result, "Domain", this.domain);
        appendAttribute(result, "Port", this.portList);
        return result.toString();
    }

    private void appendAttribute(StringBuilder builder, String name, String value) {
        if (value != null && builder != null) {
            builder.append(";$");
            builder.append(name);
            builder.append("=\"");
            builder.append(value);
            builder.append("\"");
        }
    }
}
