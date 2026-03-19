package android.app;

import android.app.ActivityThread;
import android.content.ComponentCallbacks;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.autofill.AutofillManager;
import android.view.autofill.Helper;
import java.util.ArrayList;

public class Application extends ContextWrapper implements ComponentCallbacks2 {
    private static final String TAG = "Application";
    private ArrayList<ActivityLifecycleCallbacks> mActivityLifecycleCallbacks;
    private ArrayList<OnProvideAssistDataListener> mAssistCallbacks;
    private ArrayList<ComponentCallbacks> mComponentCallbacks;
    public LoadedApk mLoadedApk;

    public interface ActivityLifecycleCallbacks {
        void onActivityCreated(Activity activity, Bundle bundle);

        void onActivityDestroyed(Activity activity);

        void onActivityPaused(Activity activity);

        void onActivityResumed(Activity activity);

        void onActivitySaveInstanceState(Activity activity, Bundle bundle);

        void onActivityStarted(Activity activity);

        void onActivityStopped(Activity activity);
    }

    public interface OnProvideAssistDataListener {
        void onProvideAssistData(Activity activity, Bundle bundle);
    }

    public Application() {
        super(null);
        this.mComponentCallbacks = new ArrayList<>();
        this.mActivityLifecycleCallbacks = new ArrayList<>();
        this.mAssistCallbacks = null;
    }

    public void onCreate() {
    }

    public void onTerminate() {
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        Object[] objArrCollectComponentCallbacks = collectComponentCallbacks();
        if (objArrCollectComponentCallbacks != null) {
            for (Object obj : objArrCollectComponentCallbacks) {
                ((ComponentCallbacks) obj).onConfigurationChanged(configuration);
            }
        }
    }

    @Override
    public void onLowMemory() {
        Object[] objArrCollectComponentCallbacks = collectComponentCallbacks();
        if (objArrCollectComponentCallbacks != null) {
            for (Object obj : objArrCollectComponentCallbacks) {
                ((ComponentCallbacks) obj).onLowMemory();
            }
        }
    }

    @Override
    public void onTrimMemory(int i) {
        Object[] objArrCollectComponentCallbacks = collectComponentCallbacks();
        if (objArrCollectComponentCallbacks != null) {
            for (Object obj : objArrCollectComponentCallbacks) {
                if (obj instanceof ComponentCallbacks2) {
                    ((ComponentCallbacks2) obj).onTrimMemory(i);
                }
            }
        }
    }

    @Override
    public void registerComponentCallbacks(ComponentCallbacks componentCallbacks) {
        synchronized (this.mComponentCallbacks) {
            this.mComponentCallbacks.add(componentCallbacks);
        }
    }

    @Override
    public void unregisterComponentCallbacks(ComponentCallbacks componentCallbacks) {
        synchronized (this.mComponentCallbacks) {
            this.mComponentCallbacks.remove(componentCallbacks);
        }
    }

    public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks activityLifecycleCallbacks) {
        synchronized (this.mActivityLifecycleCallbacks) {
            this.mActivityLifecycleCallbacks.add(activityLifecycleCallbacks);
        }
    }

    public void unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacks activityLifecycleCallbacks) {
        synchronized (this.mActivityLifecycleCallbacks) {
            this.mActivityLifecycleCallbacks.remove(activityLifecycleCallbacks);
        }
    }

    public void registerOnProvideAssistDataListener(OnProvideAssistDataListener onProvideAssistDataListener) {
        synchronized (this) {
            if (this.mAssistCallbacks == null) {
                this.mAssistCallbacks = new ArrayList<>();
            }
            this.mAssistCallbacks.add(onProvideAssistDataListener);
        }
    }

    public void unregisterOnProvideAssistDataListener(OnProvideAssistDataListener onProvideAssistDataListener) {
        synchronized (this) {
            if (this.mAssistCallbacks != null) {
                this.mAssistCallbacks.remove(onProvideAssistDataListener);
            }
        }
    }

    public static String getProcessName() {
        return ActivityThread.currentProcessName();
    }

    final void attach(Context context) {
        attachBaseContext(context);
        this.mLoadedApk = ContextImpl.getImpl(context).mPackageInfo;
    }

    void dispatchActivityCreated(Activity activity, Bundle bundle) {
        Object[] objArrCollectActivityLifecycleCallbacks = collectActivityLifecycleCallbacks();
        if (objArrCollectActivityLifecycleCallbacks != null) {
            for (Object obj : objArrCollectActivityLifecycleCallbacks) {
                ((ActivityLifecycleCallbacks) obj).onActivityCreated(activity, bundle);
            }
        }
    }

    void dispatchActivityStarted(Activity activity) {
        Object[] objArrCollectActivityLifecycleCallbacks = collectActivityLifecycleCallbacks();
        if (objArrCollectActivityLifecycleCallbacks != null) {
            for (Object obj : objArrCollectActivityLifecycleCallbacks) {
                ((ActivityLifecycleCallbacks) obj).onActivityStarted(activity);
            }
        }
    }

    void dispatchActivityResumed(Activity activity) {
        Object[] objArrCollectActivityLifecycleCallbacks = collectActivityLifecycleCallbacks();
        if (objArrCollectActivityLifecycleCallbacks != null) {
            for (Object obj : objArrCollectActivityLifecycleCallbacks) {
                ((ActivityLifecycleCallbacks) obj).onActivityResumed(activity);
            }
        }
    }

    void dispatchActivityPaused(Activity activity) {
        Object[] objArrCollectActivityLifecycleCallbacks = collectActivityLifecycleCallbacks();
        if (objArrCollectActivityLifecycleCallbacks != null) {
            for (Object obj : objArrCollectActivityLifecycleCallbacks) {
                ((ActivityLifecycleCallbacks) obj).onActivityPaused(activity);
            }
        }
    }

    void dispatchActivityStopped(Activity activity) {
        Object[] objArrCollectActivityLifecycleCallbacks = collectActivityLifecycleCallbacks();
        if (objArrCollectActivityLifecycleCallbacks != null) {
            for (Object obj : objArrCollectActivityLifecycleCallbacks) {
                ((ActivityLifecycleCallbacks) obj).onActivityStopped(activity);
            }
        }
    }

    void dispatchActivitySaveInstanceState(Activity activity, Bundle bundle) {
        Object[] objArrCollectActivityLifecycleCallbacks = collectActivityLifecycleCallbacks();
        if (objArrCollectActivityLifecycleCallbacks != null) {
            for (Object obj : objArrCollectActivityLifecycleCallbacks) {
                ((ActivityLifecycleCallbacks) obj).onActivitySaveInstanceState(activity, bundle);
            }
        }
    }

    void dispatchActivityDestroyed(Activity activity) {
        Object[] objArrCollectActivityLifecycleCallbacks = collectActivityLifecycleCallbacks();
        if (objArrCollectActivityLifecycleCallbacks != null) {
            for (Object obj : objArrCollectActivityLifecycleCallbacks) {
                ((ActivityLifecycleCallbacks) obj).onActivityDestroyed(activity);
            }
        }
    }

    private Object[] collectComponentCallbacks() {
        Object[] array;
        synchronized (this.mComponentCallbacks) {
            if (this.mComponentCallbacks.size() > 0) {
                array = this.mComponentCallbacks.toArray();
            } else {
                array = null;
            }
        }
        return array;
    }

    private Object[] collectActivityLifecycleCallbacks() {
        Object[] array;
        synchronized (this.mActivityLifecycleCallbacks) {
            if (this.mActivityLifecycleCallbacks.size() > 0) {
                array = this.mActivityLifecycleCallbacks.toArray();
            } else {
                array = null;
            }
        }
        return array;
    }

    void dispatchOnProvideAssistData(Activity activity, Bundle bundle) {
        synchronized (this) {
            if (this.mAssistCallbacks == null) {
                return;
            }
            Object[] array = this.mAssistCallbacks.toArray();
            if (array != null) {
                for (Object obj : array) {
                    ((OnProvideAssistDataListener) obj).onProvideAssistData(activity, bundle);
                }
            }
        }
    }

    @Override
    public AutofillManager.AutofillClient getAutofillClient() {
        Activity activity;
        AutofillManager.AutofillClient autofillClient = super.getAutofillClient();
        if (autofillClient != null) {
            return autofillClient;
        }
        if (Helper.sVerbose) {
            Log.v(TAG, "getAutofillClient(): null on super, trying to find activity thread");
        }
        ActivityThread activityThreadCurrentActivityThread = ActivityThread.currentActivityThread();
        if (activityThreadCurrentActivityThread == null) {
            return null;
        }
        int size = activityThreadCurrentActivityThread.mActivities.size();
        for (int i = 0; i < size; i++) {
            ActivityThread.ActivityClientRecord activityClientRecordValueAt = activityThreadCurrentActivityThread.mActivities.valueAt(i);
            if (activityClientRecordValueAt != null && (activity = activityClientRecordValueAt.activity) != null && activity.getWindow().getDecorView().hasFocus()) {
                if (Helper.sVerbose) {
                    Log.v(TAG, "getAutofillClient(): found activity for " + this + ": " + activity);
                }
                return activity;
            }
        }
        if (Helper.sVerbose) {
            Log.v(TAG, "getAutofillClient(): none of the " + size + " activities on " + this + " have focus");
        }
        return null;
    }
}
