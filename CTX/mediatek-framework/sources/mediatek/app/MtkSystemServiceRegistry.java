package mediatek.app;

import android.app.ContextImpl;
import android.app.SystemServiceRegistry;
import android.content.Context;
import android.net.ConnectivityThread;
import android.os.Looper;
import android.os.ServiceManager;
import android.util.Log;
import com.mediatek.search.SearchEngineManager;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Optional;

public final class MtkSystemServiceRegistry {
    private static final String TAG = "MtkSystemServiceRegistry";
    private static HashMap<String, SystemServiceRegistry.ServiceFetcher<?>> sSystemServiceFetchers;
    private static HashMap<Class<?>, String> sSystemServiceNames;

    private MtkSystemServiceRegistry() {
    }

    public static void registerAllService() {
        Log.i(TAG, "registerAllService start");
        registerService(SearchEngineManager.SEARCH_ENGINE_SERVICE, SearchEngineManager.class, new SystemServiceRegistry.CachedServiceFetcher<SearchEngineManager>() {
            public SearchEngineManager m0createService(ContextImpl contextImpl) {
                return new SearchEngineManager(contextImpl);
            }
        });
        registerFmService();
    }

    public static void setMtkSystemServiceName(HashMap<Class<?>, String> map, HashMap<String, SystemServiceRegistry.ServiceFetcher<?>> map2) {
        Log.i(TAG, "setMtkSystemServiceName start names" + map + ",fetchers" + map2);
        sSystemServiceNames = map;
        sSystemServiceFetchers = map2;
    }

    private static <T> void registerService(String str, Class<T> cls, SystemServiceRegistry.ServiceFetcher<T> serviceFetcher) {
        sSystemServiceNames.put(cls, str);
        sSystemServiceFetchers.put(str, serviceFetcher);
    }

    public static void registerFmService() {
        Class<?> cls;
        final Constructor<?> constructor;
        try {
            Class<?> cls2 = Class.forName("com.mediatek.fmradio.FmRadioPackageManager");
            if (cls2 != null && (cls = Class.forName((String) cls2.getMethod("getPackageName", null).invoke(null, new Object[0]))) != null && (constructor = cls.getConstructor(Context.class, Looper.class)) != null) {
                registerService("fm_radio_service", Optional.class, new SystemServiceRegistry.CachedServiceFetcher<Optional>() {
                    public Optional createService(ContextImpl contextImpl) throws ServiceManager.ServiceNotFoundException {
                        Optional optionalEmpty = Optional.empty();
                        try {
                            return Optional.of(constructor.newInstance(contextImpl, ConnectivityThread.getInstanceLooper()));
                        } catch (Exception e) {
                            Log.e(MtkSystemServiceRegistry.TAG, "Exception while creating FmRadioManager object");
                            return optionalEmpty;
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception while getting FmRadioPackageManager class");
        }
    }
}
