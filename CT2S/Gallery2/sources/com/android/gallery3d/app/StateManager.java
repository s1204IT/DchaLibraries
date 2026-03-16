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
import java.util.Stack;

public class StateManager {
    private AbstractGalleryActivity mActivity;
    private ActivityState.ResultEntry mResult;
    private boolean mIsResumed = false;
    private Stack<StateEntry> mStack = new Stack<>();

    public StateManager(AbstractGalleryActivity activity) {
        this.mActivity = activity;
    }

    public void startState(Class<? extends ActivityState> klass, Bundle data) {
        Log.v("StateManager", "startState " + klass);
        try {
            ActivityState state = klass.newInstance();
            if (!this.mStack.isEmpty()) {
                ActivityState top = getTopState();
                top.transitionOnNextPause(top.getClass(), klass, StateTransitionAnimation.Transition.Incoming);
                if (this.mIsResumed) {
                    top.onPause();
                }
            }
            UsageStatistics.onContentViewChanged("Gallery", klass.getSimpleName());
            state.initialize(this.mActivity, data);
            this.mStack.push(new StateEntry(data, state));
            state.onCreate(data, null);
            if (this.mIsResumed) {
                state.resume();
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public void startStateForResult(Class<? extends ActivityState> klass, int requestCode, Bundle data) {
        Log.v("StateManager", "startStateForResult " + klass + ", " + requestCode);
        try {
            ActivityState state = klass.newInstance();
            state.initialize(this.mActivity, data);
            state.mResult = new ActivityState.ResultEntry();
            state.mResult.requestCode = requestCode;
            if (!this.mStack.isEmpty()) {
                ActivityState as = getTopState();
                as.transitionOnNextPause(as.getClass(), klass, StateTransitionAnimation.Transition.Incoming);
                as.mReceivedResults = state.mResult;
                if (this.mIsResumed) {
                    as.onPause();
                }
            } else {
                this.mResult = state.mResult;
            }
            UsageStatistics.onContentViewChanged("Gallery", klass.getSimpleName());
            this.mStack.push(new StateEntry(data, state));
            state.onCreate(data, null);
            if (this.mIsResumed) {
                state.resume();
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

    public void onConfigurationChange(Configuration config) {
        for (StateEntry entry : this.mStack) {
            entry.activityState.onConfigurationChanged(config);
        }
    }

    public void resume() {
        if (!this.mIsResumed) {
            this.mIsResumed = true;
            if (!this.mStack.isEmpty()) {
                getTopState().resume();
            }
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

    public void notifyActivityResult(int requestCode, int resultCode, Intent data) {
        getTopState().onStateResult(requestCode, resultCode, data);
    }

    public int getStateCount() {
        return this.mStack.size();
    }

    public boolean itemSelected(MenuItem item) {
        if (!this.mStack.isEmpty()) {
            if (getTopState().onItemSelected(item)) {
                return true;
            }
            if (item.getItemId() == 16908332) {
                if (this.mStack.size() <= 1) {
                    return true;
                }
                getTopState().onBackPressed();
                return true;
            }
        }
        return false;
    }

    public void onBackPressed() {
        if (!this.mStack.isEmpty()) {
            getTopState().onBackPressed();
        }
    }

    void finishState(ActivityState state) {
        finishState(state, true);
    }

    void finishState(ActivityState activityState, boolean fireOnPause) {
        if (this.mStack.size() == 1) {
            Activity activity = (Activity) this.mActivity.getAndroidContext();
            if (this.mResult != null) {
                activity.setResult(this.mResult.resultCode, this.mResult.resultData);
            }
            activity.finish();
            if (!activity.isFinishing()) {
                Log.w("StateManager", "finish is rejected, keep the last state");
                return;
            }
            Log.v("StateManager", "no more state, finish activity");
        }
        Log.v("StateManager", "finishState " + activityState);
        if (activityState != this.mStack.peek().activityState) {
            if (activityState.isDestroyed()) {
                Log.d("StateManager", "The state is already destroyed");
                return;
            }
            throw new IllegalArgumentException("The stateview to be finished is not at the top of the stack: " + activityState + ", " + this.mStack.peek().activityState);
        }
        this.mStack.pop();
        activityState.mIsFinishing = true;
        ActivityState top = !this.mStack.isEmpty() ? this.mStack.peek().activityState : null;
        if (this.mIsResumed && fireOnPause) {
            if (top != null) {
                activityState.transitionOnNextPause(activityState.getClass(), top.getClass(), StateTransitionAnimation.Transition.Outgoing);
            }
            activityState.onPause();
        }
        this.mActivity.getGLRoot().setContentPane(null);
        activityState.onDestroy();
        if (top != null && this.mIsResumed) {
            top.resume();
        }
        if (top != null) {
            UsageStatistics.onContentViewChanged("Gallery", top.getClass().getSimpleName());
        }
    }

    public void switchState(ActivityState oldState, Class<? extends ActivityState> klass, Bundle data) {
        Log.v("StateManager", "switchState " + oldState + ", " + klass);
        if (oldState != this.mStack.peek().activityState) {
            throw new IllegalArgumentException("The stateview to be finished is not at the top of the stack: " + oldState + ", " + this.mStack.peek().activityState);
        }
        this.mStack.pop();
        if (!data.containsKey("app-bridge")) {
            oldState.transitionOnNextPause(oldState.getClass(), klass, StateTransitionAnimation.Transition.Incoming);
        }
        if (this.mIsResumed) {
            oldState.onPause();
        }
        oldState.onDestroy();
        try {
            ActivityState state = klass.newInstance();
            state.initialize(this.mActivity, data);
            this.mStack.push(new StateEntry(data, state));
            state.onCreate(data, null);
            if (this.mIsResumed) {
                state.resume();
            }
            UsageStatistics.onContentViewChanged("Gallery", klass.getSimpleName());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public void destroy() {
        Log.v("StateManager", "destroy");
        while (!this.mStack.isEmpty()) {
            this.mStack.pop().activityState.onDestroy();
        }
        this.mStack.clear();
    }

    public void restoreFromState(Bundle inState) {
        Log.v("StateManager", "restoreFromState");
        Parcelable[] list = inState.getParcelableArray("activity-state");
        ActivityState topState = null;
        for (Parcelable parcelable : list) {
            Bundle bundle = (Bundle) parcelable;
            Class<? extends ActivityState> klass = (Class) bundle.getSerializable("class");
            Bundle data = bundle.getBundle("data");
            Bundle state = bundle.getBundle("bundle");
            try {
                Log.v("StateManager", "restoreFromState " + klass);
                ActivityState activityState = (ActivityState) klass.newInstance();
                activityState.initialize(this.mActivity, data);
                activityState.onCreate(data, state);
                this.mStack.push(new StateEntry(data, activityState));
                topState = activityState;
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
        if (topState != null) {
            UsageStatistics.onContentViewChanged("Gallery", topState.getClass().getSimpleName());
        }
    }

    public void saveState(Bundle outState) {
        Log.v("StateManager", "saveState");
        Parcelable[] list = new Parcelable[this.mStack.size()];
        int i = 0;
        for (StateEntry entry : this.mStack) {
            Bundle bundle = new Bundle();
            bundle.putSerializable("class", entry.activityState.getClass());
            bundle.putBundle("data", entry.data);
            Bundle state = new Bundle();
            entry.activityState.onSaveState(state);
            bundle.putBundle("bundle", state);
            Log.v("StateManager", "saveState " + entry.activityState.getClass());
            list[i] = bundle;
            i++;
        }
        outState.putParcelableArray("activity-state", list);
    }

    public boolean hasStateClass(Class<? extends ActivityState> klass) {
        for (StateEntry entry : this.mStack) {
            if (klass.isInstance(entry.activityState)) {
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

        public StateEntry(Bundle data, ActivityState state) {
            this.data = data;
            this.activityState = state;
        }
    }
}
