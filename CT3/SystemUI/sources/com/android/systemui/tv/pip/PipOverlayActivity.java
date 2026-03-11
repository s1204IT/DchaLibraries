package com.android.systemui.tv.pip;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import com.android.systemui.R;
import com.android.systemui.tv.pip.PipManager;

public class PipOverlayActivity extends Activity implements PipManager.Listener {
    private static boolean sActivityCreated;
    private Animator mFadeInAnimation;
    private Animator mFadeOutAnimation;
    private View mGuideOverlayView;
    private final PipManager mPipManager = PipManager.getInstance();
    private final Handler mHandler = new Handler();
    private final Runnable mHideGuideOverlayRunnable = new Runnable() {
        @Override
        public void run() {
            PipOverlayActivity.this.mFadeOutAnimation.start();
        }
    };

    static void showPipOverlay(Context context) {
        if (sActivityCreated) {
            return;
        }
        Intent intent = new Intent(context, (Class<?>) PipOverlayActivity.class);
        intent.setFlags(268435456);
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchStackId(4);
        context.startActivity(intent, options.toBundle());
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        sActivityCreated = true;
        setContentView(R.layout.tv_pip_overlay);
        this.mGuideOverlayView = findViewById(R.id.guide_overlay);
        this.mPipManager.addListener(this);
        this.mFadeInAnimation = AnimatorInflater.loadAnimator(this, R.anim.tv_pip_overlay_fade_in_animation);
        this.mFadeInAnimation.setTarget(this.mGuideOverlayView);
        this.mFadeOutAnimation = AnimatorInflater.loadAnimator(this, R.anim.tv_pip_overlay_fade_out_animation);
        this.mFadeOutAnimation.setTarget(this.mGuideOverlayView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mFadeInAnimation.start();
        this.mHandler.removeCallbacks(this.mHideGuideOverlayRunnable);
        this.mHandler.postDelayed(this.mHideGuideOverlayRunnable, 4000L);
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.mHandler.removeCallbacks(this.mHideGuideOverlayRunnable);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sActivityCreated = false;
        this.mHandler.removeCallbacksAndMessages(null);
        this.mPipManager.removeListener(this);
        this.mPipManager.resumePipResizing(2);
    }

    @Override
    public void onPipEntered() {
    }

    @Override
    public void onPipActivityClosed() {
        finish();
    }

    @Override
    public void onShowPipMenu() {
        finish();
    }

    @Override
    public void onMoveToFullscreen() {
        finish();
    }

    @Override
    public void onPipResizeAboutToStart() {
        finish();
        this.mPipManager.suspendPipResizing(2);
    }
}
