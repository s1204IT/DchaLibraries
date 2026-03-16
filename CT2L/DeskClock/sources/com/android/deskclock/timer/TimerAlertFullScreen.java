package com.android.deskclock.timer;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.timer.TimerFullScreenFragment;

public class TimerAlertFullScreen extends Activity implements TimerFullScreenFragment.OnEmptyListListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timer_alert_full_screen);
        View view = findViewById(R.id.fragment_container);
        view.setSystemUiVisibility(1);
        Window win = getWindow();
        win.addFlags(4718592);
        win.addFlags(2097281);
        if (getFragment() == null) {
            TimerFullScreenFragment timerFragment = new TimerFullScreenFragment();
            Bundle args = new Bundle();
            args.putBoolean("times_up", true);
            timerFragment.setArguments(args);
            getFragmentManager().beginTransaction().add(R.id.fragment_container, timerFragment, "timer").commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setBackgroundColor(Utils.getCurrentHourColor());
        Utils.cancelTimesUpNotifications(this);
    }

    @Override
    public void onPause() {
        Utils.showTimesUpNotifications(this);
        super.onPause();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean up = event.getAction() == 1;
        switch (event.getKeyCode()) {
            case 24:
            case 25:
            case 27:
            case 80:
            case 164:
                if (!up) {
                    return true;
                }
                stopAllTimesUpTimers();
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        TimerFullScreenFragment timerFragment = getFragment();
        if (timerFragment != null) {
            timerFragment.restartAdapter();
        }
        super.onNewIntent(intent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        ViewGroup viewContainer = (ViewGroup) findViewById(R.id.fragment_container);
        viewContainer.requestLayout();
        super.onConfigurationChanged(newConfig);
    }

    protected void stopAllTimesUpTimers() {
        TimerFullScreenFragment timerFragment = getFragment();
        if (timerFragment != null) {
            timerFragment.updateAllTimesUpTimers(true);
        }
    }

    @Override
    public void onEmptyList() {
        Log.v("TimerAlertFullScreen", "onEmptyList");
        onListChanged();
        finish();
    }

    @Override
    public void onListChanged() {
        Utils.showInUseNotifications(this);
    }

    private TimerFullScreenFragment getFragment() {
        return (TimerFullScreenFragment) getFragmentManager().findFragmentByTag("timer");
    }
}
