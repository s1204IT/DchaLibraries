package org.apache.http.cookie;

import java.io.Serializable;
import java.util.Comparator;

@Deprecated
public class CookieIdentityComparator implements Serializable, Comparator<Cookie> {
    private static final long serialVersionUID = 4466565437490631532L;

    @Override
    public int compare(Cookie cookie, Cookie cookie2) {
        int iCompareTo = cookie.getName().compareTo(cookie2.getName());
        if (iCompareTo == 0) {
            String domain = cookie.getDomain();
            if (domain == null) {
                domain = "";
            }
            String domain2 = cookie2.getDomain();
            if (domain2 == null) {
                domain2 = "";
            }
            return domain.compareToIgnoreCase(domain2);
        }
        return iCompareTo;
    }
}
