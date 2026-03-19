package android.media;

import android.media.IMediaHTTPService;
import android.os.IBinder;
import android.util.Log;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.util.Iterator;
import java.util.List;

public class MediaHTTPService extends IMediaHTTPService.Stub {
    private static final String TAG = "MediaHTTPService";
    private Boolean mCookieStoreInitialized = new Boolean(false);
    private List<HttpCookie> mCookies;

    public MediaHTTPService(List<HttpCookie> list) {
        this.mCookies = list;
        Log.v(TAG, "MediaHTTPService(" + this + "): Cookies: " + list);
    }

    @Override
    public IMediaHTTPConnection makeHTTPConnection() {
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
        return new MediaHTTPConnection();
    }

    static IBinder createHttpServiceBinderIfNecessary(String str) {
        return createHttpServiceBinderIfNecessary(str, null);
    }

    static IBinder createHttpServiceBinderIfNecessary(String str, List<HttpCookie> list) {
        if (str.startsWith("http://") || str.startsWith("https://")) {
            return new MediaHTTPService(list).asBinder();
        }
        if (str.startsWith("widevine://")) {
            Log.d(TAG, "Widevine classic is no longer supported");
            return null;
        }
        return null;
    }
}
