package com.android.systemui.analytics;

import android.content.Context;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.view.MotionEvent;
import com.android.systemui.statusbar.phone.TouchAnalyticsProto$Session;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DataCollector implements SensorEventListener {
    private static DataCollector sInstance = null;
    private final Context mContext;
    private final Handler mHandler = new Handler();
    private SensorLoggerSession mCurrentSession = null;
    private boolean mEnableCollector = false;
    private boolean mTimeoutActive = false;
    private boolean mCollectBadTouches = false;
    private boolean mCornerSwiping = false;
    private boolean mTrackingStarted = false;
    protected final ContentObserver mSettingsObserver = new ContentObserver(this.mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            DataCollector.this.updateConfiguration();
        }
    };

    private DataCollector(Context context) {
        this.mContext = context;
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("data_collector_enable"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("data_collector_collect_bad_touches"), false, this.mSettingsObserver, -1);
        updateConfiguration();
    }

    public static DataCollector getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DataCollector(context);
        }
        return sInstance;
    }

    public void updateConfiguration() {
        boolean z = false;
        this.mEnableCollector = Build.IS_DEBUGGABLE && Settings.Secure.getInt(this.mContext.getContentResolver(), "data_collector_enable", 0) != 0;
        if (this.mEnableCollector && Settings.Secure.getInt(this.mContext.getContentResolver(), "data_collector_collect_bad_touches", 0) != 0) {
            z = true;
        }
        this.mCollectBadTouches = z;
    }

    private boolean sessionEntrypoint() {
        if (this.mEnableCollector && this.mCurrentSession == null) {
            onSessionStart();
            return true;
        }
        return false;
    }

    private void sessionExitpoint(int result) {
        if (!this.mEnableCollector || this.mCurrentSession == null) {
            return;
        }
        onSessionEnd(result);
    }

    private void onSessionStart() {
        this.mCornerSwiping = false;
        this.mTrackingStarted = false;
        this.mCurrentSession = new SensorLoggerSession(System.currentTimeMillis(), System.nanoTime());
    }

    private void onSessionEnd(int result) {
        SensorLoggerSession session = this.mCurrentSession;
        this.mCurrentSession = null;
        session.end(System.currentTimeMillis(), result);
        queueSession(session);
    }

    private void queueSession(final SensorLoggerSession currentSession) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                String dir;
                byte[] b = TouchAnalyticsProto$Session.toByteArray(currentSession.toProto());
                String dir2 = DataCollector.this.mContext.getFilesDir().getAbsolutePath();
                if (currentSession.getResult() != 1) {
                    if (!DataCollector.this.mCollectBadTouches) {
                        return;
                    } else {
                        dir = dir2 + "/bad_touches";
                    }
                } else {
                    dir = dir2 + "/good_touches";
                }
                File file = new File(dir);
                file.mkdir();
                File touch = new File(file, "trace_" + System.currentTimeMillis());
                try {
                    new FileOutputStream(touch).write(b);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public synchronized void onSensorChanged(SensorEvent event) {
        if (this.mEnableCollector && this.mCurrentSession != null) {
            this.mCurrentSession.addSensorEvent(event, System.nanoTime());
            enforceTimeout();
        }
    }

    private void enforceTimeout() {
        if (!this.mTimeoutActive || System.currentTimeMillis() - this.mCurrentSession.getStartTimestampMillis() <= 11000) {
            return;
        }
        onSessionEnd(2);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public boolean isEnabled() {
        return this.mEnableCollector;
    }

    public void onScreenTurningOn() {
        if (!sessionEntrypoint()) {
            return;
        }
        addEvent(0);
    }

    public void onScreenOnFromTouch() {
        if (!sessionEntrypoint()) {
            return;
        }
        addEvent(1);
    }

    public void onScreenOff() {
        addEvent(2);
        sessionExitpoint(0);
    }

    public void onSucccessfulUnlock() {
        addEvent(3);
        sessionExitpoint(1);
    }

    public void onBouncerShown() {
        addEvent(4);
    }

    public void onBouncerHidden() {
        addEvent(5);
    }

    public void onQsDown() {
        addEvent(6);
    }

    public void setQsExpanded(boolean expanded) {
        if (expanded) {
            addEvent(7);
        } else {
            addEvent(8);
        }
    }

    public void onTrackingStarted() {
        this.mTrackingStarted = true;
        addEvent(9);
    }

    public void onTrackingStopped() {
        if (!this.mTrackingStarted) {
            return;
        }
        this.mTrackingStarted = false;
        addEvent(10);
    }

    public void onNotificationActive() {
        addEvent(11);
    }

    public void onNotificationDoubleTap() {
        addEvent(13);
    }

    public void setNotificationExpanded() {
        addEvent(14);
    }

    public void onNotificatonStartDraggingDown() {
        addEvent(16);
    }

    public void onNotificatonStopDraggingDown() {
        addEvent(17);
    }

    public void onNotificationDismissed() {
        addEvent(18);
    }

    public void onNotificatonStartDismissing() {
        addEvent(19);
    }

    public void onNotificatonStopDismissing() {
        addEvent(20);
    }

    public void onCameraOn() {
        addEvent(24);
    }

    public void onLeftAffordanceOn() {
        addEvent(25);
    }

    public void onAffordanceSwipingStarted(boolean rightCorner) {
        this.mCornerSwiping = true;
        if (rightCorner) {
            addEvent(21);
        } else {
            addEvent(22);
        }
    }

    public void onAffordanceSwipingAborted() {
        if (!this.mCornerSwiping) {
            return;
        }
        this.mCornerSwiping = false;
        addEvent(23);
    }

    public void onUnlockHintStarted() {
        addEvent(26);
    }

    public void onCameraHintStarted() {
        addEvent(27);
    }

    public void onLeftAffordanceHintStarted() {
        addEvent(28);
    }

    public void onTouchEvent(MotionEvent event, int width, int height) {
        if (this.mCurrentSession == null) {
            return;
        }
        this.mCurrentSession.addMotionEvent(event);
        this.mCurrentSession.setTouchArea(width, height);
        enforceTimeout();
    }

    private void addEvent(int eventType) {
        if (!this.mEnableCollector || this.mCurrentSession == null) {
            return;
        }
        this.mCurrentSession.addPhoneEvent(eventType, System.nanoTime());
    }
}
