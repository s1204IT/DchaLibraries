package com.android.systemui.statusbar.policy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import com.android.keyguard.AppearAnimationUtils;
import com.android.systemui.R;
import com.android.systemui.qs.tiles.UserDetailItemView;
import com.android.systemui.statusbar.phone.KeyguardStatusBarView;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.policy.UserSwitcherController;

public class KeyguardUserSwitcher {
    private final Adapter mAdapter;
    private boolean mAnimating;
    private final AppearAnimationUtils mAppearAnimationUtils;
    private final KeyguardUserSwitcherScrim mBackground;
    private ObjectAnimator mBgAnimator;
    public final DataSetObserver mDataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            KeyguardUserSwitcher.this.refresh();
        }
    };
    private final KeyguardStatusBarView mStatusBarView;
    private final ViewGroup mUserSwitcher;
    private final Container mUserSwitcherContainer;
    private UserSwitcherController mUserSwitcherController;

    public KeyguardUserSwitcher(Context context, ViewStub userSwitcher, KeyguardStatusBarView statusBarView, NotificationPanelView panelView, UserSwitcherController userSwitcherController) {
        boolean keyguardUserSwitcherEnabled = context.getResources().getBoolean(R.bool.config_keyguardUserSwitcher);
        if (userSwitcherController != null && keyguardUserSwitcherEnabled) {
            this.mUserSwitcherContainer = (Container) userSwitcher.inflate();
            this.mUserSwitcher = (ViewGroup) this.mUserSwitcherContainer.findViewById(R.id.keyguard_user_switcher_inner);
            this.mBackground = new KeyguardUserSwitcherScrim(this.mUserSwitcher);
            this.mUserSwitcher.setBackground(this.mBackground);
            this.mStatusBarView = statusBarView;
            this.mStatusBarView.setKeyguardUserSwitcher(this);
            panelView.setKeyguardUserSwitcher(this);
            this.mAdapter = new Adapter(context, userSwitcherController, this);
            this.mAdapter.registerDataSetObserver(this.mDataSetObserver);
            this.mUserSwitcherController = userSwitcherController;
            this.mAppearAnimationUtils = new AppearAnimationUtils(context, 400L, -0.5f, 0.5f, AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_slow_in));
            this.mUserSwitcherContainer.setKeyguardUserSwitcher(this);
            return;
        }
        this.mUserSwitcherContainer = null;
        this.mUserSwitcher = null;
        this.mStatusBarView = null;
        this.mAdapter = null;
        this.mAppearAnimationUtils = null;
        this.mBackground = null;
    }

    public void setKeyguard(boolean keyguard, boolean animate) {
        if (this.mUserSwitcher != null) {
            if (keyguard && shouldExpandByDefault()) {
                show(animate);
            } else {
                hide(animate);
            }
        }
    }

    private boolean shouldExpandByDefault() {
        return this.mUserSwitcherController != null && this.mUserSwitcherController.isSimpleUserSwitcher();
    }

    public void show(boolean animate) {
        if (this.mUserSwitcher != null && this.mUserSwitcherContainer.getVisibility() != 0) {
            cancelAnimations();
            this.mAdapter.refresh();
            this.mUserSwitcherContainer.setVisibility(0);
            this.mStatusBarView.setKeyguardUserSwitcherShowing(true, animate);
            if (animate) {
                startAppearAnimation();
            }
        }
    }

    private void hide(boolean animate) {
        if (this.mUserSwitcher != null && this.mUserSwitcherContainer.getVisibility() == 0) {
            cancelAnimations();
            if (animate) {
                startDisappearAnimation();
            } else {
                this.mUserSwitcherContainer.setVisibility(8);
            }
            this.mStatusBarView.setKeyguardUserSwitcherShowing(false, animate);
        }
    }

    private void cancelAnimations() {
        int count = this.mUserSwitcher.getChildCount();
        for (int i = 0; i < count; i++) {
            this.mUserSwitcher.getChildAt(i).animate().cancel();
        }
        if (this.mBgAnimator != null) {
            this.mBgAnimator.cancel();
        }
        this.mUserSwitcher.animate().cancel();
        this.mAnimating = false;
    }

    private void startAppearAnimation() {
        int count = this.mUserSwitcher.getChildCount();
        View[] objects = new View[count];
        for (int i = 0; i < count; i++) {
            objects[i] = this.mUserSwitcher.getChildAt(i);
        }
        this.mUserSwitcher.setClipChildren(false);
        this.mUserSwitcher.setClipToPadding(false);
        this.mAppearAnimationUtils.startAnimation(objects, new Runnable() {
            @Override
            public void run() {
                KeyguardUserSwitcher.this.mUserSwitcher.setClipChildren(true);
                KeyguardUserSwitcher.this.mUserSwitcher.setClipToPadding(true);
            }
        });
        this.mAnimating = true;
        this.mBgAnimator = ObjectAnimator.ofInt(this.mBackground, "alpha", 0, 255);
        this.mBgAnimator.setDuration(400L);
        this.mBgAnimator.setInterpolator(PhoneStatusBar.ALPHA_IN);
        this.mBgAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                KeyguardUserSwitcher.this.mBgAnimator = null;
                KeyguardUserSwitcher.this.mAnimating = false;
            }
        });
        this.mBgAnimator.start();
    }

    private void startDisappearAnimation() {
        this.mAnimating = true;
        this.mUserSwitcher.animate().alpha(0.0f).setDuration(300L).setInterpolator(PhoneStatusBar.ALPHA_OUT).withEndAction(new Runnable() {
            @Override
            public void run() {
                KeyguardUserSwitcher.this.mUserSwitcherContainer.setVisibility(8);
                KeyguardUserSwitcher.this.mUserSwitcher.setAlpha(1.0f);
                KeyguardUserSwitcher.this.mAnimating = false;
            }
        });
    }

    public void refresh() {
        int childCount = this.mUserSwitcher.getChildCount();
        int adapterCount = this.mAdapter.getCount();
        int N = Math.max(childCount, adapterCount);
        for (int i = 0; i < N; i++) {
            if (i < adapterCount) {
                View oldView = null;
                if (i < childCount) {
                    oldView = this.mUserSwitcher.getChildAt(i);
                }
                View newView = this.mAdapter.getView(i, oldView, this.mUserSwitcher);
                if (oldView == null) {
                    this.mUserSwitcher.addView(newView);
                } else if (oldView != newView) {
                    this.mUserSwitcher.removeViewAt(i);
                    this.mUserSwitcher.addView(newView, i);
                }
            } else {
                int lastIndex = this.mUserSwitcher.getChildCount() - 1;
                this.mUserSwitcher.removeViewAt(lastIndex);
            }
        }
    }

    public void hideIfNotSimple(boolean animate) {
        if (this.mUserSwitcherContainer != null && !this.mUserSwitcherController.isSimpleUserSwitcher()) {
            hide(animate);
        }
    }

    boolean isAnimating() {
        return this.mAnimating;
    }

    public static class Adapter extends UserSwitcherController.BaseUserAdapter implements View.OnClickListener {
        private Context mContext;
        private KeyguardUserSwitcher mKeyguardUserSwitcher;

        public Adapter(Context context, UserSwitcherController controller, KeyguardUserSwitcher kgu) {
            super(controller);
            this.mContext = context;
            this.mKeyguardUserSwitcher = kgu;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            UserSwitcherController.UserRecord item = getItem(position);
            if (!(convertView instanceof UserDetailItemView) || !(convertView.getTag() instanceof UserSwitcherController.UserRecord)) {
                convertView = LayoutInflater.from(this.mContext).inflate(R.layout.keyguard_user_switcher_item, parent, false);
                convertView.setOnClickListener(this);
            }
            UserDetailItemView v = (UserDetailItemView) convertView;
            String name = getName(this.mContext, item);
            if (item.picture == null) {
                v.bind(name, getDrawable(this.mContext, item));
            } else {
                v.bind(name, item.picture);
            }
            convertView.setActivated(item.isCurrent);
            convertView.setTag(item);
            return convertView;
        }

        @Override
        public void onClick(View v) {
            UserSwitcherController.UserRecord user = (UserSwitcherController.UserRecord) v.getTag();
            if (user.isCurrent && !user.isGuest) {
                this.mKeyguardUserSwitcher.hideIfNotSimple(true);
            } else {
                switchTo(user);
            }
        }
    }

    public static class Container extends FrameLayout {
        private KeyguardUserSwitcher mKeyguardUserSwitcher;

        public Container(Context context, AttributeSet attrs) {
            super(context, attrs);
            setClipChildren(false);
        }

        public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
            this.mKeyguardUserSwitcher = keyguardUserSwitcher;
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (this.mKeyguardUserSwitcher != null && !this.mKeyguardUserSwitcher.isAnimating()) {
                this.mKeyguardUserSwitcher.hideIfNotSimple(true);
                return false;
            }
            return false;
        }
    }
}
