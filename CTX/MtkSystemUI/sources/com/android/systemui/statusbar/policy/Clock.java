package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.settingslib.Utils;
import com.android.systemui.DemoMode;
import com.android.systemui.Dependency;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.Clock;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.tuner.TunerService;
import com.mediatek.systemui.ext.ISystemUIStatusBarExt;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import libcore.icu.LocaleData;

public class Clock extends TextView implements DemoMode, CommandQueue.Callbacks, ConfigurationController.ConfigurationListener, DarkIconDispatcher.DarkReceiver, TunerService.Tunable {
    private final int mAmPmStyle;
    private boolean mAttached;
    private Calendar mCalendar;
    private SimpleDateFormat mClockFormat;
    private String mClockFormatString;
    private boolean mClockVisibleByPolicy;
    private boolean mClockVisibleByUser;
    private SimpleDateFormat mContentDescriptionFormat;
    private int mCurrentUserId;
    private final CurrentUserTracker mCurrentUserTracker;
    private boolean mDemoMode;
    private final BroadcastReceiver mIntentReceiver;
    private Locale mLocale;
    private int mNonAdaptedColor;
    private ContentObserver mObs;
    private final BroadcastReceiver mScreenReceiver;
    private final Runnable mSecondTick;
    private Handler mSecondsHandler;
    private final boolean mShowDark;
    private boolean mShowSeconds;
    private ISystemUIStatusBarExt mStatusBarExt;
    private OpSystemUICustomizationFactoryBase mSystemUIFactoryBase;
    private boolean mUseWallpaperTextColor;

    public Clock(Context context) {
        this(context, null);
    }

    public Clock(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public Clock(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mClockVisibleByPolicy = true;
        this.mClockVisibleByUser = true;
        this.mSystemUIFactoryBase = null;
        this.mStatusBarExt = null;
        this.mIntentReceiver = new AnonymousClass3();
        this.mScreenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.SCREEN_OFF".equals(action)) {
                    if (Clock.this.mSecondsHandler != null) {
                        Clock.this.mSecondsHandler.removeCallbacks(Clock.this.mSecondTick);
                    }
                } else if ("android.intent.action.SCREEN_ON".equals(action) && Clock.this.mSecondsHandler != null) {
                    Clock.this.mSecondsHandler.postAtTime(Clock.this.mSecondTick, ((SystemClock.uptimeMillis() / 1000) * 1000) + 1000);
                }
            }
        };
        this.mSecondTick = new Runnable() {
            @Override
            public void run() {
                if (Clock.this.mCalendar != null) {
                    Clock.this.updateClock();
                }
                Clock.this.mSecondsHandler.postAtTime(this, ((SystemClock.uptimeMillis() / 1000) * 1000) + 1000);
            }
        };
        TypedArray typedArrayObtainStyledAttributes = context.getTheme().obtainStyledAttributes(attributeSet, R.styleable.Clock, 0, 0);
        try {
            this.mSystemUIFactoryBase = OpSystemUICustomizationFactoryBase.getOpFactory(context);
            this.mStatusBarExt = this.mSystemUIFactoryBase.makeSystemUIStatusBar(context);
            this.mAmPmStyle = this.mStatusBarExt.getClockAmPmStyle(typedArrayObtainStyledAttributes.getInt(0, 2));
            this.mShowDark = typedArrayObtainStyledAttributes.getBoolean(1, true);
            this.mNonAdaptedColor = getCurrentTextColor();
            typedArrayObtainStyledAttributes.recycle();
            this.mCurrentUserTracker = new CurrentUserTracker(context) {
                @Override
                public void onUserSwitched(int i2) {
                    Clock.this.mCurrentUserId = i2;
                }
            };
        } catch (Throwable th) {
            typedArrayObtainStyledAttributes.recycle();
            throw th;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!this.mAttached) {
            this.mAttached = true;
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.TIME_TICK");
            intentFilter.addAction("android.intent.action.TIME_SET");
            intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
            intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
            intentFilter.addAction("android.intent.action.USER_SWITCHED");
            getContext().registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, intentFilter, null, (Handler) Dependency.get(Dependency.TIME_TICK_HANDLER));
            this.mObs = new AnonymousClass2((Handler) Dependency.get(Dependency.TIME_TICK_HANDLER));
            getContext().getContentResolver().registerContentObserver(Settings.System.getUriFor("dcha_state"), false, this.mObs, -1);
            ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "clock_seconds", "icon_blacklist");
            ((CommandQueue) SysUiServiceProvider.getComponent(getContext(), CommandQueue.class)).addCallbacks(this);
            if (this.mShowDark) {
                ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).addDarkReceiver(this);
            }
            this.mCurrentUserTracker.startTracking();
            this.mCurrentUserId = this.mCurrentUserTracker.getCurrentUserId();
        }
        this.mCalendar = Calendar.getInstance(TimeZone.getDefault());
        updateClock();
        updateShowSeconds();
    }

    class AnonymousClass2 extends ContentObserver {
        AnonymousClass2(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean z) {
            Clock.this.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    Clock.this.updateClock();
                }
            });
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mAttached) {
            getContext().getContentResolver().unregisterContentObserver(this.mObs);
            this.mObs = null;
            getContext().unregisterReceiver(this.mIntentReceiver);
            this.mAttached = false;
            ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
            ((CommandQueue) SysUiServiceProvider.getComponent(getContext(), CommandQueue.class)).removeCallbacks(this);
            if (this.mShowDark) {
                ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).removeDarkReceiver(this);
            }
            this.mCurrentUserTracker.stopTracking();
        }
    }

    class AnonymousClass3 extends BroadcastReceiver {
        AnonymousClass3() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.TIMEZONE_CHANGED")) {
                final String stringExtra = intent.getStringExtra("time-zone");
                Clock.this.getHandler().post(new Runnable() {
                    @Override
                    public final void run() {
                        Clock.AnonymousClass3.lambda$onReceive$0(this.f$0, stringExtra);
                    }
                });
            } else if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                final Locale locale = Clock.this.getResources().getConfiguration().locale;
                Clock.this.getHandler().post(new Runnable() {
                    @Override
                    public final void run() {
                        Clock.AnonymousClass3.lambda$onReceive$1(this.f$0, locale);
                    }
                });
            }
            Clock.this.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    Clock.this.updateClock();
                }
            });
        }

        public static void lambda$onReceive$0(AnonymousClass3 anonymousClass3, String str) {
            Clock.this.mCalendar = Calendar.getInstance(TimeZone.getTimeZone(str));
            if (Clock.this.mClockFormat != null) {
                Clock.this.mClockFormat.setTimeZone(Clock.this.mCalendar.getTimeZone());
            }
        }

        public static void lambda$onReceive$1(AnonymousClass3 anonymousClass3, Locale locale) {
            if (!locale.equals(Clock.this.mLocale)) {
                Clock.this.mLocale = locale;
                Clock.this.mClockFormatString = "";
            }
        }
    }

    public void setClockVisibleByUser(boolean z) {
        this.mClockVisibleByUser = z;
        updateClockVisibility();
    }

    public void setClockVisibilityByPolicy(boolean z) {
        this.mClockVisibleByPolicy = z;
        updateClockVisibility();
    }

    private void updateClockVisibility() {
        boolean z = this.mClockVisibleByPolicy && this.mClockVisibleByUser;
        ((IconLogger) Dependency.get(IconLogger.class)).onIconVisibility("clock", z);
        setVisibility(z ? 0 : 8);
    }

    final void updateClock() {
        if (this.mDemoMode) {
            return;
        }
        this.mCalendar.setTimeInMillis(System.currentTimeMillis());
        setText(getSmallTime());
        setContentDescription(this.mContentDescriptionFormat.format(this.mCalendar.getTime()));
    }

    @Override
    public void onTuningChanged(String str, String str2) {
        if ("clock_seconds".equals(str)) {
            this.mShowSeconds = (str2 == null || Integer.parseInt(str2) == 0) ? false : true;
            updateShowSeconds();
        } else {
            setClockVisibleByUser(!StatusBarIconController.getIconBlacklist(str2).contains("clock"));
            updateClockVisibility();
        }
    }

    @Override
    public void disable(int i, int i2, boolean z) {
        boolean z2 = (i & 8388608) == 0;
        if (z2 != this.mClockVisibleByPolicy) {
            setClockVisibilityByPolicy(z2);
        }
    }

    @Override
    public void onDarkChanged(Rect rect, float f, int i) {
        this.mNonAdaptedColor = DarkIconDispatcher.getTint(rect, this, i);
        if (!this.mUseWallpaperTextColor) {
            setTextColor(this.mNonAdaptedColor);
        }
    }

    @Override
    public void onDensityOrFontScaleChanged() {
        FontSizeUtils.updateFontSize(this, R.dimen.status_bar_clock_size);
        setPaddingRelative(this.mContext.getResources().getDimensionPixelSize(R.dimen.status_bar_clock_starting_padding), 0, this.mContext.getResources().getDimensionPixelSize(R.dimen.status_bar_clock_end_padding), 0);
    }

    public void useWallpaperTextColor(boolean z) {
        if (z == this.mUseWallpaperTextColor) {
            return;
        }
        this.mUseWallpaperTextColor = z;
        if (this.mUseWallpaperTextColor) {
            setTextColor(Utils.getColorAttr(this.mContext, R.attr.wallpaperTextColor));
        } else {
            setTextColor(this.mNonAdaptedColor);
        }
    }

    private void updateShowSeconds() {
        if (this.mShowSeconds) {
            if (this.mSecondsHandler == null && getDisplay() != null) {
                this.mSecondsHandler = new Handler();
                if (getDisplay().getState() == 2) {
                    this.mSecondsHandler.postAtTime(this.mSecondTick, ((SystemClock.uptimeMillis() / 1000) * 1000) + 1000);
                }
                IntentFilter intentFilter = new IntentFilter("android.intent.action.SCREEN_OFF");
                intentFilter.addAction("android.intent.action.SCREEN_ON");
                this.mContext.registerReceiver(this.mScreenReceiver, intentFilter);
                return;
            }
            return;
        }
        if (this.mSecondsHandler != null) {
            this.mContext.unregisterReceiver(this.mScreenReceiver);
            this.mSecondsHandler.removeCallbacks(this.mSecondTick);
            this.mSecondsHandler = null;
            updateClock();
        }
    }

    private final CharSequence getSmallTime() {
        String str;
        SimpleDateFormat simpleDateFormat;
        Context context = getContext();
        boolean zIs24HourFormat = DateFormat.is24HourFormat(context, this.mCurrentUserId);
        LocaleData localeData = LocaleData.get(context.getResources().getConfiguration().locale);
        if (this.mShowSeconds) {
            str = zIs24HourFormat ? localeData.timeFormat_Hms : localeData.timeFormat_hms;
        } else {
            str = zIs24HourFormat ? localeData.timeFormat_Hm : localeData.timeFormat_hm;
        }
        if (BenesseExtension.getDchaState() != 0) {
            str = "M月d日aaKK:mm";
        }
        if (!str.equals(this.mClockFormatString)) {
            this.mContentDescriptionFormat = new SimpleDateFormat(str);
            if (this.mAmPmStyle != 0) {
                int i = 0;
                boolean z = false;
                while (true) {
                    if (i < str.length()) {
                        char cCharAt = str.charAt(i);
                        if (cCharAt == '\'') {
                            z = !z;
                        }
                        if (!z && cCharAt == 'a') {
                            break;
                        }
                        i++;
                    } else {
                        i = -1;
                        break;
                    }
                }
                if (i >= 0) {
                    int i2 = i;
                    while (i2 > 0 && Character.isWhitespace(str.charAt(i2 - 1))) {
                        i2--;
                    }
                    str = str.substring(0, i2) + (char) 61184 + str.substring(i2, i) + "a\uef01" + str.substring(i + 1);
                }
            }
            simpleDateFormat = new SimpleDateFormat(str);
            this.mClockFormat = simpleDateFormat;
            this.mClockFormatString = str;
        } else {
            simpleDateFormat = this.mClockFormat;
        }
        String str2 = simpleDateFormat.format(this.mCalendar.getTime());
        if (this.mAmPmStyle != 0) {
            int iIndexOf = str2.indexOf(61184);
            int iIndexOf2 = str2.indexOf(61185);
            if (iIndexOf >= 0 && iIndexOf2 > iIndexOf) {
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(str2);
                if (this.mAmPmStyle == 2) {
                    spannableStringBuilder.delete(iIndexOf, iIndexOf2 + 1);
                } else {
                    if (this.mAmPmStyle == 1) {
                        spannableStringBuilder.setSpan(new RelativeSizeSpan(0.7f), iIndexOf, iIndexOf2, 34);
                    }
                    spannableStringBuilder.delete(iIndexOf2, iIndexOf2 + 1);
                    spannableStringBuilder.delete(iIndexOf, iIndexOf + 1);
                }
                return spannableStringBuilder;
            }
        }
        return str2;
    }

    @Override
    public void dispatchDemoCommand(String str, Bundle bundle) {
        if (!this.mDemoMode && str.equals("enter")) {
            this.mDemoMode = true;
            return;
        }
        if (this.mDemoMode && str.equals("exit")) {
            this.mDemoMode = false;
            updateClock();
            return;
        }
        if (this.mDemoMode && str.equals("clock")) {
            String string = bundle.getString("millis");
            String string2 = bundle.getString("hhmm");
            if (string != null) {
                this.mCalendar.setTimeInMillis(Long.parseLong(string));
            } else if (string2 != null && string2.length() == 4) {
                int i = Integer.parseInt(string2.substring(0, 2));
                int i2 = Integer.parseInt(string2.substring(2));
                if (DateFormat.is24HourFormat(getContext(), this.mCurrentUserId)) {
                    this.mCalendar.set(11, i);
                } else {
                    this.mCalendar.set(10, i);
                }
                this.mCalendar.set(12, i2);
            }
            setText(getSmallTime());
            setContentDescription(this.mContentDescriptionFormat.format(this.mCalendar.getTime()));
        }
    }
}
