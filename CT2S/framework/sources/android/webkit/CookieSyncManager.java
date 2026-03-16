package android.webkit;

import android.content.Context;

@Deprecated
public final class CookieSyncManager extends WebSyncManager {
    private static boolean sGetInstanceAllowed = false;
    private static CookieSyncManager sRef;

    @Override
    public void run() {
        super.run();
    }

    private CookieSyncManager() {
        super(null, null);
    }

    public static synchronized CookieSyncManager getInstance() {
        checkInstanceIsAllowed();
        if (sRef == null) {
            sRef = new CookieSyncManager();
        }
        return sRef;
    }

    public static synchronized CookieSyncManager createInstance(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Invalid context argument");
        }
        setGetInstanceIsAllowed();
        return getInstance();
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
