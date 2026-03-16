package com.android.deskclock.timer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import com.android.deskclock.AnimatorUtils;
import com.android.deskclock.DeskClock;
import com.android.deskclock.DeskClockFragment;
import com.android.deskclock.R;
import com.android.deskclock.TimerSetupView;
import com.android.deskclock.Utils;
import com.android.deskclock.VerticalViewPager;

public class TimerFragment extends DeskClockFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final TimeInterpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();
    private static final TimeInterpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();
    private TimerFragmentAdapter mAdapter;
    private ImageButton mCancel;
    private ViewGroup mContentView;
    private Transition mDeleteTransition;
    private View mLastView;
    private NotificationManager mNotificationManager;
    private SharedPreferences mPrefs;
    private TimerSetupView mSetupView;
    private View mTimerView;
    private VerticalViewPager mViewPager;
    private boolean mTicking = false;
    private ImageView[] mPageIndicators = new ImageView[4];
    private Bundle mViewState = null;
    private final ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            TimerFragment.this.highlightPageIndicator(position);
            TimerFragment.this.setTimerViewFabIcon(TimerFragment.this.getCurrentTimer());
        }
    };
    private final Runnable mClockTick = new Runnable() {
        boolean mVisible = true;

        @Override
        public void run() {
            boolean visible = Utils.getTimeNow() % 1000 < 500;
            boolean toggle = this.mVisible != visible;
            this.mVisible = visible;
            for (int i = 0; i < TimerFragment.this.mAdapter.getCount(); i++) {
                TimerObj t = TimerFragment.this.mAdapter.getTimerAt(i);
                if (t.mState == 1 || t.mState == 3) {
                    long timeLeft = t.updateTimeLeft(false);
                    if (t.mView != null) {
                        t.mView.setTime(timeLeft, false);
                        if (toggle) {
                            ImageButton addMinuteButton = (ImageButton) t.mView.findViewById(R.id.reset_add);
                            boolean canAddMinute = 38439000 - t.mTimeLeft > 60000;
                            addMinuteButton.setEnabled(canAddMinute);
                        }
                    }
                }
                if (t.mTimeLeft <= 0 && t.mState != 4 && t.mState != 5) {
                    t.mState = 3;
                    if (t.mView != null) {
                        t.mView.timesUp();
                    }
                }
                if (toggle && t.mView != null) {
                    if (t.mState == 3) {
                        t.mView.setCircleBlink(this.mVisible);
                    }
                    if (t.mState == 2) {
                        t.mView.setTextBlink(this.mVisible);
                    }
                }
            }
            TimerFragment.this.mTimerView.postDelayed(TimerFragment.this.mClockTick, 20L);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mViewState = savedInstanceState;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.timer_fragment, container, false);
        this.mContentView = (ViewGroup) view;
        this.mTimerView = view.findViewById(R.id.timer_view);
        this.mSetupView = (TimerSetupView) view.findViewById(R.id.timer_setup);
        this.mViewPager = (VerticalViewPager) view.findViewById(R.id.vertical_view_pager);
        this.mPageIndicators[0] = (ImageView) view.findViewById(R.id.page_indicator0);
        this.mPageIndicators[1] = (ImageView) view.findViewById(R.id.page_indicator1);
        this.mPageIndicators[2] = (ImageView) view.findViewById(R.id.page_indicator2);
        this.mPageIndicators[3] = (ImageView) view.findViewById(R.id.page_indicator3);
        this.mCancel = (ImageButton) view.findViewById(R.id.timer_cancel);
        this.mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TimerFragment.this.mAdapter.getCount() != 0) {
                    AnimatorListenerAdapter adapter = new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            TimerFragment.this.mSetupView.reset();
                            TimerFragment.this.mSetupView.setScaleX(1.0f);
                            TimerFragment.this.goToPagerView();
                        }
                    };
                    TimerFragment.this.createRotateAnimator(adapter, false).start();
                }
            }
        });
        this.mDeleteTransition = new AutoTransition();
        this.mDeleteTransition.setDuration(166L);
        this.mDeleteTransition.setInterpolator(new AccelerateDecelerateInterpolator());
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Context context = getActivity();
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
    }

    @Override
    public void onResume() {
        boolean goToSetUpView = true;
        super.onResume();
        if (getActivity() instanceof DeskClock) {
            DeskClock activity = (DeskClock) getActivity();
            activity.registerPageChangedListener(this);
        }
        if (this.mAdapter == null) {
            this.mAdapter = new TimerFragmentAdapter(getChildFragmentManager(), this.mPrefs);
        }
        this.mAdapter.populateTimersFromPref();
        this.mViewPager.setAdapter(this.mAdapter);
        this.mViewPager.setOnPageChangeListener(this.mOnPageChangeListener);
        this.mPrefs.registerOnSharedPreferenceChangeListener(this);
        SharedPreferences.Editor editor = this.mPrefs.edit();
        if (this.mPrefs.getBoolean("from_notification", false)) {
            editor.putBoolean("from_notification", false);
        }
        if (this.mPrefs.getBoolean("from_alert", false)) {
            editor.putBoolean("from_alert", false);
        }
        editor.apply();
        this.mCancel.setVisibility(this.mAdapter.getCount() == 0 ? 4 : 0);
        Intent newIntent = getActivity().getIntent();
        if (newIntent != null && newIntent.getBooleanExtra("deskclock.timers.gotosetup", false)) {
            goToSetUpView = true;
        } else if (newIntent != null && newIntent.getBooleanExtra("first_launch_from_api_call", false)) {
            goToSetUpView = false;
            highlightPageIndicator(0);
            this.mViewPager.setCurrentItem(0);
            newIntent.putExtra("first_launch_from_api_call", false);
        } else if (this.mViewState != null) {
            int currPage = this.mViewState.getInt("_currPage");
            this.mViewPager.setCurrentItem(currPage);
            highlightPageIndicator(currPage);
            boolean hasPreviousInput = this.mViewState.getBoolean("_setup_selected", false);
            if (!hasPreviousInput && this.mAdapter.getCount() != 0) {
                goToSetUpView = false;
            }
            this.mSetupView.restoreEntryState(this.mViewState, "entry_state");
        } else {
            highlightPageIndicator(0);
            if (this.mAdapter.getCount() != 0) {
                goToSetUpView = false;
            }
        }
        if (goToSetUpView) {
            goToSetUpView();
        } else {
            goToPagerView();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof DeskClock) {
            ((DeskClock) getActivity()).unregisterPageChangedListener(this);
        }
        this.mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        if (this.mAdapter != null) {
            this.mAdapter.saveTimersToSharedPrefs();
        }
        stopClockTicks();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mAdapter != null) {
            this.mAdapter.saveTimersToSharedPrefs();
        }
        if (this.mSetupView != null) {
            outState.putBoolean("_setup_selected", this.mSetupView.getVisibility() == 0);
            this.mSetupView.saveEntryState(outState, "entry_state");
        }
        outState.putInt("_currPage", this.mViewPager.getCurrentItem());
        this.mViewState = outState;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.mViewState = null;
    }

    @Override
    public void onPageChanged(int page) {
        if (page == 2 && this.mAdapter != null) {
            this.mAdapter.notifyDataSetChanged();
        }
    }

    private void startClockTicks() {
        this.mTimerView.postDelayed(this.mClockTick, 20L);
        this.mTicking = true;
    }

    private void stopClockTicks() {
        if (this.mTicking) {
            this.mViewPager.removeCallbacks(this.mClockTick);
            this.mTicking = false;
        }
    }

    private void goToPagerView() {
        this.mTimerView.setVisibility(0);
        this.mSetupView.setVisibility(8);
        this.mLastView = this.mTimerView;
        setLeftRightButtonAppearance();
        setFabAppearance();
        startClockTicks();
    }

    private void goToSetUpView() {
        if (this.mAdapter.getCount() == 0) {
            this.mCancel.setVisibility(4);
        } else {
            this.mCancel.setVisibility(0);
        }
        this.mTimerView.setVisibility(8);
        this.mSetupView.setVisibility(0);
        this.mSetupView.updateDeleteButtonAndDivider();
        this.mSetupView.registerStartButton(this.mFab);
        this.mLastView = this.mSetupView;
        setLeftRightButtonAppearance();
        setFabAppearance();
        stopClockTicks();
    }

    private void updateTimerState(TimerObj t, String action) {
        if ("delete_timer".equals(action)) {
            this.mAdapter.deleteTimer(t.mTimerId);
            if (this.mAdapter.getCount() == 0) {
                this.mSetupView.reset();
                goToSetUpView();
            }
        } else {
            t.writeToSharedPref(this.mPrefs);
        }
        Intent i = new Intent();
        i.setAction(action);
        i.putExtra("timer.intent.extra", t.mTimerId);
        i.addFlags(268435456);
        getActivity().sendBroadcast(i);
    }

    private void setTimerViewFabIcon(TimerObj timer) {
        Context context = getActivity();
        if (context != null && timer != null && this.mFab != null) {
            Resources r = context.getResources();
            switch (timer.mState) {
                case 1:
                    this.mFab.setVisibility(0);
                    this.mFab.setContentDescription(r.getString(R.string.timer_stop));
                    this.mFab.setImageResource(R.drawable.ic_fab_pause);
                    break;
                case 2:
                case 5:
                    this.mFab.setVisibility(0);
                    this.mFab.setContentDescription(r.getString(R.string.timer_start));
                    this.mFab.setImageResource(R.drawable.ic_fab_play);
                    break;
                case 3:
                    this.mFab.setVisibility(0);
                    this.mFab.setContentDescription(r.getString(R.string.timer_stop));
                    this.mFab.setImageResource(R.drawable.ic_fab_stop);
                    break;
                case 4:
                    this.mFab.setVisibility(4);
                    break;
            }
        }
    }

    private Animator getRotateFromAnimator(View view) {
        new ObjectAnimator();
        Animator animator = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.SCALE_X, 1.0f, 0.0f);
        animator.setDuration(150L);
        animator.setInterpolator(DECELERATE_INTERPOLATOR);
        return animator;
    }

    private Animator getRotateToAnimator(View view) {
        new ObjectAnimator();
        Animator animator = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.SCALE_X, 0.0f, 1.0f);
        animator.setDuration(150L);
        animator.setInterpolator(ACCELERATE_INTERPOLATOR);
        return animator;
    }

    private Animator getScaleFooterButtonsAnimator(final boolean show) {
        AnimatorSet animatorSet = new AnimatorSet();
        ImageButton imageButton = this.mLeftButton;
        float[] fArr = new float[2];
        fArr[0] = show ? 0.0f : 1.0f;
        fArr[1] = show ? 1.0f : 0.0f;
        Animator leftButtonAnimator = AnimatorUtils.getScaleAnimator(imageButton, fArr);
        ImageButton imageButton2 = this.mRightButton;
        float[] fArr2 = new float[2];
        fArr2[0] = show ? 0.0f : 1.0f;
        fArr2[1] = show ? 1.0f : 0.0f;
        Animator rightButtonAnimator = AnimatorUtils.getScaleAnimator(imageButton2, fArr2);
        float fabStartScale = (show && this.mFab.getVisibility() == 4) ? 0.0f : 1.0f;
        ImageButton imageButton3 = this.mFab;
        float[] fArr3 = new float[2];
        fArr3[0] = fabStartScale;
        fArr3[1] = show ? 1.0f : 0.0f;
        Animator fabAnimator = AnimatorUtils.getScaleAnimator(imageButton3, fArr3);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                TimerFragment.this.mLeftButton.setVisibility(show ? 0 : 4);
                TimerFragment.this.mRightButton.setVisibility(show ? 0 : 4);
                TimerFragment.this.restoreScale(TimerFragment.this.mLeftButton);
                TimerFragment.this.restoreScale(TimerFragment.this.mRightButton);
                TimerFragment.this.restoreScale(TimerFragment.this.mFab);
            }
        });
        animatorSet.setDuration(show ? 300L : 150L);
        animatorSet.play(leftButtonAnimator).with(rightButtonAnimator).with(fabAnimator);
        return animatorSet;
    }

    private void restoreScale(View view) {
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
    }

    private Animator createRotateAnimator(AnimatorListenerAdapter adapter, boolean toSetup) {
        AnimatorSet animatorSet = new AnimatorSet();
        Animator rotateFrom = getRotateFromAnimator(toSetup ? this.mTimerView : this.mSetupView);
        rotateFrom.addListener(adapter);
        Animator rotateTo = getRotateToAnimator(toSetup ? this.mSetupView : this.mTimerView);
        Animator expandFooterButton = getScaleFooterButtonsAnimator(!toSetup);
        animatorSet.play(rotateFrom).before(rotateTo).with(expandFooterButton);
        return animatorSet;
    }

    @Override
    public void onFabClick(View view) {
        if (this.mLastView != this.mTimerView) {
            AnimatorListenerAdapter adapter = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    int timerLength = TimerFragment.this.mSetupView.getTime();
                    TimerObj timerObj = new TimerObj(((long) timerLength) * 1000, TimerFragment.this.getActivity());
                    timerObj.mState = 1;
                    TimerFragment.this.updateTimerState(timerObj, "start_timer");
                    TimerFragment.this.mAdapter.addTimer(timerObj);
                    TimerFragment.this.mViewPager.setCurrentItem(0);
                    TimerFragment.this.highlightPageIndicator(0);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    TimerFragment.this.mSetupView.reset();
                    TimerFragment.this.mSetupView.setScaleX(1.0f);
                    TimerFragment.this.goToPagerView();
                }
            };
            createRotateAnimator(adapter, false).start();
            return;
        }
        TimerObj t = getCurrentTimer();
        switch (t.mState) {
            case 1:
                t.mState = 2;
                t.mView.pause();
                t.updateTimeLeft(true);
                updateTimerState(t, "timer_stop");
                break;
            case 2:
            case 5:
                t.mState = 1;
                t.mStartTime = Utils.getTimeNow() - (t.mOriginalLength - t.mTimeLeft);
                t.mView.start();
                updateTimerState(t, "start_timer");
                break;
            case 3:
                if (t.mDeleteAfterUse) {
                    cancelTimerNotification(t.mTimerId);
                    t.mState = 6;
                    updateTimerState(t, "delete_timer");
                } else {
                    t.mState = 5;
                    t.mOriginalLength = t.mSetupLength;
                    t.mTimeLeft = t.mSetupLength;
                    t.mView.stop();
                    t.mView.setTime(t.mTimeLeft, false);
                    t.mView.set(t.mOriginalLength, t.mTimeLeft, false);
                    updateTimerState(t, "timer_reset");
                    cancelTimerNotification(t.mTimerId);
                }
                break;
        }
        setTimerViewFabIcon(t);
    }

    private TimerObj getCurrentTimer() {
        int currPage;
        if (this.mViewPager != null && (currPage = this.mViewPager.getCurrentItem()) < this.mAdapter.getCount()) {
            return this.mAdapter.getTimerAt(currPage);
        }
        return null;
    }

    @Override
    public void setFabAppearance() {
        DeskClock activity = (DeskClock) getActivity();
        if (this.mFab != null) {
            if (activity.getSelectedTab() != 2) {
                this.mFab.setVisibility(0);
                return;
            }
            if (this.mLastView == this.mTimerView) {
                setTimerViewFabIcon(getCurrentTimer());
            } else if (this.mSetupView != null) {
                this.mSetupView.registerStartButton(this.mFab);
                this.mFab.setImageResource(R.drawable.ic_fab_play);
                this.mFab.setContentDescription(getString(R.string.timer_start));
            }
        }
    }

    @Override
    public void setLeftRightButtonAppearance() {
        DeskClock activity = (DeskClock) getActivity();
        if (this.mLeftButton != null && this.mRightButton != null && activity.getSelectedTab() == 2) {
            this.mLeftButton.setEnabled(true);
            this.mRightButton.setEnabled(true);
            this.mLeftButton.setVisibility(this.mLastView != this.mTimerView ? 8 : 0);
            this.mRightButton.setVisibility(this.mLastView == this.mTimerView ? 0 : 8);
            this.mLeftButton.setImageResource(R.drawable.ic_delete);
            this.mLeftButton.setContentDescription(getString(R.string.timer_delete));
            this.mRightButton.setImageResource(R.drawable.ic_add_timer);
            this.mRightButton.setContentDescription(getString(R.string.timer_add_timer));
        }
    }

    @Override
    public void onRightButtonClick(View view) {
        AnimatorListenerAdapter adapter = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                TimerFragment.this.mSetupView.reset();
                TimerFragment.this.mTimerView.setScaleX(1.0f);
                TimerFragment.this.goToSetUpView();
            }
        };
        createRotateAnimator(adapter, true).start();
    }

    @Override
    public void onLeftButtonClick(View view) {
        final TimerObj timer = getCurrentTimer();
        if (timer != null) {
            if (timer.mState == 3) {
                this.mNotificationManager.cancel(timer.mTimerId);
            }
            if (this.mAdapter.getCount() == 1) {
                AnimatorListenerAdapter adapter = new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        TimerFragment.this.mTimerView.setScaleX(1.0f);
                        TimerFragment.this.deleteTimer(timer);
                    }
                };
                createRotateAnimator(adapter, true).start();
            } else {
                TransitionManager.beginDelayedTransition(this.mContentView, this.mDeleteTransition);
                deleteTimer(timer);
            }
        }
    }

    private void deleteTimer(TimerObj timer) {
        timer.mState = 6;
        updateTimerState(timer, "delete_timer");
        highlightPageIndicator(this.mViewPager.getCurrentItem());
        setFabAppearance();
    }

    private void highlightPageIndicator(int position) {
        int lastDotRes;
        int i = R.drawable.ic_swipe_circle_dark;
        int count = this.mAdapter.getCount();
        if (count <= 4) {
            int i2 = 0;
            while (i2 < 4) {
                if (count < 2 || i2 >= count) {
                    this.mPageIndicators[i2].setVisibility(8);
                } else {
                    paintIndicator(i2, position == i2 ? R.drawable.ic_swipe_circle_light : R.drawable.ic_swipe_circle_dark);
                }
                i2++;
            }
            return;
        }
        int belowCount = (count - position) - 1;
        if (position < 3) {
            for (int i3 = 0; i3 < position; i3++) {
                paintIndicator(i3, R.drawable.ic_swipe_circle_dark);
            }
            paintIndicator(position, R.drawable.ic_swipe_circle_light);
            for (int i4 = position + 1; i4 < 3; i4++) {
                paintIndicator(i4, R.drawable.ic_swipe_circle_dark);
            }
            paintIndicator(3, R.drawable.ic_swipe_circle_bottom);
            return;
        }
        paintIndicator(0, R.drawable.ic_swipe_circle_top);
        for (int i5 = 1; i5 < 2; i5++) {
            paintIndicator(i5, R.drawable.ic_swipe_circle_dark);
        }
        if (belowCount != 0) {
            i = R.drawable.ic_swipe_circle_light;
        }
        paintIndicator(2, i);
        if (belowCount == 0) {
            lastDotRes = R.drawable.ic_swipe_circle_light;
        } else if (belowCount == 1) {
            lastDotRes = R.drawable.ic_swipe_circle_dark;
        } else {
            lastDotRes = R.drawable.ic_swipe_circle_bottom;
        }
        paintIndicator(3, lastDotRes);
    }

    private void paintIndicator(int position, int res) {
        this.mPageIndicators[position].setVisibility(0);
        this.mPageIndicators[position].setImageResource(res);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (prefs.equals(this.mPrefs)) {
            if ((key.equals("from_alert") && prefs.getBoolean("from_alert", false)) || (key.equals("from_notification") && prefs.getBoolean("from_notification", false))) {
                SharedPreferences.Editor editor = this.mPrefs.edit();
                editor.putBoolean(key, false);
                editor.apply();
                this.mAdapter.populateTimersFromPref();
                this.mViewPager.setAdapter(this.mAdapter);
                if (this.mViewState != null) {
                    int currPage = this.mViewState.getInt("_currPage");
                    this.mViewPager.setCurrentItem(currPage);
                    highlightPageIndicator(currPage);
                } else {
                    highlightPageIndicator(0);
                }
                setFabAppearance();
            }
        }
    }

    public void setLabel(TimerObj timer, String label) {
        timer.mLabel = label;
        updateTimerState(timer, "timer_update");
        this.mAdapter.notifyDataSetChanged();
    }

    public void onPlusOneButtonPressed(TimerObj t) {
        switch (t.mState) {
            case 1:
                t.addTime(60000L);
                long timeLeft = t.updateTimeLeft(false);
                t.mView.setTime(timeLeft, false);
                t.mView.setLength(timeLeft);
                this.mAdapter.notifyDataSetChanged();
                updateTimerState(t, "timer_update");
                break;
            case 2:
            case 4:
                t.mState = 5;
                t.mTimeLeft = t.mSetupLength;
                t.mOriginalLength = t.mSetupLength;
                t.mView.stop();
                t.mView.setTime(t.mTimeLeft, false);
                t.mView.set(t.mOriginalLength, t.mTimeLeft, false);
                updateTimerState(t, "timer_reset");
                break;
            case 3:
                t.mState = 1;
                t.mStartTime = Utils.getTimeNow();
                t.mOriginalLength = 60000L;
                t.mTimeLeft = 60000L;
                t.mView.setTime(t.mTimeLeft, false);
                t.mView.set(t.mOriginalLength, t.mTimeLeft, true);
                t.mView.start();
                updateTimerState(t, "timer_reset");
                updateTimerState(t, "start_timer");
                cancelTimerNotification(t.mTimerId);
                break;
        }
        setFabAppearance();
    }

    private void cancelTimerNotification(int timerId) {
        this.mNotificationManager.cancel(timerId);
    }
}
