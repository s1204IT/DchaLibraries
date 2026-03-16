package com.android.calendar;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ViewSwitcher;
import com.android.calendar.CalendarController;

public class DayFragment extends Fragment implements ViewSwitcher.ViewFactory, CalendarController.EventHandler {
    EventLoader mEventLoader;
    protected Animation mInAnimationBackward;
    protected Animation mInAnimationForward;
    private int mNumDays;
    protected Animation mOutAnimationBackward;
    protected Animation mOutAnimationForward;
    Time mSelectedDay = new Time();
    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            if (DayFragment.this.isAdded()) {
                String tz = Utils.getTimeZone(DayFragment.this.getActivity(), DayFragment.this.mTZUpdater);
                DayFragment.this.mSelectedDay.timezone = tz;
                DayFragment.this.mSelectedDay.normalize(true);
            }
        }
    };
    protected ViewSwitcher mViewSwitcher;

    public DayFragment() {
        this.mSelectedDay.setToNow();
    }

    public DayFragment(long timeMillis, int numOfDays) {
        this.mNumDays = numOfDays;
        if (timeMillis == 0) {
            this.mSelectedDay.setToNow();
        } else {
            this.mSelectedDay.set(timeMillis);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Context context = getActivity();
        this.mInAnimationForward = AnimationUtils.loadAnimation(context, R.anim.slide_left_in);
        this.mOutAnimationForward = AnimationUtils.loadAnimation(context, R.anim.slide_left_out);
        this.mInAnimationBackward = AnimationUtils.loadAnimation(context, R.anim.slide_right_in);
        this.mOutAnimationBackward = AnimationUtils.loadAnimation(context, R.anim.slide_right_out);
        this.mEventLoader = new EventLoader(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.day_activity, (ViewGroup) null);
        this.mViewSwitcher = (ViewSwitcher) v.findViewById(R.id.switcher);
        this.mViewSwitcher.setFactory(this);
        this.mViewSwitcher.getCurrentView().requestFocus();
        ((DayView) this.mViewSwitcher.getCurrentView()).updateTitle();
        return v;
    }

    @Override
    public View makeView() {
        this.mTZUpdater.run();
        DayView view = new DayView(getActivity(), CalendarController.getInstance(getActivity()), this.mViewSwitcher, this.mEventLoader, this.mNumDays);
        view.setId(1);
        view.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        view.setSelected(this.mSelectedDay, false, false);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mEventLoader.startBackgroundThread();
        this.mTZUpdater.run();
        eventsChanged();
        DayView view = (DayView) this.mViewSwitcher.getCurrentView();
        view.handleOnResume();
        view.restartCurrentTimeUpdates();
        DayView view2 = (DayView) this.mViewSwitcher.getNextView();
        view2.handleOnResume();
        view2.restartCurrentTimeUpdates();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        long time = getSelectedTimeInMillis();
        if (time != -1) {
            outState.putLong("key_restore_time", time);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        ((DayView) this.mViewSwitcher.getCurrentView()).cleanup();
        DayView view = (DayView) this.mViewSwitcher.getNextView();
        view.cleanup();
        this.mEventLoader.stopBackgroundThread();
        view.stopEventsAnimation();
        ((DayView) this.mViewSwitcher.getNextView()).stopEventsAnimation();
    }

    private void goTo(Time goToTime, boolean ignoreTime, boolean animateToday) {
        if (this.mViewSwitcher == null) {
            this.mSelectedDay.set(goToTime);
            return;
        }
        DayView currentView = (DayView) this.mViewSwitcher.getCurrentView();
        int diff = currentView.compareToVisibleTimeRange(goToTime);
        if (diff == 0) {
            currentView.setSelected(goToTime, ignoreTime, animateToday);
            return;
        }
        if (diff > 0) {
            this.mViewSwitcher.setInAnimation(this.mInAnimationForward);
            this.mViewSwitcher.setOutAnimation(this.mOutAnimationForward);
        } else {
            this.mViewSwitcher.setInAnimation(this.mInAnimationBackward);
            this.mViewSwitcher.setOutAnimation(this.mOutAnimationBackward);
        }
        DayView next = (DayView) this.mViewSwitcher.getNextView();
        if (ignoreTime) {
            next.setFirstVisibleHour(currentView.getFirstVisibleHour());
        }
        next.setSelected(goToTime, ignoreTime, animateToday);
        next.reloadEvents();
        this.mViewSwitcher.showNext();
        next.requestFocus();
        next.updateTitle();
        next.restartCurrentTimeUpdates();
    }

    public long getSelectedTimeInMillis() {
        DayView view;
        if (this.mViewSwitcher == null || (view = (DayView) this.mViewSwitcher.getCurrentView()) == null) {
            return -1L;
        }
        return view.getSelectedTimeInMillis();
    }

    public void eventsChanged() {
        if (this.mViewSwitcher != null) {
            DayView view = (DayView) this.mViewSwitcher.getCurrentView();
            view.clearCachedEvents();
            view.reloadEvents();
            ((DayView) this.mViewSwitcher.getNextView()).clearCachedEvents();
        }
    }

    @Override
    public long getSupportedEventTypes() {
        return 160L;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo msg) {
        if (msg.eventType == 32) {
            goTo(msg.selectedTime, (msg.extraLong & 1) != 0, (msg.extraLong & 8) != 0);
        } else if (msg.eventType == 128) {
            eventsChanged();
        }
    }
}
