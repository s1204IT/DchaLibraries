package com.android.systemui.classifier;

import android.content.Context;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityManager;
import com.android.systemui.Dependency;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.analytics.DataCollector;
import com.android.systemui.statusbar.StatusBarState;
import com.android.systemui.util.AsyncSensorManager;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class FalsingManager implements SensorEventListener {
    private static final int[] CLASSIFIER_SENSORS = {8};
    private static final int[] COLLECTOR_SENSORS = {1, 4, 8, 5, 11};
    private static FalsingManager sInstance = null;
    private final AccessibilityManager mAccessibilityManager;
    private final Context mContext;
    private final DataCollector mDataCollector;
    private final HumanInteractionClassifier mHumanInteractionClassifier;
    private Runnable mPendingWtf;
    private boolean mScreenOn;
    private boolean mShowingAod;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mEnforceBouncer = false;
    private boolean mBouncerOn = false;
    private boolean mBouncerOffOnDown = false;
    private boolean mSessionActive = false;
    private boolean mIsTouchScreen = true;
    private int mState = 0;
    protected final ContentObserver mSettingsObserver = new ContentObserver(this.mHandler) { // from class: com.android.systemui.classifier.FalsingManager.1
        @Override // android.database.ContentObserver
        public void onChange(boolean z) {
            FalsingManager.this.updateConfiguration();
        }
    };
    private final SensorManager mSensorManager = (SensorManager) Dependency.get(AsyncSensorManager.class);
    private final UiOffloadThread mUiOffloadThread = (UiOffloadThread) Dependency.get(UiOffloadThread.class);

    private FalsingManager(Context context) {
        this.mContext = context;
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService(AccessibilityManager.class);
        this.mDataCollector = DataCollector.getInstance(this.mContext);
        this.mHumanInteractionClassifier = HumanInteractionClassifier.getInstance(this.mContext);
        this.mScreenOn = ((PowerManager) context.getSystemService(PowerManager.class)).isInteractive();
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("falsing_manager_enforce_bouncer"), false, this.mSettingsObserver, -1);
        updateConfiguration();
    }

    public static FalsingManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new FalsingManager(context);
        }
        return sInstance;
    }

    private void updateConfiguration() {
        this.mEnforceBouncer = Settings.Secure.getInt(this.mContext.getContentResolver(), "falsing_manager_enforce_bouncer", 0) != 0;
    }

    private boolean shouldSessionBeActive() {
        boolean z = FalsingLog.ENABLED;
        return isEnabled() && this.mScreenOn && this.mState == 1 && !this.mShowingAod;
    }

    private boolean sessionEntrypoint() {
        if (!this.mSessionActive && shouldSessionBeActive()) {
            onSessionStart();
            return true;
        }
        return false;
    }

    private void sessionExitpoint(boolean z) {
        if (this.mSessionActive) {
            if (z || !shouldSessionBeActive()) {
                this.mSessionActive = false;
                this.mUiOffloadThread.submit(new Runnable() { // from class: com.android.systemui.classifier.-$$Lambda$FalsingManager$y6A_bA-S5VQwPHuaVePl4zXiK1I
                    @Override // java.lang.Runnable
                    public final void run() {
                        FalsingManager falsingManager = this.f$0;
                        falsingManager.mSensorManager.unregisterListener(falsingManager);
                    }
                });
            }
        }
    }

    public void updateSessionActive() {
        if (shouldSessionBeActive()) {
            sessionEntrypoint();
        } else {
            sessionExitpoint(false);
        }
    }

    private void onSessionStart() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onSessionStart", "classifierEnabled=" + isClassiferEnabled());
            clearPendingWtf();
        }
        this.mBouncerOn = false;
        this.mSessionActive = true;
        if (this.mHumanInteractionClassifier.isEnabled()) {
            registerSensors(CLASSIFIER_SENSORS);
        }
        if (this.mDataCollector.isEnabledFull()) {
            registerSensors(COLLECTOR_SENSORS);
        }
        if (this.mDataCollector.isEnabled()) {
            this.mDataCollector.onFalsingSessionStarted();
        }
    }

    private void registerSensors(int[] iArr) {
        for (int i : iArr) {
            final Sensor defaultSensor = this.mSensorManager.getDefaultSensor(i);
            if (defaultSensor != null) {
                this.mUiOffloadThread.submit(new Runnable() { // from class: com.android.systemui.classifier.-$$Lambda$FalsingManager$XiT4szturm5rmaM7rTzbChKqPRY
                    @Override // java.lang.Runnable
                    public final void run() {
                        FalsingManager falsingManager = this.f$0;
                        falsingManager.mSensorManager.registerListener(falsingManager, defaultSensor, 1);
                    }
                });
            }
        }
    }

    public boolean isClassiferEnabled() {
        return this.mHumanInteractionClassifier.isEnabled();
    }

    private boolean isEnabled() {
        return this.mHumanInteractionClassifier.isEnabled() || this.mDataCollector.isEnabled();
    }

    public boolean isFalseTouch() {
        if (FalsingLog.ENABLED && !this.mSessionActive && ((PowerManager) this.mContext.getSystemService(PowerManager.class)).isInteractive() && this.mPendingWtf == null) {
            boolean zIsEnabled = isEnabled();
            boolean z = this.mScreenOn;
            final String shortString = StatusBarState.toShortString(this.mState);
            final Throwable th = new Throwable("here");
            FalsingLog.wLogcat("isFalseTouch", "Session is not active, yet there's a query for a false touch. enabled=" + (zIsEnabled ? 1 : 0) + " mScreenOn=" + (z ? 1 : 0) + " mState=" + shortString + ". Escalating to WTF if screen does not turn on soon.");
            final int i = zIsEnabled ? 1 : 0;
            final int i2 = z ? 1 : 0;
            this.mPendingWtf = new Runnable() { // from class: com.android.systemui.classifier.-$$Lambda$FalsingManager$ysaxoOhQjefK1Np-0ekN-RBwhN8
                @Override // java.lang.Runnable
                public final void run() {
                    FalsingManager falsingManager = this.f$0;
                    FalsingLog.wtf("isFalseTouch", "Session did not become active after query for a false touch. enabled=" + i + '/' + (falsingManager.isEnabled() ? 1 : 0) + " mScreenOn=" + i2 + '/' + (falsingManager.mScreenOn ? 1 : 0) + " mState=" + shortString + '/' + StatusBarState.toShortString(falsingManager.mState) + ". Look for warnings ~1000ms earlier to see root cause.", th);
                }
            };
            this.mHandler.postDelayed(this.mPendingWtf, 1000L);
        }
        if (!this.mAccessibilityManager.isTouchExplorationEnabled() && this.mIsTouchScreen) {
            return this.mHumanInteractionClassifier.isFalseTouch();
        }
        return false;
    }

    private void clearPendingWtf() {
        if (this.mPendingWtf != null) {
            this.mHandler.removeCallbacks(this.mPendingWtf);
            this.mPendingWtf = null;
        }
    }

    @Override // android.hardware.SensorEventListener
    public synchronized void onSensorChanged(SensorEvent sensorEvent) {
        this.mDataCollector.onSensorChanged(sensorEvent);
        this.mHumanInteractionClassifier.onSensorChanged(sensorEvent);
    }

    @Override // android.hardware.SensorEventListener
    public void onAccuracyChanged(Sensor sensor, int i) {
        this.mDataCollector.onAccuracyChanged(sensor, i);
    }

    public boolean shouldEnforceBouncer() {
        return this.mEnforceBouncer;
    }

    public void setShowingAod(boolean z) {
        this.mShowingAod = z;
        updateSessionActive();
    }

    public void setStatusBarState(int i) {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("setStatusBarState", "from=" + StatusBarState.toShortString(this.mState) + " to=" + StatusBarState.toShortString(i));
        }
        this.mState = i;
        updateSessionActive();
    }

    public void onScreenTurningOn() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onScreenTurningOn", "from=" + (this.mScreenOn ? 1 : 0));
            clearPendingWtf();
        }
        this.mScreenOn = true;
        if (sessionEntrypoint()) {
            this.mDataCollector.onScreenTurningOn();
        }
    }

    public void onScreenOnFromTouch() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onScreenOnFromTouch", "from=" + (this.mScreenOn ? 1 : 0));
        }
        this.mScreenOn = true;
        if (sessionEntrypoint()) {
            this.mDataCollector.onScreenOnFromTouch();
        }
    }

    public void onScreenOff() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onScreenOff", "from=" + (this.mScreenOn ? 1 : 0));
        }
        this.mDataCollector.onScreenOff();
        this.mScreenOn = false;
        sessionExitpoint(false);
    }

    public void onSucccessfulUnlock() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onSucccessfulUnlock", "");
        }
        this.mDataCollector.onSucccessfulUnlock();
    }

    public void onBouncerShown() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onBouncerShown", "from=" + (this.mBouncerOn ? 1 : 0));
        }
        if (!this.mBouncerOn) {
            this.mBouncerOn = true;
            this.mDataCollector.onBouncerShown();
        }
    }

    public void onBouncerHidden() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onBouncerHidden", "from=" + (this.mBouncerOn ? 1 : 0));
        }
        if (this.mBouncerOn) {
            this.mBouncerOn = false;
            this.mDataCollector.onBouncerHidden();
        }
    }

    public void onQsDown() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onQsDown", "");
        }
        this.mHumanInteractionClassifier.setType(0);
        this.mDataCollector.onQsDown();
    }

    public void setQsExpanded(boolean z) {
        this.mDataCollector.setQsExpanded(z);
    }

    public void onTrackingStarted(boolean z) {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onTrackingStarted", "");
        }
        this.mHumanInteractionClassifier.setType(z ? 8 : 4);
        this.mDataCollector.onTrackingStarted();
    }

    public void onTrackingStopped() {
        this.mDataCollector.onTrackingStopped();
    }

    public void onNotificationActive() {
        this.mDataCollector.onNotificationActive();
    }

    public void onNotificationDoubleTap(boolean z, float f, float f2) {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onNotificationDoubleTap", "accepted=" + z + " dx=" + f + " dy=" + f2 + " (px)");
        }
        this.mDataCollector.onNotificationDoubleTap();
    }

    public void setNotificationExpanded() {
        this.mDataCollector.setNotificationExpanded();
    }

    public void onNotificatonStartDraggingDown() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onNotificatonStartDraggingDown", "");
        }
        this.mHumanInteractionClassifier.setType(2);
        this.mDataCollector.onNotificatonStartDraggingDown();
    }

    public void onNotificatonStopDraggingDown() {
        this.mDataCollector.onNotificatonStopDraggingDown();
    }

    public void onNotificationDismissed() {
        this.mDataCollector.onNotificationDismissed();
    }

    public void onNotificatonStartDismissing() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onNotificatonStartDismissing", "");
        }
        this.mHumanInteractionClassifier.setType(1);
        this.mDataCollector.onNotificatonStartDismissing();
    }

    public void onNotificatonStopDismissing() {
        this.mDataCollector.onNotificatonStopDismissing();
    }

    public void onCameraOn() {
        this.mDataCollector.onCameraOn();
    }

    public void onLeftAffordanceOn() {
        this.mDataCollector.onLeftAffordanceOn();
    }

    public void onAffordanceSwipingStarted(boolean z) {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onAffordanceSwipingStarted", "");
        }
        if (z) {
            this.mHumanInteractionClassifier.setType(6);
        } else {
            this.mHumanInteractionClassifier.setType(5);
        }
        this.mDataCollector.onAffordanceSwipingStarted(z);
    }

    public void onAffordanceSwipingAborted() {
        this.mDataCollector.onAffordanceSwipingAborted();
    }

    public void onUnlockHintStarted() {
        this.mDataCollector.onUnlockHintStarted();
    }

    public void onCameraHintStarted() {
        this.mDataCollector.onCameraHintStarted();
    }

    public void onLeftAffordanceHintStarted() {
        this.mDataCollector.onLeftAffordanceHintStarted();
    }

    public void onTouchEvent(MotionEvent motionEvent, int i, int i2) {
        if (motionEvent.getAction() == 0) {
            this.mIsTouchScreen = motionEvent.isFromSource(4098);
            this.mBouncerOffOnDown = !this.mBouncerOn;
        }
        if (this.mSessionActive) {
            if (!this.mBouncerOn) {
                this.mDataCollector.onTouchEvent(motionEvent, i, i2);
            }
            if (this.mBouncerOffOnDown) {
                this.mHumanInteractionClassifier.onTouchEvent(motionEvent);
            }
        }
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("FALSING MANAGER");
        printWriter.print("classifierEnabled=");
        printWriter.println(isClassiferEnabled() ? 1 : 0);
        printWriter.print("mSessionActive=");
        printWriter.println(this.mSessionActive ? 1 : 0);
        printWriter.print("mBouncerOn=");
        printWriter.println(this.mSessionActive ? 1 : 0);
        printWriter.print("mState=");
        printWriter.println(StatusBarState.toShortString(this.mState));
        printWriter.print("mScreenOn=");
        printWriter.println(this.mScreenOn ? 1 : 0);
        printWriter.println();
    }

    public Uri reportRejectedTouch() {
        if (this.mDataCollector.isEnabled()) {
            return this.mDataCollector.reportRejectedTouch();
        }
        return null;
    }

    public boolean isReportingEnabled() {
        return this.mDataCollector.isReportingEnabled();
    }
}
