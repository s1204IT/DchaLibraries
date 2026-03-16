package com.android.deskclock.timer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import com.android.deskclock.CircleButtonsLayout;
import com.android.deskclock.DeskClock;
import com.android.deskclock.DeskClockFragment;
import com.android.deskclock.LabelDialogFragment;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.TimerSetupView;
import com.android.deskclock.Utils;
import com.android.deskclock.widget.sgv.GridAdapter;
import com.android.deskclock.widget.sgv.StaggeredGridView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

public class TimerFullScreenFragment extends DeskClockFragment implements SharedPreferences.OnSharedPreferenceChangeListener, View.OnClickListener {
    private static final Interpolator REVEAL_INTERPOLATOR = new PathInterpolator(0.0f, 0.0f, 0.2f, 1.0f);
    private TimersListAdapter mAdapter;
    private int mColumnCount;
    private ImageButton mFab;
    private NotificationManager mNotificationManager;
    private OnEmptyListListener mOnEmptyListListener;
    private SharedPreferences mPrefs;
    private TimerSetupView mTimerSetup;
    private StaggeredGridView mTimersList;
    private View mTimersListPage;
    private Bundle mViewState;
    private boolean mTicking = false;
    private View mLastVisibleView = null;
    private final Runnable mClockTick = new Runnable() {
        boolean mVisible = true;

        @Override
        public void run() {
            boolean visible = Utils.getTimeNow() % 1000 < 500;
            boolean toggle = this.mVisible != visible;
            this.mVisible = visible;
            for (int i = 0; i < TimerFullScreenFragment.this.mAdapter.getCount(); i++) {
                TimerObj t = TimerFullScreenFragment.this.mAdapter.getItem(i);
                if (t.mState == 1 || t.mState == 3) {
                    long timeLeft = t.updateTimeLeft(false);
                    if (t.mView != null) {
                        t.mView.setTime(timeLeft, false);
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
            TimerFullScreenFragment.this.mTimersList.postDelayed(TimerFullScreenFragment.this.mClockTick, 20L);
        }
    };

    public interface OnEmptyListListener {
        void onEmptyList();

        void onListChanged();
    }

    class ClickAction {
        public int mAction;
        public TimerObj mTimer;

        public ClickAction(int action, TimerObj t) {
            this.mAction = action;
            this.mTimer = t;
        }
    }

    TimersListAdapter createAdapter(Context context, SharedPreferences prefs) {
        return this.mOnEmptyListListener == null ? new TimersListAdapter(context, prefs) : new TimesUpListAdapter(context, prefs);
    }

    private class TimersListAdapter extends GridAdapter {
        Context mContext;
        ArrayList<TimerObj> mTimers = new ArrayList<>();
        private final Comparator<TimerObj> mTimersCompare = new Comparator<TimerObj>() {
            protected int getSection(TimerObj timerObj) {
                switch (timerObj.mState) {
                    case 1:
                    case 2:
                        return 1;
                    case 3:
                        return 0;
                    default:
                        return 2;
                }
            }

            @Override
            public int compare(TimerObj o1, TimerObj o2) {
                int section1 = getSection(o1);
                int section2 = getSection(o2);
                return section1 != section2 ? section1 < section2 ? -1 : 1 : (section1 == 0 || section1 == 1) ? o1.mTimeLeft >= o2.mTimeLeft ? 1 : -1 : o1.mSetupLength >= o2.mSetupLength ? 1 : -1;
            }
        };
        SharedPreferences mmPrefs;

        public TimersListAdapter(Context context, SharedPreferences prefs) {
            this.mContext = context;
            this.mmPrefs = prefs;
        }

        @Override
        public int getCount() {
            return this.mTimers.size();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public TimerObj getItem(int p) {
            return this.mTimers.get(p);
        }

        @Override
        public long getItemId(int p) {
            if (p < 0 || p >= this.mTimers.size()) {
                return 0L;
            }
            return this.mTimers.get(p).mTimerId;
        }

        protected int findTimerPositionById(int id) {
            for (int i = 0; i < this.mTimers.size(); i++) {
                TimerObj t = this.mTimers.get(i);
                if (t.mTimerId == id) {
                    return i;
                }
            }
            return -1;
        }

        public void removeTimer(TimerObj timerObj) {
            int position = findTimerPositionById(timerObj.mTimerId);
            if (position >= 0) {
                this.mTimers.remove(position);
                notifyDataSetChanged();
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
            TimerListItem v = (TimerListItem) inflater.inflate(R.layout.timer_list_item, (ViewGroup) null);
            final TimerObj o = getItem(position);
            o.mView = v;
            long timeLeft = o.updateTimeLeft(false);
            boolean drawRed = o.mState != 5;
            v.set(o.mOriginalLength, timeLeft, drawRed);
            v.setTime(timeLeft, true);
            switch (o.mState) {
                case 1:
                    v.start();
                    break;
                case 3:
                    v.timesUp();
                    break;
                case 4:
                    v.done();
                    break;
            }
            CountingTimerView countingTimerView = (CountingTimerView) v.findViewById(R.id.timer_time_text);
            countingTimerView.registerVirtualButtonAction(new Runnable() {
                @Override
                public void run() {
                    TimerFullScreenFragment.this.onClickHelper(TimerFullScreenFragment.this.new ClickAction(1, o));
                }
            });
            CircleButtonsLayout circleLayout = (CircleButtonsLayout) v.findViewById(R.id.timer_circle);
            circleLayout.setCircleTimerViewIds(R.id.timer_time, R.id.reset_add, R.id.timer_label, R.id.timer_label_text);
            ImageButton resetAddButton = (ImageButton) v.findViewById(R.id.reset_add);
            resetAddButton.setTag(TimerFullScreenFragment.this.new ClickAction(2, o));
            v.setResetAddButton(true, TimerFullScreenFragment.this);
            FrameLayout label = (FrameLayout) v.findViewById(R.id.timer_label);
            TextView labelIcon = (TextView) v.findViewById(R.id.timer_label_placeholder);
            TextView labelText = (TextView) v.findViewById(R.id.timer_label_text);
            if (o.mLabel.equals("")) {
                labelText.setVisibility(8);
                labelIcon.setVisibility(0);
            } else {
                labelText.setText(o.mLabel);
                labelText.setVisibility(0);
                labelIcon.setVisibility(8);
            }
            if (TimerFullScreenFragment.this.getActivity() instanceof DeskClock) {
                label.setOnTouchListener(new DeskClock.OnTapListener(TimerFullScreenFragment.this.getActivity(), labelText) {
                    @Override
                    protected void processClick(View v2) {
                        TimerFullScreenFragment.this.onLabelPressed(o);
                    }
                });
            } else {
                labelIcon.setVisibility(4);
            }
            return v;
        }

        @Override
        public int getItemColumnSpan(Object item, int position) {
            if (getCount() == 1) {
                return TimerFullScreenFragment.this.mColumnCount;
            }
            return 1;
        }

        public void addTimer(TimerObj t) {
            this.mTimers.add(0, t);
            sort();
        }

        public void onSaveInstanceState(Bundle outState) {
            TimerObj.putTimersInSharedPrefs(this.mmPrefs, this.mTimers);
        }

        public void onRestoreInstanceState(Bundle outState) {
            TimerObj.getTimersFromSharedPrefs(this.mmPrefs, this.mTimers);
            sort();
        }

        public void saveGlobalState() {
            TimerObj.putTimersInSharedPrefs(this.mmPrefs, this.mTimers);
        }

        public void sort() {
            if (getCount() > 0) {
                Collections.sort(this.mTimers, this.mTimersCompare);
                notifyDataSetChanged();
            }
        }
    }

    private class TimesUpListAdapter extends TimersListAdapter {
        public TimesUpListAdapter(Context context, SharedPreferences prefs) {
            super(context, prefs);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
        }

        @Override
        public void saveGlobalState() {
        }

        @Override
        public void onRestoreInstanceState(Bundle outState) {
            TimerObj.getTimersFromSharedPrefs(this.mmPrefs, this.mTimers, 3);
            if (getCount() == 0) {
                TimerFullScreenFragment.this.mOnEmptyListListener.onEmptyList();
            } else {
                Collections.sort(this.mTimers, new Comparator<TimerObj>() {
                    @Override
                    public int compare(TimerObj o1, TimerObj o2) {
                        return o1.mTimeLeft < o2.mTimeLeft ? -1 : 1;
                    }
                });
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            this.mViewState = savedInstanceState;
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.timer_full_screen_fragment, container, false);
        Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey("times_up") && bundle.getBoolean("times_up", false)) {
            try {
                this.mOnEmptyListListener = (OnEmptyListListener) getActivity();
            } catch (ClassCastException e) {
                Log.wtf("TimerFragment1", getActivity().toString() + " must implement OnEmptyListListener");
            }
        }
        this.mFab = (ImageButton) v.findViewById(R.id.fab);
        this.mTimersList = (StaggeredGridView) v.findViewById(R.id.timers_list);
        this.mColumnCount = getResources().getInteger(R.integer.timer_column_count);
        this.mTimersList.setColumnCount(this.mColumnCount);
        this.mTimersList.setGuardAgainstJaggedEdges(true);
        this.mTimersListPage = v.findViewById(R.id.timers_list_page);
        this.mTimerSetup = (TimerSetupView) v.findViewById(R.id.timer_setup);
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        this.mNotificationManager = (NotificationManager) getActivity().getSystemService("notification");
        return v;
    }

    @Override
    public void onDestroyView() {
        this.mViewState = new Bundle();
        saveViewState(this.mViewState);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        Intent newIntent = null;
        if (getActivity() instanceof DeskClock) {
            DeskClock activity = (DeskClock) getActivity();
            activity.registerPageChangedListener(this);
            newIntent = activity.getIntent();
        }
        super.onResume();
        this.mPrefs.registerOnSharedPreferenceChangeListener(this);
        this.mAdapter = createAdapter(getActivity(), this.mPrefs);
        this.mAdapter.onRestoreInstanceState(null);
        float dividerHeight = getResources().getDimension(R.dimen.timer_divider_height);
        if (getActivity() instanceof DeskClock) {
            View footerView = getActivity().getLayoutInflater().inflate(R.layout.blank_footer_view, (ViewGroup) this.mTimersList, false);
            ViewGroup.LayoutParams params = footerView.getLayoutParams();
            params.height = (int) (params.height - dividerHeight);
            footerView.setLayoutParams(params);
            this.mAdapter.setFooterView(footerView);
        }
        if (this.mPrefs.getBoolean("from_notification", false)) {
            SharedPreferences.Editor editor = this.mPrefs.edit();
            editor.putBoolean("from_notification", false);
            editor.apply();
        }
        if (this.mPrefs.getBoolean("from_alert", false)) {
            SharedPreferences.Editor editor2 = this.mPrefs.edit();
            editor2.putBoolean("from_alert", false);
            editor2.apply();
        }
        this.mTimersList.setAdapter(this.mAdapter);
        this.mLastVisibleView = null;
        setPage();
        View v = getView();
        if (v != null) {
            getView().setVisibility(0);
        }
        if (newIntent != null) {
            processIntent(newIntent);
        }
        this.mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimerFullScreenFragment.this.revealAnimation(TimerFullScreenFragment.this.mFab, TimerFullScreenFragment.this.getActivity().getResources().getColor(R.color.clock_white));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        TimerFullScreenFragment.this.updateAllTimesUpTimers(false);
                    }
                }, 333L);
            }
        });
    }

    private void revealAnimation(View centerView, int color) {
        Activity activity = getActivity();
        View decorView = activity.getWindow().getDecorView();
        final ViewGroupOverlay overlay = (ViewGroupOverlay) decorView.getOverlay();
        final View revealView = new View(activity);
        revealView.setRight(decorView.getWidth());
        revealView.setBottom(decorView.getHeight());
        revealView.setBackgroundColor(color);
        overlay.add(revealView);
        int[] clearLocation = new int[2];
        centerView.getLocationInWindow(clearLocation);
        clearLocation[0] = clearLocation[0] + (centerView.getWidth() / 2);
        clearLocation[1] = clearLocation[1] + (centerView.getHeight() / 2);
        int revealCenterX = clearLocation[0] - revealView.getLeft();
        int revealCenterY = clearLocation[1] - revealView.getTop();
        int xMax = Math.max(revealCenterX, decorView.getWidth() - revealCenterX);
        int yMax = Math.max(revealCenterY, decorView.getHeight() - revealCenterY);
        float revealRadius = (float) Math.sqrt(Math.pow(xMax, 2.0d) + Math.pow(yMax, 2.0d));
        Animator revealAnimator = ViewAnimationUtils.createCircularReveal(revealView, revealCenterX, revealCenterY, 0.0f, revealRadius);
        revealAnimator.setInterpolator(REVEAL_INTERPOLATOR);
        ValueAnimator fadeAnimator = ObjectAnimator.ofFloat(revealView, (Property<View, Float>) View.ALPHA, 1.0f);
        fadeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                overlay.remove(revealView);
            }
        });
        AnimatorSet alertAnimator = new AnimatorSet();
        alertAnimator.setDuration(333L);
        alertAnimator.play(revealAnimator).before(fadeAnimator);
        alertAnimator.start();
    }

    @Override
    public void onPause() {
        if (getActivity() instanceof DeskClock) {
            ((DeskClock) getActivity()).unregisterPageChangedListener(this);
        }
        super.onPause();
        stopClockTicks();
        if (this.mAdapter != null) {
            this.mAdapter.saveGlobalState();
        }
        this.mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        View v = getView();
        if (v != null) {
            v.setVisibility(4);
        }
    }

    @Override
    public void onPageChanged(int page) {
        if (page == 2 && this.mAdapter != null) {
            this.mAdapter.sort();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mAdapter != null) {
            this.mAdapter.onSaveInstanceState(outState);
        }
        if (this.mTimerSetup != null) {
            saveViewState(outState);
        } else if (this.mViewState != null) {
            outState.putAll(this.mViewState);
        }
    }

    private void saveViewState(Bundle outState) {
        this.mTimerSetup.saveEntryState(outState, "entry_state");
    }

    public void setPage() {
        boolean switchToSetupView;
        if (this.mViewState != null) {
            switchToSetupView = false;
            this.mTimerSetup.restoreEntryState(this.mViewState, "entry_state");
            this.mViewState = null;
        } else {
            switchToSetupView = this.mAdapter.getCount() == 0;
        }
        if (switchToSetupView) {
            gotoSetupView();
        } else {
            gotoTimersView();
        }
    }

    private void resetTimer(TimerObj t) {
        t.mState = 5;
        long j = t.mSetupLength;
        t.mOriginalLength = j;
        t.mTimeLeft = j;
        if (t.mView != null) {
            t.mView.stop();
            t.mView.setTime(t.mTimeLeft, false);
            t.mView.set(t.mOriginalLength, t.mTimeLeft, false);
        }
        updateTimersState(t, "timer_reset");
    }

    public void updateAllTimesUpTimers(boolean stop) {
        boolean notifyChange = false;
        LinkedList<TimerObj> timesupTimers = new LinkedList<>();
        for (int i = 0; i < this.mAdapter.getCount(); i++) {
            TimerObj timerObj = this.mAdapter.getItem(i);
            if (timerObj.mState == 3) {
                timesupTimers.addFirst(timerObj);
                notifyChange = true;
            }
        }
        while (timesupTimers.size() > 0) {
            TimerObj t = timesupTimers.remove();
            if (stop) {
                onStopButtonPressed(t);
            } else {
                resetTimer(t);
            }
        }
        if (notifyChange) {
            SharedPreferences.Editor editor = this.mPrefs.edit();
            editor.putBoolean("from_alert", true);
            editor.apply();
        }
    }

    private void gotoSetupView() {
        if (this.mLastVisibleView == null || this.mLastVisibleView.getId() == R.id.timer_setup) {
            this.mTimerSetup.setVisibility(0);
            this.mTimerSetup.setScaleX(1.0f);
            this.mTimersListPage.setVisibility(8);
        } else {
            ObjectAnimator a = ObjectAnimator.ofFloat(this.mTimersListPage, (Property<View, Float>) View.SCALE_X, 1.0f, 0.0f);
            a.setInterpolator(new AccelerateInterpolator());
            a.setDuration(125L);
            a.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    TimerFullScreenFragment.this.mTimersListPage.setVisibility(8);
                    TimerFullScreenFragment.this.mTimerSetup.setScaleX(0.0f);
                    TimerFullScreenFragment.this.mTimerSetup.setVisibility(0);
                    ObjectAnimator b = ObjectAnimator.ofFloat(TimerFullScreenFragment.this.mTimerSetup, (Property<TimerSetupView, Float>) View.SCALE_X, 0.0f, 1.0f);
                    b.setInterpolator(new DecelerateInterpolator());
                    b.setDuration(225L);
                    b.start();
                }
            });
            a.start();
        }
        stopClockTicks();
        this.mTimerSetup.updateDeleteButtonAndDivider();
        this.mLastVisibleView = this.mTimerSetup;
    }

    private void gotoTimersView() {
        if (this.mLastVisibleView == null || this.mLastVisibleView.getId() == R.id.timers_list_page) {
            this.mTimerSetup.setVisibility(8);
            this.mTimersListPage.setVisibility(0);
            this.mTimersListPage.setScaleX(1.0f);
        } else {
            ObjectAnimator a = ObjectAnimator.ofFloat(this.mTimerSetup, (Property<TimerSetupView, Float>) View.SCALE_X, 1.0f, 0.0f);
            a.setInterpolator(new AccelerateInterpolator());
            a.setDuration(125L);
            a.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    TimerFullScreenFragment.this.mTimerSetup.setVisibility(8);
                    TimerFullScreenFragment.this.mTimersListPage.setScaleX(0.0f);
                    TimerFullScreenFragment.this.mTimersListPage.setVisibility(0);
                    ObjectAnimator b = ObjectAnimator.ofFloat(TimerFullScreenFragment.this.mTimersListPage, (Property<View, Float>) View.SCALE_X, 0.0f, 1.0f);
                    b.setInterpolator(new DecelerateInterpolator());
                    b.setDuration(225L);
                    b.start();
                }
            });
            a.start();
        }
        startClockTicks();
        this.mLastVisibleView = this.mTimersListPage;
    }

    @Override
    public void onClick(View v) {
        ClickAction tag = (ClickAction) v.getTag();
        onClickHelper(tag);
    }

    private void onClickHelper(ClickAction clickAction) {
        switch (clickAction.mAction) {
            case 1:
                onStopButtonPressed(clickAction.mTimer);
                break;
            case 2:
                onPlusOneButtonPressed(clickAction.mTimer);
                break;
            case 3:
                TimerObj t = clickAction.mTimer;
                if (t.mState == 3) {
                    cancelTimerNotification(t.mTimerId);
                }
                t.mState = 6;
                updateTimersState(t, "delete_timer");
                break;
        }
    }

    private void onPlusOneButtonPressed(TimerObj t) {
        switch (t.mState) {
            case 1:
                t.addTime(60000L);
                long timeLeft = t.updateTimeLeft(false);
                t.mView.setTime(timeLeft, false);
                t.mView.setLength(timeLeft);
                this.mAdapter.notifyDataSetChanged();
                updateTimersState(t, "timer_update");
                break;
            case 2:
            case 4:
                t.mState = 5;
                long j = t.mSetupLength;
                t.mOriginalLength = j;
                t.mTimeLeft = j;
                t.mView.stop();
                t.mView.setTime(t.mTimeLeft, false);
                t.mView.set(t.mOriginalLength, t.mTimeLeft, false);
                updateTimersState(t, "timer_reset");
                break;
            case 3:
                t.mState = 1;
                t.mStartTime = Utils.getTimeNow();
                t.mOriginalLength = 60000L;
                t.mTimeLeft = 60000L;
                updateTimersState(t, "timer_reset");
                updateTimersState(t, "start_timer");
                updateTimesUpMode(t);
                cancelTimerNotification(t.mTimerId);
                break;
        }
    }

    private void onStopButtonPressed(TimerObj t) {
        switch (t.mState) {
            case 1:
                t.mState = 2;
                t.mView.pause();
                t.updateTimeLeft(true);
                updateTimersState(t, "timer_stop");
                break;
            case 2:
                t.mState = 1;
                t.mStartTime = Utils.getTimeNow() - (t.mOriginalLength - t.mTimeLeft);
                t.mView.start();
                updateTimersState(t, "start_timer");
                break;
            case 3:
                if (t.mDeleteAfterUse) {
                    cancelTimerNotification(t.mTimerId);
                    t.mState = 6;
                    updateTimersState(t, "delete_timer");
                } else {
                    t.mState = 4;
                    if (t.mView != null) {
                        t.mView.done();
                    }
                    updateTimersState(t, "timer_done");
                    cancelTimerNotification(t.mTimerId);
                    updateTimesUpMode(t);
                }
                break;
            case 5:
                t.mState = 1;
                t.mStartTime = Utils.getTimeNow() - (t.mOriginalLength - t.mTimeLeft);
                t.mView.start();
                updateTimersState(t, "start_timer");
                break;
        }
    }

    private void onLabelPressed(TimerObj t) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("label_dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        LabelDialogFragment newFragment = LabelDialogFragment.newInstance(t, t.mLabel, getTag());
        newFragment.show(ft, "label_dialog");
    }

    private void startClockTicks() {
        this.mTimersList.postDelayed(this.mClockTick, 20L);
        this.mTicking = true;
    }

    private void stopClockTicks() {
        if (this.mTicking) {
            this.mTimersList.removeCallbacks(this.mClockTick);
            this.mTicking = false;
        }
    }

    private void updateTimersState(TimerObj t, String action) {
        if ("delete_timer".equals(action)) {
            LogUtils.e("~~ update timer state", new Object[0]);
            t.deleteFromSharedPref(this.mPrefs);
        } else {
            t.writeToSharedPref(this.mPrefs);
        }
        Intent i = new Intent();
        i.setAction(action);
        i.putExtra("timer.intent.extra", t.mTimerId);
        i.addFlags(268435456);
        getActivity().sendBroadcast(i);
    }

    private void cancelTimerNotification(int timerId) {
        this.mNotificationManager.cancel(timerId);
    }

    private void updateTimesUpMode(TimerObj timerObj) {
        if (this.mOnEmptyListListener != null && timerObj.mState != 3) {
            this.mAdapter.removeTimer(timerObj);
            if (this.mAdapter.getCount() == 0) {
                this.mOnEmptyListListener.onEmptyList();
            } else {
                this.mOnEmptyListListener.onListChanged();
            }
        }
    }

    public void restartAdapter() {
        this.mAdapter = createAdapter(getActivity(), this.mPrefs);
        this.mAdapter.onRestoreInstanceState(null);
    }

    public void processIntent(Intent intent) {
        if (intent.getBooleanExtra("deskclock.timers.gotosetup", false)) {
            gotoSetupView();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (prefs.equals(this.mPrefs)) {
            if ((key.equals("from_alert") && prefs.getBoolean("from_alert", false)) || (key.equals("from_notification") && prefs.getBoolean("from_notification", false))) {
                SharedPreferences.Editor editor = this.mPrefs.edit();
                editor.putBoolean(key, false);
                editor.apply();
                this.mAdapter = createAdapter(getActivity(), this.mPrefs);
                this.mAdapter.onRestoreInstanceState(null);
                this.mTimersList.setAdapter(this.mAdapter);
            }
        }
    }

    @Override
    public void onFabClick(View view) {
        if (this.mLastVisibleView != this.mTimersListPage) {
            int timerLength = this.mTimerSetup.getTime();
            if (timerLength != 0) {
                TimerObj t = new TimerObj(((long) timerLength) * 1000, getActivity());
                t.mState = 1;
                this.mAdapter.addTimer(t);
                updateTimersState(t, "start_timer");
                gotoTimersView();
                this.mTimerSetup.reset();
                this.mTimersList.setFirstPositionAndOffsets(this.mAdapter.findTimerPositionById(t.mTimerId), 0);
                return;
            }
            return;
        }
        this.mTimerSetup.reset();
        gotoSetupView();
    }
}
