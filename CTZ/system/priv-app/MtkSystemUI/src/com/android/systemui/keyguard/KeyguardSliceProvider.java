package com.android.systemui.keyguard;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.icu.text.DateFormat;
import android.icu.text.DisplayContext;
import android.net.Uri;
import android.os.Handler;
import android.os.Trace;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.NextAlarmControllerImpl;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.statusbar.policy.ZenModeControllerImpl;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/* loaded from: classes.dex */
public class KeyguardSliceProvider extends SliceProvider implements NextAlarmController.NextAlarmChangeCallback, ZenModeController.Callback {

    @VisibleForTesting
    static final int ALARM_VISIBILITY_HOURS = 12;
    private static KeyguardSliceProvider sInstance;
    protected AlarmManager mAlarmManager;
    protected final Uri mAlarmUri;
    protected ContentResolver mContentResolver;
    private final Date mCurrentTime;
    private DateFormat mDateFormat;
    private String mDatePattern;
    protected final Uri mDateUri;
    protected final Uri mDndUri;
    private final Handler mHandler;

    @VisibleForTesting
    final BroadcastReceiver mIntentReceiver;
    private String mLastText;
    private String mNextAlarm;
    private NextAlarmController mNextAlarmController;
    private AlarmManager.AlarmClockInfo mNextAlarmInfo;
    private PendingIntent mPendingIntent;
    private boolean mRegistered;
    protected final Uri mSliceUri;
    private final AlarmManager.OnAlarmListener mUpdateNextAlarm;
    private ZenModeController mZenModeController;

    public KeyguardSliceProvider() {
        this(new Handler());
    }

    public static KeyguardSliceProvider getAttachedInstance() {
        return sInstance;
    }

    @VisibleForTesting
    KeyguardSliceProvider(Handler handler) {
        this.mCurrentTime = new Date();
        this.mUpdateNextAlarm = new AlarmManager.OnAlarmListener() { // from class: com.android.systemui.keyguard.-$$Lambda$KeyguardSliceProvider$IhzByd8TsqFuOrSyuGurVskyPLo
            @Override // android.app.AlarmManager.OnAlarmListener
            public final void onAlarm() {
                this.f$0.updateNextAlarm();
            }
        };
        this.mIntentReceiver = new BroadcastReceiver() { // from class: com.android.systemui.keyguard.KeyguardSliceProvider.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.TIME_TICK".equals(action) || "android.intent.action.DATE_CHANGED".equals(action) || "android.intent.action.TIME_SET".equals(action) || "android.intent.action.TIMEZONE_CHANGED".equals(action) || "android.intent.action.LOCALE_CHANGED".equals(action)) {
                    if ("android.intent.action.LOCALE_CHANGED".equals(action) || "android.intent.action.TIMEZONE_CHANGED".equals(action)) {
                        Handler handler2 = KeyguardSliceProvider.this.mHandler;
                        final KeyguardSliceProvider keyguardSliceProvider = KeyguardSliceProvider.this;
                        handler2.post(new Runnable() { // from class: com.android.systemui.keyguard.-$$Lambda$UyKQdSvY5FC_Xuudb07cmVSuhU4
                            @Override // java.lang.Runnable
                            public final void run() {
                                keyguardSliceProvider.cleanDateFormat();
                            }
                        });
                    }
                    Handler handler3 = KeyguardSliceProvider.this.mHandler;
                    final KeyguardSliceProvider keyguardSliceProvider2 = KeyguardSliceProvider.this;
                    handler3.post(new Runnable() { // from class: com.android.systemui.keyguard.-$$Lambda$iHhuB2gJvIMUcE0UtET88pejsrA
                        @Override // java.lang.Runnable
                        public final void run() {
                            keyguardSliceProvider2.updateClock();
                        }
                    });
                }
            }
        };
        this.mHandler = handler;
        this.mSliceUri = Uri.parse("content://com.android.systemui.keyguard/main");
        this.mDateUri = Uri.parse("content://com.android.systemui.keyguard/date");
        this.mAlarmUri = Uri.parse("content://com.android.systemui.keyguard/alarm");
        this.mDndUri = Uri.parse("content://com.android.systemui.keyguard/dnd");
    }

    @Override // androidx.slice.SliceProvider
    public Slice onBindSlice(Uri uri) {
        Trace.beginSection("KeyguardSliceProvider#onBindSlice");
        ListBuilder listBuilder = new ListBuilder(getContext(), this.mSliceUri);
        listBuilder.addRow(new ListBuilder.RowBuilder(listBuilder, this.mDateUri).setTitle(this.mLastText));
        addNextAlarm(listBuilder);
        addZenMode(listBuilder);
        addPrimaryAction(listBuilder);
        Slice sliceBuild = listBuilder.build();
        Trace.endSection();
        return sliceBuild;
    }

    protected void addPrimaryAction(ListBuilder listBuilder) {
        listBuilder.addRow(new ListBuilder.RowBuilder(listBuilder, Uri.parse("content://com.android.systemui.keyguard/action")).setPrimaryAction(new SliceAction(this.mPendingIntent, Icon.createWithResource(getContext(), R.drawable.ic_access_alarms_big), this.mLastText)));
    }

    protected void addNextAlarm(ListBuilder listBuilder) {
        if (TextUtils.isEmpty(this.mNextAlarm)) {
            return;
        }
        listBuilder.addRow(new ListBuilder.RowBuilder(listBuilder, this.mAlarmUri).setTitle(this.mNextAlarm).addEndItem(Icon.createWithResource(getContext(), R.drawable.ic_access_alarms_big)));
    }

    protected void addZenMode(ListBuilder listBuilder) {
        if (!isDndSuppressingNotifications()) {
            return;
        }
        listBuilder.addRow(new ListBuilder.RowBuilder(listBuilder, this.mDndUri).setContentDescription(getContext().getResources().getString(R.string.accessibility_quick_settings_dnd)).addEndItem(Icon.createWithResource(getContext(), R.drawable.stat_sys_dnd)));
    }

    protected boolean isDndSuppressingNotifications() {
        return this.mZenModeController.getZen() != 0 && ((this.mZenModeController.getConfig().suppressedVisualEffects & 256) != 0);
    }

    @Override // androidx.slice.SliceProvider
    public boolean onCreateSliceProvider() {
        this.mAlarmManager = (AlarmManager) getContext().getSystemService(AlarmManager.class);
        this.mContentResolver = getContext().getContentResolver();
        this.mNextAlarmController = new NextAlarmControllerImpl(getContext());
        this.mNextAlarmController.addCallback(this);
        this.mZenModeController = new ZenModeControllerImpl(getContext(), this.mHandler);
        this.mZenModeController.addCallback(this);
        this.mDatePattern = getContext().getString(R.string.system_ui_aod_date_pattern);
        this.mPendingIntent = PendingIntent.getActivity(getContext(), 0, new Intent(getContext(), (Class<?>) KeyguardSliceProvider.class), 0);
        sInstance = this;
        registerClockUpdate();
        updateClock();
        return true;
    }

    @Override // com.android.systemui.statusbar.policy.ZenModeController.Callback
    public void onZenChanged(int i) {
        this.mContentResolver.notifyChange(this.mSliceUri, null);
    }

    @Override // com.android.systemui.statusbar.policy.ZenModeController.Callback
    public void onConfigChanged(ZenModeConfig zenModeConfig) {
        this.mContentResolver.notifyChange(this.mSliceUri, null);
    }

    private void updateNextAlarm() {
        if (withinNHours(this.mNextAlarmInfo, 12)) {
            this.mNextAlarm = android.text.format.DateFormat.format(android.text.format.DateFormat.is24HourFormat(getContext(), ActivityManager.getCurrentUser()) ? "H:mm" : "h:mm", this.mNextAlarmInfo.getTriggerTime()).toString();
        } else {
            this.mNextAlarm = "";
        }
        this.mContentResolver.notifyChange(this.mSliceUri, null);
    }

    private boolean withinNHours(AlarmManager.AlarmClockInfo alarmClockInfo, int i) {
        if (alarmClockInfo == null) {
            return false;
        }
        return this.mNextAlarmInfo.getTriggerTime() <= System.currentTimeMillis() + TimeUnit.HOURS.toMillis((long) i);
    }

    private void registerClockUpdate() {
        if (this.mRegistered) {
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.DATE_CHANGED");
        intentFilter.addAction("android.intent.action.TIME_SET");
        intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        intentFilter.addAction("android.intent.action.LOCALE_CHANGED");
        getContext().registerReceiver(this.mIntentReceiver, intentFilter, null, null);
        this.mRegistered = true;
    }

    @VisibleForTesting
    boolean isRegistered() {
        return this.mRegistered;
    }

    protected void updateClock() {
        String formattedDate = getFormattedDate();
        if (!formattedDate.equals(this.mLastText)) {
            this.mLastText = formattedDate;
            this.mContentResolver.notifyChange(this.mSliceUri, null);
        }
    }

    protected String getFormattedDate() {
        if (this.mDateFormat == null) {
            DateFormat instanceForSkeleton = DateFormat.getInstanceForSkeleton(this.mDatePattern, Locale.getDefault());
            instanceForSkeleton.setContext(DisplayContext.CAPITALIZATION_FOR_STANDALONE);
            this.mDateFormat = instanceForSkeleton;
        }
        this.mCurrentTime.setTime(System.currentTimeMillis());
        return this.mDateFormat.format(this.mCurrentTime);
    }

    @VisibleForTesting
    void cleanDateFormat() {
        this.mDateFormat = null;
    }

    @Override // com.android.systemui.statusbar.policy.NextAlarmController.NextAlarmChangeCallback
    public void onNextAlarmChanged(AlarmManager.AlarmClockInfo alarmClockInfo) {
        this.mNextAlarmInfo = alarmClockInfo;
        this.mAlarmManager.cancel(this.mUpdateNextAlarm);
        long triggerTime = this.mNextAlarmInfo == null ? -1L : this.mNextAlarmInfo.getTriggerTime() - TimeUnit.HOURS.toMillis(12L);
        if (triggerTime > 0) {
            this.mAlarmManager.setExact(1, triggerTime, "lock_screen_next_alarm", this.mUpdateNextAlarm, this.mHandler);
        }
        updateNextAlarm();
    }
}
