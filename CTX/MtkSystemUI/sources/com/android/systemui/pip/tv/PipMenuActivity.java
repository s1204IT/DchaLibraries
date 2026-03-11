package com.android.systemui.pip.tv;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.os.Bundle;
import com.android.systemui.R;
import com.android.systemui.pip.tv.PipManager;
import java.util.Collections;

public class PipMenuActivity extends Activity implements PipManager.Listener {
    private Animator mFadeInAnimation;
    private Animator mFadeOutAnimation;
    private PipControlsView mPipControlsView;
    private final PipManager mPipManager = PipManager.getInstance();
    private boolean mRestorePipSizeWhenClose;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (!this.mPipManager.isPipShown()) {
            finish();
        }
        setContentView(R.layout.tv_pip_menu);
        this.mPipManager.addListener(this);
        this.mRestorePipSizeWhenClose = true;
        this.mPipControlsView = (PipControlsView) findViewById(R.id.pip_controls);
        this.mFadeInAnimation = AnimatorInflater.loadAnimator(this, R.anim.tv_pip_menu_fade_in_animation);
        this.mFadeInAnimation.setTarget(this.mPipControlsView);
        this.mFadeOutAnimation = AnimatorInflater.loadAnimator(this, R.anim.tv_pip_menu_fade_out_animation);
        this.mFadeOutAnimation.setTarget(this.mPipControlsView);
        onPipMenuActionsChanged((ParceledListSlice) getIntent().getParcelableExtra("custom_actions"));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        onPipMenuActionsChanged((ParceledListSlice) getIntent().getParcelableExtra("custom_actions"));
    }

    private void restorePipAndFinish() {
        if (this.mRestorePipSizeWhenClose) {
            this.mPipManager.resizePinnedStack(1);
        }
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mFadeInAnimation.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mFadeOutAnimation.start();
        restorePipAndFinish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mPipManager.removeListener(this);
        this.mPipManager.resumePipResizing(1);
    }

    @Override
    public void onBackPressed() {
        restorePipAndFinish();
    }

    @Override
    public void onPipEntered() {
    }

    @Override
    public void onPipActivityClosed() {
        finish();
    }

    @Override
    public void onPipMenuActionsChanged(ParceledListSlice parceledListSlice) {
        this.mPipControlsView.setActions(parceledListSlice != null && !parceledListSlice.getList().isEmpty() ? parceledListSlice.getList() : Collections.EMPTY_LIST);
    }

    @Override
    public void onShowPipMenu() {
    }

    @Override
    public void onMoveToFullscreen() {
        this.mRestorePipSizeWhenClose = false;
        finish();
    }

    @Override
    public void onPipResizeAboutToStart() {
        finish();
        this.mPipManager.suspendPipResizing(1);
    }
}
