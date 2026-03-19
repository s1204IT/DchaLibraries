package com.android.calendar;

import android.app.Activity;
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
import com.mediatek.calendar.LogUtil;
import com.mediatek.calendar.PDebug;

public class DayFragment extends Fragment implements ViewSwitcher.ViewFactory, CalendarController.EventHandler {
    EventLoader mEventLoader;
    protected Animation mInAnimationBackward;
    protected Animation mInAnimationForward;
    private int mNumDays;
    protected Animation mOutAnimationBackward;
    protected Animation mOutAnimationForward;
    Time mSelectedDay;
    private final Runnable mTZUpdater;
    protected ViewSwitcher mViewSwitcher;

    public DayFragment() {
        this.mSelectedDay = new Time();
        this.mTZUpdater = new Runnable() {
            @Override
            public void run() {
                if (!DayFragment.this.isAdded()) {
                    return;
                }
                DayFragment.this.mSelectedDay.timezone = Utils.getTimeZone(DayFragment.this.getActivity(), DayFragment.this.mTZUpdater);
                DayFragment.this.mSelectedDay.normalize(true);
            }
        };
        this.mSelectedDay.setToNow();
    }

    public DayFragment(Context context, long j, int i) {
        this.mSelectedDay = new Time();
        this.mTZUpdater = new Runnable() {
            @Override
            public void run() {
                if (!DayFragment.this.isAdded()) {
                    return;
                }
                DayFragment.this.mSelectedDay.timezone = Utils.getTimeZone(DayFragment.this.getActivity(), DayFragment.this.mTZUpdater);
                DayFragment.this.mSelectedDay.normalize(true);
            }
        };
        this.mNumDays = i;
        this.mSelectedDay = Utils.getValidTimeInCalendar(context, j);
    }

    @Override
    public void onCreate(Bundle bundle) {
        PDebug.EndAndStart("AllInOneActivity.onCreate->DayFragment.onCreate", "DayFragment.onCreate");
        PDebug.Start("DayFragment.onCreate.superOnCreate");
        super.onCreate(bundle);
        PDebug.End("DayFragment.onCreate.superOnCreate");
        Activity activity = getActivity();
        PDebug.Start("DayFragment.onCreate.loadAnimations");
        this.mInAnimationForward = AnimationUtils.loadAnimation(activity, R.anim.slide_left_in);
        this.mOutAnimationForward = AnimationUtils.loadAnimation(activity, R.anim.slide_left_out);
        this.mInAnimationBackward = AnimationUtils.loadAnimation(activity, R.anim.slide_right_in);
        this.mOutAnimationBackward = AnimationUtils.loadAnimation(activity, R.anim.slide_right_out);
        PDebug.End("DayFragment.onCreate.loadAnimations");
        this.mEventLoader = new EventLoader(activity);
        PDebug.EndAndStart("DayFragment.onCreate", "DayFragment.onCreate->DayFragment.onCreateView");
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        PDebug.EndAndStart("DayFragment.onCreate->DayFragment.onCreateView", "DayFragment.onCreateView");
        PDebug.Start("DayFragment.onCreateView.inflateViewSwitcher");
        View viewInflate = layoutInflater.inflate(R.layout.day_activity, (ViewGroup) null);
        this.mViewSwitcher = (ViewSwitcher) viewInflate.findViewById(R.id.switcher);
        PDebug.End("DayFragment.onCreateView.inflateViewSwitcher");
        this.mViewSwitcher.setFactory(this);
        PDebug.Start("DayFragment.onCreateView.updateViewSwitcher");
        this.mViewSwitcher.getCurrentView().requestFocus();
        ((DayView) this.mViewSwitcher.getCurrentView()).updateTitle();
        PDebug.End("DayFragment.onCreateView.updateViewSwitcher");
        PDebug.EndAndStart("DayFragment.onCreateView", "DayFragment.onCreateView->AllInOneActivity.onResume");
        return viewInflate;
    }

    @Override
    public View makeView() {
        PDebug.Start("DayFragment.makeView");
        this.mTZUpdater.run();
        DayView dayView = new DayView(getActivity(), CalendarController.getInstance(getActivity()), this.mViewSwitcher, this.mEventLoader, this.mNumDays);
        dayView.setId(1);
        dayView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        dayView.setSelected(this.mSelectedDay, false, false);
        PDebug.End("DayFragment.makeView");
        return dayView;
    }

    @Override
    public void onResume() {
        PDebug.EndAndStart("AllInOneActivity.onResume->DayFragment.onResume", "DayFragment.onResume");
        super.onResume();
        this.mEventLoader.startBackgroundThread();
        this.mTZUpdater.run();
        eventsChanged();
        DayView dayView = (DayView) this.mViewSwitcher.getCurrentView();
        dayView.handleOnResume();
        dayView.restartCurrentTimeUpdates();
        DayView dayView2 = (DayView) this.mViewSwitcher.getNextView();
        dayView2.handleOnResume();
        dayView2.restartCurrentTimeUpdates();
        PDebug.End("DayFragment.onResume");
        PDebug.Start("DayFragment.onResume->DayView.onSizeChanged");
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        long selectedTimeInMillis = getSelectedTimeInMillis();
        if (selectedTimeInMillis != -1) {
            bundle.putLong("key_restore_time", selectedTimeInMillis);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        ((DayView) this.mViewSwitcher.getCurrentView()).cleanup();
        DayView dayView = (DayView) this.mViewSwitcher.getNextView();
        dayView.cleanup();
        this.mEventLoader.stopBackgroundThread();
        dayView.stopEventsAnimation();
        ((DayView) this.mViewSwitcher.getNextView()).stopEventsAnimation();
    }

    private void goTo(Time time, boolean z, boolean z2) {
        if (this.mViewSwitcher == null) {
            this.mSelectedDay.set(time);
            return;
        }
        DayView dayView = (DayView) this.mViewSwitcher.getCurrentView();
        if (dayView == null) {
            LogUtil.e("DayFragment", "getCurrentView() return null,return");
            return;
        }
        dayView.selectionFocusShow(false);
        int iCompareToVisibleTimeRange = dayView.compareToVisibleTimeRange(time);
        if (iCompareToVisibleTimeRange == 0) {
            dayView.setSelected(time, z, z2);
            return;
        }
        if (iCompareToVisibleTimeRange > 0) {
            this.mViewSwitcher.setInAnimation(this.mInAnimationForward);
            this.mViewSwitcher.setOutAnimation(this.mOutAnimationForward);
        } else {
            this.mViewSwitcher.setInAnimation(this.mInAnimationBackward);
            this.mViewSwitcher.setOutAnimation(this.mOutAnimationBackward);
        }
        DayView dayView2 = (DayView) this.mViewSwitcher.getNextView();
        dayView2.selectionFocusShow(false);
        if (z) {
            dayView2.setFirstVisibleHour(dayView.getFirstVisibleHour());
        }
        dayView2.setSelected(time, z, z2);
        dayView2.reloadEvents();
        this.mViewSwitcher.showNext();
        dayView2.requestFocus();
        dayView2.updateTitle();
        dayView2.restartCurrentTimeUpdates();
    }

    public long getSelectedTimeInMillis() {
        DayView dayView;
        if (this.mViewSwitcher == null || (dayView = (DayView) this.mViewSwitcher.getCurrentView()) == null) {
            return -1L;
        }
        return dayView.getSelectedTimeInMillis();
    }

    public void eventsChanged() {
        PDebug.Start("DayFragment.eventsChanged");
        if (this.mViewSwitcher == null) {
            return;
        }
        DayView dayView = (DayView) this.mViewSwitcher.getCurrentView();
        dayView.clearCachedEvents();
        dayView.reloadEvents();
        ((DayView) this.mViewSwitcher.getNextView()).clearCachedEvents();
        PDebug.End("DayFragment.eventsChanged");
    }

    @Override
    public long getSupportedEventTypes() {
        return 160L;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo eventInfo) {
        if (eventInfo.eventType == 32) {
            goTo(eventInfo.selectedTime, (eventInfo.extraLong & 1) != 0, (eventInfo.extraLong & 8) != 0);
        } else if (eventInfo.eventType == 128) {
            eventsChanged();
        }
    }
}
