package org.apache.http.impl.cookie;

import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecFactory;
import org.apache.http.cookie.params.CookieSpecPNames;
import org.apache.http.params.HttpParams;

@Deprecated
public class RFC2109SpecFactory implements CookieSpecFactory {
    @Override
    public CookieSpec newInstance(HttpParams params) {
        return params != null ? new RFC2109Spec((String[]) params.getParameter(CookieSpecPNames.DATE_PATTERNS), params.getBooleanParameter(CookieSpecPNames.SINGLE_COOKIE_HEADER, false)) : new RFC2109Spec();
    }
}
