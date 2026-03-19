package java.net;

public interface CookiePolicy {
    public static final CookiePolicy ACCEPT_ALL = new CookiePolicy() {
        @Override
        public boolean shouldAccept(URI uri, HttpCookie httpCookie) {
            return true;
        }
    };
    public static final CookiePolicy ACCEPT_NONE = new CookiePolicy() {
        @Override
        public boolean shouldAccept(URI uri, HttpCookie httpCookie) {
            return false;
        }
    };
    public static final CookiePolicy ACCEPT_ORIGINAL_SERVER = new CookiePolicy() {
        @Override
        public boolean shouldAccept(URI uri, HttpCookie httpCookie) {
            if (uri == null || httpCookie == null) {
                return false;
            }
            return HttpCookie.domainMatches(httpCookie.getDomain(), uri.getHost());
        }
    };

    boolean shouldAccept(URI uri, HttpCookie httpCookie);
}
