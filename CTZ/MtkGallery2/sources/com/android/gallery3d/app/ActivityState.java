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
            if ("android.intent.action.BATTERY_CHANGED".equals(intent.getAction())) {
                boolean z = intent.getIntExtra("plugged", 0) != 0;
                if (z != ActivityState.this.mPlugged) {
                    ActivityState.this.mPlugged = z;
                    ActivityState.this.setScreenFlags();
                }
            }
        }
    };
    protected boolean mNotSetActionBarVisibiltyWhenResume = false;

    protected static class ResultEntry {
        public int requestCode;
        public int resultCode = 0;
        public Intent resultData;

        protected ResultEntry() {
        }
    }

    protected ActivityState() {
    }

    protected void setContentPane(GLView gLView) {
        this.mContentPane = gLView;
        if (this.mIntroAnimation != null) {
            this.mContentPane.setIntroAnimation(this.mIntroAnimation);
            this.mIntroAnimation = null;
        }
        this.mContentPane.setBackgroundColor(getBackgroundColor());
        this.mActivity.getGLRoot().setContentPane(this.mContentPane);
    }

    void initialize(AbstractGalleryActivity abstractGalleryActivity, Bundle bundle) {
        this.mActivity = abstractGalleryActivity;
        this.mData = bundle;
    }

    public Bundle getData() {
        return this.mData;
    }

    protected void onBackPressed() {
        this.mActivity.getStateManager().finishState(this);
    }

    protected void setStateResult(int i, Intent intent) {
        if (this.mResult == null) {
            return;
        }
        this.mResult.resultCode = i;
        this.mResult.resultData = intent;
    }

    protected void onConfigurationChanged(Configuration configuration) {
    }

    protected void onSaveState(Bundle bundle) {
    }

    protected void onStateResult(int i, int i2, Intent intent) {
    }

    protected int getBackgroundColorId() {
        return R.color.default_background;
    }

    protected float[] getBackgroundColor() {
        return this.mBackgroundColor;
    }

    protected void onCreate(Bundle bundle, Bundle bundle2) {
        this.mBackgroundColor = GalleryUtils.intColorToFloatARGBArray(this.mActivity.getResources().getColor(getBackgroundColorId()));
    }

    private void setScreenFlags() {
        Window window = this.mActivity.getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        if ((this.mFlags & 8) != 0 || (this.mPlugged && (this.mFlags & 4) != 0)) {
            attributes.flags |= 128;
        } else {
            attributes.flags &= -129;
        }
        if ((this.mFlags & 16) != 0) {
            attributes.flags |= 1;
        } else {
            attributes.flags &= -2;
        }
        if ((this.mFlags & 32) != 0) {
            attributes.flags |= 524288;
        } else {
            attributes.flags &= -524289;
        }
        window.setAttributes(attributes);
    }

    protected void transitionOnNextPause(Class<? extends ActivityState> cls, Class<? extends ActivityState> cls2, StateTransitionAnimation.Transition transition) {
        if (cls == SinglePhotoPage.class && cls2 == AlbumPage.class) {
            this.mNextTransition = StateTransitionAnimation.Transition.Outgoing;
        } else if (cls == AlbumPage.class && cls2 == SinglePhotoPage.class) {
            this.mNextTransition = StateTransitionAnimation.Transition.PhotoIncoming;
        } else {
            this.mNextTransition = transition;
        }
    }

    protected void performHapticFeedback(int i) {
        this.mActivity.getWindow().getDecorView().performHapticFeedback(i, 1);
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
        AbstractGalleryActivity abstractGalleryActivity = this.mActivity;
        ActionBar actionBar = abstractGalleryActivity.getActionBar();
        boolean z = false;
        if (actionBar != null) {
            if (!this.mNotSetActionBarVisibiltyWhenResume) {
                if ((this.mFlags & 1) != 0) {
                    actionBar.hide();
                } else {
                    actionBar.show();
                }
            }
            this.mActivity.getGalleryActionBar().setDisplayOptions(this.mActivity.getStateManager().getStateCount() > 1, true);
            actionBar.setNavigationMode(0);
        }
        abstractGalleryActivity.invalidateOptionsMenu();
        setScreenFlags();
        if ((this.mFlags & 2) != 0) {
            z = true;
        }
        this.mActivity.getGLRoot().setLightsOutMode(z);
        ResultEntry resultEntry = this.mReceivedResults;
        if (resultEntry != null) {
            this.mReceivedResults = null;
            onStateResult(resultEntry.requestCode, resultEntry.resultCode, resultEntry.resultData);
        }
        if ((this.mFlags & 4) != 0) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
            abstractGalleryActivity.registerReceiver(this.mPowerIntentReceiver, intentFilter);
        }
        onResume();
        this.mActivity.getTransitionStore().clear();
    }

    protected void onResume() {
        RawTexture rawTexture = (RawTexture) this.mActivity.getTransitionStore().get(PreparePageFadeoutTexture.KEY_FADE_TEXTURE);
        this.mNextTransition = (StateTransitionAnimation.Transition) this.mActivity.getTransitionStore().get("transition-in", StateTransitionAnimation.Transition.None);
        if (this.mNextTransition != StateTransitionAnimation.Transition.None) {
            this.mIntroAnimation = new StateTransitionAnimation(this.mNextTransition, rawTexture);
            this.mNextTransition = StateTransitionAnimation.Transition.None;
        }
    }

    protected boolean onCreateActionBar(Menu menu) {
        return true;
    }

    protected boolean onItemSelected(MenuItem menuItem) {
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

    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
    }

    public void setProviderSensive(boolean z) {
    }

    public void fakeProviderChange() {
    }
}
