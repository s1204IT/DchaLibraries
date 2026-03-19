package android.webkit;

import android.annotation.SystemApi;
import android.os.RemoteException;

@SystemApi
public final class WebViewUpdateService {
    private WebViewUpdateService() {
    }

    public static WebViewProviderInfo[] getAllWebViewPackages() {
        IWebViewUpdateService updateService = getUpdateService();
        if (updateService == null) {
            return new WebViewProviderInfo[0];
        }
        try {
            return updateService.getAllWebViewPackages();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static WebViewProviderInfo[] getValidWebViewPackages() {
        IWebViewUpdateService updateService = getUpdateService();
        if (updateService == null) {
            return new WebViewProviderInfo[0];
        }
        try {
            return updateService.getValidWebViewPackages();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static String getCurrentWebViewPackageName() {
        IWebViewUpdateService updateService = getUpdateService();
        if (updateService == null) {
            return null;
        }
        try {
            return updateService.getCurrentWebViewPackageName();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static IWebViewUpdateService getUpdateService() {
        return WebViewFactory.getUpdateService();
    }
}
