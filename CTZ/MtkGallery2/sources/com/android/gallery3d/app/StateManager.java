package com.android.gallery3d.app;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import com.android.gallery3d.anim.StateTransitionAnimation;
import com.android.gallery3d.app.ActivityState;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.UsageStatistics;
import com.mediatek.omadrm.OmaDrmInfoRequest;
import com.mediatek.plugin.builder.PluginDescriptorBuilder;
import java.util.Iterator;
import java.util.Stack;

public class StateManager {
    private AbstractGalleryActivity mActivity;
    private ActivityState.ResultEntry mResult;
    private boolean mIsResumed = false;
    private Stack<StateEntry> mStack = new Stack<>();

    public StateManager(AbstractGalleryActivity abstractGalleryActivity) {
        this.mActivity = abstractGalleryActivity;
    }

    public void startState(Class<? extends ActivityState> cls, Bundle bundle) {
        Log.v("Gallery2/StateManager", "startState " + cls);
        try {
            ActivityState activityStateNewInstance = cls.newInstance();
            if (!this.mStack.isEmpty()) {
                ActivityState topState = getTopState();
                topState.transitionOnNextPause(topState.getClass(), cls, StateTransitionAnimation.Transition.Incoming);
                if (this.mIsResumed) {
                    topState.onPause();
                }
            }
            UsageStatistics.onContentViewChanged("Gallery", cls.getSimpleName());
            activityStateNewInstance.initialize(this.mActivity, bundle);
            this.mStack.push(new StateEntry(bundle, activityStateNewInstance));
            activityStateNewInstance.onCreate(bundle, null);
            if (this.mIsResumed) {
                activityStateNewInstance.resume();
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public void startStateForResult(Class<? extends ActivityState> cls, int i, Bundle bundle) {
        Log.v("Gallery2/StateManager", "startStateForResult " + cls + ", " + i);
        try {
            ActivityState activityStateNewInstance = cls.newInstance();
            activityStateNewInstance.initialize(this.mActivity, bundle);
            activityStateNewInstance.mResult = new ActivityState.ResultEntry();
            activityStateNewInstance.mResult.requestCode = i;
            if (!this.mStack.isEmpty()) {
                ActivityState topState = getTopState();
                topState.transitionOnNextPause(topState.getClass(), cls, StateTransitionAnimation.Transition.Incoming);
                topState.mReceivedResults = activityStateNewInstance.mResult;
                if (this.mIsResumed) {
                    topState.onPause();
                }
            } else {
                this.mResult = activityStateNewInstance.mResult;
            }
            UsageStatistics.onContentViewChanged("Gallery", cls.getSimpleName());
            this.mStack.push(new StateEntry(bundle, activityStateNewInstance));
            activityStateNewInstance.onCreate(bundle, null);
            if (this.mIsResumed) {
                activityStateNewInstance.resume();
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public boolean createOptionsMenu(Menu menu) {
        if (this.mStack.isEmpty()) {
            return false;
        }
        return getTopState().onCreateActionBar(menu);
    }

    public void onConfigurationChange(Configuration configuration) {
        Iterator<StateEntry> it = this.mStack.iterator();
        while (it.hasNext()) {
            it.next().activityState.onConfigurationChanged(configuration);
        }
    }

    public void resume() {
        if (this.mIsResumed) {
            return;
        }
        this.mIsResumed = true;
        if (!this.mStack.isEmpty()) {
            getTopState().resume();
        }
    }

    public void pause() {
        if (this.mIsResumed) {
            this.mIsResumed = false;
            if (!this.mStack.isEmpty()) {
                getTopState().onPause();
            }
        }
    }

    public void notifyActivityResult(int i, int i2, Intent intent) {
        getTopState().onStateResult(i, i2, intent);
    }

    public int getStateCount() {
        return this.mStack.size();
    }

    public boolean itemSelected(MenuItem menuItem) {
        if (!this.mStack.isEmpty()) {
            if (getTopState().onItemSelected(menuItem)) {
                return true;
            }
            if (menuItem.getItemId() == 16908332) {
                if (this.mStack.size() > 1) {
                    getTopState().onBackPressed();
                }
                return true;
            }
            return false;
        }
        return false;
    }

    public void onBackPressed() {
        if (!this.mStack.isEmpty()) {
            getTopState().onBackPressed();
        }
    }

    public void finishState(ActivityState activityState) {
        finishState(activityState, true);
    }

    void finishState(ActivityState activityState, boolean z) {
        if (this.mStack.size() == 1) {
            Activity activity = (Activity) this.mActivity.getAndroidContext();
            if (this.mResult != null) {
                activity.setResult(this.mResult.resultCode, this.mResult.resultData);
            }
            activity.finish();
            if (!activity.isFinishing()) {
                Log.w("Gallery2/StateManager", "finish is rejected, keep the last state");
                return;
            }
            Log.v("Gallery2/StateManager", "no more state, finish activity");
        } else if (this.mStack.size() == 0) {
            return;
        }
        Log.v("Gallery2/StateManager", "finishState " + activityState);
        if (activityState != this.mStack.peek().activityState) {
            if (activityState.isDestroyed()) {
                Log.d("Gallery2/StateManager", "The state is already destroyed");
                return;
            }
            throw new IllegalArgumentException("The stateview to be finished is not at the top of the stack: " + activityState + ", " + this.mStack.peek().activityState);
        }
        this.mStack.pop();
        activityState.mIsFinishing = true;
        ActivityState activityState2 = !this.mStack.isEmpty() ? this.mStack.peek().activityState : null;
        if (this.mIsResumed && z) {
            if (activityState2 != null) {
                activityState.transitionOnNextPause(activityState.getClass(), activityState2.getClass(), StateTransitionAnimation.Transition.Outgoing);
            }
            activityState.onPause();
        }
        this.mActivity.getGLRoot().setContentPane(null);
        activityState.onDestroy();
        if (activityState2 != null && this.mIsResumed) {
            activityState2.resume();
        }
        if (activityState2 != null) {
            UsageStatistics.onContentViewChanged("Gallery", activityState2.getClass().getSimpleName());
        }
    }

    public void switchState(ActivityState activityState, Class<? extends ActivityState> cls, Bundle bundle) {
        Log.v("Gallery2/StateManager", "switchState " + activityState + ", " + cls);
        if (activityState != this.mStack.peek().activityState) {
            throw new IllegalArgumentException("The stateview to be finished is not at the top of the stack: " + activityState + ", " + this.mStack.peek().activityState);
        }
        this.mStack.pop();
        if (!bundle.containsKey("app-bridge")) {
            activityState.transitionOnNextPause(activityState.getClass(), cls, StateTransitionAnimation.Transition.Incoming);
        }
        if (this.mIsResumed) {
            activityState.onPause();
        }
        activityState.onDestroy();
        try {
            ActivityState activityStateNewInstance = cls.newInstance();
            activityStateNewInstance.initialize(this.mActivity, bundle);
            this.mStack.push(new StateEntry(bundle, activityStateNewInstance));
            activityStateNewInstance.onCreate(bundle, null);
            if (this.mIsResumed) {
                activityStateNewInstance.resume();
            }
            UsageStatistics.onContentViewChanged("Gallery", cls.getSimpleName());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public void destroy() {
        Log.v("Gallery2/StateManager", "destroy");
        while (!this.mStack.isEmpty()) {
            this.mStack.pop().activityState.onDestroy();
        }
        this.mStack.clear();
    }

    public void restoreFromState(Bundle bundle) {
        Log.v("Gallery2/StateManager", "restoreFromState");
        Parcelable[] parcelableArray = bundle.getParcelableArray("activity-state");
        int length = parcelableArray.length;
        ActivityState activityState = null;
        int i = 0;
        while (i < length) {
            Bundle bundle2 = (Bundle) parcelableArray[i];
            Class cls = (Class) bundle2.getSerializable(PluginDescriptorBuilder.VALUE_CLASS);
            Bundle bundle3 = bundle2.getBundle(OmaDrmInfoRequest.KEY_DATA);
            Bundle bundle4 = bundle2.getBundle("bundle");
            try {
                Log.v("Gallery2/StateManager", "restoreFromState " + cls);
                ActivityState activityState2 = (ActivityState) cls.newInstance();
                activityState2.initialize(this.mActivity, bundle3);
                if ((activityState2 instanceof AlbumSetPage) && bundle4 != null && bundle3 != null) {
                    boolean zReuseDataManager = this.mActivity.getDataManager().reuseDataManager(bundle4.getString("data-manager-object"), bundle4.getString("process-id"));
                    int i2 = bundle3.getInt("selected-cluster", 1);
                    if (!zReuseDataManager && i2 != 1) {
                        Log.v("Gallery2/StateManager", "<restoreFromState> CLUSTER BY:" + i2);
                        bundle3.putString("media-path", this.mActivity.getDataManager().getTopSetPath(3));
                        bundle3.putInt("selected-cluster", 1);
                    }
                }
                if (bundle4 != null && bundle4.getBoolean("has-result")) {
                    activityState2.mResult = new ActivityState.ResultEntry();
                    activityState2.mResult.requestCode = bundle4.getInt("result-request-code");
                    if (!this.mStack.isEmpty()) {
                        this.mStack.peek().activityState.mReceivedResults = activityState2.mResult;
                    }
                }
                activityState2.onCreate(bundle3, bundle4);
                this.mStack.push(new StateEntry(bundle3, activityState2));
                i++;
                activityState = activityState2;
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
        if (activityState != null) {
            UsageStatistics.onContentViewChanged("Gallery", activityState.getClass().getSimpleName());
        }
    }

    public void saveState(Bundle bundle) {
        Log.v("Gallery2/StateManager", "saveState");
        Parcelable[] parcelableArr = new Parcelable[this.mStack.size()];
        int i = 0;
        for (StateEntry stateEntry : this.mStack) {
            Bundle bundle2 = new Bundle();
            bundle2.putSerializable(PluginDescriptorBuilder.VALUE_CLASS, stateEntry.activityState.getClass());
            bundle2.putBundle(OmaDrmInfoRequest.KEY_DATA, stateEntry.data);
            Bundle bundle3 = new Bundle();
            stateEntry.activityState.onSaveState(bundle3);
            if (stateEntry.activityState.mResult != null) {
                bundle3.putBoolean("has-result", true);
                bundle3.putInt("result-request-code", stateEntry.activityState.mResult.requestCode);
            }
            bundle2.putBundle("bundle", bundle3);
            Log.v("Gallery2/StateManager", "saveState " + stateEntry.activityState.getClass());
            parcelableArr[i] = bundle2;
            i++;
        }
        bundle.putParcelableArray("activity-state", parcelableArr);
    }

    public boolean hasStateClass(Class<? extends ActivityState> cls) {
        Iterator<StateEntry> it = this.mStack.iterator();
        while (it.hasNext()) {
            if (cls.isInstance(it.next().activityState)) {
                return true;
            }
        }
        return false;
    }

    public ActivityState getTopState() {
        Utils.assertTrue(!this.mStack.isEmpty());
        return this.mStack.peek().activityState;
    }

    private static class StateEntry {
        public ActivityState activityState;
        public Bundle data;

        public StateEntry(Bundle bundle, ActivityState activityState) {
            this.data = bundle;
            this.activityState = activityState;
        }
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        if (this.mStack.isEmpty()) {
            return false;
        }
        return getTopState().onPrepareOptionsMenu(menu);
    }
}
