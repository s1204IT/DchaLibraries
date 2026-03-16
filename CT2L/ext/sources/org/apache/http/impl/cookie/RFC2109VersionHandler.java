package org.apache.http.impl.cookie;

import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SetCookie;

@Deprecated
public class RFC2109VersionHandler extends AbstractCookieAttributeHandler {
    @Override
    public void parse(SetCookie cookie, String value) throws MalformedCookieException {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        }
        if (value == null) {
            throw new MalformedCookieException("Missing value for version attribute");
        }
        if (value.trim().length() == 0) {
            throw new MalformedCookieException("Blank value for version attribute");
        }
        try {
            cookie.setVersion(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            throw new MalformedCookieException("Invalid version: " + e.getMessage());
        }
    }

    @Override
    public void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        }
        if (cookie.getVersion() < 0) {
            throw new MalformedCookieException("Cookie version may not be negative");
        }
    }
}
