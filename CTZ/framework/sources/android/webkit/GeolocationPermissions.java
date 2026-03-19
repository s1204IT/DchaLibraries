package android.webkit;

import android.annotation.SystemApi;
import java.util.Set;

public class GeolocationPermissions {

    public interface Callback {
        void invoke(String str, boolean z, boolean z2);
    }

    public static GeolocationPermissions getInstance() {
        return WebViewFactory.getProvider().getGeolocationPermissions();
    }

    public void getOrigins(ValueCallback<Set<String>> valueCallback) {
    }

    public void getAllowed(String str, ValueCallback<Boolean> valueCallback) {
    }

    public void clear(String str) {
    }

    public void allow(String str) {
    }

    public void clearAll() {
    }

    @SystemApi
    public GeolocationPermissions() {
    }
}
