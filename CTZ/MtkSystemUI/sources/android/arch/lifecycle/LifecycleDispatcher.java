package android.arch.lifecycle;

import android.app.Activity;
import android.os.Bundle;
import java.util.concurrent.atomic.AtomicBoolean;

class LifecycleDispatcher {
    private static AtomicBoolean sInitialized = new AtomicBoolean(false);

    static class DispatcherActivityCallback extends EmptyActivityLifecycleCallbacks {
        DispatcherActivityCallback() {
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            ReportFragment.injectIfNeededIn(activity);
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }
    }
}
