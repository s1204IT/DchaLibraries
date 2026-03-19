package android.webkit;

import android.content.Context;

@Deprecated
public final class CookieSyncManager extends WebSyncManager {
    private static boolean sGetInstanceAllowed = false;
    private static final Object sLock = new Object();
    private static CookieSyncManager sRef;

    @Override
    public void run() {
        super.run();
    }

    private CookieSyncManager() {
        super(null, null);
    }

    public static CookieSyncManager getInstance() {
        CookieSyncManager cookieSyncManager;
        synchronized (sLock) {
            checkInstanceIsAllowed();
            if (sRef == null) {
                sRef = new CookieSyncManager();
            }
            cookieSyncManager = sRef;
        }
        return cookieSyncManager;
    }

    public static CookieSyncManager createInstance(Context context) {
        CookieSyncManager cookieSyncManager;
        synchronized (sLock) {
            if (context == null) {
                throw new IllegalArgumentException("Invalid context argument");
            }
            setGetInstanceIsAllowed();
            cookieSyncManager = getInstance();
        }
        return cookieSyncManager;
    }

    @Override
    @Deprecated
    public void sync() {
        CookieManager.getInstance().flush();
    }

    @Override
    @Deprecated
    protected void syncFromRamToFlash() {
        CookieManager.getInstance().flush();
    }

    @Override
    @Deprecated
    public void resetSync() {
    }

    @Override
    @Deprecated
    public void startSync() {
    }

    @Override
    @Deprecated
    public void stopSync() {
    }

    static void setGetInstanceIsAllowed() {
        sGetInstanceAllowed = true;
    }

    private static void checkInstanceIsAllowed() {
        if (!sGetInstanceAllowed) {
            throw new IllegalStateException("CookieSyncManager::createInstance() needs to be called before CookieSyncManager::getInstance()");
        }
    }
}
