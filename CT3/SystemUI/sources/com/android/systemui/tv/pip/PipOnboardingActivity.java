package com.android.systemui.tv.pip;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import com.android.systemui.R;
import com.android.systemui.tv.pip.PipManager;

public class PipOnboardingActivity extends Activity implements PipManager.Listener {
    private AnimatorSet mEnterAnimator;
    private final PipManager mPipManager = PipManager.getInstance();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.tv_pip_onboarding);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PipOnboardingActivity.this.finish();
            }
        });
        this.mPipManager.addListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mEnterAnimator = new AnimatorSet();
        this.mEnterAnimator.playTogether(loadAnimator(R.id.background, R.anim.tv_pip_onboarding_background_enter_animation), loadAnimator(R.id.remote, R.anim.tv_pip_onboarding_image_enter_animation), loadAnimator(R.id.remote_button, R.anim.tv_pip_onboarding_image_enter_animation), loadAnimator(R.id.title, R.anim.tv_pip_onboarding_title_enter_animation), loadAnimator(R.id.description, R.anim.tv_pip_onboarding_description_enter_animation), loadAnimator(R.id.button, R.anim.tv_pip_onboarding_button_enter_animation));
        this.mEnterAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                ImageView button = (ImageView) PipOnboardingActivity.this.findViewById(R.id.remote_button);
                ((AnimationDrawable) button.getDrawable()).start();
            }
        });
        int delay = getResources().getInteger(R.integer.tv_pip_onboarding_anim_start_delay);
        this.mEnterAnimator.setStartDelay(delay);
        this.mEnterAnimator.start();
    }

    private Animator loadAnimator(int viewResId, int animResId) {
        Animator animator = AnimatorInflater.loadAnimator(this, animResId);
        animator.setTarget(findViewById(viewResId));
        return animator;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (this.mEnterAnimator.isStarted()) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (this.mEnterAnimator.isStarted()) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onPause() {
        super.onPause();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mPipManager.removeListener(this);
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
    }
}
