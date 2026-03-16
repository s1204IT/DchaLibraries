package com.android.gallery3d.app;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import com.android.gallery3d.R;
import com.android.gallery3d.anim.StateTransitionAnimation;
import com.android.gallery3d.glrenderer.RawTexture;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.PreparePageFadeoutTexture;
import com.android.gallery3d.util.GalleryUtils;

public abstract class ActivityState {
    protected AbstractGalleryActivity mActivity;
    protected float[] mBackgroundColor;
    private GLView mContentPane;
    protected Bundle mData;
    protected int mFlags;
    private StateTransitionAnimation mIntroAnimation;
    protected ResultEntry mReceivedResults;
    protected ResultEntry mResult;
    private boolean mDestroyed = false;
    private boolean mPlugged = false;
    boolean mIsFinishing = false;
    private StateTransitionAnimation.Transition mNextTransition = StateTransitionAnimation.Transition.None;
    BroadcastReceiver mPowerIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                boolean plugged = intent.getIntExtra("plugged", 0) != 0;
                if (plugged != ActivityState.this.mPlugged) {
                    ActivityState.this.mPlugged = plugged;
                    ActivityState.this.setScreenFlags();
                }
            }
        }
    };

    protected static class ResultEntry {
        public int requestCode;
        public int resultCode = 0;
        public Intent resultData;

        protected ResultEntry() {
        }
    }

    protected ActivityState() {
    }

    protected void setContentPane(GLView content) {
        this.mContentPane = content;
        if (this.mIntroAnimation != null) {
            this.mContentPane.setIntroAnimation(this.mIntroAnimation);
            this.mIntroAnimation = null;
        }
        this.mContentPane.setBackgroundColor(getBackgroundColor());
        this.mActivity.getGLRoot().setContentPane(this.mContentPane);
    }

    void initialize(AbstractGalleryActivity activity, Bundle data) {
        this.mActivity = activity;
        this.mData = data;
    }

    public Bundle getData() {
        return this.mData;
    }

    protected void onBackPressed() {
        this.mActivity.getStateManager().finishState(this);
    }

    protected void setStateResult(int resultCode, Intent data) {
        if (this.mResult != null) {
            this.mResult.resultCode = resultCode;
            this.mResult.resultData = data;
        }
    }

    protected void onConfigurationChanged(Configuration config) {
    }

    protected void onSaveState(Bundle outState) {
    }

    protected void onStateResult(int requestCode, int resultCode, Intent data) {
    }

    protected int getBackgroundColorId() {
        return R.color.default_background;
    }

    protected float[] getBackgroundColor() {
        return this.mBackgroundColor;
    }

    protected void onCreate(Bundle data, Bundle storedState) {
        this.mBackgroundColor = GalleryUtils.intColorToFloatARGBArray(this.mActivity.getResources().getColor(getBackgroundColorId()));
    }

    private void setScreenFlags() {
        Window win = this.mActivity.getWindow();
        WindowManager.LayoutParams params = win.getAttributes();
        if ((this.mFlags & 8) != 0 || (this.mPlugged && (this.mFlags & 4) != 0)) {
            params.flags |= 128;
        } else {
            params.flags &= -129;
        }
        if ((this.mFlags & 16) != 0) {
            params.flags |= 1;
        } else {
            params.flags &= -2;
        }
        if ((this.mFlags & 32) != 0) {
            params.flags |= 524288;
        } else {
            params.flags &= -524289;
        }
        win.setAttributes(params);
    }

    protected void transitionOnNextPause(Class<? extends ActivityState> outgoing, Class<? extends ActivityState> incoming, StateTransitionAnimation.Transition hint) {
        if (outgoing == SinglePhotoPage.class && incoming == AlbumPage.class) {
            this.mNextTransition = StateTransitionAnimation.Transition.Outgoing;
        } else if (outgoing == AlbumPage.class && incoming == SinglePhotoPage.class) {
            this.mNextTransition = StateTransitionAnimation.Transition.PhotoIncoming;
        } else {
            this.mNextTransition = hint;
        }
    }

    protected void performHapticFeedback(int feedbackConstant) {
        this.mActivity.getWindow().getDecorView().performHapticFeedback(feedbackConstant, 1);
    }

    protected void onPause() {
        if ((this.mFlags & 4) != 0) {
            this.mActivity.unregisterReceiver(this.mPowerIntentReceiver);
        }
        if (this.mNextTransition != StateTransitionAnimation.Transition.None) {
            this.mActivity.getTransitionStore().put("transition-in", this.mNextTransition);
            PreparePageFadeoutTexture.prepareFadeOutTexture(this.mActivity, this.mContentPane);
            this.mNextTransition = StateTransitionAnimation.Transition.None;
        }
    }

    void resume() {
        AbstractGalleryActivity activity = this.mActivity;
        ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            if ((this.mFlags & 1) != 0) {
                actionBar.hide();
            } else {
                actionBar.show();
            }
            int stateCount = this.mActivity.getStateManager().getStateCount();
            this.mActivity.getGalleryActionBar().setDisplayOptions(stateCount > 1, true);
            actionBar.setNavigationMode(0);
        }
        activity.invalidateOptionsMenu();
        setScreenFlags();
        boolean lightsOut = (this.mFlags & 2) != 0;
        this.mActivity.getGLRoot().setLightsOutMode(lightsOut);
        ResultEntry entry = this.mReceivedResults;
        if (entry != null) {
            this.mReceivedResults = null;
            onStateResult(entry.requestCode, entry.resultCode, entry.resultData);
        }
        if ((this.mFlags & 4) != 0) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.BATTERY_CHANGED");
            activity.registerReceiver(this.mPowerIntentReceiver, filter);
        }
        onResume();
        this.mActivity.getTransitionStore().clear();
    }

    protected void onResume() {
        RawTexture fade = (RawTexture) this.mActivity.getTransitionStore().get("fade_texture");
        this.mNextTransition = (StateTransitionAnimation.Transition) this.mActivity.getTransitionStore().get("transition-in", StateTransitionAnimation.Transition.None);
        if (this.mNextTransition != StateTransitionAnimation.Transition.None) {
            this.mIntroAnimation = new StateTransitionAnimation(this.mNextTransition, fade);
            this.mNextTransition = StateTransitionAnimation.Transition.None;
        }
    }

    protected boolean onCreateActionBar(Menu menu) {
        return true;
    }

    protected boolean onItemSelected(MenuItem item) {
        return false;
    }

    protected void onDestroy() {
        this.mDestroyed = true;
    }

    boolean isDestroyed() {
        return this.mDestroyed;
    }

    protected MenuInflater getSupportMenuInflater() {
        return this.mActivity.getMenuInflater();
    }
}
