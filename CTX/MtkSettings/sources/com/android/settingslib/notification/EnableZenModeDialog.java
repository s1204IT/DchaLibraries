package com.android.settingslib.notification;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.service.notification.Condition;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.policy.PhoneWindow;
import com.android.settingslib.R;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;

public class EnableZenModeDialog {

    @VisibleForTesting
    protected static final int COUNTDOWN_ALARM_CONDITION_INDEX = 2;

    @VisibleForTesting
    protected static final int COUNTDOWN_CONDITION_INDEX = 1;

    @VisibleForTesting
    protected static final int FOREVER_CONDITION_INDEX = 0;
    private AlarmManager mAlarmManager;
    private boolean mAttached;

    @VisibleForTesting
    protected Context mContext;

    @VisibleForTesting
    protected Uri mForeverId;

    @VisibleForTesting
    protected LayoutInflater mLayoutInflater;

    @VisibleForTesting
    protected NotificationManager mNotificationManager;
    private int mUserId;

    @VisibleForTesting
    protected TextView mZenAlarmWarning;
    private RadioGroup mZenRadioGroup;

    @VisibleForTesting
    protected LinearLayout mZenRadioGroupContent;
    private static final boolean DEBUG = Log.isLoggable("EnableZenModeDialog", 3);
    private static final int[] MINUTE_BUCKETS = ZenModeConfig.MINUTE_BUCKETS;
    private static final int MIN_BUCKET_MINUTES = MINUTE_BUCKETS[0];
    private static final int MAX_BUCKET_MINUTES = MINUTE_BUCKETS[MINUTE_BUCKETS.length - 1];
    private static final int DEFAULT_BUCKET_INDEX = Arrays.binarySearch(MINUTE_BUCKETS, 60);
    private int mBucketIndex = -1;
    private int MAX_MANUAL_DND_OPTIONS = 3;

    public EnableZenModeDialog(Context context) {
        this.mContext = context;
    }

    public Dialog createDialog() {
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        this.mForeverId = Condition.newId(this.mContext).appendPath("forever").build();
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mUserId = this.mContext.getUserId();
        this.mAttached = false;
        AlertDialog.Builder positiveButton = new AlertDialog.Builder(this.mContext).setTitle(R.string.zen_mode_settings_turn_on_dialog_title).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.zen_mode_enable_dialog_turn_on, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ConditionTag conditionTagAt = EnableZenModeDialog.this.getConditionTagAt(EnableZenModeDialog.this.mZenRadioGroup.getCheckedRadioButtonId());
                if (EnableZenModeDialog.this.isForever(conditionTagAt.condition)) {
                    MetricsLogger.action(EnableZenModeDialog.this.mContext, 1259);
                } else if (EnableZenModeDialog.this.isAlarm(conditionTagAt.condition)) {
                    MetricsLogger.action(EnableZenModeDialog.this.mContext, 1261);
                } else if (EnableZenModeDialog.this.isCountdown(conditionTagAt.condition)) {
                    MetricsLogger.action(EnableZenModeDialog.this.mContext, 1260);
                } else {
                    Slog.d("EnableZenModeDialog", "Invalid manual condition: " + conditionTagAt.condition);
                }
                EnableZenModeDialog.this.mNotificationManager.setZenMode(1, EnableZenModeDialog.this.getRealConditionId(conditionTagAt.condition), "EnableZenModeDialog");
            }
        });
        View contentView = getContentView();
        bindConditions(forever());
        positiveButton.setView(contentView);
        return positiveButton.create();
    }

    private void hideAllConditions() {
        int childCount = this.mZenRadioGroupContent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            this.mZenRadioGroupContent.getChildAt(i).setVisibility(8);
        }
        this.mZenAlarmWarning.setVisibility(8);
    }

    protected View getContentView() {
        if (this.mLayoutInflater == null) {
            this.mLayoutInflater = new PhoneWindow(this.mContext).getLayoutInflater();
        }
        View viewInflate = this.mLayoutInflater.inflate(R.layout.zen_mode_turn_on_dialog_container, (ViewGroup) null);
        ScrollView scrollView = (ScrollView) viewInflate.findViewById(R.id.container);
        this.mZenRadioGroup = (RadioGroup) scrollView.findViewById(R.id.zen_radio_buttons);
        this.mZenRadioGroupContent = (LinearLayout) scrollView.findViewById(R.id.zen_radio_buttons_content);
        this.mZenAlarmWarning = (TextView) scrollView.findViewById(R.id.zen_alarm_warning);
        for (int i = 0; i < this.MAX_MANUAL_DND_OPTIONS; i++) {
            View viewInflate2 = this.mLayoutInflater.inflate(R.layout.zen_mode_radio_button, (ViewGroup) this.mZenRadioGroup, false);
            this.mZenRadioGroup.addView(viewInflate2);
            viewInflate2.setId(i);
            View viewInflate3 = this.mLayoutInflater.inflate(R.layout.zen_mode_condition, (ViewGroup) this.mZenRadioGroupContent, false);
            viewInflate3.setId(this.MAX_MANUAL_DND_OPTIONS + i);
            this.mZenRadioGroupContent.addView(viewInflate3);
        }
        hideAllConditions();
        return viewInflate;
    }

    @VisibleForTesting
    protected void bind(Condition condition, View view, int i) {
        if (condition == null) {
            throw new IllegalArgumentException("condition must not be null");
        }
        boolean z = condition.state == 1;
        final ConditionTag conditionTag = view.getTag() != null ? (ConditionTag) view.getTag() : new ConditionTag();
        view.setTag(conditionTag);
        boolean z2 = conditionTag.rb == null;
        if (conditionTag.rb == null) {
            conditionTag.rb = (RadioButton) this.mZenRadioGroup.getChildAt(i);
        }
        conditionTag.condition = condition;
        final Uri conditionId = getConditionId(conditionTag.condition);
        if (DEBUG) {
            Log.d("EnableZenModeDialog", "bind i=" + this.mZenRadioGroupContent.indexOfChild(view) + " first=" + z2 + " condition=" + conditionId);
        }
        conditionTag.rb.setEnabled(z);
        conditionTag.rb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean z3) {
                if (z3) {
                    conditionTag.rb.setChecked(true);
                    if (EnableZenModeDialog.DEBUG) {
                        Log.d("EnableZenModeDialog", "onCheckedChanged " + conditionId);
                    }
                    MetricsLogger.action(EnableZenModeDialog.this.mContext, 164);
                    EnableZenModeDialog.this.updateAlarmWarningText(conditionTag.condition);
                }
            }
        });
        updateUi(conditionTag, view, condition, z, i, conditionId);
        view.setVisibility(0);
    }

    @VisibleForTesting
    protected ConditionTag getConditionTagAt(int i) {
        return (ConditionTag) this.mZenRadioGroupContent.getChildAt(i).getTag();
    }

    @VisibleForTesting
    protected void bindConditions(Condition condition) {
        bind(forever(), this.mZenRadioGroupContent.getChildAt(0), 0);
        if (condition == null) {
            bindGenericCountdown();
            bindNextAlarm(getTimeUntilNextAlarmCondition());
            return;
        }
        if (isForever(condition)) {
            getConditionTagAt(0).rb.setChecked(true);
            bindGenericCountdown();
            bindNextAlarm(getTimeUntilNextAlarmCondition());
        } else if (isAlarm(condition)) {
            bindGenericCountdown();
            bindNextAlarm(condition);
            getConditionTagAt(2).rb.setChecked(true);
        } else if (isCountdown(condition)) {
            bindNextAlarm(getTimeUntilNextAlarmCondition());
            bind(condition, this.mZenRadioGroupContent.getChildAt(1), 1);
            getConditionTagAt(1).rb.setChecked(true);
        } else {
            Slog.d("EnableZenModeDialog", "Invalid manual condition: " + condition);
        }
    }

    public static Uri getConditionId(Condition condition) {
        if (condition != null) {
            return condition.id;
        }
        return null;
    }

    public Condition forever() {
        return new Condition(Condition.newId(this.mContext).appendPath("forever").build(), foreverSummary(this.mContext), "", "", 0, 1, 0);
    }

    public long getNextAlarm() {
        AlarmManager.AlarmClockInfo nextAlarmClock = this.mAlarmManager.getNextAlarmClock(this.mUserId);
        if (nextAlarmClock != null) {
            return nextAlarmClock.getTriggerTime();
        }
        return 0L;
    }

    @VisibleForTesting
    protected boolean isAlarm(Condition condition) {
        return condition != null && ZenModeConfig.isValidCountdownToAlarmConditionId(condition.id);
    }

    @VisibleForTesting
    protected boolean isCountdown(Condition condition) {
        return condition != null && ZenModeConfig.isValidCountdownConditionId(condition.id);
    }

    public boolean isForever(Condition condition) {
        return condition != null && this.mForeverId.equals(condition.id);
    }

    public Uri getRealConditionId(Condition condition) {
        if (isForever(condition)) {
            return null;
        }
        return getConditionId(condition);
    }

    private String foreverSummary(Context context) {
        return context.getString(android.R.string.oneMonthDurationPast);
    }

    private static void setToMidnight(Calendar calendar) {
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
    }

    @VisibleForTesting
    protected Condition getTimeUntilNextAlarmCondition() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        setToMidnight(gregorianCalendar);
        gregorianCalendar.add(5, 6);
        long nextAlarm = getNextAlarm();
        if (nextAlarm > 0) {
            GregorianCalendar gregorianCalendar2 = new GregorianCalendar();
            gregorianCalendar2.setTimeInMillis(nextAlarm);
            setToMidnight(gregorianCalendar2);
            if (gregorianCalendar.compareTo((Calendar) gregorianCalendar2) >= 0) {
                return ZenModeConfig.toNextAlarmCondition(this.mContext, nextAlarm, ActivityManager.getCurrentUser());
            }
            return null;
        }
        return null;
    }

    @VisibleForTesting
    protected void bindGenericCountdown() {
        this.mBucketIndex = DEFAULT_BUCKET_INDEX;
        Condition timeCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
        if (!this.mAttached || getConditionTagAt(1).condition == null) {
            bind(timeCondition, this.mZenRadioGroupContent.getChildAt(1), 1);
        }
    }

    private void updateUi(final ConditionTag conditionTag, final View view, Condition condition, boolean z, final int i, Uri uri) {
        if (conditionTag.lines == null) {
            conditionTag.lines = view.findViewById(android.R.id.content);
            conditionTag.lines.setAccessibilityLiveRegion(1);
        }
        if (conditionTag.line1 == null) {
            conditionTag.line1 = (TextView) view.findViewById(android.R.id.text1);
        }
        if (conditionTag.line2 == null) {
            conditionTag.line2 = (TextView) view.findViewById(android.R.id.text2);
        }
        String str = !TextUtils.isEmpty(condition.line1) ? condition.line1 : condition.summary;
        String str2 = condition.line2;
        conditionTag.line1.setText(str);
        if (TextUtils.isEmpty(str2)) {
            conditionTag.line2.setVisibility(8);
        } else {
            conditionTag.line2.setVisibility(0);
            conditionTag.line2.setText(str2);
        }
        conditionTag.lines.setEnabled(z);
        conditionTag.lines.setAlpha(z ? 1.0f : 0.4f);
        conditionTag.lines.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                conditionTag.rb.setChecked(true);
            }
        });
        ImageView imageView = (ImageView) view.findViewById(android.R.id.button1);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                EnableZenModeDialog.this.onClickTimeButton(view, conditionTag, false, i);
            }
        });
        ImageView imageView2 = (ImageView) view.findViewById(android.R.id.button2);
        imageView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                EnableZenModeDialog.this.onClickTimeButton(view, conditionTag, true, i);
            }
        });
        long jTryParseCountdownConditionId = ZenModeConfig.tryParseCountdownConditionId(uri);
        if (i == 1 && jTryParseCountdownConditionId > 0) {
            imageView.setVisibility(0);
            imageView2.setVisibility(0);
            if (this.mBucketIndex > -1) {
                imageView.setEnabled(this.mBucketIndex > 0);
                imageView2.setEnabled(this.mBucketIndex < MINUTE_BUCKETS.length - 1);
            } else {
                imageView.setEnabled(jTryParseCountdownConditionId - System.currentTimeMillis() > ((long) (MIN_BUCKET_MINUTES * 60000)));
                imageView2.setEnabled(!Objects.equals(condition.summary, ZenModeConfig.toTimeCondition(this.mContext, MAX_BUCKET_MINUTES, ActivityManager.getCurrentUser()).summary));
            }
            imageView.setAlpha(imageView.isEnabled() ? 1.0f : 0.5f);
            imageView2.setAlpha(imageView2.isEnabled() ? 1.0f : 0.5f);
            return;
        }
        imageView.setVisibility(8);
        imageView2.setVisibility(8);
    }

    @VisibleForTesting
    protected void bindNextAlarm(Condition condition) {
        View childAt = this.mZenRadioGroupContent.getChildAt(2);
        ConditionTag conditionTag = (ConditionTag) childAt.getTag();
        if (condition != null && (!this.mAttached || conditionTag == null || conditionTag.condition == null)) {
            bind(condition, childAt, 2);
        }
        ConditionTag conditionTag2 = (ConditionTag) childAt.getTag();
        int i = 0;
        boolean z = (conditionTag2 == null || conditionTag2.condition == null) ? false : true;
        this.mZenRadioGroup.getChildAt(2).setVisibility(z ? 0 : 8);
        if (!z) {
            i = 8;
        }
        childAt.setVisibility(i);
    }

    public void onClickTimeButton(View view, ConditionTag conditionTag, boolean z, int i) {
        Condition timeCondition;
        int i2;
        MetricsLogger.action(this.mContext, 163, z);
        int length = MINUTE_BUCKETS.length;
        if (this.mBucketIndex == -1) {
            long jTryParseCountdownConditionId = ZenModeConfig.tryParseCountdownConditionId(getConditionId(conditionTag.condition));
            long jCurrentTimeMillis = System.currentTimeMillis();
            for (int i3 = 0; i3 < length; i3++) {
                if (!z) {
                    i2 = (length - 1) - i3;
                } else {
                    i2 = i3;
                }
                int i4 = MINUTE_BUCKETS[i2];
                long j = jCurrentTimeMillis + ((long) (60000 * i4));
                if ((z && j > jTryParseCountdownConditionId) || (!z && j < jTryParseCountdownConditionId)) {
                    this.mBucketIndex = i2;
                    timeCondition = ZenModeConfig.toTimeCondition(this.mContext, j, i4, ActivityManager.getCurrentUser(), false);
                    break;
                }
            }
            timeCondition = null;
            if (timeCondition == null) {
                this.mBucketIndex = DEFAULT_BUCKET_INDEX;
                timeCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
            }
        } else {
            this.mBucketIndex = Math.max(0, Math.min(length - 1, this.mBucketIndex + (z ? 1 : -1)));
            timeCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
        }
        bind(timeCondition, view, i);
        updateAlarmWarningText(conditionTag.condition);
        conditionTag.rb.setChecked(true);
    }

    public void updateAlarmWarningText(Condition condition) {
        String strComputeAlarmWarningText = computeAlarmWarningText(condition);
        this.mZenAlarmWarning.setText(strComputeAlarmWarningText);
        this.mZenAlarmWarning.setVisibility(strComputeAlarmWarningText == null ? 8 : 0);
    }

    @VisibleForTesting
    protected String computeAlarmWarningText(Condition condition) {
        int i;
        if ((this.mNotificationManager.getNotificationPolicy().priorityCategories & 32) != 0) {
            return null;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        long nextAlarm = getNextAlarm();
        if (nextAlarm < jCurrentTimeMillis) {
            return null;
        }
        if (condition == null || isForever(condition)) {
            i = R.string.zen_alarm_warning_indef;
        } else {
            long jTryParseCountdownConditionId = ZenModeConfig.tryParseCountdownConditionId(condition.id);
            if (jTryParseCountdownConditionId > jCurrentTimeMillis && nextAlarm < jTryParseCountdownConditionId) {
                i = R.string.zen_alarm_warning;
            } else {
                i = 0;
            }
        }
        if (i == 0) {
            return null;
        }
        return this.mContext.getResources().getString(i, getTime(nextAlarm, jCurrentTimeMillis));
    }

    @VisibleForTesting
    protected String getTime(long j, long j2) {
        boolean z = j - j2 < 86400000;
        boolean zIs24HourFormat = DateFormat.is24HourFormat(this.mContext, ActivityManager.getCurrentUser());
        return this.mContext.getResources().getString(z ? R.string.alarm_template : R.string.alarm_template_far, DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), z ? zIs24HourFormat ? "Hm" : "hma" : zIs24HourFormat ? "EEEHm" : "EEEhma"), j));
    }

    @VisibleForTesting
    protected static class ConditionTag {
        public Condition condition;
        public TextView line1;
        public TextView line2;
        public View lines;
        public RadioButton rb;

        protected ConditionTag() {
        }
    }
}
