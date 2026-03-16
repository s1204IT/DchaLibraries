package org.apache.http.impl.cookie;

import gov.nist.core.Separators;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieAttributeHandler;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SetCookie;

@Deprecated
public class BasicPathHandler implements CookieAttributeHandler {
    @Override
    public void parse(SetCookie cookie, String value) throws MalformedCookieException {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        }
        if (value == null || value.trim().length() == 0) {
            value = Separators.SLASH;
        }
        cookie.setPath(value);
    }

    @Override
    public void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException {
        if (!match(cookie, origin)) {
            throw new MalformedCookieException("Illegal path attribute \"" + cookie.getPath() + "\". Path of origin: \"" + origin.getPath() + Separators.DOUBLE_QUOTE);
        }
    }

    @Override
    public boolean match(Cookie cookie, CookieOrigin origin) {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        }
        if (origin == null) {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
        String targetpath = origin.getPath();
        String topmostPath = cookie.getPath();
        if (topmostPath == null) {
            topmostPath = Separators.SLASH;
        }
        if (topmostPath.length() > 1 && topmostPath.endsWith(Separators.SLASH)) {
            topmostPath = topmostPath.substring(0, topmostPath.length() - 1);
        }
        boolean match = targetpath.startsWith(topmostPath);
        if (!match || targetpath.length() == topmostPath.length() || topmostPath.endsWith(Separators.SLASH)) {
            return match;
        }
        return targetpath.charAt(topmostPath.length()) == '/';
    }
}
