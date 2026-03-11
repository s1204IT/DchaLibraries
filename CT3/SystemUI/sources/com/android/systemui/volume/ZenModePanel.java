package com.android.systemui.volume;

import android.animation.LayoutTransition;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.service.notification.Condition;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MathUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.volume.Interaction;
import com.android.systemui.volume.SegmentedButtons;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;

public class ZenModePanel extends LinearLayout {
    private boolean mAttached;
    private int mAttachedZen;
    private int mBucketIndex;
    private Callback mCallback;
    private Condition[] mConditions;
    private final Context mContext;
    private ZenModeController mController;
    private boolean mCountdownConditionSupported;
    private Condition mExitCondition;
    private boolean mExpanded;
    private final Uri mForeverId;
    private final H mHandler;
    private boolean mHidden;
    protected final LayoutInflater mInflater;
    private final Interaction.Callback mInteractionCallback;
    private final ZenPrefs mPrefs;
    private boolean mRequestingConditions;
    private Condition mSessionExitCondition;
    private int mSessionZen;
    private final SpTexts mSpTexts;
    private String mTag;
    private Condition mTimeCondition;
    private final TransitionHelper mTransitionHelper;
    private boolean mVoiceCapable;
    private TextView mZenAlarmWarning;
    protected SegmentedButtons mZenButtons;
    protected final SegmentedButtons.Callback mZenButtonsCallback;
    private final ZenModeController.Callback mZenCallback;
    protected LinearLayout mZenConditions;
    private View mZenIntroduction;
    private View mZenIntroductionConfirm;
    private TextView mZenIntroductionCustomize;
    private TextView mZenIntroductionMessage;
    private RadioGroup mZenRadioGroup;
    private LinearLayout mZenRadioGroupContent;
    private static final boolean DEBUG = Log.isLoggable("ZenModePanel", 3);
    private static final int[] MINUTE_BUCKETS = ZenModeConfig.MINUTE_BUCKETS;
    private static final int MIN_BUCKET_MINUTES = MINUTE_BUCKETS[0];
    private static final int MAX_BUCKET_MINUTES = MINUTE_BUCKETS[MINUTE_BUCKETS.length - 1];
    private static final int DEFAULT_BUCKET_INDEX = Arrays.binarySearch(MINUTE_BUCKETS, 60);
    public static final Intent ZEN_SETTINGS = new Intent("android.settings.ZEN_MODE_SETTINGS");
    public static final Intent ZEN_PRIORITY_SETTINGS = new Intent("android.settings.ZEN_MODE_PRIORITY_SETTINGS");

    public interface Callback {
        void onExpanded(boolean z);

        void onInteraction();

        void onPrioritySettings();
    }

    public ZenModePanel(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mHandler = new H(this, null);
        this.mTransitionHelper = new TransitionHelper(this, 0 == true ? 1 : 0);
        this.mTag = "ZenModePanel/" + Integer.toHexString(System.identityHashCode(this));
        this.mBucketIndex = -1;
        this.mZenCallback = new ZenModeController.Callback() {
            @Override
            public void onManualRuleChanged(ZenModeConfig.ZenRule rule) {
                ZenModePanel.this.mHandler.obtainMessage(2, rule).sendToTarget();
            }
        };
        this.mZenButtonsCallback = new SegmentedButtons.Callback() {
            @Override
            public void onSelected(Object value, boolean fromClick) {
                if (value == null || !ZenModePanel.this.mZenButtons.isShown() || !ZenModePanel.this.isAttachedToWindow()) {
                    return;
                }
                final int zen = ((Integer) value).intValue();
                if (fromClick) {
                    MetricsLogger.action(ZenModePanel.this.mContext, 165, zen);
                }
                if (ZenModePanel.DEBUG) {
                    Log.d(ZenModePanel.this.mTag, "mZenButtonsCallback selected=" + zen);
                }
                final Uri realConditionId = ZenModePanel.this.getRealConditionId(ZenModePanel.this.mSessionExitCondition);
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        ZenModePanel.this.mController.setZen(zen, realConditionId, "ZenModePanel.selectZen");
                        if (zen == 0) {
                            return;
                        }
                        Prefs.putInt(ZenModePanel.this.mContext, "DndFavoriteZen", zen);
                    }
                });
            }

            @Override
            public void onInteraction() {
                ZenModePanel.this.fireInteraction();
            }
        };
        this.mInteractionCallback = new Interaction.Callback() {
            @Override
            public void onInteraction() {
                ZenModePanel.this.fireInteraction();
            }
        };
        this.mContext = context;
        this.mPrefs = new ZenPrefs(this, 0 == true ? 1 : 0);
        this.mInflater = LayoutInflater.from(this.mContext.getApplicationContext());
        this.mForeverId = Condition.newId(this.mContext).appendPath("forever").build();
        this.mSpTexts = new SpTexts(this.mContext);
        this.mVoiceCapable = Util.isVoiceCapable(this.mContext);
        if (DEBUG) {
            Log.d(this.mTag, "new ZenModePanel");
        }
    }

    protected void createZenButtons() {
        this.mZenButtons = (SegmentedButtons) findViewById(R.id.zen_buttons);
        this.mZenButtons.addButton(R.string.interruption_level_none_twoline, R.string.interruption_level_none_with_warning, 2);
        this.mZenButtons.addButton(R.string.interruption_level_alarms_twoline, R.string.interruption_level_alarms, 3);
        this.mZenButtons.addButton(R.string.interruption_level_priority_twoline, R.string.interruption_level_priority, 1);
        this.mZenButtons.setCallback(this.mZenButtonsCallback);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        createZenButtons();
        this.mZenIntroduction = findViewById(R.id.zen_introduction);
        this.mZenIntroductionMessage = (TextView) findViewById(R.id.zen_introduction_message);
        this.mSpTexts.add(this.mZenIntroductionMessage);
        this.mZenIntroductionConfirm = findViewById(R.id.zen_introduction_confirm);
        this.mZenIntroductionConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZenModePanel.this.confirmZenIntroduction();
            }
        });
        this.mZenIntroductionCustomize = (TextView) findViewById(R.id.zen_introduction_customize);
        this.mZenIntroductionCustomize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZenModePanel.this.confirmZenIntroduction();
                if (ZenModePanel.this.mCallback == null) {
                    return;
                }
                ZenModePanel.this.mCallback.onPrioritySettings();
            }
        });
        this.mSpTexts.add(this.mZenIntroductionCustomize);
        this.mZenConditions = (LinearLayout) findViewById(R.id.zen_conditions);
        this.mZenAlarmWarning = (TextView) findViewById(R.id.zen_alarm_warning);
        this.mZenRadioGroup = (RadioGroup) findViewById(R.id.zen_radio_buttons);
        this.mZenRadioGroupContent = (LinearLayout) findViewById(R.id.zen_radio_buttons_content);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mZenButtons == null) {
            return;
        }
        this.mZenButtons.updateLocale();
    }

    public void confirmZenIntroduction() {
        String prefKey = prefKeyForConfirmation(getSelectedZen(0));
        if (prefKey == null) {
            return;
        }
        if (DEBUG) {
            Log.d("ZenModePanel", "confirmZenIntroduction " + prefKey);
        }
        Prefs.putBoolean(this.mContext, prefKey, true);
        this.mHandler.sendEmptyMessage(3);
    }

    private static String prefKeyForConfirmation(int zen) {
        switch (zen) {
            case 1:
                return "DndConfirmedPriorityIntroduction";
            case 2:
                return "DndConfirmedSilenceIntroduction";
            default:
                return null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DEBUG) {
            Log.d(this.mTag, "onAttachedToWindow");
        }
        this.mAttached = true;
        this.mAttachedZen = getSelectedZen(-1);
        this.mSessionZen = this.mAttachedZen;
        this.mTransitionHelper.clear();
        this.mController.addCallback(this.mZenCallback);
        setSessionExitCondition(copy(this.mExitCondition));
        updateWidgets();
        setRequestingConditions(this.mHidden ? false : true);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (DEBUG) {
            Log.d(this.mTag, "onDetachedFromWindow");
        }
        checkForAttachedZenChange();
        this.mAttached = false;
        this.mAttachedZen = -1;
        this.mSessionZen = -1;
        this.mController.removeCallback(this.mZenCallback);
        setSessionExitCondition(null);
        setRequestingConditions(false);
        this.mTransitionHelper.clear();
    }

    private void setSessionExitCondition(Condition condition) {
        if (Objects.equals(condition, this.mSessionExitCondition)) {
            return;
        }
        if (DEBUG) {
            Log.d(this.mTag, "mSessionExitCondition=" + getConditionId(condition));
        }
        this.mSessionExitCondition = condition;
    }

    private void checkForAttachedZenChange() {
        int selectedZen = getSelectedZen(-1);
        if (DEBUG) {
            Log.d(this.mTag, "selectedZen=" + selectedZen);
        }
        if (selectedZen == this.mAttachedZen) {
            return;
        }
        if (DEBUG) {
            Log.d(this.mTag, "attachedZen: " + this.mAttachedZen + " -> " + selectedZen);
        }
        if (selectedZen != 2) {
            return;
        }
        this.mPrefs.trackNoneSelected();
    }

    private void setExpanded(boolean expanded) {
        if (expanded == this.mExpanded) {
            return;
        }
        if (DEBUG) {
            Log.d(this.mTag, "setExpanded " + expanded);
        }
        this.mExpanded = expanded;
        if (this.mExpanded && isShown()) {
            ensureSelection();
        }
        updateWidgets();
        fireExpanded();
    }

    private void setRequestingConditions(boolean requesting) {
        if (this.mRequestingConditions == requesting) {
            return;
        }
        if (DEBUG) {
            Log.d(this.mTag, "setRequestingConditions " + requesting);
        }
        this.mRequestingConditions = requesting;
        if (this.mRequestingConditions) {
            this.mTimeCondition = parseExistingTimeCondition(this.mContext, this.mExitCondition);
            if (this.mTimeCondition != null) {
                this.mBucketIndex = -1;
            } else {
                this.mBucketIndex = DEFAULT_BUCKET_INDEX;
                this.mTimeCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
            }
            if (DEBUG) {
                Log.d(this.mTag, "Initial bucket index: " + this.mBucketIndex);
            }
            this.mConditions = null;
            handleUpdateConditions();
            return;
        }
        hideAllConditions();
    }

    protected void addZenConditions(int count) {
        for (int i = 0; i < count; i++) {
            View rb = this.mInflater.inflate(R.layout.zen_mode_button, (ViewGroup) this, false);
            rb.setId(i);
            this.mZenRadioGroup.addView(rb);
            View rbc = this.mInflater.inflate(R.layout.zen_mode_condition, (ViewGroup) this, false);
            rbc.setId(i + count);
            this.mZenRadioGroupContent.addView(rbc);
        }
    }

    public void init(ZenModeController controller) {
        this.mController = controller;
        this.mCountdownConditionSupported = this.mController.isCountdownConditionSupported();
        int countdownDelta = this.mCountdownConditionSupported ? 2 : 0;
        int minConditions = countdownDelta + 1;
        addZenConditions(minConditions);
        this.mSessionZen = getSelectedZen(-1);
        handleUpdateManualRule(this.mController.getManualRule());
        if (DEBUG) {
            Log.d(this.mTag, "init mExitCondition=" + this.mExitCondition);
        }
        hideAllConditions();
    }

    private void setExitCondition(Condition exitCondition) {
        if (Objects.equals(this.mExitCondition, exitCondition)) {
            return;
        }
        this.mExitCondition = exitCondition;
        if (DEBUG) {
            Log.d(this.mTag, "mExitCondition=" + getConditionId(this.mExitCondition));
        }
        updateWidgets();
    }

    private static Uri getConditionId(Condition condition) {
        if (condition != null) {
            return condition.id;
        }
        return null;
    }

    public Uri getRealConditionId(Condition condition) {
        if (isForever(condition)) {
            return null;
        }
        return getConditionId(condition);
    }

    private static boolean sameConditionId(Condition lhs, Condition rhs) {
        if (lhs == null) {
            return rhs == null;
        }
        if (rhs != null) {
            return lhs.id.equals(rhs.id);
        }
        return false;
    }

    private static Condition copy(Condition condition) {
        if (condition == null) {
            return null;
        }
        return condition.copy();
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public void handleUpdateManualRule(ZenModeConfig.ZenRule rule) {
        int zen = rule != null ? rule.zenMode : 0;
        handleUpdateZen(zen);
        Condition c = rule != null ? rule.condition : null;
        handleExitConditionChanged(c);
    }

    private void handleUpdateZen(int zen) {
        if (this.mSessionZen != -1 && this.mSessionZen != zen) {
            setExpanded(isShown());
            this.mSessionZen = zen;
        }
        this.mZenButtons.setSelectedValue(Integer.valueOf(zen), false);
        updateWidgets();
        handleUpdateConditions();
        if (!this.mExpanded) {
            return;
        }
        Condition selected = getSelectedCondition();
        if (Objects.equals(this.mExitCondition, selected)) {
            return;
        }
        select(selected);
    }

    private void handleExitConditionChanged(Condition exitCondition) {
        setExitCondition(exitCondition);
        if (DEBUG) {
            Log.d(this.mTag, "handleExitConditionChanged " + this.mExitCondition);
        }
        int N = getVisibleConditions();
        for (int i = 0; i < N; i++) {
            ConditionTag tag = getConditionTagAt(i);
            if (tag != null && sameConditionId(tag.condition, this.mExitCondition)) {
                bind(exitCondition, this.mZenRadioGroupContent.getChildAt(i), i);
            }
        }
    }

    private Condition getSelectedCondition() {
        int N = getVisibleConditions();
        for (int i = 0; i < N; i++) {
            ConditionTag tag = getConditionTagAt(i);
            if (tag != null && tag.rb.isChecked()) {
                return tag.condition;
            }
        }
        return null;
    }

    private int getSelectedZen(int defValue) {
        Object zen = this.mZenButtons.getSelectedValue();
        if (zen == null) {
            return defValue;
        }
        int defValue2 = ((Integer) zen).intValue();
        return defValue2;
    }

    public void updateWidgets() {
        boolean introduction;
        int i;
        if (this.mTransitionHelper.isTransitioning()) {
            this.mTransitionHelper.pendingUpdateWidgets();
            return;
        }
        int zen = getSelectedZen(0);
        boolean zenImportant = zen == 1;
        boolean zenNone = zen == 2;
        if (zenImportant && !this.mPrefs.mConfirmedPriorityIntroduction) {
            introduction = true;
        } else {
            introduction = zenNone && !this.mPrefs.mConfirmedSilenceIntroduction;
        }
        this.mZenButtons.setVisibility(this.mHidden ? 8 : 0);
        this.mZenIntroduction.setVisibility(introduction ? 0 : 8);
        if (introduction) {
            TextView textView = this.mZenIntroductionMessage;
            if (zenImportant) {
                i = R.string.zen_priority_introduction;
            } else {
                i = this.mVoiceCapable ? R.string.zen_silence_introduction_voice : R.string.zen_silence_introduction;
            }
            textView.setText(i);
            this.mZenIntroductionCustomize.setVisibility(zenImportant ? 0 : 8);
        }
        String warning = computeAlarmWarningText(zenNone);
        this.mZenAlarmWarning.setVisibility(warning == null ? 8 : 0);
        this.mZenAlarmWarning.setText(warning);
    }

    private String computeAlarmWarningText(boolean zenNone) {
        String skeleton;
        if (!zenNone) {
            return null;
        }
        long now = System.currentTimeMillis();
        long nextAlarm = this.mController.getNextAlarm();
        if (nextAlarm < now) {
            return null;
        }
        int warningRes = 0;
        if (this.mSessionExitCondition == null || isForever(this.mSessionExitCondition)) {
            warningRes = R.string.zen_alarm_warning_indef;
        } else {
            long time = ZenModeConfig.tryParseCountdownConditionId(this.mSessionExitCondition.id);
            if (time > now && nextAlarm < time) {
                warningRes = R.string.zen_alarm_warning;
            }
        }
        if (warningRes == 0) {
            return null;
        }
        boolean soon = nextAlarm - now < 86400000;
        boolean is24 = DateFormat.is24HourFormat(this.mContext, ActivityManager.getCurrentUser());
        if (soon) {
            skeleton = is24 ? "Hm" : "hma";
        } else {
            skeleton = is24 ? "EEEHm" : "EEEhma";
        }
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        CharSequence formattedTime = DateFormat.format(pattern, nextAlarm);
        int templateRes = soon ? R.string.alarm_template : R.string.alarm_template_far;
        String template = getResources().getString(templateRes, formattedTime);
        return getResources().getString(warningRes, template);
    }

    private static Condition parseExistingTimeCondition(Context context, Condition condition) {
        if (condition == null) {
            return null;
        }
        long time = ZenModeConfig.tryParseCountdownConditionId(condition.id);
        if (time == 0) {
            return null;
        }
        long now = System.currentTimeMillis();
        long span = time - now;
        if (span <= 0 || span > MAX_BUCKET_MINUTES * 60000) {
            return null;
        }
        return ZenModeConfig.toTimeCondition(context, time, Math.round(span / 60000.0f), ActivityManager.getCurrentUser(), false);
    }

    private void handleUpdateConditions() {
        if (this.mTransitionHelper.isTransitioning()) {
            return;
        }
        int conditionCount = this.mConditions == null ? 0 : this.mConditions.length;
        if (DEBUG) {
            Log.d(this.mTag, "handleUpdateConditions conditionCount=" + conditionCount);
        }
        bind(forever(), this.mZenRadioGroupContent.getChildAt(0), 0);
        if (this.mCountdownConditionSupported && this.mTimeCondition != null) {
            bind(this.mTimeCondition, this.mZenRadioGroupContent.getChildAt(1), 1);
        }
        if (this.mCountdownConditionSupported) {
            Condition nextAlarmCondition = getTimeUntilNextAlarmCondition();
            if (nextAlarmCondition != null) {
                this.mZenRadioGroup.getChildAt(2).setVisibility(0);
                this.mZenRadioGroupContent.getChildAt(2).setVisibility(0);
                bind(nextAlarmCondition, this.mZenRadioGroupContent.getChildAt(2), 2);
            } else {
                this.mZenRadioGroup.getChildAt(2).setVisibility(8);
                this.mZenRadioGroupContent.getChildAt(2).setVisibility(8);
            }
        }
        if (this.mExpanded && isShown()) {
            ensureSelection();
        }
        this.mZenConditions.setVisibility(this.mSessionZen == 0 ? 8 : 0);
    }

    private Condition forever() {
        return new Condition(this.mForeverId, foreverSummary(this.mContext), "", "", 0, 1, 0);
    }

    private static String foreverSummary(Context context) {
        return context.getString(android.R.string.lockscreen_instructions_when_pattern_disabled);
    }

    private Condition getTimeUntilNextAlarmCondition() {
        GregorianCalendar weekRange = new GregorianCalendar();
        long now = weekRange.getTimeInMillis();
        setToMidnight(weekRange);
        weekRange.add(5, 6);
        long nextAlarmMs = this.mController.getNextAlarm();
        if (nextAlarmMs > 0) {
            GregorianCalendar nextAlarm = new GregorianCalendar();
            nextAlarm.setTimeInMillis(nextAlarmMs);
            setToMidnight(nextAlarm);
            if (weekRange.compareTo((Calendar) nextAlarm) >= 0) {
                return ZenModeConfig.toNextAlarmCondition(this.mContext, now, nextAlarmMs, ActivityManager.getCurrentUser());
            }
            return null;
        }
        return null;
    }

    private void setToMidnight(Calendar calendar) {
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
    }

    private ConditionTag getConditionTagAt(int index) {
        return (ConditionTag) this.mZenRadioGroupContent.getChildAt(index).getTag();
    }

    private int getVisibleConditions() {
        int rt = 0;
        int N = this.mZenRadioGroupContent.getChildCount();
        for (int i = 0; i < N; i++) {
            rt += this.mZenRadioGroupContent.getChildAt(i).getVisibility() == 0 ? 1 : 0;
        }
        return rt;
    }

    private void hideAllConditions() {
        int N = this.mZenRadioGroupContent.getChildCount();
        for (int i = 0; i < N; i++) {
            this.mZenRadioGroupContent.getChildAt(i).setVisibility(8);
        }
    }

    private void ensureSelection() {
        int visibleConditions = getVisibleConditions();
        if (visibleConditions == 0) {
            return;
        }
        for (int i = 0; i < visibleConditions; i++) {
            ConditionTag tag = getConditionTagAt(i);
            if (tag != null && tag.rb.isChecked()) {
                if (DEBUG) {
                    Log.d(this.mTag, "Not selecting a default, checked=" + tag.condition);
                    return;
                }
                return;
            }
        }
        ConditionTag foreverTag = getConditionTagAt(0);
        if (foreverTag == null) {
            return;
        }
        if (DEBUG) {
            Log.d(this.mTag, "Selecting a default");
        }
        int favoriteIndex = this.mPrefs.getMinuteIndex();
        if (favoriteIndex == -1 || !this.mCountdownConditionSupported) {
            foreverTag.rb.setChecked(true);
            return;
        }
        this.mTimeCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[favoriteIndex], ActivityManager.getCurrentUser());
        this.mBucketIndex = favoriteIndex;
        bind(this.mTimeCondition, this.mZenRadioGroupContent.getChildAt(1), 1);
        getConditionTagAt(1).rb.setChecked(true);
    }

    private static boolean isCountdown(Condition c) {
        if (c != null) {
            return ZenModeConfig.isValidCountdownConditionId(c.id);
        }
        return false;
    }

    private boolean isForever(Condition c) {
        if (c != null) {
            return this.mForeverId.equals(c.id);
        }
        return false;
    }

    private void bind(Condition condition, final View row, final int rowId) {
        if (condition == null) {
            throw new IllegalArgumentException("condition must not be null");
        }
        boolean enabled = condition.state == 1;
        final ConditionTag tag = row.getTag() != null ? (ConditionTag) row.getTag() : new ConditionTag(null);
        row.setTag(tag);
        boolean first = tag.rb == null;
        if (tag.rb == null) {
            tag.rb = (RadioButton) this.mZenRadioGroup.getChildAt(rowId);
        }
        tag.condition = condition;
        final Uri conditionId = getConditionId(tag.condition);
        if (DEBUG) {
            Log.d(this.mTag, "bind i=" + this.mZenRadioGroupContent.indexOfChild(row) + " first=" + first + " condition=" + conditionId);
        }
        tag.rb.setEnabled(enabled);
        tag.rb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!ZenModePanel.this.mExpanded || !isChecked) {
                    return;
                }
                tag.rb.setChecked(true);
                if (ZenModePanel.DEBUG) {
                    Log.d(ZenModePanel.this.mTag, "onCheckedChanged " + conditionId);
                }
                MetricsLogger.action(ZenModePanel.this.mContext, 164);
                ZenModePanel.this.select(tag.condition);
                ZenModePanel.this.announceConditionSelection(tag);
            }
        });
        if (tag.lines == null) {
            tag.lines = row.findViewById(android.R.id.content);
        }
        if (tag.line1 == null) {
            tag.line1 = (TextView) row.findViewById(android.R.id.text1);
            this.mSpTexts.add(tag.line1);
        }
        if (tag.line2 == null) {
            tag.line2 = (TextView) row.findViewById(android.R.id.text2);
            this.mSpTexts.add(tag.line2);
        }
        String line1 = !TextUtils.isEmpty(condition.line1) ? condition.line1 : condition.summary;
        String line2 = condition.line2;
        tag.line1.setText(line1);
        if (TextUtils.isEmpty(line2)) {
            tag.line2.setVisibility(8);
        } else {
            tag.line2.setVisibility(0);
            tag.line2.setText(line2);
        }
        tag.lines.setEnabled(enabled);
        tag.lines.setAlpha(enabled ? 1.0f : 0.4f);
        ImageView button1 = (ImageView) row.findViewById(android.R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZenModePanel.this.onClickTimeButton(row, tag, false, rowId);
            }
        });
        ImageView button2 = (ImageView) row.findViewById(android.R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZenModePanel.this.onClickTimeButton(row, tag, true, rowId);
            }
        });
        tag.lines.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tag.rb.setChecked(true);
            }
        });
        long time = ZenModeConfig.tryParseCountdownConditionId(conditionId);
        if (rowId != 2 && time > 0) {
            button1.setVisibility(0);
            button2.setVisibility(0);
            if (this.mBucketIndex > -1) {
                button1.setEnabled(this.mBucketIndex > 0);
                button2.setEnabled(this.mBucketIndex < MINUTE_BUCKETS.length + (-1));
            } else {
                long span = time - System.currentTimeMillis();
                button1.setEnabled(span > ((long) (MIN_BUCKET_MINUTES * 60000)));
                Condition maxCondition = ZenModeConfig.toTimeCondition(this.mContext, MAX_BUCKET_MINUTES, ActivityManager.getCurrentUser());
                button2.setEnabled(!Objects.equals(condition.summary, maxCondition.summary));
            }
            button1.setAlpha(button1.isEnabled() ? 1.0f : 0.5f);
            button2.setAlpha(button2.isEnabled() ? 1.0f : 0.5f);
        } else {
            button1.setVisibility(8);
            button2.setVisibility(8);
        }
        if (first) {
            Interaction.register(tag.rb, this.mInteractionCallback);
            Interaction.register(tag.lines, this.mInteractionCallback);
            Interaction.register(button1, this.mInteractionCallback);
            Interaction.register(button2, this.mInteractionCallback);
        }
        row.setVisibility(0);
    }

    public void announceConditionSelection(ConditionTag tag) {
        String modeText;
        int zen = getSelectedZen(0);
        switch (zen) {
            case 1:
                modeText = this.mContext.getString(R.string.interruption_level_priority);
                break;
            case 2:
                modeText = this.mContext.getString(R.string.interruption_level_none);
                break;
            case 3:
                modeText = this.mContext.getString(R.string.interruption_level_alarms);
                break;
            default:
                return;
        }
        announceForAccessibility(this.mContext.getString(R.string.zen_mode_and_condition, modeText, tag.line1.getText()));
    }

    public void onClickTimeButton(View row, ConditionTag tag, boolean up, int rowId) {
        MetricsLogger.action(this.mContext, 163, up);
        Condition newCondition = null;
        int N = MINUTE_BUCKETS.length;
        if (this.mBucketIndex == -1) {
            Uri conditionId = getConditionId(tag.condition);
            long time = ZenModeConfig.tryParseCountdownConditionId(conditionId);
            long now = System.currentTimeMillis();
            for (int i = 0; i < N; i++) {
                int j = up ? i : (N - 1) - i;
                int bucketMinutes = MINUTE_BUCKETS[j];
                long bucketTime = now + ((long) (60000 * bucketMinutes));
                if ((up && bucketTime > time) || (!up && bucketTime < time)) {
                    this.mBucketIndex = j;
                    newCondition = ZenModeConfig.toTimeCondition(this.mContext, bucketTime, bucketMinutes, ActivityManager.getCurrentUser(), false);
                    break;
                }
            }
            if (newCondition == null) {
                this.mBucketIndex = DEFAULT_BUCKET_INDEX;
                newCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
            }
        } else {
            this.mBucketIndex = Math.max(0, Math.min(N - 1, (up ? 1 : -1) + this.mBucketIndex));
            newCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
        }
        this.mTimeCondition = newCondition;
        bind(this.mTimeCondition, row, rowId);
        tag.rb.setChecked(true);
        select(this.mTimeCondition);
        announceConditionSelection(tag);
    }

    public void select(Condition condition) {
        if (DEBUG) {
            Log.d(this.mTag, "select " + condition);
        }
        if (this.mSessionZen == -1 || this.mSessionZen == 0) {
            if (DEBUG) {
                Log.d(this.mTag, "Ignoring condition selection outside of manual zen");
                return;
            }
            return;
        }
        final Uri realConditionId = getRealConditionId(condition);
        if (this.mController != null) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    ZenModePanel.this.mController.setZen(ZenModePanel.this.mSessionZen, realConditionId, "ZenModePanel.selectCondition");
                }
            });
        }
        setExitCondition(condition);
        if (realConditionId == null) {
            this.mPrefs.setMinuteIndex(-1);
        } else if (isCountdown(condition) && this.mBucketIndex != -1) {
            this.mPrefs.setMinuteIndex(this.mBucketIndex);
        }
        setSessionExitCondition(copy(condition));
    }

    public void fireInteraction() {
        if (this.mCallback == null) {
            return;
        }
        this.mCallback.onInteraction();
    }

    private void fireExpanded() {
        if (this.mCallback == null) {
            return;
        }
        this.mCallback.onExpanded(this.mExpanded);
    }

    private final class H extends Handler {
        H(ZenModePanel this$0, H h) {
            this();
        }

        private H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2:
                    ZenModePanel.this.handleUpdateManualRule((ZenModeConfig.ZenRule) msg.obj);
                    break;
                case 3:
                    ZenModePanel.this.updateWidgets();
                    break;
            }
        }
    }

    private static class ConditionTag {
        Condition condition;
        TextView line1;
        TextView line2;
        View lines;
        RadioButton rb;

        ConditionTag(ConditionTag conditionTag) {
            this();
        }

        private ConditionTag() {
        }
    }

    private final class ZenPrefs implements SharedPreferences.OnSharedPreferenceChangeListener {
        private boolean mConfirmedPriorityIntroduction;
        private boolean mConfirmedSilenceIntroduction;
        private int mMinuteIndex;
        private final int mNoneDangerousThreshold;
        private int mNoneSelected;

        ZenPrefs(ZenModePanel this$0, ZenPrefs zenPrefs) {
            this();
        }

        private ZenPrefs() {
            this.mNoneDangerousThreshold = ZenModePanel.this.mContext.getResources().getInteger(R.integer.zen_mode_alarm_warning_threshold);
            Prefs.registerListener(ZenModePanel.this.mContext, this);
            updateMinuteIndex();
            updateNoneSelected();
            updateConfirmedPriorityIntroduction();
            updateConfirmedSilenceIntroduction();
        }

        public void trackNoneSelected() {
            this.mNoneSelected = clampNoneSelected(this.mNoneSelected + 1);
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "Setting none selected: " + this.mNoneSelected + " threshold=" + this.mNoneDangerousThreshold);
            }
            Prefs.putInt(ZenModePanel.this.mContext, "DndNoneSelected", this.mNoneSelected);
        }

        public int getMinuteIndex() {
            return this.mMinuteIndex;
        }

        public void setMinuteIndex(int minuteIndex) {
            int minuteIndex2 = clampIndex(minuteIndex);
            if (minuteIndex2 == this.mMinuteIndex) {
                return;
            }
            this.mMinuteIndex = clampIndex(minuteIndex2);
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "Setting favorite minute index: " + this.mMinuteIndex);
            }
            Prefs.putInt(ZenModePanel.this.mContext, "DndCountdownMinuteIndex", this.mMinuteIndex);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            updateMinuteIndex();
            updateNoneSelected();
            updateConfirmedPriorityIntroduction();
            updateConfirmedSilenceIntroduction();
        }

        private void updateMinuteIndex() {
            this.mMinuteIndex = clampIndex(Prefs.getInt(ZenModePanel.this.mContext, "DndCountdownMinuteIndex", ZenModePanel.DEFAULT_BUCKET_INDEX));
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "Favorite minute index: " + this.mMinuteIndex);
            }
        }

        private int clampIndex(int index) {
            return MathUtils.constrain(index, -1, ZenModePanel.MINUTE_BUCKETS.length - 1);
        }

        private void updateNoneSelected() {
            this.mNoneSelected = clampNoneSelected(Prefs.getInt(ZenModePanel.this.mContext, "DndNoneSelected", 0));
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "None selected: " + this.mNoneSelected);
            }
        }

        private int clampNoneSelected(int noneSelected) {
            return MathUtils.constrain(noneSelected, 0, Integer.MAX_VALUE);
        }

        private void updateConfirmedPriorityIntroduction() {
            boolean confirmed = Prefs.getBoolean(ZenModePanel.this.mContext, "DndConfirmedPriorityIntroduction", false);
            if (confirmed == this.mConfirmedPriorityIntroduction) {
                return;
            }
            this.mConfirmedPriorityIntroduction = confirmed;
            if (!ZenModePanel.DEBUG) {
                return;
            }
            Log.d(ZenModePanel.this.mTag, "Confirmed priority introduction: " + this.mConfirmedPriorityIntroduction);
        }

        private void updateConfirmedSilenceIntroduction() {
            boolean confirmed = Prefs.getBoolean(ZenModePanel.this.mContext, "DndConfirmedSilenceIntroduction", false);
            if (confirmed == this.mConfirmedSilenceIntroduction) {
                return;
            }
            this.mConfirmedSilenceIntroduction = confirmed;
            if (!ZenModePanel.DEBUG) {
                return;
            }
            Log.d(ZenModePanel.this.mTag, "Confirmed silence introduction: " + this.mConfirmedSilenceIntroduction);
        }
    }

    private final class TransitionHelper implements LayoutTransition.TransitionListener, Runnable {
        private boolean mPendingUpdateWidgets;
        private boolean mTransitioning;
        private final ArraySet<View> mTransitioningViews;

        TransitionHelper(ZenModePanel this$0, TransitionHelper transitionHelper) {
            this();
        }

        private TransitionHelper() {
            this.mTransitioningViews = new ArraySet<>();
        }

        public void clear() {
            this.mTransitioningViews.clear();
            this.mPendingUpdateWidgets = false;
        }

        public void pendingUpdateWidgets() {
            this.mPendingUpdateWidgets = true;
        }

        public boolean isTransitioning() {
            return !this.mTransitioningViews.isEmpty();
        }

        @Override
        public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
            this.mTransitioningViews.add(view);
            updateTransitioning();
        }

        @Override
        public void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
            this.mTransitioningViews.remove(view);
            updateTransitioning();
        }

        @Override
        public void run() {
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "TransitionHelper run mPendingUpdateWidgets=" + this.mPendingUpdateWidgets);
            }
            if (this.mPendingUpdateWidgets) {
                ZenModePanel.this.updateWidgets();
            }
            this.mPendingUpdateWidgets = false;
        }

        private void updateTransitioning() {
            boolean transitioning = isTransitioning();
            if (this.mTransitioning == transitioning) {
                return;
            }
            this.mTransitioning = transitioning;
            if (ZenModePanel.DEBUG) {
                Log.d(ZenModePanel.this.mTag, "TransitionHelper mTransitioning=" + this.mTransitioning);
            }
            if (this.mTransitioning) {
                return;
            }
            if (this.mPendingUpdateWidgets) {
                ZenModePanel.this.mHandler.post(this);
            } else {
                this.mPendingUpdateWidgets = false;
            }
        }
    }
}
