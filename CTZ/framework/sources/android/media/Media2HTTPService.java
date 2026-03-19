package android.media;

import android.util.Log;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.util.Iterator;
import java.util.List;

public class Media2HTTPService {
    private static final String TAG = "Media2HTTPService";
    private Boolean mCookieStoreInitialized = new Boolean(false);
    private List<HttpCookie> mCookies;

    public Media2HTTPService(List<HttpCookie> list) {
        this.mCookies = list;
        Log.v(TAG, "Media2HTTPService(" + this + "): Cookies: " + list);
    }

    public Media2HTTPConnection makeHTTPConnection() {
        synchronized (this.mCookieStoreInitialized) {
            if (!this.mCookieStoreInitialized.booleanValue()) {
                CookieHandler cookieManager = CookieHandler.getDefault();
                if (cookieManager == null) {
                    cookieManager = new CookieManager();
                    CookieHandler.setDefault(cookieManager);
                    Log.v(TAG, "makeHTTPConnection: CookieManager created: " + cookieManager);
                } else {
                    Log.v(TAG, "makeHTTPConnection: CookieHandler (" + cookieManager + ") exists.");
                }
                if (this.mCookies != null) {
                    if (cookieManager instanceof CookieManager) {
                        CookieStore cookieStore = ((CookieManager) cookieManager).getCookieStore();
                        Iterator<HttpCookie> it = this.mCookies.iterator();
                        while (it.hasNext()) {
                            try {
                                cookieStore.add(null, it.next());
                            } catch (Exception e) {
                                Log.v(TAG, "makeHTTPConnection: CookieStore.add" + e);
                            }
                        }
                    } else {
                        Log.w(TAG, "makeHTTPConnection: The installed CookieHandler is not a CookieManager. Can’t add the provided cookies to the cookie store.");
                    }
                }
                this.mCookieStoreInitialized = true;
                Log.v(TAG, "makeHTTPConnection(" + this + "): cookieHandler: " + cookieManager + " Cookies: " + this.mCookies);
            }
        }
        return new Media2HTTPConnection();
    }

    static Media2HTTPService createHTTPService(String str) {
        return createHTTPService(str, null);
    }

    static Media2HTTPService createHTTPService(String str, List<HttpCookie> list) {
        if (str.startsWith("http://") || str.startsWith("https://")) {
            return new Media2HTTPService(list);
        }
        if (str.startsWith("widevine://")) {
            Log.d(TAG, "Widevine classic is no longer supported");
            return null;
        }
        return null;
    }
}
