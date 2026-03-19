package com.android.deskclock.timer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.view.ViewPager;
import android.util.Property;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import com.android.deskclock.AnimatorUtils;
import com.android.deskclock.DeskClock;
import com.android.deskclock.DeskClockFragment;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Timer;
import com.android.deskclock.data.TimerListener;
import com.android.deskclock.data.TimerStringFormatter;
import com.android.deskclock.events.Events;
import com.android.deskclock.uidata.UiDataModel;
import com.google.android.flexbox.BuildConfig;
import java.io.Serializable;
import java.util.Arrays;

public final class TimerFragment extends DeskClockFragment {
    private static final String EXTRA_TIMER_SETUP = "com.android.deskclock.action.TIMER_SETUP";
    private static final String KEY_TIMER_SETUP_STATE = "timer_setup_input";
    private TimerPagerAdapter mAdapter;
    private TimerSetupView mCreateTimerView;
    private boolean mCreatingTimer;
    private View mCurrentView;
    private ImageView[] mPageIndicators;
    private final Runnable mTimeUpdateRunnable;
    private final TimerPageChangeListener mTimerPageChangeListener;
    private Serializable mTimerSetupState;
    private final TimerListener mTimerWatcher;
    private View mTimersView;
    private ViewPager mViewPager;

    public static Intent createTimerSetupIntent(Context context) {
        return new Intent(context, (Class<?>) DeskClock.class).putExtra(EXTRA_TIMER_SETUP, true);
    }

    public TimerFragment() {
        super(UiDataModel.Tab.TIMERS);
        this.mTimerPageChangeListener = new TimerPageChangeListener();
        this.mTimeUpdateRunnable = new TimeUpdateRunnable();
        this.mTimerWatcher = new TimerWatcher();
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(R.layout.timer_fragment, viewGroup, false);
        this.mAdapter = new TimerPagerAdapter(getChildFragmentManager());
        this.mViewPager = (ViewPager) viewInflate.findViewById(R.id.vertical_view_pager);
        this.mViewPager.setAdapter(this.mAdapter);
        this.mViewPager.addOnPageChangeListener(this.mTimerPageChangeListener);
        this.mTimersView = viewInflate.findViewById(R.id.timer_view);
        this.mCreateTimerView = (TimerSetupView) viewInflate.findViewById(R.id.timer_setup);
        this.mCreateTimerView.setFabContainer(this);
        this.mPageIndicators = new ImageView[]{(ImageView) viewInflate.findViewById(R.id.page_indicator0), (ImageView) viewInflate.findViewById(R.id.page_indicator1), (ImageView) viewInflate.findViewById(R.id.page_indicator2), (ImageView) viewInflate.findViewById(R.id.page_indicator3)};
        DataModel.getDataModel().addTimerListener(this.mAdapter);
        DataModel.getDataModel().addTimerListener(this.mTimerWatcher);
        if (bundle != null) {
            this.mTimerSetupState = bundle.getSerializable(KEY_TIMER_SETUP_STATE);
        }
        return viewInflate;
    }

    @Override
    public void onStart() {
        int id;
        Timer timer;
        super.onStart();
        updatePageIndicators();
        Intent intent = getActivity().getIntent();
        boolean booleanExtra = false;
        if (intent != null) {
            booleanExtra = intent.getBooleanExtra(EXTRA_TIMER_SETUP, false);
            intent.removeExtra(EXTRA_TIMER_SETUP);
            id = intent.getIntExtra(TimerService.EXTRA_TIMER_ID, -1);
            intent.removeExtra(TimerService.EXTRA_TIMER_ID);
        } else {
            id = -1;
        }
        if (id != -1) {
            showTimersView(9);
        } else if (!hasTimers() || booleanExtra || this.mTimerSetupState != null) {
            showCreateTimerView(9);
            if (this.mTimerSetupState != null) {
                this.mCreateTimerView.setState(this.mTimerSetupState);
                this.mTimerSetupState = null;
            }
        } else {
            showTimersView(9);
        }
        if (id == -1) {
            Timer mostRecentExpiredTimer = DataModel.getDataModel().getMostRecentExpiredTimer();
            if (mostRecentExpiredTimer != null) {
                id = mostRecentExpiredTimer.getId();
            } else {
                id = -1;
            }
        }
        if (id != -1 && (timer = DataModel.getDataModel().getTimer(id)) != null) {
            this.mViewPager.setCurrentItem(DataModel.getDataModel().getTimers().indexOf(timer));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra(TimerService.EXTRA_TIMER_ID)) {
            int intExtra = intent.getIntExtra(TimerService.EXTRA_TIMER_ID, -1);
            intent.removeExtra(TimerService.EXTRA_TIMER_ID);
            Timer timer = DataModel.getDataModel().getTimer(intExtra);
            if (timer != null) {
                this.mViewPager.setCurrentItem(DataModel.getDataModel().getTimers().indexOf(timer));
                animateToView(this.mTimersView, null, false);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        stopUpdatingTime();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        DataModel.getDataModel().removeTimerListener(this.mAdapter);
        DataModel.getDataModel().removeTimerListener(this.mTimerWatcher);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (this.mCurrentView == this.mCreateTimerView) {
            this.mTimerSetupState = this.mCreateTimerView.getState();
            bundle.putSerializable(KEY_TIMER_SETUP_STATE, this.mTimerSetupState);
        }
    }

    private void updateFab(@NonNull ImageView imageView, boolean z) {
        if (this.mCurrentView != this.mTimersView) {
            if (this.mCurrentView == this.mCreateTimerView) {
                if (this.mCreateTimerView.hasValidInput()) {
                    imageView.setImageResource(R.drawable.ic_start_white_24dp);
                    imageView.setContentDescription(imageView.getResources().getString(R.string.timer_start));
                    imageView.setVisibility(0);
                    return;
                } else {
                    imageView.setContentDescription(null);
                    imageView.setVisibility(4);
                    return;
                }
            }
            return;
        }
        if (getTimer() == null) {
            imageView.setVisibility(4);
        }
        imageView.setVisibility(0);
        switch (r0.getState()) {
            case RUNNING:
                if (z) {
                    imageView.setImageResource(R.drawable.ic_play_pause_animation);
                } else {
                    imageView.setImageResource(R.drawable.ic_play_pause);
                }
                imageView.setContentDescription(imageView.getResources().getString(R.string.timer_stop));
                break;
            case RESET:
                if (z) {
                    imageView.setImageResource(R.drawable.ic_stop_play_animation);
                } else {
                    imageView.setImageResource(R.drawable.ic_pause_play);
                }
                imageView.setContentDescription(imageView.getResources().getString(R.string.timer_start));
                break;
            case PAUSED:
                if (z) {
                    imageView.setImageResource(R.drawable.ic_pause_play_animation);
                } else {
                    imageView.setImageResource(R.drawable.ic_pause_play);
                }
                imageView.setContentDescription(imageView.getResources().getString(R.string.timer_start));
                break;
            case MISSED:
            case EXPIRED:
                imageView.setImageResource(R.drawable.ic_stop_white_24dp);
                imageView.setContentDescription(imageView.getResources().getString(R.string.timer_stop));
                break;
        }
    }

    @Override
    public void onUpdateFab(@NonNull ImageView imageView) {
        updateFab(imageView, false);
    }

    @Override
    public void onMorphFab(@NonNull ImageView imageView) {
        updateFab(imageView, Utils.isNOrLater());
        AnimatorUtils.startDrawableAnimation(imageView);
    }

    @Override
    public void onUpdateFabButtons(@NonNull Button button, @NonNull Button button2) {
        if (this.mCurrentView == this.mTimersView) {
            button.setClickable(true);
            button.setText(R.string.timer_delete);
            button.setContentDescription(button.getResources().getString(R.string.timer_delete));
            button.setVisibility(0);
            button2.setClickable(true);
            button2.setText(R.string.timer_add_timer);
            button2.setContentDescription(button2.getResources().getString(R.string.timer_add_timer));
            button2.setVisibility(0);
            return;
        }
        if (this.mCurrentView == this.mCreateTimerView) {
            button.setClickable(true);
            button.setText(R.string.timer_cancel);
            button.setContentDescription(button.getResources().getString(R.string.timer_cancel));
            button.setVisibility(hasTimers() ? 0 : 4);
            button2.setVisibility(4);
        }
    }

    @Override
    public void onFabClick(@NonNull ImageView imageView) {
        if (this.mCurrentView != this.mTimersView) {
            if (this.mCurrentView == this.mCreateTimerView) {
                this.mCreatingTimer = true;
                try {
                    Timer timerAddTimer = DataModel.getDataModel().addTimer(this.mCreateTimerView.getTimeInMillis(), BuildConfig.FLAVOR, false);
                    Events.sendTimerEvent(R.string.action_create, R.string.label_deskclock);
                    DataModel.getDataModel().startTimer(timerAddTimer);
                    Events.sendTimerEvent(R.string.action_start, R.string.label_deskclock);
                    this.mViewPager.setCurrentItem(0);
                    this.mCreatingTimer = false;
                    animateToView(this.mTimersView, null, true);
                    return;
                } catch (Throwable th) {
                    this.mCreatingTimer = false;
                    throw th;
                }
            }
            return;
        }
        Timer timer = getTimer();
        if (timer == null) {
            return;
        }
        Context context = imageView.getContext();
        long remainingTime = timer.getRemainingTime();
        switch (timer.getState()) {
            case RUNNING:
                DataModel.getDataModel().pauseTimer(timer);
                Events.sendTimerEvent(R.string.action_stop, R.string.label_deskclock);
                if (remainingTime > 0) {
                    this.mTimersView.announceForAccessibility(TimerStringFormatter.formatString(context, R.string.timer_accessibility_stopped, remainingTime, true));
                    return;
                }
                return;
            case RESET:
            case PAUSED:
                DataModel.getDataModel().startTimer(timer);
                Events.sendTimerEvent(R.string.action_start, R.string.label_deskclock);
                if (remainingTime > 0) {
                    this.mTimersView.announceForAccessibility(TimerStringFormatter.formatString(context, R.string.timer_accessibility_started, remainingTime, true));
                    return;
                }
                return;
            case MISSED:
            case EXPIRED:
                DataModel.getDataModel().resetOrDeleteTimer(timer, R.string.label_deskclock);
                return;
            default:
                return;
        }
    }

    @Override
    public void onLeftButtonClick(@NonNull Button button) {
        if (this.mCurrentView == this.mTimersView) {
            Timer timer = getTimer();
            if (timer == null) {
                return;
            }
            if (this.mAdapter.getCount() > 1) {
                animateTimerRemove(timer);
            } else {
                animateToView(this.mCreateTimerView, timer, false);
            }
            button.announceForAccessibility(getActivity().getString(R.string.timer_deleted));
            return;
        }
        if (this.mCurrentView == this.mCreateTimerView && this.mAdapter.getCount() > 0) {
            this.mCreateTimerView.reset();
            animateToView(this.mTimersView, null, false);
            button.announceForAccessibility(getActivity().getString(R.string.timer_canceled));
        }
    }

    @Override
    public void onRightButtonClick(@NonNull Button button) {
        if (this.mCurrentView != this.mCreateTimerView) {
            animateToView(this.mCreateTimerView, null, true);
        }
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (this.mCurrentView == this.mCreateTimerView) {
            return this.mCreateTimerView.onKeyDown(i, keyEvent);
        }
        return super.onKeyDown(i, keyEvent);
    }

    private void updatePageIndicators() {
        int[] iArrComputePageIndicatorStates = computePageIndicatorStates(this.mViewPager.getCurrentItem(), this.mPageIndicators.length, this.mAdapter.getCount());
        for (int i = 0; i < iArrComputePageIndicatorStates.length; i++) {
            int i2 = iArrComputePageIndicatorStates[i];
            ImageView imageView = this.mPageIndicators[i];
            if (i2 == 0) {
                imageView.setVisibility(8);
            } else {
                imageView.setVisibility(0);
                imageView.setImageResource(i2);
            }
        }
    }

    @VisibleForTesting
    static int[] computePageIndicatorStates(int i, int i2, int i3) {
        int iMin = Math.min(i2, i3);
        int i4 = i - (iMin / 2);
        int i5 = (i4 + iMin) - 1;
        if (i5 >= i3) {
            i5 = i3 - 1;
            i4 = (i5 - iMin) + 1;
        }
        if (i4 < 0) {
            i5 = iMin - 1;
            i4 = 0;
        }
        int[] iArr = new int[i2];
        Arrays.fill(iArr, 0);
        if (iMin >= 2) {
            Arrays.fill(iArr, 0, iMin, R.drawable.ic_swipe_circle_dark);
            if (i4 > 0) {
                iArr[0] = R.drawable.ic_swipe_circle_top;
            }
            if (i5 < i3 - 1) {
                iArr[iMin - 1] = R.drawable.ic_swipe_circle_bottom;
            }
            iArr[i - i4] = R.drawable.ic_swipe_circle_light;
            return iArr;
        }
        return iArr;
    }

    private void showCreateTimerView(int i) {
        stopUpdatingTime();
        this.mTimersView.setVisibility(8);
        this.mCreateTimerView.setVisibility(0);
        this.mCurrentView = this.mCreateTimerView;
        updateFab(i);
    }

    private void showTimersView(int i) {
        this.mTimerSetupState = null;
        this.mTimersView.setVisibility(0);
        this.mCreateTimerView.setVisibility(8);
        this.mCurrentView = this.mTimersView;
        updateFab(i);
        startUpdatingTime();
    }

    private void animateTimerRemove(final Timer timer) {
        long shortAnimationDuration = UiDataModel.getUiDataModel().getShortAnimationDuration();
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mViewPager, (Property<ViewPager, Float>) View.ALPHA, 1.0f, 0.0f);
        objectAnimatorOfFloat.setDuration(shortAnimationDuration);
        objectAnimatorOfFloat.setInterpolator(new DecelerateInterpolator());
        objectAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                DataModel.getDataModel().removeTimer(timer);
                Events.sendTimerEvent(R.string.action_delete, R.string.label_deskclock);
            }
        });
        ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(this.mViewPager, (Property<ViewPager, Float>) View.ALPHA, 0.0f, 1.0f);
        objectAnimatorOfFloat2.setDuration(shortAnimationDuration);
        objectAnimatorOfFloat2.setInterpolator(new AccelerateInterpolator());
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(objectAnimatorOfFloat).before(objectAnimatorOfFloat2);
        animatorSet.start();
    }

    private void animateToView(final View view, final Timer timer, final boolean z) {
        if (this.mCurrentView == view) {
            return;
        }
        final boolean z2 = view == this.mTimersView;
        if (z2) {
            this.mTimersView.setVisibility(0);
        } else {
            this.mCreateTimerView.setVisibility(0);
        }
        updateFab(32);
        final long longAnimationDuration = UiDataModel.getUiDataModel().getLongAnimationDuration();
        final ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
        viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (viewTreeObserver.isAlive()) {
                    viewTreeObserver.removeOnPreDrawListener(this);
                }
                View viewFindViewById = TimerFragment.this.mTimersView.findViewById(R.id.timer_time);
                float y = viewFindViewById != null ? viewFindViewById.getY() + viewFindViewById.getHeight() : 0.0f;
                if (!z) {
                    y = -y;
                }
                view.setTranslationY(-y);
                TimerFragment.this.mCurrentView.setTranslationY(0.0f);
                view.setAlpha(0.0f);
                TimerFragment.this.mCurrentView.setAlpha(1.0f);
                ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(TimerFragment.this.mCurrentView, (Property<View, Float>) View.TRANSLATION_Y, y);
                ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.TRANSLATION_Y, 0.0f);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(objectAnimatorOfFloat, objectAnimatorOfFloat2);
                animatorSet.setDuration(longAnimationDuration);
                animatorSet.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);
                ObjectAnimator objectAnimatorOfFloat3 = ObjectAnimator.ofFloat(TimerFragment.this.mCurrentView, (Property<View, Float>) View.ALPHA, 0.0f);
                objectAnimatorOfFloat3.setDuration(longAnimationDuration / 2);
                objectAnimatorOfFloat3.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animator) {
                        super.onAnimationStart(animator);
                        TimerFragment.this.updateFab(128);
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        super.onAnimationEnd(animator);
                        if (z2) {
                            TimerFragment.this.showTimersView(64);
                            TimerFragment.this.mCreateTimerView.reset();
                        } else {
                            TimerFragment.this.showCreateTimerView(64);
                        }
                        if (timer != null) {
                            DataModel.getDataModel().removeTimer(timer);
                            Events.sendTimerEvent(R.string.action_delete, R.string.label_deskclock);
                        }
                        TimerFragment.this.updateFab(9);
                    }
                });
                ObjectAnimator objectAnimatorOfFloat4 = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.ALPHA, 1.0f);
                objectAnimatorOfFloat4.setDuration(longAnimationDuration / 2);
                objectAnimatorOfFloat4.setStartDelay(longAnimationDuration / 2);
                AnimatorSet animatorSet2 = new AnimatorSet();
                animatorSet2.playTogether(objectAnimatorOfFloat3, objectAnimatorOfFloat4, animatorSet);
                animatorSet2.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        super.onAnimationEnd(animator);
                        TimerFragment.this.mTimersView.setTranslationY(0.0f);
                        TimerFragment.this.mCreateTimerView.setTranslationY(0.0f);
                        TimerFragment.this.mTimersView.setAlpha(1.0f);
                        TimerFragment.this.mCreateTimerView.setAlpha(1.0f);
                    }
                });
                animatorSet2.start();
                return true;
            }
        });
    }

    private boolean hasTimers() {
        return this.mAdapter.getCount() > 0;
    }

    private Timer getTimer() {
        if (this.mViewPager == null || this.mAdapter.getCount() == 0) {
            return null;
        }
        return this.mAdapter.getTimer(this.mViewPager.getCurrentItem());
    }

    private void startUpdatingTime() {
        stopUpdatingTime();
        this.mViewPager.post(this.mTimeUpdateRunnable);
    }

    private void stopUpdatingTime() {
        this.mViewPager.removeCallbacks(this.mTimeUpdateRunnable);
    }

    private class TimeUpdateRunnable implements Runnable {
        private TimeUpdateRunnable() {
        }

        @Override
        public void run() {
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            if (!TimerFragment.this.mAdapter.updateTime()) {
                return;
            }
            TimerFragment.this.mTimersView.postDelayed(this, Math.max(0L, (jElapsedRealtime + 20) - SystemClock.elapsedRealtime()));
        }
    }

    private class TimerPageChangeListener extends ViewPager.SimpleOnPageChangeListener {
        private TimerPageChangeListener() {
        }

        @Override
        public void onPageSelected(int i) {
            TimerFragment.this.updatePageIndicators();
            TimerFragment.this.updateFab(9);
            TimerFragment.this.startUpdatingTime();
        }

        @Override
        public void onPageScrollStateChanged(int i) {
            if (i == 1) {
                TimerFragment.this.startUpdatingTime();
            }
        }
    }

    private class TimerWatcher implements TimerListener {
        private TimerWatcher() {
        }

        @Override
        public void timerAdded(Timer timer) {
            TimerFragment.this.updatePageIndicators();
            if (!TimerFragment.this.mCreatingTimer) {
                TimerFragment.this.updateFab(9);
            }
        }

        @Override
        public void timerUpdated(Timer timer, Timer timer2) {
            if (timer.isReset() && !timer2.isReset()) {
                TimerFragment.this.startUpdatingTime();
            }
            int iIndexOf = DataModel.getDataModel().getTimers().indexOf(timer2);
            if (timer.isExpired() || !timer2.isExpired() || iIndexOf == TimerFragment.this.mViewPager.getCurrentItem()) {
                if (TimerFragment.this.mCurrentView == TimerFragment.this.mTimersView && iIndexOf == TimerFragment.this.mViewPager.getCurrentItem() && timer.getState() != timer2.getState()) {
                    if (!timer.isPaused() || !timer2.isReset()) {
                        TimerFragment.this.updateFab(3);
                        return;
                    }
                    return;
                }
                return;
            }
            TimerFragment.this.mViewPager.setCurrentItem(iIndexOf, true);
        }

        @Override
        public void timerRemoved(Timer timer) {
            TimerFragment.this.updatePageIndicators();
            TimerFragment.this.updateFab(9);
            if (TimerFragment.this.mCurrentView == TimerFragment.this.mTimersView && TimerFragment.this.mAdapter.getCount() == 0) {
                TimerFragment.this.animateToView(TimerFragment.this.mCreateTimerView, null, false);
            }
        }
    }
}
