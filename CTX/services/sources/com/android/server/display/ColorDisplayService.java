package com.android.server.display;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.MathUtils;
import android.util.Slog;
import android.view.animation.AnimationUtils;
import com.android.internal.app.ColorDisplayController;
import com.android.server.SystemService;
import com.android.server.twilight.TwilightListener;
import com.android.server.twilight.TwilightManager;
import com.android.server.twilight.TwilightState;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public final class ColorDisplayService extends SystemService implements ColorDisplayController.Callback {
    private static final ColorMatrixEvaluator COLOR_MATRIX_EVALUATOR;
    private static final float[] MATRIX_IDENTITY = new float[16];
    private static final String TAG = "ColorDisplayService";
    private static final long TRANSITION_DURATION = 3000;
    private AutoMode mAutoMode;
    private boolean mBootCompleted;
    private ValueAnimator mColorMatrixAnimator;
    private final float[] mColorTempCoefficients;
    private ColorDisplayController mController;
    private int mCurrentUser;
    private final Handler mHandler;
    private Boolean mIsActivated;
    private float[] mMatrixNight;
    private ContentObserver mUserSetupObserver;

    static {
        Matrix.setIdentityM(MATRIX_IDENTITY, 0);
        COLOR_MATRIX_EVALUATOR = new ColorMatrixEvaluator();
    }

    public ColorDisplayService(Context context) {
        super(context);
        this.mMatrixNight = new float[16];
        this.mColorTempCoefficients = new float[9];
        this.mCurrentUser = -10000;
        this.mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onBootPhase(int i) {
        if (i >= 1000) {
            this.mBootCompleted = true;
            if (this.mCurrentUser != -10000 && this.mUserSetupObserver == null) {
                setUp();
            }
        }
    }

    @Override
    public void onStartUser(int i) {
        super.onStartUser(i);
        if (this.mCurrentUser == -10000) {
            onUserChanged(i);
        }
    }

    @Override
    public void onSwitchUser(int i) {
        super.onSwitchUser(i);
        onUserChanged(i);
    }

    @Override
    public void onStopUser(int i) {
        super.onStopUser(i);
        if (this.mCurrentUser == i) {
            onUserChanged(-10000);
        }
    }

    private void onUserChanged(int i) {
        final ContentResolver contentResolver = getContext().getContentResolver();
        if (this.mCurrentUser != -10000) {
            if (this.mUserSetupObserver != null) {
                contentResolver.unregisterContentObserver(this.mUserSetupObserver);
                this.mUserSetupObserver = null;
            } else if (this.mBootCompleted) {
                tearDown();
            }
        }
        this.mCurrentUser = i;
        if (this.mCurrentUser != -10000) {
            if (!isUserSetupCompleted(contentResolver, this.mCurrentUser)) {
                this.mUserSetupObserver = new ContentObserver(this.mHandler) {
                    @Override
                    public void onChange(boolean z, Uri uri) {
                        if (ColorDisplayService.isUserSetupCompleted(contentResolver, ColorDisplayService.this.mCurrentUser)) {
                            contentResolver.unregisterContentObserver(this);
                            ColorDisplayService.this.mUserSetupObserver = null;
                            if (ColorDisplayService.this.mBootCompleted) {
                                ColorDisplayService.this.setUp();
                            }
                        }
                    }
                };
                contentResolver.registerContentObserver(Settings.Secure.getUriFor("user_setup_complete"), false, this.mUserSetupObserver, this.mCurrentUser);
            } else if (this.mBootCompleted) {
                setUp();
            }
        }
    }

    private static boolean isUserSetupCompleted(ContentResolver contentResolver, int i) {
        return Settings.Secure.getIntForUser(contentResolver, "user_setup_complete", 0, i) == 1;
    }

    private void setUp() {
        Slog.d(TAG, "setUp: currentUser=" + this.mCurrentUser);
        this.mController = new ColorDisplayController(getContext(), this.mCurrentUser);
        this.mController.setListener(this);
        onDisplayColorModeChanged(this.mController.getColorMode());
        this.mIsActivated = null;
        setCoefficientMatrix(getContext(), DisplayTransformManager.needsLinearColorMatrix());
        setMatrix(this.mController.getColorTemperature(), this.mMatrixNight);
        onAutoModeChanged(this.mController.getAutoMode());
        if (this.mIsActivated == null) {
            onActivated(this.mController.isActivated());
        }
    }

    private void tearDown() {
        Slog.d(TAG, "tearDown: currentUser=" + this.mCurrentUser);
        if (this.mController != null) {
            this.mController.setListener((ColorDisplayController.Callback) null);
            this.mController = null;
        }
        if (this.mAutoMode != null) {
            this.mAutoMode.onStop();
            this.mAutoMode = null;
        }
        if (this.mColorMatrixAnimator != null) {
            this.mColorMatrixAnimator.end();
            this.mColorMatrixAnimator = null;
        }
    }

    public void onActivated(boolean z) {
        if (this.mIsActivated == null || this.mIsActivated.booleanValue() != z) {
            Slog.i(TAG, z ? "Turning on night display" : "Turning off night display");
            this.mIsActivated = Boolean.valueOf(z);
            if (this.mAutoMode != null) {
                this.mAutoMode.onActivated(z);
            }
            applyTint(false);
        }
    }

    public void onAutoModeChanged(int i) {
        Slog.d(TAG, "onAutoModeChanged: autoMode=" + i);
        if (this.mAutoMode != null) {
            this.mAutoMode.onStop();
            this.mAutoMode = null;
        }
        if (i == 1) {
            this.mAutoMode = new CustomAutoMode();
        } else if (i == 2) {
            this.mAutoMode = new TwilightAutoMode();
        }
        if (this.mAutoMode != null) {
            this.mAutoMode.onStart();
        }
    }

    public void onCustomStartTimeChanged(LocalTime localTime) {
        Slog.d(TAG, "onCustomStartTimeChanged: startTime=" + localTime);
        if (this.mAutoMode != null) {
            this.mAutoMode.onCustomStartTimeChanged(localTime);
        }
    }

    public void onCustomEndTimeChanged(LocalTime localTime) {
        Slog.d(TAG, "onCustomEndTimeChanged: endTime=" + localTime);
        if (this.mAutoMode != null) {
            this.mAutoMode.onCustomEndTimeChanged(localTime);
        }
    }

    public void onColorTemperatureChanged(int i) {
        setMatrix(i, this.mMatrixNight);
        applyTint(true);
    }

    public void onDisplayColorModeChanged(int i) {
        if (i == -1) {
            return;
        }
        if (this.mColorMatrixAnimator != null) {
            this.mColorMatrixAnimator.cancel();
        }
        setCoefficientMatrix(getContext(), DisplayTransformManager.needsLinearColorMatrix(i));
        setMatrix(this.mController.getColorTemperature(), this.mMatrixNight);
        ((DisplayTransformManager) getLocalService(DisplayTransformManager.class)).setColorMode(i, (this.mIsActivated == null || !this.mIsActivated.booleanValue()) ? MATRIX_IDENTITY : this.mMatrixNight);
    }

    public void onAccessibilityTransformChanged(boolean z) {
        onDisplayColorModeChanged(this.mController.getColorMode());
    }

    private void setCoefficientMatrix(Context context, boolean z) {
        int i;
        Resources resources = context.getResources();
        if (z) {
            i = R.array.config_companionDevicePackages;
        } else {
            i = R.array.config_companionPermSyncEnabledCerts;
        }
        String[] stringArray = resources.getStringArray(i);
        for (int i2 = 0; i2 < 9 && i2 < stringArray.length; i2++) {
            this.mColorTempCoefficients[i2] = Float.parseFloat(stringArray[i2]);
        }
    }

    private void applyTint(boolean z) {
        if (this.mColorMatrixAnimator != null) {
            this.mColorMatrixAnimator.cancel();
        }
        final DisplayTransformManager displayTransformManager = (DisplayTransformManager) getLocalService(DisplayTransformManager.class);
        float[] colorMatrix = displayTransformManager.getColorMatrix(100);
        final float[] fArr = this.mIsActivated.booleanValue() ? this.mMatrixNight : MATRIX_IDENTITY;
        if (z) {
            displayTransformManager.setColorMatrix(100, fArr);
            return;
        }
        ColorMatrixEvaluator colorMatrixEvaluator = COLOR_MATRIX_EVALUATOR;
        Object[] objArr = new Object[2];
        if (colorMatrix == null) {
            colorMatrix = MATRIX_IDENTITY;
        }
        objArr[0] = colorMatrix;
        objArr[1] = fArr;
        this.mColorMatrixAnimator = ValueAnimator.ofObject(colorMatrixEvaluator, objArr);
        this.mColorMatrixAnimator.setDuration(TRANSITION_DURATION);
        this.mColorMatrixAnimator.setInterpolator(AnimationUtils.loadInterpolator(getContext(), R.interpolator.fast_out_slow_in));
        this.mColorMatrixAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                displayTransformManager.setColorMatrix(100, (float[]) valueAnimator.getAnimatedValue());
            }
        });
        this.mColorMatrixAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean mIsCancelled;

            @Override
            public void onAnimationCancel(Animator animator) {
                this.mIsCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (!this.mIsCancelled) {
                    displayTransformManager.setColorMatrix(100, fArr);
                }
                ColorDisplayService.this.mColorMatrixAnimator = null;
            }
        });
        this.mColorMatrixAnimator.start();
    }

    private void setMatrix(int i, float[] fArr) {
        if (fArr.length != 16) {
            Slog.d(TAG, "The display transformation matrix must be 4x4");
            return;
        }
        Matrix.setIdentityM(this.mMatrixNight, 0);
        float f = i * i;
        float f2 = i;
        float f3 = (this.mColorTempCoefficients[0] * f) + (this.mColorTempCoefficients[1] * f2) + this.mColorTempCoefficients[2];
        float f4 = (this.mColorTempCoefficients[3] * f) + (this.mColorTempCoefficients[4] * f2) + this.mColorTempCoefficients[5];
        float f5 = (f * this.mColorTempCoefficients[6]) + (f2 * this.mColorTempCoefficients[7]) + this.mColorTempCoefficients[8];
        fArr[0] = f3;
        fArr[5] = f4;
        fArr[10] = f5;
    }

    public static LocalDateTime getDateTimeBefore(LocalTime localTime, LocalDateTime localDateTime) {
        LocalDateTime localDateTimeOf = LocalDateTime.of(localDateTime.getYear(), localDateTime.getMonth(), localDateTime.getDayOfMonth(), localTime.getHour(), localTime.getMinute());
        return localDateTimeOf.isAfter(localDateTime) ? localDateTimeOf.minusDays(1L) : localDateTimeOf;
    }

    public static LocalDateTime getDateTimeAfter(LocalTime localTime, LocalDateTime localDateTime) {
        LocalDateTime localDateTimeOf = LocalDateTime.of(localDateTime.getYear(), localDateTime.getMonth(), localDateTime.getDayOfMonth(), localTime.getHour(), localTime.getMinute());
        return localDateTimeOf.isBefore(localDateTime) ? localDateTimeOf.plusDays(1L) : localDateTimeOf;
    }

    private abstract class AutoMode implements ColorDisplayController.Callback {
        public abstract void onStart();

        public abstract void onStop();

        private AutoMode() {
        }
    }

    private class CustomAutoMode extends AutoMode implements AlarmManager.OnAlarmListener {
        private final AlarmManager mAlarmManager;
        private LocalTime mEndTime;
        private LocalDateTime mLastActivatedTime;
        private LocalTime mStartTime;
        private final BroadcastReceiver mTimeChangedReceiver;

        CustomAutoMode() {
            super();
            this.mAlarmManager = (AlarmManager) ColorDisplayService.this.getContext().getSystemService("alarm");
            this.mTimeChangedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    CustomAutoMode.this.updateActivated();
                }
            };
        }

        private void updateActivated() {
            LocalDateTime localDateTimeNow = LocalDateTime.now();
            LocalDateTime dateTimeBefore = ColorDisplayService.getDateTimeBefore(this.mStartTime, localDateTimeNow);
            LocalDateTime dateTimeAfter = ColorDisplayService.getDateTimeAfter(this.mEndTime, dateTimeBefore);
            boolean zIsBefore = localDateTimeNow.isBefore(dateTimeAfter);
            if (this.mLastActivatedTime != null && this.mLastActivatedTime.isBefore(localDateTimeNow) && this.mLastActivatedTime.isAfter(dateTimeBefore) && (this.mLastActivatedTime.isAfter(dateTimeAfter) || localDateTimeNow.isBefore(dateTimeAfter))) {
                zIsBefore = ColorDisplayService.this.mController.isActivated();
            }
            if (ColorDisplayService.this.mIsActivated == null || ColorDisplayService.this.mIsActivated.booleanValue() != zIsBefore) {
                ColorDisplayService.this.mController.setActivated(zIsBefore);
            }
            updateNextAlarm(ColorDisplayService.this.mIsActivated, localDateTimeNow);
        }

        private void updateNextAlarm(Boolean bool, LocalDateTime localDateTime) {
            if (bool != null) {
                this.mAlarmManager.setExact(1, (bool.booleanValue() ? ColorDisplayService.getDateTimeAfter(this.mEndTime, localDateTime) : ColorDisplayService.getDateTimeAfter(this.mStartTime, localDateTime)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), ColorDisplayService.TAG, this, null);
            }
        }

        @Override
        public void onStart() {
            IntentFilter intentFilter = new IntentFilter("android.intent.action.TIME_SET");
            intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
            ColorDisplayService.this.getContext().registerReceiver(this.mTimeChangedReceiver, intentFilter);
            this.mStartTime = ColorDisplayService.this.mController.getCustomStartTime();
            this.mEndTime = ColorDisplayService.this.mController.getCustomEndTime();
            this.mLastActivatedTime = ColorDisplayService.this.mController.getLastActivatedTime();
            updateActivated();
        }

        @Override
        public void onStop() {
            ColorDisplayService.this.getContext().unregisterReceiver(this.mTimeChangedReceiver);
            this.mAlarmManager.cancel(this);
            this.mLastActivatedTime = null;
        }

        public void onActivated(boolean z) {
            this.mLastActivatedTime = ColorDisplayService.this.mController.getLastActivatedTime();
            updateNextAlarm(Boolean.valueOf(z), LocalDateTime.now());
        }

        public void onCustomStartTimeChanged(LocalTime localTime) {
            this.mStartTime = localTime;
            this.mLastActivatedTime = null;
            updateActivated();
        }

        public void onCustomEndTimeChanged(LocalTime localTime) {
            this.mEndTime = localTime;
            this.mLastActivatedTime = null;
            updateActivated();
        }

        @Override
        public void onAlarm() {
            Slog.d(ColorDisplayService.TAG, "onAlarm");
            updateActivated();
        }
    }

    private class TwilightAutoMode extends AutoMode implements TwilightListener {
        private final TwilightManager mTwilightManager;

        TwilightAutoMode() {
            super();
            this.mTwilightManager = (TwilightManager) ColorDisplayService.this.getLocalService(TwilightManager.class);
        }

        private void updateActivated(TwilightState twilightState) {
            if (twilightState == null) {
                return;
            }
            boolean zIsNight = twilightState.isNight();
            LocalDateTime lastActivatedTime = ColorDisplayService.this.mController.getLastActivatedTime();
            if (lastActivatedTime != null) {
                LocalDateTime localDateTimeNow = LocalDateTime.now();
                LocalDateTime localDateTimeSunrise = twilightState.sunrise();
                LocalDateTime localDateTimeSunset = twilightState.sunset();
                if (lastActivatedTime.isBefore(localDateTimeNow)) {
                    if (lastActivatedTime.isBefore(localDateTimeSunset) ^ lastActivatedTime.isBefore(localDateTimeSunrise)) {
                        zIsNight = ColorDisplayService.this.mController.isActivated();
                    }
                }
            }
            if (ColorDisplayService.this.mIsActivated == null || ColorDisplayService.this.mIsActivated.booleanValue() != zIsNight) {
                ColorDisplayService.this.mController.setActivated(zIsNight);
            }
        }

        @Override
        public void onStart() {
            this.mTwilightManager.registerListener(this, ColorDisplayService.this.mHandler);
            updateActivated(this.mTwilightManager.getLastTwilightState());
        }

        @Override
        public void onStop() {
            this.mTwilightManager.unregisterListener(this);
        }

        public void onActivated(boolean z) {
        }

        @Override
        public void onTwilightStateChanged(TwilightState twilightState) {
            StringBuilder sb = new StringBuilder();
            sb.append("onTwilightStateChanged: isNight=");
            sb.append(twilightState == null ? null : Boolean.valueOf(twilightState.isNight()));
            Slog.d(ColorDisplayService.TAG, sb.toString());
            updateActivated(twilightState);
        }
    }

    private static class ColorMatrixEvaluator implements TypeEvaluator<float[]> {
        private final float[] mResultMatrix;

        private ColorMatrixEvaluator() {
            this.mResultMatrix = new float[16];
        }

        @Override
        public float[] evaluate(float f, float[] fArr, float[] fArr2) {
            for (int i = 0; i < this.mResultMatrix.length; i++) {
                this.mResultMatrix[i] = MathUtils.lerp(fArr[i], fArr2[i], f);
            }
            return this.mResultMatrix;
        }
    }
}
