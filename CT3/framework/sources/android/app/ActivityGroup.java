package android.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.SuperNotCalledException;
import java.util.HashMap;

@Deprecated
public class ActivityGroup extends Activity {
    static final String PARENT_NON_CONFIG_INSTANCE_KEY = "android:parent_non_config_instance";
    private static final String STATES_KEY = "android:states";
    protected LocalActivityManager mLocalActivityManager;

    public ActivityGroup() {
        this(true);
    }

    public ActivityGroup(boolean singleActivityMode) {
        this.mLocalActivityManager = new LocalActivityManager(this, singleActivityMode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mLocalActivityManager.dispatchCreate(savedInstanceState != null ? savedInstanceState.getBundle(STATES_KEY) : null);
    }

    @Override
    protected void onResume() throws SuperNotCalledException {
        super.onResume();
        this.mLocalActivityManager.dispatchResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle state = this.mLocalActivityManager.saveInstanceState();
        if (state == null) {
            return;
        }
        outState.putBundle(STATES_KEY, state);
    }

    @Override
    protected void onPause() throws SuperNotCalledException {
        super.onPause();
        this.mLocalActivityManager.dispatchPause(isFinishing());
    }

    @Override
    protected void onStop() throws SuperNotCalledException {
        super.onStop();
        this.mLocalActivityManager.dispatchStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mLocalActivityManager.dispatchDestroy(isFinishing());
    }

    @Override
    public HashMap<String, Object> onRetainNonConfigurationChildInstances() {
        return this.mLocalActivityManager.dispatchRetainNonConfigurationInstance();
    }

    public Activity getCurrentActivity() {
        return this.mLocalActivityManager.getCurrentActivity();
    }

    public final LocalActivityManager getLocalActivityManager() {
        return this.mLocalActivityManager;
    }

    @Override
    void dispatchActivityResult(String who, int requestCode, int resultCode, Intent data) {
        Activity act;
        if (who != null && (act = this.mLocalActivityManager.getActivity(who)) != null) {
            act.onActivityResult(requestCode, resultCode, data);
        } else {
            super.dispatchActivityResult(who, requestCode, resultCode, data);
        }
    }
}
