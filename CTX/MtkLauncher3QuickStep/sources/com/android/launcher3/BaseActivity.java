package com.android.launcher3;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.View;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.logging.UserEventDispatcher;
import com.android.launcher3.uioverrides.UiFactory;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.util.SystemUiController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public abstract class BaseActivity extends Activity implements UserEventDispatcher.UserEventDelegate {
    private static final int ACTIVITY_STATE_RESUMED = 2;
    private static final int ACTIVITY_STATE_STARTED = 1;
    private static final int ACTIVITY_STATE_USER_ACTIVE = 4;
    public static final int INVISIBLE_ALL = 3;
    public static final int INVISIBLE_BY_APP_TRANSITIONS = 2;
    public static final int INVISIBLE_BY_STATE_HANDLER = 1;
    private int mActivityFlags;
    protected DeviceProfile mDeviceProfile;
    private int mForceInvisible;
    protected SystemUiController mSystemUiController;
    protected UserEventDispatcher mUserEventDispatcher;
    private final ArrayList<DeviceProfile.OnDeviceProfileChangeListener> mDPChangeListeners = new ArrayList<>();
    private final ArrayList<MultiWindowModeChangedListener> mMultiWindowModeChangedListeners = new ArrayList<>();

    @Retention(RetentionPolicy.SOURCE)
    public @interface ActivityFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface InvisibilityFlags {
    }

    public interface MultiWindowModeChangedListener {
        void onMultiWindowModeChanged(boolean z);
    }

    public DeviceProfile getDeviceProfile() {
        return this.mDeviceProfile;
    }

    public View.AccessibilityDelegate getAccessibilityDelegate() {
        return null;
    }

    @Override
    public void modifyUserEvent(LauncherLogProto.LauncherEvent launcherEvent) {
    }

    public final UserEventDispatcher getUserEventDispatcher() {
        if (this.mUserEventDispatcher == null) {
            this.mUserEventDispatcher = UserEventDispatcher.newInstance(this, this.mDeviceProfile, this);
        }
        return this.mUserEventDispatcher;
    }

    public boolean isInMultiWindowModeCompat() {
        return Utilities.ATLEAST_NOUGAT && isInMultiWindowMode();
    }

    public static BaseActivity fromContext(Context context) {
        if (context instanceof BaseActivity) {
            return (BaseActivity) context;
        }
        return (BaseActivity) ((ContextWrapper) context).getBaseContext();
    }

    public SystemUiController getSystemUiController() {
        if (this.mSystemUiController == null) {
            this.mSystemUiController = new SystemUiController(getWindow());
        }
        return this.mSystemUiController;
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
    }

    @Override
    protected void onStart() {
        this.mActivityFlags |= 1;
        super.onStart();
    }

    @Override
    protected void onResume() {
        this.mActivityFlags |= 6;
        super.onResume();
    }

    @Override
    protected void onUserLeaveHint() {
        this.mActivityFlags &= -5;
        super.onUserLeaveHint();
    }

    @Override
    public void onMultiWindowModeChanged(boolean z, Configuration configuration) {
        super.onMultiWindowModeChanged(z, configuration);
        for (int size = this.mMultiWindowModeChangedListeners.size() - 1; size >= 0; size--) {
            this.mMultiWindowModeChangedListeners.get(size).onMultiWindowModeChanged(z);
        }
    }

    @Override
    protected void onStop() {
        this.mActivityFlags &= -6;
        this.mForceInvisible = 0;
        super.onStop();
    }

    @Override
    protected void onPause() {
        this.mActivityFlags &= -3;
        super.onPause();
        getSystemUiController().updateUiState(4, 0);
    }

    public boolean isStarted() {
        return (this.mActivityFlags & 1) != 0;
    }

    public boolean hasBeenResumed() {
        return (this.mActivityFlags & 2) != 0;
    }

    public boolean isUserActive() {
        return (this.mActivityFlags & 4) != 0;
    }

    public void addOnDeviceProfileChangeListener(DeviceProfile.OnDeviceProfileChangeListener onDeviceProfileChangeListener) {
        this.mDPChangeListeners.add(onDeviceProfileChangeListener);
    }

    public void removeOnDeviceProfileChangeListener(DeviceProfile.OnDeviceProfileChangeListener onDeviceProfileChangeListener) {
        this.mDPChangeListeners.remove(onDeviceProfileChangeListener);
    }

    protected void dispatchDeviceProfileChanged() {
        for (int size = this.mDPChangeListeners.size() - 1; size >= 0; size--) {
            this.mDPChangeListeners.get(size).onDeviceProfileChanged(this.mDeviceProfile);
        }
    }

    public void addMultiWindowModeChangedListener(MultiWindowModeChangedListener multiWindowModeChangedListener) {
        this.mMultiWindowModeChangedListeners.add(multiWindowModeChangedListener);
    }

    public void removeMultiWindowModeChangedListener(MultiWindowModeChangedListener multiWindowModeChangedListener) {
        this.mMultiWindowModeChangedListeners.remove(multiWindowModeChangedListener);
    }

    public void addForceInvisibleFlag(int i) {
        this.mForceInvisible = i | this.mForceInvisible;
    }

    public void clearForceInvisibleFlag(int i) {
        this.mForceInvisible = (~i) & this.mForceInvisible;
    }

    public boolean isForceInvisible() {
        return this.mForceInvisible != 0;
    }

    @Override
    public void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (!UiFactory.dumpActivity(this, printWriter)) {
            super.dump(str, fileDescriptor, printWriter, strArr);
        }
    }

    protected void dumpMisc(PrintWriter printWriter) {
        printWriter.println(" deviceProfile isTransposed=" + getDeviceProfile().isVerticalBarLayout());
        printWriter.println(" orientation=" + getResources().getConfiguration().orientation);
        printWriter.println(" mSystemUiController: " + this.mSystemUiController);
        printWriter.println(" mActivityFlags: " + this.mActivityFlags);
        printWriter.println(" mForceInvisible: " + this.mForceInvisible);
    }
}
