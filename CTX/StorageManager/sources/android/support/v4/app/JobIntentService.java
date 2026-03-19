package android.support.v4.app;

import android.app.Service;
import android.content.ComponentName;
import android.os.Build;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class JobIntentService extends Service {
    final ArrayList<Object> mCompatQueue;
    static final Object sLock = new Object();
    static final HashMap<ComponentName, Object> sClassWorkEnqueuer = new HashMap<>();
    boolean mInterruptIfStopped = false;
    boolean mStopped = false;
    boolean mDestroyed = false;

    public JobIntentService() {
        if (Build.VERSION.SDK_INT >= 26) {
            this.mCompatQueue = null;
        } else {
            this.mCompatQueue = new ArrayList<>();
        }
    }
}
