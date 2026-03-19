package org.apache.http.impl.cookie;

import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieAttributeHandler;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SetCookie;

@Deprecated
public class BasicPathHandler implements CookieAttributeHandler {
    @Override
    public void parse(SetCookie setCookie, String str) throws MalformedCookieException {
        if (setCookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        }
        if (str == null || str.trim().length() == 0) {
            str = "/";
        }
        setCookie.setPath(str);
    }

    @Override
    public void validate(Cookie cookie, CookieOrigin cookieOrigin) throws MalformedCookieException {
        if (!match(cookie, cookieOrigin)) {
            throw new MalformedCookieException("Illegal path attribute \"" + cookie.getPath() + "\". Path of origin: \"" + cookieOrigin.getPath() + "\"");
        }
    }

    @Override
    public boolean match(Cookie cookie, CookieOrigin cookieOrigin) {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        }
        if (cookieOrigin == null) {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
        String path = cookieOrigin.getPath();
        String path2 = cookie.getPath();
        if (path2 == null) {
            path2 = "/";
        }
        if (path2.length() > 1 && path2.endsWith("/")) {
            path2 = path2.substring(0, path2.length() - 1);
        }
        boolean zStartsWith = path.startsWith(path2);
        return (!zStartsWith || path.length() == path2.length() || path2.endsWith("/")) ? zStartsWith : path.charAt(path2.length()) == '/';
    }
}
