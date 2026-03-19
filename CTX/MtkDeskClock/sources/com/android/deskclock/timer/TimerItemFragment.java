package com.android.deskclock.timer;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.deskclock.LabelDialogFragment;
import com.android.deskclock.R;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Timer;
import com.android.deskclock.data.TimerStringFormatter;
import com.android.deskclock.events.Events;

public class TimerItemFragment extends Fragment {
    private static final String KEY_TIMER_ID = "KEY_TIMER_ID";
    private int mTimerId;

    public static TimerItemFragment newInstance(Timer timer) {
        TimerItemFragment timerItemFragment = new TimerItemFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_TIMER_ID, timer.getId());
        timerItemFragment.setArguments(bundle);
        return timerItemFragment;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mTimerId = getArguments().getInt(KEY_TIMER_ID);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        Timer timer = getTimer();
        if (timer == null) {
            return null;
        }
        TimerItem timerItem = (TimerItem) layoutInflater.inflate(R.layout.timer_item, viewGroup, false);
        timerItem.findViewById(R.id.reset_add).setOnClickListener(new ResetAddListener());
        timerItem.findViewById(R.id.timer_label).setOnClickListener(new EditLabelListener());
        timerItem.findViewById(R.id.timer_time_text).setOnClickListener(new TimeTextListener());
        timerItem.update(timer);
        return timerItem;
    }

    boolean updateTime() {
        TimerItem timerItem = (TimerItem) getView();
        if (timerItem != null) {
            timerItem.update(getTimer());
            return !r1.isReset();
        }
        return false;
    }

    int getTimerId() {
        return this.mTimerId;
    }

    Timer getTimer() {
        return DataModel.getDataModel().getTimer(getTimerId());
    }

    private final class ResetAddListener implements View.OnClickListener {
        private ResetAddListener() {
        }

        @Override
        public void onClick(View view) {
            Timer timer = TimerItemFragment.this.getTimer();
            if (timer.isPaused()) {
                DataModel.getDataModel().resetOrDeleteTimer(timer, R.string.label_deskclock);
                return;
            }
            if (timer.isRunning() || timer.isExpired() || timer.isMissed()) {
                DataModel.getDataModel().addTimerMinute(timer);
                Events.sendTimerEvent(R.string.action_add_minute, R.string.label_deskclock);
                Context context = view.getContext();
                long remainingTime = TimerItemFragment.this.getTimer().getRemainingTime();
                if (remainingTime > 0) {
                    view.announceForAccessibility(TimerStringFormatter.formatString(context, R.string.timer_accessibility_one_minute_added, remainingTime, true));
                }
            }
        }
    }

    private final class EditLabelListener implements View.OnClickListener {
        private EditLabelListener() {
        }

        @Override
        public void onClick(View view) {
            LabelDialogFragment.show(TimerItemFragment.this.getFragmentManager(), LabelDialogFragment.newInstance(TimerItemFragment.this.getTimer()));
        }
    }

    private final class TimeTextListener implements View.OnClickListener {
        private TimeTextListener() {
        }

        @Override
        public void onClick(View view) {
            Timer timer = TimerItemFragment.this.getTimer();
            if (timer.isPaused() || timer.isReset()) {
                DataModel.getDataModel().startTimer(timer);
            } else if (timer.isRunning()) {
                DataModel.getDataModel().pauseTimer(timer);
            }
        }
    }
}
