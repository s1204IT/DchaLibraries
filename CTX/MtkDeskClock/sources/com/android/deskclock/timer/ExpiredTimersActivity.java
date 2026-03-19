package com.android.deskclock.timer;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.deskclock.BaseActivity;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Timer;
import com.android.deskclock.data.TimerListener;
import java.util.Iterator;
import java.util.List;

public class ExpiredTimersActivity extends BaseActivity {
    private ViewGroup mExpiredTimersScrollView;
    private ViewGroup mExpiredTimersView;
    private final Runnable mTimeUpdateRunnable;
    private final TimerListener mTimerChangeWatcher;

    public ExpiredTimersActivity() {
        this.mTimeUpdateRunnable = new TimeUpdateRunnable();
        this.mTimerChangeWatcher = new TimerChangeWatcher();
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        List<Timer> expiredTimers = getExpiredTimers();
        if (expiredTimers.size() == 0) {
            LogUtils.i("No expired timers, skipping display.", new Object[0]);
            finish();
            return;
        }
        setContentView(R.layout.expired_timers_activity);
        this.mExpiredTimersView = (ViewGroup) findViewById(R.id.expired_timers_list);
        this.mExpiredTimersScrollView = (ViewGroup) findViewById(R.id.expired_timers_scroll);
        findViewById(R.id.fab).setOnClickListener(new FabClickListener());
        findViewById(R.id.expired_timers_activity).setSystemUiVisibility(1);
        getWindow().addFlags(6815873);
        sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
        if (!getResources().getBoolean(R.bool.rotateAlarmAlert)) {
            setRequestedOrientation(5);
        }
        Iterator<Timer> it = expiredTimers.iterator();
        while (it.hasNext()) {
            addTimer(it.next());
        }
        DataModel.getDataModel().addTimerListener(this.mTimerChangeWatcher);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startUpdatingTime();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopUpdatingTime();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DataModel.getDataModel().removeTimerListener(this.mTimerChangeWatcher);
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent keyEvent) {
        if (keyEvent.getAction() == 1) {
            int keyCode = keyEvent.getKeyCode();
            if (keyCode != 27 && keyCode != 80 && keyCode != 164) {
                switch (keyCode) {
                }
            }
            DataModel.getDataModel().resetExpiredTimers(R.string.label_hardware_button);
            return true;
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    private void startUpdatingTime() {
        stopUpdatingTime();
        this.mExpiredTimersView.post(this.mTimeUpdateRunnable);
    }

    private void stopUpdatingTime() {
        this.mExpiredTimersView.removeCallbacks(this.mTimeUpdateRunnable);
    }

    private void addTimer(Timer timer) {
        TransitionManager.beginDelayedTransition(this.mExpiredTimersScrollView, new AutoTransition());
        final int id = timer.getId();
        TimerItem timerItem = (TimerItem) getLayoutInflater().inflate(R.layout.timer_item, this.mExpiredTimersView, false);
        timerItem.setId(id);
        this.mExpiredTimersView.addView(timerItem);
        TextView textView = (TextView) timerItem.findViewById(R.id.timer_label);
        textView.setHint((CharSequence) null);
        textView.setVisibility(TextUtils.isEmpty(timer.getLabel()) ? 8 : 0);
        timerItem.findViewById(R.id.reset_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DataModel.getDataModel().addTimerMinute(DataModel.getDataModel().getTimer(id));
            }
        });
        List<Timer> expiredTimers = getExpiredTimers();
        if (expiredTimers.size() == 1) {
            centerFirstTimer();
        } else if (expiredTimers.size() == 2) {
            uncenterFirstTimer();
        }
    }

    private void removeTimer(Timer timer) {
        TransitionManager.beginDelayedTransition(this.mExpiredTimersScrollView, new AutoTransition());
        int id = timer.getId();
        int childCount = this.mExpiredTimersView.getChildCount();
        int i = 0;
        while (true) {
            if (i >= childCount) {
                break;
            }
            View childAt = this.mExpiredTimersView.getChildAt(i);
            if (childAt.getId() != id) {
                i++;
            } else {
                this.mExpiredTimersView.removeView(childAt);
                break;
            }
        }
        List<Timer> expiredTimers = getExpiredTimers();
        if (expiredTimers.isEmpty()) {
            finish();
        } else if (expiredTimers.size() == 1) {
            centerFirstTimer();
        }
    }

    private void centerFirstTimer() {
        ((FrameLayout.LayoutParams) this.mExpiredTimersView.getLayoutParams()).gravity = 17;
        this.mExpiredTimersView.requestLayout();
    }

    private void uncenterFirstTimer() {
        ((FrameLayout.LayoutParams) this.mExpiredTimersView.getLayoutParams()).gravity = 0;
        this.mExpiredTimersView.requestLayout();
    }

    private List<Timer> getExpiredTimers() {
        return DataModel.getDataModel().getExpiredTimers();
    }

    private class TimeUpdateRunnable implements Runnable {
        private TimeUpdateRunnable() {
        }

        @Override
        public void run() {
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            int childCount = ExpiredTimersActivity.this.mExpiredTimersView.getChildCount();
            for (int i = 0; i < childCount; i++) {
                TimerItem timerItem = (TimerItem) ExpiredTimersActivity.this.mExpiredTimersView.getChildAt(i);
                Timer timer = DataModel.getDataModel().getTimer(timerItem.getId());
                if (timer != null) {
                    timerItem.update(timer);
                }
            }
            ExpiredTimersActivity.this.mExpiredTimersView.postDelayed(this, Math.max(0L, (jElapsedRealtime + 20) - SystemClock.elapsedRealtime()));
        }
    }

    private class FabClickListener implements View.OnClickListener {
        private FabClickListener() {
        }

        @Override
        public void onClick(View view) {
            ExpiredTimersActivity.this.stopUpdatingTime();
            DataModel.getDataModel().removeTimerListener(ExpiredTimersActivity.this.mTimerChangeWatcher);
            DataModel.getDataModel().resetExpiredTimers(R.string.label_deskclock);
            ExpiredTimersActivity.this.finish();
        }
    }

    private class TimerChangeWatcher implements TimerListener {
        private TimerChangeWatcher() {
        }

        @Override
        public void timerAdded(Timer timer) {
            if (timer.isExpired()) {
                ExpiredTimersActivity.this.addTimer(timer);
            }
        }

        @Override
        public void timerUpdated(Timer timer, Timer timer2) {
            if (!timer.isExpired() && timer2.isExpired()) {
                ExpiredTimersActivity.this.addTimer(timer2);
            } else if (timer.isExpired() && !timer2.isExpired()) {
                ExpiredTimersActivity.this.removeTimer(timer);
            }
        }

        @Override
        public void timerRemoved(Timer timer) {
            if (timer.isExpired()) {
                ExpiredTimersActivity.this.removeTimer(timer);
            }
        }
    }
}
